package com.minhtran.phimne;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.webkit.WebView;
import android.view.View;
import android.webkit.WebSettings;
import android.content.pm.PackageInfo;

import androidx.webkit.WebViewCompat;

public class WebViewFactory {

    protected static final String GOOGLE_WEBVIEW_PACKAGE = "com.google.android.webview";

    public static WebView createWebView(Context context) {
        // Check and log current WebView provider
        checkWebViewProvider(context);

        // Create WebView with optimized settings for Google WebView
        WebView webView = new WebView(context);

        // Apply Google WebView specific optimizations
        optimizeForGoogleWebView(webView);

        return webView;
    }

    private static void checkWebViewProvider(Context context) {
        try {
            android.content.pm.PackageInfo webViewPackage = WebViewCompat.getCurrentWebViewPackage(context);
            if (webViewPackage != null) {
                Log.i("WebViewFactory", "Current WebView provider: " + webViewPackage.packageName);
                Log.i("WebViewFactory", "WebView version: " + webViewPackage.versionName);

                if (webViewPackage.packageName.equals(GOOGLE_WEBVIEW_PACKAGE)) {
                    Log.i("WebViewFactory", "✓ Google WebView implementation is active.");
                } else {
                    Log.w("WebViewFactory", "⚠ Not using Google WebView. Currently active: " + webViewPackage.packageName);

                    // Check if Google WebView is available but not active
                    if (isGoogleWebViewAvailable(context)) {
                        Log.i("WebViewFactory", "Google WebView is installed but not active. To use it, please go to Android Developer Options > WebView implementation and select 'Google WebView'.");
                    } else {
                        Log.w("WebViewFactory", "Google WebView is not installed or enabled on this device.");
                    }
                }
            } else {
                Log.e("WebViewFactory", "Could not determine current WebView provider.");
            }
        } catch (Exception e) {
            Log.e("WebViewFactory", "Error checking WebView provider", e);
        }
    }

    private static boolean isGoogleWebViewAvailable(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(GOOGLE_WEBVIEW_PACKAGE, 0);
            return appInfo.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static void optimizeForGoogleWebView(WebView webView) {
        try {
            // Global: debugging toggle
            WebView.setWebContentsDebuggingEnabled(false);

            // Confirm we’re on Google’s WebView
            PackageInfo webViewPackage;

            webViewPackage = WebViewCompat.getCurrentWebViewPackage(webView.getContext());
            if (webViewPackage != null &&
                    GOOGLE_WEBVIEW_PACKAGE.equals(webViewPackage.packageName)) {

                Log.d("WebViewFactory", "Applying Google WebView optimizations");

                // === Use the passed‐in instance! ===
                WebSettings settings = webView.getSettings();
                // Example instance‑level tweaks:
                settings.setCacheMode(WebSettings.LOAD_DEFAULT);
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                settings.setSafeBrowsingEnabled(true);

                // Remove the glow effect on overscroll
                webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

            }
        } catch (Exception e) {
            Log.e("WebViewFactory", "Error applying WebView optimizations", e);
        }
    }
}
