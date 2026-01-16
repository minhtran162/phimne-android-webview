package com.minhtran.phimne;

import static android.provider.Settings.Secure.*;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.app.UiModeManager;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout container;

    // Loading Screen Components
    private RelativeLayout loadingScreen;
    private TextView loadingText;
    private ProgressBar progressBar;

    private boolean pageFinished = false;
    private boolean progressComplete = false;

    private static final String staticServerUrl = BuildConfig.STATIC_SERVER;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        initializeViews();

        // Setup orientation and window
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Check if URL was passed via intent
        String intentUrl = getUrlFromIntent();

        // Connect to static server
        connectToUrl(Objects.requireNonNullElse(intentUrl, staticServerUrl));

    }

    private void initializeViews() {
        container = findViewById(R.id.Container);

        // Loading Screen
        loadingScreen = findViewById(R.id.loadingScreen);
        ImageView loadingLogo = findViewById(R.id.loadingLogo);
        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);

        if (loadingLogo != null) {
            int orientation = getResources().getConfiguration().orientation;
            float widthDp = orientation == Configuration.ORIENTATION_LANDSCAPE ? 250f : 150f;
            int widthPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    widthDp,
                    getResources().getDisplayMetrics()
            );
            android.view.ViewGroup.LayoutParams params = loadingLogo.getLayoutParams();
            if (params != null) {
                params.width = widthPx;
                params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                loadingLogo.setLayoutParams(params);
            }
        }
    }

    private void connectToUrl(String url) {
        // Show loading
        showLoadingScreen();
        updateLoadingText(getString(R.string.initializing));

        // Setup WebView
        container.post(() -> setupWebView(url));
    }

    private boolean isTvDevice() {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        boolean uiModeTv = uiModeManager != null
                && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
        PackageManager pm = getPackageManager();
        boolean hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK);
        return uiModeTv || hasLeanback;
    }

    private String getUrlFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            String intentUrl = intent.getStringExtra("url");
            if (intentUrl != null && !intentUrl.isEmpty()) {
                return intentUrl;
            }

            if (intent.getData() != null) {
                String deepLinkUrl = intent.getData().toString();
                Log.i("MainActivity", "Using URL from deep link: " + deepLinkUrl);
                return deepLinkUrl;
            }
        }
        return null;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(String url) {
        // Destroy existing WebView
        destroyWebView();

        // Create WebView using the WebViewFactory
        webView = WebViewFactory.createWebView(this);

        // Add WebView to the container
        if (container != null) {
            container.addView(webView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            // Ensure loading screen stays on top
            loadingScreen.bringToFront();
        } else {
            Log.e("MainActivity", "Container not found in layout.");
            return;
        }

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setTextZoom(100);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setGeolocationEnabled(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setSupportMultipleWindows(false);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        if (isTvDevice()) {
            String tvUserAgent =
                    "Mozilla/5.0 (Linux; Android TV " +
                            android.os.Build.VERSION.RELEASE +
                            "; " +
                            android.os.Build.MODEL +
                            ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36 PhimNeAndroidTV/1.0";
            webSettings.setUserAgentString(tvUserAgent);
        }

        // Set WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectJavaScript(view);
                showLoadingScreen();
                updateLoadingText(getString(R.string.loading));
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String requestUrl = request.getUrl().toString();
                if (requestUrl.startsWith("https://")) {
                    return false; // Let WebView handle it
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Inject JavaScript polyfills and security measures
                injectJavaScript(view);

                pageFinished = true;
                checkLoadingComplete();
                hideSystemBars();
                hideLoadingScreen();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    hideLoadingScreen();
                    showConnectionError();
                }
            }
        });

        // Set WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                }
                if (newProgress < 100) {
                    updateLoadingText(getString(R.string.loading) + newProgress + "%");
                } else {
                    progressComplete = true;
                    updateLoadingText(getString(R.string.completing));
                    checkLoadingComplete();
                }
            }
        });

        updateLoadingText(getString(R.string.initializing));
        webView.loadUrl(url);
        hideSystemBars();
    }

    private void injectJavaScript(WebView view) {
        // Modern crypto.randomUUID() polyfill
        String jsPolyfill =
                "(function(){" +
                        "  var g = (typeof self !== 'undefined') ? self : window;" +
                        "  if (!g.crypto) {" +
                        "    g.crypto = {};" +
                        "  }" +
                        "  if (typeof g.crypto.getRandomValues !== 'function') {" +
                        "    g.crypto.getRandomValues = function(arr) {" +
                        "      for (var i = 0; i < arr.length; i++) {" +
                        "        arr[i] = Math.floor(Math.random() * 256);" +
                        "      }" +
                        "      return arr;" +
                        "    };" +
                        "  }" +
                        "  if (typeof g.crypto.randomUUID !== 'function') {" +
                        "    g.crypto.randomUUID = function() {" +
                        "      function s4() {" +
                        "        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);" +
                        "      }" +
                        "      return s4() + s4() + '-' + s4() + '-4' + s4().substr(1) + '-' + s4() + '-' + s4() + s4() + s4();" +
                        "    };" +
                        "  }" +
                        "})();";

        view.evaluateJavascript(jsPolyfill, null);

        // Get device info
        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(getContentResolver(), ANDROID_ID);
        String deviceName = android.os.Build.MODEL;
        String appName = isTvDevice() ? getString(R.string.app_tv) : getString(R.string.app_mobile);
        String appVersion = BuildConfig.VERSION_NAME;

        String injectClientInfo =
                "(function() {" +
                "  var deviceInfo = {" +
                "    deviceId: '" + deviceId + "'," +
                "    deviceName: '" + deviceName + "'," +
                "    appName: '" + appName + "'," +
                "    appVersion: '" + appVersion + "'" +
                "  };" +
                "  if (!window.NativeInterface) {" +
                "    window.NativeInterface = {" +
                "      getDeviceInformation: function() {" +
                "        return JSON.stringify(deviceInfo);" +
                "      }," +
                "      enableFullscreen: function() {}," +
                "      disableFullscreen: function() {}," +
                "      openUrl: function(url) {}," +
                "      updateMediaSession: function(mediaInfo) {}," +
                "      hideMediaSession: function() {}," +
                "      updateVolumeLevel: function(value) {}," +
                "      downloadFiles: function(downloadInfoJson) {}," +
                "      openClientSettings: function() {}," +
                "      openServerSelection: function() {}," +
                "      execCast: function(action, argsJson) {}," +
                "      exitApp: function() {}" +
                "    };" +
                "  }" +
                "  function initNativeShell() {" +
                "  const features = ['filedownload','displaylanguage','subtitleappearancesettings','subtitleburnsettings','exit','htmlaudioautoplay','htmlvideoautoplay','externallinks','clientsettings','multiserver','physicalvolumecontrol','remotecontrol','castmenuhashchange'];" +
                "  let deviceId;" +
                "  let deviceName;" +
                "  let appName;" +
                "  let appVersion;" +
                "  window.NativeShell = {" +
                "    enableFullscreen() {" +
                "    }," +
                "    disableFullscreen() {" +
                "    }," +
                "    openUrl(url, target) {" +
                "    }," +
                "    updateMediaSession(mediaInfo) {" +
                "    }," +
                "    hideMediaSession() {" +
                "    }," +
                "    updateVolumeLevel(value) {" +
                "    }," +
                "    };" +
                "  function getDeviceProfile(profileBuilder, item) {" +
                "      const profile = profileBuilder({" +
                "          enableMkvProgressive: false" +
                "      });" +
                "      profile.CodecProfiles = profile.CodecProfiles.filter(function (i) {" +
                "          return i.Type === 'Audio';" +
                "      });" +
                "      profile.CodecProfiles.push({" +
                "          Type: 'Video'," +
                "          Container: 'avi'," +
                "          Conditions: [" +
                "              {" +
                "                  Condition: 'NotEquals'," +
                "                  Property: 'VideoCodecTag'," +
                "                  Value: 'xvid'" +
                "              }" +
                "          ]" +
                "      });" +
                "      profile.CodecProfiles.push({" +
                "          Type: 'Video'," +
                "          Codec: 'h264'," +
                "          Conditions: [" +
                "              {" +
                "                  Condition: 'EqualsAny'," +
                "                  Property: 'VideoProfile'," +
                "                  Value: 'high|main|baseline|constrained baseline'" +
                "              }," +
                "              {" +
                "                  Condition: 'LessThanEqual'," +
                "                  Property: 'VideoLevel'," +
                "                  Value: '41'" +
                "              }]" +
                "      });" +
                "      profile.TranscodingProfiles.reduce(function (profiles, p) {" +
                "          if (p.Type === 'Video' && p.CopyTimestamps === true && p.VideoCodec === 'h264') {" +
                "              p.AudioCodec += ',ac3';" +
                "              profiles.push(p);" +
                "          }" +
                "          return profiles;" +
                "      }, []);" +
                "      return profile;" +
                "  }" +
                "  window.NativeShell.AppHost = {" +
                "    init() {" +
                "      try {" +
                "        const result = JSON.parse(window.NativeInterface.getDeviceInformation());" +
                "        deviceId = result.deviceId;" +
                "        deviceName = result.deviceName;" +
                "        appName = result.appName;" +
                "        appVersion = result.appVersion;" +
                "        return Promise.resolve({" +
                "          deviceId," +
                "          deviceName," +
                "          appName," +
                "          appVersion" +
                "        });" +
                "      } catch (e) {" +
                "        return Promise.reject(e);" +
                "      }" +
                "    }," +
                "    getDefaultLayout() {" +
                "       return " + (isTvDevice() ? "'tv'" : "'mobile'") + ";" + 
                "    }," +
                "    supports(command) {" +
                "      return features.includes(command.toLowerCase());" +
                "    }," +
                "    getDeviceProfile," +
                "    getSyncProfile: getDeviceProfile," +
                "    deviceName() {" +
                "      return deviceName;" +
                "    }," +
                "    deviceId() {" +
                "      return deviceId;" +
                "    }," +
                "    appName() {" +
                "      return appName;" +
                "    }," +
                "    appVersion() {" +
                "      return appVersion;" +
                "    }," +
                "    exit() {" +
                "      window.NativeInterface.exitApp();" +
                "    }" +
                "  };" +
                "  }" +
                "  if (document.readyState === 'complete') {" +
                "    initNativeShell();" +
                "  } else {" +
                "    window.addEventListener('load', initNativeShell, { once: true });" +
                "  }" +
                "})();";
        view.evaluateJavascript(injectClientInfo, null);
    }

    private void showConnectionError() {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error_connection_title))
                .setMessage(getString(R.string.error_connection_message))
                .setPositiveButton(getString(R.string.btn_retry), (dialog, which) -> {
                    if (webView != null) {
                        webView.reload();
                        showLoadingScreen();
                    } else {
                        connectToUrl(staticServerUrl);
                    }
                })
                .setNegativeButton(getString(R.string.btn_exit), (dialog, which) -> finish())
                .setCancelable(false)
                .show());
    }

    private void showLoadingScreen() {
        runOnUiThread(() -> {
            if (loadingScreen != null) {
                loadingScreen.setVisibility(View.VISIBLE);
                pageFinished = false;
                progressComplete = false;
            }
            if (progressBar != null) {
                progressBar.setProgress(0);
            }
        });
    }

    private void hideLoadingScreen() {
        runOnUiThread(() -> {
            if (loadingScreen != null) {
                loadingScreen.setVisibility(View.GONE);
            }
        });
    }

    private void updateLoadingText(String text) {
        runOnUiThread(() -> {
            if (loadingText != null) {
                loadingText.setText(text);
            }
        });
    }

    private void checkLoadingComplete() {
        if (pageFinished && progressComplete) {
            hideSystemBars();
            hideLoadingScreen();
        }
    }

    /**
     * Handle new intents (for URL changes while app is running)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String newUrl = getUrlFromIntent();
        if (newUrl != null && webView != null) {
            connectToUrl(newUrl);
        }
    }

    /**
     * Properly cleans up and destroys the WebView instance.
     */
    private void destroyWebView() {
        if (webView != null) {
            // Remove the WebView from its parent view
            if (container != null) {
                container.removeView(webView);
            }
            // Perform cleanup
            webView.getSettings().setJavaScriptEnabled(false);
            webView.getSettings().setDomStorageEnabled(false);
            webView.loadUrl("about:blank");
            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(null);
            webView.clearHistory();
            webView.clearCache(true);
            webView.clearFormData();
            webView.destroy();
            webView = null; // Set to null to allow garbage collection
        }
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finishAndRemoveTask();
        Process.killProcess(Process.myPid());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        destroyWebView();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)) {
            if (webView != null && webView.canGoBack()) {
                // Allow navigation within WebView
                webView.goBack();
            } else {
                // WebView is loaded but can't go back - exit app
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
            if (webView != null) {
                webView.requestFocus();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().getDecorView().requestFocus();
    }
}
