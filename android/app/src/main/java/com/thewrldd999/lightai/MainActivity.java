
package com.thewrldd999.lightai;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String result;

        try {
            System.loadLibrary("ggml-base");
            System.loadLibrary("ggml-cpu");
            System.loadLibrary("ggml");
            System.loadLibrary("llama");

            result = "SUCCESS!\n\n"
                    + "All four native libraries loaded.\n\n"
                    + "Your 32-bit ARM engine is recognised by Android.";

        } catch (UnsatisfiedLinkError e) {

            result = "ENGINE LOAD FAILED\n\n"
                    + e.getMessage();
        }

        new AlertDialog.Builder(this)
                .setTitle("Light AI — Engine Test")
                .setMessage(result)
                .setPositiveButton("Continue", (dialog, which) -> {
                    openLightAI();
                })
                .setCancelable(false)
                .show();
    }

    private void openLightAI() {

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        webView.loadUrl(
            "https://thewrldd999.github.io/Light-AI/"
        );

        setContentView(webView);
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
            }
