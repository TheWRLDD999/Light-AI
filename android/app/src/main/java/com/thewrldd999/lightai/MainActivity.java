
package com.thewrldd999.lightai;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private WebView webView;

    private ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private volatile boolean modelReady = false;
    private volatile boolean downloading = false;
    private boolean startupCheckStarted = false;

    private static final String MODEL_URL =
        "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf";

    private static final String MODEL_NAME =
        "lightai-model.gguf";

    static {
        System.loadLibrary("ggml-base");
        System.loadLibrary("ggml-cpu");
        System.loadLibrary("ggml");
        System.loadLibrary("llama");
        System.loadLibrary("lightai");
    }

    public native String getEngineStatus();
    public native String loadModel(String path);
    public native String generateResponse(String message);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openLightAI();
    }

    private void openLightAI() {

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(
            new LightAIBridge(), "LightAI"
        );

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(
                    WebView view, String url) {

                super.onPageFinished(view, url);

                if (!startupCheckStarted) {
                    startupCheckStarted = true;
                    loadSavedModelAutomatically();
                }
            }
        });

        setContentView(webView);

        webView.loadUrl(
            "https://thewrldd999.github.io/Light-AI/"
        );
    }

    private void loadSavedModelAutomatically() {

        File modelFile = new File(
            getFilesDir(), MODEL_NAME
        );

        if (!modelFile.exists()) {
            return;
        }

        if (modelReady || downloading) {
            return;
        }

        downloading = true;

        sendStatus("Loading your saved AI model...");

        executor.execute(() -> {

            try {

                String result =
                    loadModel(modelFile.getAbsolutePath());

                modelReady = result.contains(
                    "loaded successfully"
                );

                sendStatus(result);

            } catch (Exception e) {

                modelReady = false;

                sendStatus(
                    "Automatic loading failed: " +
                    e.getMessage()
                );

            } finally {
                downloading = false;
            }
        });
    }

    private void sendStatus(String message) {

        if (webView == null) return;

        webView.post(() -> {

            String value = JSONObject.quote(message);

            webView.evaluateJavascript(
                "if(window.onLightAIStatus){" +
                "window.onLightAIStatus(" + value + ");}",
                null
            );
        });
    }

    private void sendResponse(String message) {

        if (webView == null) return;

        webView.post(() -> {

            String value = JSONObject.quote(message);

            webView.evaluateJavascript(
                "if(window.onLightAIResponse){" +
                "window.onLightAIResponse(" + value + ");}",
                null
            );
        });
    }

    public class LightAIBridge {

        @JavascriptInterface
        public String getModelStatus() {

            if (modelReady) return "READY";

            if (downloading) return "DOWNLOADING";

            return "NOT_READY";
        }

        @JavascriptInterface
        public void downloadModel() {

            if (modelReady || downloading) return;

            downloading = true;

            executor.execute(() -> {

                File modelFile = new File(
                    getFilesDir(), MODEL_NAME
                );

                File temporaryFile = new File(
                    getFilesDir(), MODEL_NAME + ".part"
                );

                HttpURLConnection connection = null;

                try {

                    if (modelFile.exists()) {

                        sendStatus(
                            "Loading saved AI model..."
                        );

                        String result =
                            loadModel(modelFile.getAbsolutePath());

                        modelReady = result.contains(
                            "loaded successfully"
                        );

                        sendStatus(result);

                        return;
                    }

                    if (getFilesDir().getUsableSpace()
                            < 650L * 1024L * 1024L) {

                        sendStatus(
                            "Not enough storage space. " +
                            "Please free up some space."
                        );

                        return;
                    }

                    sendStatus(
                        "Connecting to model download..."
                    );

                    URL url = new URL(MODEL_URL);

                    connection =
                        (HttpURLConnection) url.openConnection();

                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(120000);
                    connection.setInstanceFollowRedirects(true);

                    connection.connect();

                    if (connection.getResponseCode() != 200) {

                        throw new Exception(
                            "Download failed: HTTP " +
                            connection.getResponseCode()
                        );
                    }

                    int total = connection.getContentLength();

                    try (
                        InputStream input =
                            connection.getInputStream();

                        FileOutputStream output =
                            new FileOutputStream(temporaryFile)
                    ) {

                        byte[] buffer = new byte[32768];

                        long downloaded = 0;

                        int count;
                        int lastPercent = -1;

                        while ((count = input.read(buffer)) != -1) {

                            output.write(buffer, 0, count);

                            downloaded += count;

                            if (total > 0) {

                                int percent = (int)
                                    (downloaded * 100 / total);

                                if (percent != lastPercent) {

                                    lastPercent = percent;

                                    sendStatus(
                                        "Downloading AI model: " +
                                        percent + "%"
                                    );
                                }
                            }
                        }
                    }

                    if (!temporaryFile.renameTo(modelFile)) {

                        throw new Exception(
                            "Could not save downloaded model."
                        );
                    }

                    sendStatus(
                        "Download complete. Loading AI..."
                    );

                    String result =
                        loadModel(modelFile.getAbsolutePath());

                    modelReady = result.contains(
                        "loaded successfully"
                    );

                    sendStatus(result);

                } catch (Exception e) {

                    temporaryFile.delete();

                    modelReady = false;

                    sendStatus(
                        "Model setup failed: " +
                        e.getMessage()
                    );

                } finally {

                    if (connection != null) {
                        connection.disconnect();
                    }

                    downloading = false;
                }
            });
        }

        @JavascriptInterface
        public void sendMessage(String message) {

            if (!modelReady) {

                sendResponse(
                    "Please download and load the AI model first."
                );

                return;
            }

            executor.execute(() -> {

                try {

                    String result =
                        generateResponse(message);

                    sendResponse(result);

                } catch (Exception e) {

                    sendResponse(
                        "Sorry, something went wrong: " +
                        e.getMessage()
                    );
                }
            });
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (executor != null) {
            executor.shutdownNow();
        }

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
        }
                                        
