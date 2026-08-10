package eformx.app;

import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends ComponentActivity {

    private WebView webView;
    private FrameLayout fullScreenLoadingOverlay;
    private FrameLayout errorOverlay;
    private TextView loadingTitleTv;
    private TextView loadingSubtitleTv;
    private ValueCallback<Uri[]> filePathCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (filePathCallback == null) return;
                    Uri[] results = null;
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String dataString = result.getData().getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        } else if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            results = new Uri[count];
                            for (int i = 0; i < count; i++) {
                                results[i] = result.getData().getClipData().getItemAt(i).getUri();
                            }
                        }
                    }
                    filePathCallback.onReceiveValue(results);
                    filePathCallback = null;
                }
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        android.util.Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    android.util.Log.d("FCM_TOKEN", token);
                });

        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("FCM", "Successfully subscribed to topic 'all'");
                    } else {
                        android.util.Log.w("FCM", "Topic subscription failed", task.getException());
                    }
                });

        checkAndRequestAutoStartPermission();

        int themeColor = Color.WHITE;
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(themeColor);

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.WHITE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        container.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Ultra-Professional Minimal Grey Loading Overlay (Exact Match with Screenshot)
        fullScreenLoadingOverlay = new FrameLayout(this);
        fullScreenLoadingOverlay.setBackgroundColor(Color.parseColor("#EAEEF3"));

        LinearLayout centerLoadingLayout = new LinearLayout(this);
        centerLoadingLayout.setOrientation(LinearLayout.VERTICAL);
        centerLoadingLayout.setGravity(Gravity.CENTER);

        ProgressBar centerSpinner = new ProgressBar(this);
        if (centerSpinner.getIndeterminateDrawable() != null) {
            centerSpinner.getIndeterminateDrawable().setColorFilter(
                    Color.parseColor("#475569"), android.graphics.PorterDuff.Mode.SRC_IN);
        }
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                dpToPx(48), dpToPx(48));
        spinnerParams.gravity = Gravity.CENTER;
        spinnerParams.bottomMargin = dpToPx(20);
        centerLoadingLayout.addView(centerSpinner, spinnerParams);

        loadingTitleTv = new TextView(this);
        loadingTitleTv.setText("Loading Application Form...");
        loadingTitleTv.setTextSize(18);
        loadingTitleTv.setTextColor(Color.parseColor("#1E293B"));
        loadingTitleTv.setTypeface(Typeface.DEFAULT_BOLD);
        loadingTitleTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dpToPx(6);
        centerLoadingLayout.addView(loadingTitleTv, titleParams);

        loadingSubtitleTv = new TextView(this);
        loadingSubtitleTv.setText("Please wait a moment");
        loadingSubtitleTv.setTextSize(14);
        loadingSubtitleTv.setTextColor(Color.parseColor("#64748B"));
        loadingSubtitleTv.setGravity(Gravity.CENTER);
        centerLoadingLayout.addView(loadingSubtitleTv);

        FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.gravity = Gravity.CENTER;
        fullScreenLoadingOverlay.addView(centerLoadingLayout, centerParams);

        container.addView(fullScreenLoadingOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Custom Branded Offline / Error Screen Overlay (Exact Match with User Screenshot)
        errorOverlay = new FrameLayout(this);
        errorOverlay.setBackgroundColor(Color.parseColor("#F8FAFC"));

        LinearLayout mainErrorLayout = new LinearLayout(this);
        mainErrorLayout.setOrientation(LinearLayout.VERTICAL);
        mainErrorLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainErrorLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Card container
        LinearLayout errorCard = new LinearLayout(this);
        errorCard.setOrientation(LinearLayout.VERTICAL);
        errorCard.setGravity(Gravity.CENTER_HORIZONTAL);
        errorCard.setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(28));

        GradientDrawable errBg = new GradientDrawable();
        errBg.setColor(Color.WHITE);
        errBg.setCornerRadius(dpToPx(24));
        errorCard.setBackground(errBg);
        errorCard.setElevation(dpToPx(6));

        // 1. Circular Wifi Off Badge Container
        FrameLayout badgeContainer = new FrameLayout(this);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        badgeBg.setColor(Color.parseColor("#F5F3FF"));
        badgeBg.setStroke(dpToPx(1), Color.parseColor("#EDE9FE"));
        badgeContainer.setBackground(badgeBg);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dpToPx(96), dpToPx(96));
        badgeParams.gravity = Gravity.CENTER_HORIZONTAL;
        badgeParams.bottomMargin = dpToPx(24);

        View wifiOffIconView = new View(this) {
            private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                int w = getWidth();
                int h = getHeight();
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setColor(Color.parseColor("#7C3AED"));
                paint.setStrokeWidth((float) dpToPx(3));
                paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);

                float cx = w / 2.0f;
                float cy = h / 2.0f + dpToPx(4);

                android.graphics.RectF r1 = new android.graphics.RectF(cx - dpToPx(22), cy - dpToPx(22), cx + dpToPx(22), cy + dpToPx(22));
                canvas.drawArc(r1, 215, 110, false, paint);

                android.graphics.RectF r2 = new android.graphics.RectF(cx - dpToPx(14), cy - dpToPx(14), cx + dpToPx(14), cy + dpToPx(14));
                canvas.drawArc(r2, 220, 100, false, paint);

                paint.setStyle(android.graphics.Paint.Style.FILL);
                canvas.drawCircle(cx, cy + dpToPx(6), (float) dpToPx(3), paint);

                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth((float) dpToPx(3));
                canvas.drawLine(cx - dpToPx(20), cy + dpToPx(18), cx + dpToPx(20), cy - dpToPx(20), paint);
            }
        };
        badgeContainer.addView(wifiOffIconView, new FrameLayout.LayoutParams(dpToPx(96), dpToPx(96)));
        errorCard.addView(badgeContainer, badgeParams);

        // 2. Title: "No Internet Connection"
        TextView errTitleTv = new TextView(this);
        errTitleTv.setText("No Internet Connection");
        errTitleTv.setTextSize(20);
        errTitleTv.setTextColor(Color.parseColor("#1E1B4B"));
        errTitleTv.setTypeface(Typeface.DEFAULT_BOLD);
        errTitleTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams errTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errTitleParams.bottomMargin = dpToPx(10);
        errorCard.addView(errTitleTv, errTitleParams);

        // 3. Subtitle: "Please check your internet connection\nand try again."
        TextView errSubTv = new TextView(this);
        errSubTv.setText("Please check your internet connection\nand try again.");
        errSubTv.setTextSize(14);
        errSubTv.setTextColor(Color.parseColor("#64748B"));
        errSubTv.setGravity(Gravity.CENTER);
        errSubTv.setLineSpacing(dpToPx(2), 1.0f);
        LinearLayout.LayoutParams errSubParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errSubParams.bottomMargin = dpToPx(24);
        errorCard.addView(errSubTv, errSubParams);

        // 4. Primary Button: "🔄  Try Again"
        TextView retryBtn = new TextView(this);
        retryBtn.setText("🔄   Try Again");
        retryBtn.setTextSize(15);
        retryBtn.setTextColor(Color.WHITE);
        retryBtn.setTypeface(Typeface.DEFAULT_BOLD);
        retryBtn.setGravity(Gravity.CENTER);
        retryBtn.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#7C3AED"));
        btnBg.setCornerRadius(dpToPx(12));
        retryBtn.setBackground(btnBg);

        retryBtn.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                hideErrorOverlay();
                if (webView != null) {
                    webView.reload();
                }
            } else {
                Toast.makeText(MainActivity.this, "Still offline. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams retryBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        retryBtnParams.bottomMargin = dpToPx(20);
        errorCard.addView(retryBtn, retryBtnParams);

        // 5. Divider Line: "─────  OR  ─────"
        LinearLayout dividerLayout = new LinearLayout(this);
        dividerLayout.setOrientation(LinearLayout.HORIZONTAL);
        dividerLayout.setGravity(Gravity.CENTER);

        View line1 = new View(this);
        line1.setBackgroundColor(Color.parseColor("#E2E8F0"));
        LinearLayout.LayoutParams line1Params = new LinearLayout.LayoutParams(0, dpToPx(1), 1.0f);

        TextView orTv = new TextView(this);
        orTv.setText("OR");
        orTv.setTextSize(12);
        orTv.setTextColor(Color.parseColor("#94A3B8"));
        orTv.setPadding(dpToPx(12), 0, dpToPx(12), 0);

        View line2 = new View(this);
        line2.setBackgroundColor(Color.parseColor("#E2E8F0"));
        LinearLayout.LayoutParams line2Params = new LinearLayout.LayoutParams(0, dpToPx(1), 1.0f);

        dividerLayout.addView(line1, line1Params);
        dividerLayout.addView(orTv);
        dividerLayout.addView(line2, line2Params);

        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dividerParams.bottomMargin = dpToPx(20);
        errorCard.addView(dividerLayout, dividerParams);

        // 6. Secondary Button: "🌐  Check Connection"
        TextView checkConnBtn = new TextView(this);
        checkConnBtn.setText("🌐   Check Connection");
        checkConnBtn.setTextSize(14);
        checkConnBtn.setTextColor(Color.parseColor("#7C3AED"));
        checkConnBtn.setTypeface(Typeface.DEFAULT_BOLD);
        checkConnBtn.setGravity(Gravity.CENTER);
        checkConnBtn.setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10));

        checkConnBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
            } catch (Exception e) {
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                } catch (Exception ex) {
                    Toast.makeText(MainActivity.this, "Cannot open network settings", Toast.LENGTH_SHORT).show();
                }
            }
        });
        errorCard.addView(checkConnBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams errContainerCardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errContainerCardParams.setMargins(dpToPx(24), 0, dpToPx(24), 0);
        mainErrorLayout.addView(errorCard, errContainerCardParams);

        // 7. Footer text at bottom: "🎧  Still having trouble? Contact support"
        TextView supportFooterTv = new TextView(this);
        supportFooterTv.setText("🎧   Still having trouble? Contact support");
        supportFooterTv.setTextSize(13);
        supportFooterTv.setTextColor(Color.parseColor("#64748B"));
        supportFooterTv.setGravity(Gravity.CENTER);
        supportFooterTv.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(24));

        supportFooterTv.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=Hi%20Support,%20I%20am%20facing%20connection%20issues%20on%20eFormX%20app"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Opening support...", Toast.LENGTH_SHORT).show();
            }
        });

        mainErrorLayout.addView(supportFooterTv);

        FrameLayout.LayoutParams mainErrorParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mainErrorParams.gravity = Gravity.CENTER;
        errorOverlay.addView(mainErrorLayout, mainErrorParams);
        errorOverlay.setVisibility(View.GONE);

        container.addView(errorOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));



        setContentView(container);

        registerNetworkCallback();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDatabasePath(getDir("databases", MODE_PRIVATE).getPath());
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Aggressive Caching Strategy: Use local disk cache first for instant 0ms loads
        if (isNetworkAvailable()) {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        }

        // High Render Priority
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        cookieManager.flush();

        String userAgent = webSettings.getUserAgentString();
        if (userAgent != null && userAgent.contains("; wv")) {
            webSettings.setUserAgentString(userAgent.replace("; wv", ""));
        }

        webView.addJavascriptInterface(new WebAppInterface(), "AndroidShare");

        webView.setWebViewClient(new WebViewClient() {
            private void injectSharePolyfill(WebView view) {
                String js = "if (window.AndroidShare) {" +
                        "  navigator.share = function(data) {" +
                        "    return new Promise(function(resolve, reject) {" +
                        "      try {" +
                        "        var title = (data && data.title) ? data.title : '';" +
                        "        var text = (data && data.text) ? data.text : '';" +
                        "        var url = (data && data.url) ? data.url : '';" +
                        "        window.AndroidShare.share(title, text, url);" +
                        "        resolve();" +
                        "      } catch(e) { reject(e); }" +
                        "    });" +
                        "  };" +
                        "}";
                view.evaluateJavascript(js, null);
            }

            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                handler.proceed();
            }

            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (isShortCallbackUrl(url)) {
                        return new android.webkit.WebResourceResponse("text/html", "UTF-8", new java.io.ByteArrayInputStream(new byte[0]));
                    }
                    if (url.startsWith("eformx://") || url.startsWith("eformx:/")) {
                        final String targetUrl = parseEformxUrl(url);
                        runOnUiThread(() -> {
                            if (webView != null) {
                                String currentUrl = webView.getUrl();
                                if (currentUrl == null || !currentUrl.replaceAll("/$", "").equalsIgnoreCase(targetUrl.replaceAll("/$", ""))) {
                                    webView.stopLoading();
                                    webView.loadUrl(targetUrl);
                                }
                            }
                        });
                        return new android.webkit.WebResourceResponse("text/html", "UTF-8", new java.io.ByteArrayInputStream(new byte[0]));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (handleUrl(view, url)) {
                        return true;
                    }
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (handleUrl(view, url)) {
                    return true;
                }
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                applySmartCacheStrategy(url);
                injectSharePolyfill(view);
                if (!isNetworkAvailable()) {
                    view.stopLoading();
                    showErrorOverlay();
                    if (fullScreenLoadingOverlay != null) {
                        fullScreenLoadingOverlay.setVisibility(View.GONE);
                    }

                    return;
                } else {
                    hideErrorOverlay();
                }

                if (fullScreenLoadingOverlay != null) {
                    if (url != null) {
                        String lowerUrl = url.toLowerCase();
                        if (lowerUrl.contains("dashboard") || lowerUrl.contains("panel")) {
                            loadingTitleTv.setText("Loading Dashboard...");
                        } else if (lowerUrl.contains("login") || lowerUrl.contains("auth")) {
                            loadingTitleTv.setText("Opening Login Portal...");
                        } else if (lowerUrl.contains("form") || lowerUrl.contains("apply")) {
                            loadingTitleTv.setText("Loading Application Form...");
                        } else {
                            loadingTitleTv.setText("Loading eFormX Services...");
                        }
                    } else {
                        loadingTitleTv.setText("Loading eFormX Services...");
                    }
                    fullScreenLoadingOverlay.setVisibility(View.VISIBLE);
                }

                if (url != null && url.startsWith("eformx://")) {
                    view.stopLoading();
                    String targetUrl = parseEformxUrl(url);
                    view.loadUrl(targetUrl);
                    return;
                }
                if (handleUrl(view, url)) {
                    view.stopLoading();
                    if (fullScreenLoadingOverlay != null) {
                        fullScreenLoadingOverlay.setVisibility(View.GONE);
                    }
                    return;
                }
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                injectSharePolyfill(view);
                CookieManager.getInstance().flush();
                if (view != null && view.getProgress() >= 100) {
                    if (fullScreenLoadingOverlay != null) {
                        fullScreenLoadingOverlay.setVisibility(View.GONE);
                    }
                }

                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                if (fullScreenLoadingOverlay != null) {
                    fullScreenLoadingOverlay.setVisibility(View.GONE);
                }

                if (request != null && request.isForMainFrame()) {
                    showErrorOverlay();
                }
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (url.startsWith("eformx://")) {
                        view.stopLoading();
                        String targetUrl = parseEformxUrl(url);
                        view.loadUrl(targetUrl);
                        return;
                    }
                }
                super.onReceivedError(view, request, error);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {

                if (newProgress >= 100) {
                    if (fullScreenLoadingOverlay != null && fullScreenLoadingOverlay.getVisibility() == View.VISIBLE) {
                        fullScreenLoadingOverlay.postDelayed(() -> fullScreenLoadingOverlay.setVisibility(View.GONE), 200);
                    }
                }
                super.onProgressChanged(view, newProgress);
            }
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        handleUrl(MainActivity.this.webView, url);
                        return true;
                    }
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                        if (request != null && request.getUrl() != null) {
                            handleUrl(MainActivity.this.webView, request.getUrl().toString());
                        }
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    fileChooserLauncher.launch(intent);
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    Toast.makeText(MainActivity.this, "Cannot open file chooser", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("Downloading file...");
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));

                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(getApplicationContext(), "Downloading File...", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Download Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    showExitConfirmationDialog();
                }
            }
        });

        handleNotificationOrDeepLinkIntent(getIntent());
    }

    private void handleNotificationOrDeepLinkIntent(Intent intent) {
        if (intent == null) return;

        if (intent.hasExtra("target_url")) {
            String targetUrl = intent.getStringExtra("target_url");
            String openType = intent.getStringExtra("open_type");

            if (targetUrl != null && !targetUrl.isEmpty()) {
                if ("external_browser".equalsIgnoreCase(openType)) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Error opening external browser: " + e.getMessage());
                    }
                } else if (webView != null) {
                    webView.loadUrl(targetUrl);
                }
                return;
            }
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            handleIntentData(intent.getData().toString());
        } else if (webView != null && webView.getUrl() == null) {
            webView.loadUrl("https://eformx.com/app.php");
        }
    }

    private void handleIntentData(String rawUrl) {
        if (rawUrl == null || webView == null) return;

        if (isShortCallbackUrl(rawUrl)) {
            return;
        }

        if (rawUrl.startsWith("eformx://") || rawUrl.startsWith("eformx:/")) {
            String targetUrl = parseEformxUrl(rawUrl);
            String currentUrl = webView.getUrl();
            if (currentUrl != null && targetUrl != null) {
                String normCurrent = currentUrl.replaceAll("/$", "");
                String normTarget = targetUrl.replaceAll("/$", "");
                if (normCurrent.equalsIgnoreCase(normTarget)) {
                    return;
                }
            }
            if (targetUrl != null && !targetUrl.isEmpty()) {
                webView.loadUrl(targetUrl);
            }
            return;
        }

        webView.loadUrl(rawUrl);
    }

    private boolean isExternalBrowserRequested(String url) {
        if (url == null) return false;
        boolean hasExternal = url.contains("browser=external") || url.contains("browser=extrunal");
        boolean hasCallbackApp = url.contains("calback=app") || url.contains("callback=app");
        boolean hasShareLink = url.contains("share_link=true");
        return hasExternal && !hasCallbackApp && !hasShareLink;
    }

    private String parseEformxUrl(String rawUrl) {
        if (rawUrl == null || !rawUrl.startsWith("eformx://")) {
            return rawUrl;
        }
        String stripped = rawUrl.substring("eformx://".length()).trim();

        try {
            stripped = java.net.URLDecoder.decode(stripped, "UTF-8");
        } catch (Exception ignored) {}

        int httpsIndex = stripped.indexOf("https://");
        int httpIndex = stripped.indexOf("http://");

        if (httpsIndex != -1) {
            return stripped.substring(httpsIndex);
        } else if (httpIndex != -1) {
            return stripped.substring(httpIndex);
        }

        if (stripped.startsWith("https//")) {
            return "https://" + stripped.substring("https//".length());
        } else if (stripped.startsWith("http//")) {
            return "http://" + stripped.substring("http//".length());
        } else if (stripped.startsWith("https:/")) {
            return "https://" + stripped.substring("https:/".length());
        } else if (stripped.startsWith("http:/")) {
            return "http://" + stripped.substring("http:/".length());
        }

        return "https://apply.eformx.com/" + stripped;
    }

    private String convertToWhatsappScheme(String rawUrl) {
        if (rawUrl == null) return rawUrl;
        if (rawUrl.startsWith("whatsapp://")) {
            return rawUrl;
        }
        try {
            Uri uri = Uri.parse(rawUrl);
            String phone = null;
            String text = uri.getQueryParameter("text");

            if (rawUrl.contains("wa.me/")) {
                String path = uri.getPath();
                if (path != null && path.length() > 1) {
                    phone = path.substring(1);
                }
            } else if (rawUrl.contains("api.whatsapp.com")) {
                phone = uri.getQueryParameter("phone");
            }

            StringBuilder sb = new StringBuilder("whatsapp://send?");
            if (phone != null && !phone.isEmpty()) {
                sb.append("phone=").append(phone);
            }
            if (text != null && !text.isEmpty()) {
                if (phone != null && !phone.isEmpty()) {
                    sb.append("&");
                }
                sb.append("text=").append(Uri.encode(text));
            }
            return sb.toString();
        } catch (Exception e) {
            return rawUrl;
        }
    }

    private void applySmartCacheStrategy(String url) {
        if (url == null || webView == null) return;
        try {
            Uri uri = Uri.parse(url);
            String cacheParam = uri.getQueryParameter("cache");
            if (cacheParam != null) {
                android.content.SharedPreferences prefs = getSharedPreferences("eformx_prefs", MODE_PRIVATE);
                String savedCacheId = prefs.getString("saved_cache_id", null);

                if (savedCacheId != null && savedCacheId.equals(cacheParam)) {
                    // Same cache ID! Use local disk cache first for instant 0ms load with smooth fallback
                    webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                } else {
                    // New/Different cache ID! Fetch fresh page from network and update savedCacheId
                    if (isNetworkAvailable()) {
                        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                    } else {
                        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                    }
                    prefs.edit().putString("saved_cache_id", cacheParam).apply();
                }
            } else {
                // Normal page without cache parameter
                if (isNetworkAvailable()) {
                    webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                } else {
                    webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean handleUrl(WebView view, String url) {
        if (url == null) return false;
        applySmartCacheStrategy(url);

        if (url.contains("api.whatsapp.com") || url.contains("wa.me") || url.startsWith("whatsapp://")) {
            runOnUiThread(() -> {
                try {
                    String whatsappUrl = convertToWhatsappScheme(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent directIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(directIntent);
                    } catch (Exception ex) {
                        Toast.makeText(MainActivity.this, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return true;
        }

        if (isShortCallbackUrl(url)) {
            return true;
        }

        if (url.startsWith("eformx://") || url.startsWith("eformx:/")) {
            String targetUrl = parseEformxUrl(url);
            String currentUrl = view != null ? view.getUrl() : null;
            if (currentUrl != null && targetUrl != null) {
                String normCurrent = currentUrl.replaceAll("/$", "");
                String normTarget = targetUrl.replaceAll("/$", "");
                if (normCurrent.equalsIgnoreCase(normTarget)) {
                    return true;
                }
            }
            if (targetUrl != null && !targetUrl.isEmpty()) {
                view.loadUrl(targetUrl);
            }
            return true;
        }

        if (handleNonHttpScheme(url)) {
            return true;
        }

        if (isExternalBrowserRequested(url)) {
            try {
                CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
                customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
                return true;
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return false;
    }

    private boolean isShortCallbackUrl(String url) {
        if (url == null) return false;
        return url.startsWith("eformx:/?calback=app") ||
               url.startsWith("eformx://?calback=app") ||
               url.startsWith("eformx:?calback=app") ||
               url.startsWith("eformx:/?callback=app") ||
               url.startsWith("eformx://?callback=app") ||
               url.startsWith("eformx:?callback=app") ||
               url.equals("eformx://") ||
               url.equals("eformx:/") ||
               url.equals("eformx:");
    }

    private boolean handleNonHttpScheme(String url) {
        if (url == null || url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") || url.startsWith("javascript:")) {
            return false;
        }
        if (url.startsWith("eformx://") || url.startsWith("eformx:/")) {
            return false;
        }
        runOnUiThread(() -> {
            try {
                Intent intent;
                if (url.startsWith("intent://")) {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                } else {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                }
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            } catch (Exception e) {
                try {
                    Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(fallbackIntent);
                } catch (Exception ex) {
                    Toast.makeText(MainActivity.this, "No app available to handle this link", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationOrDeepLinkIntent(intent);
    }

    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void share(String title, String text, String url) {
            runOnUiThread(() -> {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    String shareContent = "";
                    if (text != null && !text.isEmpty()) {
                        shareContent += text;
                    }
                    if (url != null && !url.isEmpty()) {
                        if (!shareContent.isEmpty()) shareContent += "\n";
                        shareContent += url;
                    }
                    if (shareContent.isEmpty() && title != null) {
                        shareContent = title;
                    }
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, title != null ? title : "");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
                    Intent chooser = Intent.createChooser(shareIntent, title != null && !title.isEmpty() ? title : "Share via");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(chooser);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void showErrorOverlay() {
        if (errorOverlay != null) {
            errorOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideErrorOverlay() {
        if (errorOverlay != null) {
            errorOverlay.setVisibility(View.GONE);
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                    return capabilities != null && (
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
                } else {
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    return activeNetwork != null && activeNetwork.isConnected();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void registerNetworkCallback() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onLost(Network network) {
                        runOnUiThread(() -> showErrorOverlay());
                    }

                    @Override
                    public void onAvailable(Network network) {
                        runOnUiThread(() -> {
                            if (errorOverlay != null && errorOverlay.getVisibility() == View.VISIBLE) {
                                hideErrorOverlay();
                            }
                        });
                    }
                };
                cm.registerDefaultNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unregisterNetworkCallback() {
        try {
            if (networkCallback != null) {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                if (cm != null) {
                    cm.unregisterNetworkCallback(networkCallback);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterNetworkCallback();
        super.onDestroy();
    }

    private void showExitConfirmationDialog() {
        try {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Exit App?")
                    .setMessage("Are you sure you want to exit eFormX?")
                    .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            finishAffinity();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                return true;
            } else {
                showExitConfirmationDialog();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void checkAndRequestAutoStartPermission() {
        try {
            String manufacturer = Build.MANUFACTURER.toLowerCase();
            Intent intent = new Intent();

            if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
                intent.setComponent(new android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if (manufacturer.contains("oppo")) {
                intent.setComponent(new android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if (manufacturer.contains("vivo")) {
                intent.setComponent(new android.content.ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
                intent.setComponent(new android.content.ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            } else if (manufacturer.contains("letv")) {
                intent.setComponent(new android.content.ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"));
            } else if (manufacturer.contains("asus")) {
                intent.setComponent(new android.content.ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity"));
            }

            if (intent.getComponent() != null && getPackageManager().queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY).size() > 0) {
                startActivity(intent);
            }
        } catch (Exception e) {
            android.util.Log.e("AutoStart", "Unable to open Auto-Start settings: " + e.getMessage());
        }
    }
}
