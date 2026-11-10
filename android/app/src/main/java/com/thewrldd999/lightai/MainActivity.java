
package com.thewrldd999.lightai;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Build;
import android.os.Process;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] abis = Build.SUPPORTED_ABIS;
        String architecture = Process.is64Bit()
                ? "64-bit Android"
                : "32-bit Android";

        StringBuilder details = new StringBuilder();
        details.append("Android architecture: ")
               .append(architecture)
               .append("\nSupported ABIs:\n");

        for (String abi : abis) {
            details.append("• ").append(abi).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Light AI — Device Check")
                .setMessage(details.toString())
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
