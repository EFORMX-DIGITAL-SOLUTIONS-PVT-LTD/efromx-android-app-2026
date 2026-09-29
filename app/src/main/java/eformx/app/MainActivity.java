package eformx.app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import org.json.JSONObject;
import java.util.Locale;
import java.util.TimeZone;
import eformx.app.BuildConfig;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.content.FileProvider;
import android.provider.MediaStore;
import java.io.File;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

public class MainActivity extends ComponentActivity {

    private WebView webView;
    private ProgressBar topProgressBar;
    private FrameLayout fullScreenLoadingOverlay;
    private FrameLayout errorOverlay;
    private TextView loadingTitleTv;
    private TextView loadingSubtitleTv;
    private ValueCallback<Uri[]> filePathCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private AndroidBridge androidBridge;
    private boolean isErrorState = false;
    private boolean hasLoadedAnyPageSuccessfully = false;
    private Uri cameraImageUri;
    private AppUpdateManager appUpdateManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private final android.os.Handler loadingWatchdogHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable loadingWatchdogRunnable = () -> {
        if (fullScreenLoadingOverlay != null && fullScreenLoadingOverlay.getVisibility() == View.VISIBLE) {
            fullScreenLoadingOverlay.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dismissSourceNotification(getIntent());

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (filePathCallback == null)
                        return;
                    Uri[] results = null;
                    if (result.getResultCode() == RESULT_OK) {
                        if (result.getData() == null
                                || (result.getData().getData() == null && result.getData().getClipData() == null)) {
                            if (cameraImageUri != null) {
                                results = new Uri[] { cameraImageUri };
                            }
                        } else {
                            String dataString = result.getData().getDataString();
                            if (dataString != null) {
                                results = new Uri[] { Uri.parse(dataString) };
                            } else if (result.getData().getClipData() != null) {
                                int count = result.getData().getClipData().getItemCount();
                                results = new Uri[count];
                                for (int i = 0; i < count; i++) {
                                    results[i] = result.getData().getClipData().getItemAt(i).getUri();
                                }
                            }
                        }
                    }
                    filePathCallback.onReceiveValue(results);
                    filePathCallback = null;
                });

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            false);
                    if (fineGranted || coarseGranted) {
                        Toast.makeText(MainActivity.this, "Location permission granted", Toast.LENGTH_SHORT).show();
                        if (androidBridge != null) {
                            androidBridge.initLocationListener();
                        }
                    } else if (result.containsKey(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Toast.makeText(MainActivity.this, "Location permission denied", Toast.LENGTH_SHORT).show();
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && result.containsKey(android.Manifest.permission.POST_NOTIFICATIONS)) {
                        Boolean notifGranted = result.get(android.Manifest.permission.POST_NOTIFICATIONS);
                        if (Boolean.TRUE.equals(notifGranted)) {
                            Toast.makeText(MainActivity.this, "Notification permission granted", Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            Toast.makeText(MainActivity.this, "Notification permission denied", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        Toast.makeText(MainActivity.this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                            Toast.makeText(MainActivity.this, "कृपया सेटिंग्स में जाकर नोटिफिकेशन चालू करें",
                                    Toast.LENGTH_LONG).show();
                            openNotificationSettings();
                        } else {
                            Toast.makeText(MainActivity.this, "Notification permission denied", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });

        androidBridge = new AndroidBridge(this, locationPermissionLauncher);
        checkAndRequestAllPermissions();

        // Auto-Cache Flush on App Version Bump or API Version Change
        try {
            SharedPreferences prefs = getSharedPreferences("eformx_prefs", MODE_PRIVATE);
            int currentVersionCode = BuildConfig.VERSION_CODE;
            int lastSavedVersionCode = prefs.getInt("last_saved_version_code", -1);
            boolean apiVersionChanged = prefs.getBoolean("cache_flush_needed", false);

            if (lastSavedVersionCode != currentVersionCode || apiVersionChanged) {
                prefs.edit()
                        .putInt("last_saved_version_code", currentVersionCode)
                        .putBoolean("cache_flush_needed", false)
                        .apply();
                if (webView != null) {
                    webView.clearCache(true);
                }
                android.webkit.WebStorage.getInstance().deleteAllData();
            }
        } catch (Exception ignored) {
        }

        // Ensure any old tracking notification is cancelled
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(
                    android.content.Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(2002);
            }
        } catch (Exception ignored) {
        }

        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        android.util.Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    android.util.Log.d("FCM_TOKEN", token);
                    if (token != null && !token.trim().isEmpty()) {
                        getSharedPreferences("eformx_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("fcm_token", token)
                                .apply();
                        MyFirebaseMessagingService.registerFcmTokenOnServer(MainActivity.this, token);
                    }
                });

        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("FCM", "Successfully subscribed to topic 'all'");
                    } else {
                        android.util.Log.w("FCM", "Topic subscription failed", task.getException());
                    }
                });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.WHITE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        container.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        topProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        topProgressBar.setMax(100);
        topProgressBar.setIndeterminate(false);
        topProgressBar.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            topProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4F46E5")));
        }
        FrameLayout.LayoutParams pbParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(3));
        pbParams.gravity = Gravity.TOP;
        container.addView(topProgressBar, pbParams);

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
        fullScreenLoadingOverlay.setOnClickListener(v -> {
            fullScreenLoadingOverlay.setVisibility(View.GONE);
        });

        container.addView(fullScreenLoadingOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Custom Branded Offline / Error Screen Overlay (Exact Match with User
        // Screenshot)
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
            private final android.graphics.Paint paint = new android.graphics.Paint(
                    android.graphics.Paint.ANTI_ALIAS_FLAG);

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

                android.graphics.RectF r1 = new android.graphics.RectF(cx - dpToPx(22), cy - dpToPx(22),
                        cx + dpToPx(22), cy + dpToPx(22));
                canvas.drawArc(r1, 215, 110, false, paint);

                android.graphics.RectF r2 = new android.graphics.RectF(cx - dpToPx(14), cy - dpToPx(14),
                        cx + dpToPx(14), cy + dpToPx(14));
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

        // 4. Primary Button: "🔄 Try Again"
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
                Toast.makeText(MainActivity.this, "Still offline. Please check your connection.", Toast.LENGTH_SHORT)
                        .show();
            }
        });
        LinearLayout.LayoutParams retryBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        retryBtnParams.bottomMargin = dpToPx(20);
        errorCard.addView(retryBtn, retryBtnParams);

        // 5. Divider Line: "───── OR ─────"
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

        // 6. Secondary Button: "🌐 Check Connection"
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

        // 7. Footer text at bottom: "🎧 Still having trouble? Contact support"
        TextView supportFooterTv = new TextView(this);
        supportFooterTv.setText("🎧   Still having trouble? Contact support");
        supportFooterTv.setTextSize(13);
        supportFooterTv.setTextColor(Color.parseColor("#64748B"));
        supportFooterTv.setGravity(Gravity.CENTER);
        supportFooterTv.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(24));

        supportFooterTv.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "https://wa.me/?text=Hi%20Support,%20I%20am%20facing%20connection%20issues%20on%20eFormX%20app"));
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

        applyStatusBarAppearance();

        ViewCompat.setOnApplyWindowInsetsListener(container, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        registerNetworkCallback();

        WebSettings webSettings = webView.getSettings();
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDatabasePath(getDir("databases", MODE_PRIVATE).getPath());
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Live Data when Online, Offline Cache when Offline
        if (isNetworkAvailable()) {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
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
        webView.addJavascriptInterface(androidBridge, "Android");

        webView.setWebViewClient(new WebViewClient() {
            private void injectSharePolyfill(WebView view) {
                String token = getSharedPreferences("eformx_prefs", MODE_PRIVATE).getString("fcm_token", "");
                String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                if (deviceId == null)
                    deviceId = "";

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
                        "}" +
                        "if (typeof window.Notification === 'undefined') {" +
                        "  window.Notification = function(title, options) {};" +
                        "}" +
                        "try {" +
                        "  Object.defineProperty(window.Notification, 'permission', {" +
                        "    get: function() { return (window.Android && window.Android.hasNotificationPermission && window.Android.hasNotificationPermission()) ? 'granted' : 'default'; },"
                        +
                        "    configurable: true" +
                        "  });" +
                        "} catch(e) {}" +
                        "window.Notification.requestPermission = function(cb) {" +
                        "  if (window.Android && window.Android.openNotificationPermission) {" +
                        "    window.Android.openNotificationPermission();" +
                        "  }" +
                        "  var perm = (window.Android && window.Android.hasNotificationPermission && window.Android.hasNotificationPermission()) ? 'granted' : 'default';"
                        +
                        "  if (typeof cb === 'function') cb(perm);" +
                        "  return Promise.resolve(perm);" +
                        "};" +
                        "window.fcm_token = '" + token + "';" +
                        "window.device_id = '" + deviceId + "';" +
                        "if (!window._eformx_click_hooked) {" +
                        "  window._eformx_click_hooked = true;" +
                        "  function _eformx_enrich(a) {" +
                        "    try {" +
                        "      var h = a.getAttribute('href');" +
                        "      var did = window.device_id || '';" +
                        "      if (h && did && (h.indexOf('http://') === 0 || h.indexOf('https://') === 0 || h.indexOf('eformx://') === 0) && h.indexOf('platform_refrence=') === -1) {"
                        +
                        "        var sep = h.indexOf('?') !== -1 ? '&' : '?';" +
                        "        a.setAttribute('href', h + sep + 'platform_refrence=android&platform_refrence_id=' + encodeURIComponent(did));"
                        +
                        "      }" +
                        "    } catch(e) {}" +
                        "  }" +
                        "  function _eformx_scan() {" +
                        "    try {" +
                        "      var links = document.querySelectorAll('a[href]');" +
                        "      for (var i = 0; i < links.length; i++) { _eformx_enrich(links[i]); }" +
                        "    } catch(e) {}" +
                        "  }" +
                        "  _eformx_scan();" +
                        "  setInterval(_eformx_scan, 1500);" +
                        "  document.addEventListener('click', function(e) {" +
                        "    try {" +
                        "      var el = e.target.closest('[data-android=\"open\"]');" +
                        "      if (el && window.Android && window.Android.showProcessLoader) {" +
                        "        var page = el.getAttribute('data-page') || '';" +
                        "        window.Android.showProcessLoader(page);" +
                        "      }" +
                        "      var a = e.target.closest('a');" +
                        "      if (a) { _eformx_enrich(a); }" +
                        "    } catch(err) {}" +
                        "  }, true);" +
                        "}";
                view.evaluateJavascript(js, null);
            }

            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler,
                    android.net.http.SslError error) {
                handler.proceed();
            }

            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view,
                    android.webkit.WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (isShortCallbackUrl(url)) {
                        return new android.webkit.WebResourceResponse("text/html", "UTF-8",
                                new java.io.ByteArrayInputStream(new byte[0]));
                    }
                    if (url.startsWith("eformx://") || url.startsWith("eformx:/")) {
                        final String targetUrl = parseEformxUrl(url);
                        runOnUiThread(() -> {
                            if (topProgressBar != null) {
                                topProgressBar.setVisibility(View.VISIBLE);
                                topProgressBar.setProgress(20);
                            }
                            if (webView != null) {
                                String currentUrl = webView.getUrl();
                                if (currentUrl == null || !currentUrl.replaceAll("/$", "")
                                        .equalsIgnoreCase(targetUrl.replaceAll("/$", ""))) {
                                    webView.stopLoading();
                                    webView.loadUrl(targetUrl);
                                }
                            }
                        });
                        return new android.webkit.WebResourceResponse("text/html", "UTF-8",
                                new java.io.ByteArrayInputStream(new byte[0]));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (topProgressBar != null) {
                        topProgressBar.setVisibility(View.VISIBLE);
                        topProgressBar.setProgress(20);
                    }
                    url = appendPlatformParams(url);
                    if (handleUrl(view, url)) {
                        return true;
                    }
                    String currentUrl = view != null ? view.getUrl() : null;
                    if (currentUrl == null || !currentUrl.equals(url)) {
                        view.loadUrl(url);
                        return true;
                    }
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (topProgressBar != null) {
                    topProgressBar.setVisibility(View.VISIBLE);
                    topProgressBar.setProgress(20);
                }
                url = appendPlatformParams(url);
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

                loadingWatchdogHandler.removeCallbacks(loadingWatchdogRunnable);
                loadingWatchdogHandler.postDelayed(loadingWatchdogRunnable, 5000);

                if (topProgressBar != null) {
                    topProgressBar.setVisibility(View.VISIBLE);
                    topProgressBar.setProgress(25);
                }

                if (fullScreenLoadingOverlay != null && !hasLoadedAnyPageSuccessfully) {
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
                loadingWatchdogHandler.removeCallbacks(loadingWatchdogRunnable);
                injectSharePolyfill(view);
                CookieManager.getInstance().flush();
                if (topProgressBar != null) {
                    topProgressBar.setProgress(100);
                    topProgressBar.postDelayed(() -> {
                        if (topProgressBar != null) {
                            topProgressBar.setVisibility(View.GONE);
                            topProgressBar.setProgress(0);
                        }
                    }, 250);
                }
                if (fullScreenLoadingOverlay != null) {
                    fullScreenLoadingOverlay.setVisibility(View.GONE);
                }
                if (!isErrorState && url != null && !url.startsWith("data:") && !url.contains("error")) {
                    hasLoadedAnyPageSuccessfully = true;
                }
                if (url != null && !url.startsWith("data:")) {
                    isErrorState = false;
                }
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request,
                    android.webkit.WebResourceError error) {
                if (topProgressBar != null) {
                    topProgressBar.setVisibility(View.GONE);
                }
                if (fullScreenLoadingOverlay != null) {
                    fullScreenLoadingOverlay.setVisibility(View.GONE);
                }
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (url.startsWith("eformx://")) {
                        view.stopLoading();
                        String targetUrl = parseEformxUrl(url);
                        view.loadUrl(targetUrl);
                        return;
                    }
                    if (request.isForMainFrame()) {
                        String errorMsg = (error != null && error.getDescription() != null)
                                ? error.getDescription().toString()
                                : "";
                        if (errorMsg.contains("ERR_CACHE_MISS") && isNetworkAvailable()) {
                            view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                            view.loadUrl(url);
                            return;
                        }
                        if (view != null) {
                            view.stopLoading();
                        }
                        showCustomErrorPage(view, url, errorMsg);
                        return;
                    }
                }
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (topProgressBar != null) {
                    topProgressBar.setVisibility(View.GONE);
                }
                if (fullScreenLoadingOverlay != null) {
                    fullScreenLoadingOverlay.setVisibility(View.GONE);
                }
                if (failingUrl != null && failingUrl.startsWith("eformx://")) {
                    view.stopLoading();
                    String targetUrl = parseEformxUrl(failingUrl);
                    view.loadUrl(targetUrl);
                    return;
                }
                if (description != null && description.contains("ERR_CACHE_MISS") && isNetworkAvailable()) {
                    view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                    view.loadUrl(failingUrl);
                    return;
                }
                if (view != null) {
                    view.stopLoading();
                }
                showCustomErrorPage(view, failingUrl, description);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (topProgressBar != null) {
                    topProgressBar.setVisibility(View.VISIBLE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        topProgressBar.setProgress(newProgress, true);
                    } else {
                        topProgressBar.setProgress(newProgress);
                    }
                    if (newProgress >= 100) {
                        topProgressBar.postDelayed(() -> {
                            if (topProgressBar != null) {
                                topProgressBar.setVisibility(View.GONE);
                                topProgressBar.setProgress(0);
                            }
                        }, 250);
                    }
                }
                if (newProgress >= 60) {
                    loadingWatchdogHandler.removeCallbacks(loadingWatchdogRunnable);
                    if (fullScreenLoadingOverlay != null && fullScreenLoadingOverlay.getVisibility() == View.VISIBLE) {
                        fullScreenLoadingOverlay.setVisibility(View.GONE);
                    }
                }
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                    android.os.Message resultMsg) {
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
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    File photoFile = File.createTempFile("CAM_" + System.currentTimeMillis() + "_", ".jpg", storageDir);
                    cameraImageUri = FileProvider.getUriForFile(
                            MainActivity.this,
                            getPackageName() + ".fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                } catch (Exception e) {
                    cameraImageUri = null;
                }

                Intent contentSelectionIntent = fileChooserParams.createIntent();
                Intent[] intentArray = (takePictureIntent != null && cameraImageUri != null)
                        ? new Intent[] { takePictureIntent }
                        : new Intent[0];

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Select File or Capture Photo");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                try {
                    fileChooserLauncher.launch(chooserIntent);
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
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType,
                    long contentLength) {
                if (url == null)
                    return;

                if (url.startsWith("data:")) {
                    if (androidBridge != null) {
                        String mime = mimeType != null ? mimeType : "application/pdf";
                        String filename = URLUtil.guessFileName(url, contentDisposition, mime);
                        androidBridge.saveBase64File(url, mime, filename);
                    }
                    return;
                }

                if (url.startsWith("blob:")) {
                    String mime = mimeType != null ? mimeType : "application/pdf";
                    String filename = URLUtil.guessFileName(url, contentDisposition, mime);
                    String js = "fetch('" + url + "').then(r => r.blob()).then(blob => {" +
                            "  var reader = new FileReader();" +
                            "  reader.onloadend = function() {" +
                            "    if (window.Android && window.Android.saveBase64File) {" +
                            "      window.Android.saveBase64File(reader.result, '" + mime + "', '" + filename + "');" +
                            "    }" +
                            "  };" +
                            "  reader.readAsDataURL(blob);" +
                            "}).catch(e => console.error(e));";
                    webView.evaluateJavascript(js, null);
                    return;
                }

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
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                            URLUtil.guessFileName(url, contentDisposition, mimeType));

                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(getApplicationContext(), "Downloading File...", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Download Failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackAction();
            }
        });

        handleNotificationOrDeepLinkIntent(getIntent());
    }

    private void handleNotificationOrDeepLinkIntent(Intent intent) {
        if (intent == null)
            return;

        if (intent.hasExtra("target_url")) {
            String targetUrl = appendPlatformParams(intent.getStringExtra("target_url"));

            if (targetUrl != null && !targetUrl.isEmpty()) {
                if (webView != null) {
                    applySmartCacheStrategy(targetUrl);
                    webView.loadUrl(targetUrl);
                }
                return;
            }
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            handleIntentData(intent.getData().toString());
        } else if (webView != null && webView.getUrl() == null) {
            String defaultUrl = getSharedPreferences(SecureConfig.getPrefsName(), MODE_PRIVATE)
                    .getString(SecureConfig.getKeyRedirectUrl(), SecureConfig.getDefaultWebUrl());
            defaultUrl = appendPlatformParams(defaultUrl);
            applySmartCacheStrategy(defaultUrl);
            webView.loadUrl(defaultUrl);
        }
    }

    private void handleIntentData(String rawUrl) {
        if (rawUrl == null || webView == null)
            return;

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
                applySmartCacheStrategy(targetUrl);
                webView.loadUrl(targetUrl);
            }
            return;
        }

        String finalUrl = appendPlatformParams(rawUrl);
        applySmartCacheStrategy(finalUrl);
        webView.loadUrl(finalUrl);
    }

    private boolean isExternalBrowserRequested(String url) {
        if (url == null)
            return false;
        boolean hasExternal = url.contains("browser=external") || url.contains("browser=extrunal");
        boolean hasCallbackApp = url.contains("calback=app") || url.contains("callback=app");
        boolean hasShareLink = url.contains("share_link=true");
        return hasExternal && !hasCallbackApp && !hasShareLink;
    }

    public void openAppUrl(String rawUrl) {
        runOnUiThread(() -> {
            if (rawUrl == null || rawUrl.trim().isEmpty()) {
                Intent bringToFront = new Intent(this, MainActivity.class);
                bringToFront.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(bringToFront);
                return;
            }

            String targetUrl = rawUrl.trim();
            if (targetUrl.startsWith("eformx://") || targetUrl.startsWith("eformx:/")) {
                return;
            }

            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                targetUrl = "https://" + targetUrl;
            }

            try {
                Uri uri = Uri.parse(targetUrl);
                if (uri.getQueryParameter("utm_source") == null) {
                    targetUrl = uri.buildUpon().appendQueryParameter("utm_source", "android").build().toString();
                }
            } catch (Exception ignored) {
            }

            if (webView != null) {
                targetUrl = appendPlatformParams(targetUrl);
                applySmartCacheStrategy(targetUrl);
                webView.loadUrl(targetUrl);
            }
        });
    }

    public void showProcessLoader(String page) {
        runOnUiThread(() -> {
            if (fullScreenLoadingOverlay != null) {
                if (page != null && !page.trim().isEmpty()) {
                    String cleanPage = page.trim();
                    String capPage = cleanPage.substring(0, 1).toUpperCase() + cleanPage.substring(1);
                    loadingTitleTv.setText("Processing your request (" + capPage + ")...");
                } else {
                    loadingTitleTv.setText("Processing your request...");
                }
                loadingSubtitleTv.setText("Please wait a moment");
                fullScreenLoadingOverlay.setVisibility(View.VISIBLE);
            }
            if (topProgressBar != null) {
                topProgressBar.setVisibility(View.VISIBLE);
                topProgressBar.setProgress(20);
            }
            loadingWatchdogHandler.removeCallbacks(loadingWatchdogRunnable);
            loadingWatchdogHandler.postDelayed(loadingWatchdogRunnable, 5000);
        });
    }

    private String appendPlatformParams(String url) {
        if (url == null || url.trim().isEmpty())
            return url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return url;
        }

        try {
            Uri uri = Uri.parse(url);
            if (uri.getQueryParameter("platform_refrence") != null) {
                return url;
            }

            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null) {
                deviceId = "";
            }

            Uri.Builder builder = uri.buildUpon();
            builder.appendQueryParameter("platform_refrence", "android");
            builder.appendQueryParameter("platform_refrence_id", deviceId);
            return builder.build().toString();
        } catch (Exception e) {
            return url;
        }
    }

    private String parseEformxUrl(String rawUrl) {
        if (rawUrl == null || !rawUrl.startsWith("eformx://")) {
            return rawUrl;
        }
        String stripped = rawUrl.substring("eformx://".length()).trim();

        try {
            stripped = java.net.URLDecoder.decode(stripped, "UTF-8");
        } catch (Exception ignored) {
        }

        int httpsIndex = stripped.indexOf("https://");
        int httpIndex = stripped.indexOf("http://");

        if (httpsIndex != -1) {
            return appendPlatformParams(stripped.substring(httpsIndex));
        } else if (httpIndex != -1) {
            return appendPlatformParams(stripped.substring(httpIndex));
        }

        if (stripped.startsWith("https//")) {
            return appendPlatformParams("https://" + stripped.substring("https//".length()));
        } else if (stripped.startsWith("http//")) {
            return appendPlatformParams("http://" + stripped.substring("http//".length()));
        } else if (stripped.startsWith("https:/")) {
            return appendPlatformParams("https://" + stripped.substring("https:/".length()));
        } else if (stripped.startsWith("http:/")) {
            return appendPlatformParams("http://" + stripped.substring("http:/".length()));
        }

        // If stripped already contains a valid host like apply.eformx.com or eformx.com
        if (stripped.startsWith("apply.eformx.com") || stripped.startsWith("eformx.com")
                || stripped.startsWith("www.eformx.com")
                || stripped.matches("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*|\\?.*)?$")) {
            return appendPlatformParams("https://" + stripped);
        }

        String baseUrl = getSharedPreferences("eformx_prefs", MODE_PRIVATE)
                .getString("redirect_url", "https://eformx.com/");
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return appendPlatformParams(baseUrl + stripped);
    }

    private boolean isUrlInCacheWhitelist(String url) {
        if (url == null || url.trim().isEmpty())
            return false;
        try {
            SharedPreferences prefs = getSharedPreferences("eformx_prefs", MODE_PRIVATE);
            java.util.Set<String> whitelist = prefs.getStringSet("catche_url_whitelist", null);
            if (whitelist != null && !whitelist.isEmpty()) {
                String cleanUrl = url.trim().toLowerCase();
                for (String cachedPattern : whitelist) {
                    if (cachedPattern == null || cachedPattern.trim().isEmpty())
                        continue;
                    String cleanPattern = cachedPattern.trim().toLowerCase();

                    // 1. Direct or prefix match
                    if (cleanUrl.equals(cleanPattern) || cleanUrl.startsWith(cleanPattern)
                            || cleanPattern.startsWith(cleanUrl)) {
                        return true;
                    }
                    // 2. Domain / Host match
                    try {
                        Uri cachedUri = Uri.parse(cleanPattern);
                        Uri currentUri = Uri.parse(cleanUrl);
                        if (cachedUri.getHost() != null && currentUri.getHost() != null) {
                            if (cachedUri.getHost().equalsIgnoreCase(currentUri.getHost())) {
                                String cachedPath = cachedUri.getPath();
                                String currentPath = currentUri.getPath();
                                if (cachedPath == null || cachedPath.isEmpty() || cachedPath.equals("/")) {
                                    return true;
                                }
                                if (currentPath != null && currentPath.startsWith(cachedPath)) {
                                    return true;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void applySmartCacheStrategy(String url) {
        if (url == null || webView == null)
            return;
        try {
            WebSettings settings = webView.getSettings();
            if (!isNetworkAvailable()) {
                settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                return;
            }

            // Important: Only cache websites listed in catche_url
            if (isUrlInCacheWhitelist(url)) {
                // In catche_url: Store and aggressively load from disk cache for super-fast
                // speed!
                settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            } else {
                // Not in catche_url: Load fresh live data, do not store in cache
                settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            }
        } catch (Exception e) {
            try {
                webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            } catch (Exception ignored) {
            }
        }
    }

    private void showCustomErrorPage(WebView view, String failingUrl, String errorMsg) {
        if (view == null)
            return;
        isErrorState = true;
        if (fullScreenLoadingOverlay != null) {
            fullScreenLoadingOverlay.setVisibility(View.GONE);
        }
        String defaultUrl = getSharedPreferences("eformx_prefs", MODE_PRIVATE)
                .getString("redirect_url", "https://eformx.com/");
        String safeUrl = (failingUrl != null && !failingUrl.isEmpty() && !failingUrl.contains("app.php"))
                ? failingUrl
                : defaultUrl;
        String cleanErrorMsg = (errorMsg != null && !errorMsg.isEmpty()) ? errorMsg : "";
        String errorBadgeHtml = !cleanErrorMsg.isEmpty() ? "<div class=\"error-badge\">" + cleanErrorMsg + "</div>"
                : "";

        String htmlData = "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">"
                + "<style>"
                + "  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; -webkit-tap-highlight-color: transparent; }"
                + "  body { background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%); color: #0f172a; min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px; text-align: center; overflow: hidden; }"
                + "  .container { width: 100%; max-width: 380px; display: flex; flex-direction: column; align-items: center; animation: fadeInUp 0.5s ease-out; }"
                + "  .illustration-wrapper { position: relative; width: 120px; height: 120px; margin-bottom: 24px; display: flex; align-items: center; justify-content: center; }"
                + "  .glow-bg { position: absolute; width: 100%; height: 100%; background: radial-gradient(circle, rgba(37,99,235,0.15) 0%, rgba(37,99,235,0) 70%); border-radius: 50%; animation: pulseGlow 3s infinite ease-in-out; }"
                + "  .icon-circle { position: relative; width: 88px; height: 88px; background: #ffffff; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04); border: 1px solid #e2e8f0; }"
                + "  .icon-circle svg { width: 44px; height: 44px; stroke: #2563eb; fill: none; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }"
                + "  .badge-offline { position: absolute; bottom: 4px; right: 4px; width: 26px; height: 26px; background: #ef4444; border-radius: 50%; border: 3px solid #ffffff; display: flex; align-items: center; justify-content: center; color: #ffffff; box-shadow: 0 2px 5px rgba(239,68,68,0.3); }"
                + "  .badge-offline svg { width: 12px; height: 12px; stroke: #ffffff; stroke-width: 3; fill: none; }"
                + "  h2 { font-size: 22px; font-weight: 700; color: #0f172a; margin-bottom: 8px; letter-spacing: -0.02em; }"
                + "  p { font-size: 14px; color: #64748b; line-height: 1.5; margin-bottom: 16px; max-width: 320px; font-weight: 400; }"
                + "  .error-badge { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; font-size: 13px; font-weight: 600; font-family: monospace, sans-serif; padding: 6px 16px; border-radius: 20px; margin-bottom: 28px; display: inline-block; word-break: break-all; max-width: 340px; }"
                + "  .btn-retry { background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%); color: #ffffff; border: none; padding: 14px 36px; font-size: 16px; font-weight: 600; border-radius: 50px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; gap: 10px; width: 100%; max-width: 240px; box-shadow: 0 10px 20px -5px rgba(37, 99, 235, 0.4); transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); outline: none; }"
                + "  .btn-retry:active { transform: scale(0.96); box-shadow: 0 4px 12px -2px rgba(37, 99, 235, 0.5); }"
                + "  .btn-retry svg { width: 18px; height: 18px; stroke: currentColor; fill: none; stroke-width: 2.5; stroke-linecap: round; stroke-linejoin: round; transition: transform 0.3s ease; }"
                + "  .btn-retry:active svg { transform: rotate(180deg); }"
                + "  @keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }"
                + "  @keyframes pulseGlow { 0%, 100% { transform: scale(1); opacity: 0.6; } 50% { transform: scale(1.15); opacity: 1; } }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "  <div class=\"container\">"
                + "    <div class=\"illustration-wrapper\">"
                + "      <div class=\"glow-bg\"></div>"
                + "      <div class=\"icon-circle\">"
                + "        <svg viewBox=\"0 0 24 24\"><path d=\"M1 1l22 22M16.72 11.06A10.94 10.94 0 0 1 19 12.55M5 12.55a10.94 10.94 0 0 1 5.17-2.39M10.71 5.05A16 16 0 0 1 22.58 9M1.42 9a15.91 15.91 0 0 1 4.7-2.88M8.53 16.11a6 6 0 0 1 6.95 0M12 20h.01\"/></svg>"
                + "        <div class=\"badge-offline\"><svg viewBox=\"0 0 24 24\"><line x1=\"18\" y1=\"6\" x2=\"6\" y2=\"18\"></line><line x1=\"6\" y1=\"6\" x2=\"18\" y2=\"18\"></line></svg></div>"
                + "      </div>"
                + "    </div>"
                + "    <h2>App Not Loaded</h2>"
                + "    <p>Could not connect to the server. Please check your internet connection and try again.</p>"
                + errorBadgeHtml
                + "    <button class=\"btn-retry\" onclick=\"if(window.AndroidShare && window.AndroidShare.reloadApp){window.AndroidShare.reloadApp();}else{location.href='"
                + defaultUrl.replace("'", "\\'") + "';}\">"
                + "      <svg viewBox=\"0 0 24 24\"><path d=\"M23 4v6h-6M1 20v-6h6\"></path><path d=\"M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15\"></path></svg>"
                + "      <span>Reload</span>"
                + "    </button>"
                + "  </div>"
                + "</body>"
                + "</html>";

        view.stopLoading();
        if (!hasLoadedAnyPageSuccessfully) {
            view.clearHistory();
        }
        view.post(() -> view.loadDataWithBaseURL(defaultUrl, htmlData, "text/html", "UTF-8", null));
    }

    private String convertToWhatsappScheme(String rawUrl) {
        if (rawUrl == null)
            return rawUrl;
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

    private boolean handleUrl(WebView view, String rawUrl) {
        if (rawUrl == null)
            return false;
        final String url = appendPlatformParams(rawUrl);
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
                        Toast.makeText(MainActivity.this, "WhatsApp is not installed on this device",
                                Toast.LENGTH_SHORT).show();
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
        if (url == null)
            return false;
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
        if (url == null || url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:")
                || url.startsWith("javascript:")) {
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
                    Toast.makeText(MainActivity.this, "No app available to handle this link", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        dismissSourceNotification(intent);
        handleNotificationOrDeepLinkIntent(intent);
    }

    private void dismissSourceNotification(Intent intent) {
        if (intent != null) {
            int notifId = intent.getIntExtra("notification_id", -1);
            boolean isCallAnswer = intent.getBooleanExtra("is_call_answer", false);
            if (notifId != -1) {
                try {
                    android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(
                            Context.NOTIFICATION_SERVICE);
                    if (nm != null) {
                        nm.cancel(notifId);
                    }
                } catch (Exception ignored) {
                }
            }
            if (isCallAnswer || notifId != -1) {
                MyFirebaseMessagingService.stopAllMediaAndTTS(this);
            }
        }
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
                        if (!shareContent.isEmpty())
                            shareContent += "\n";
                        shareContent += url;
                    }
                    if (shareContent.isEmpty() && title != null) {
                        shareContent = title;
                    }
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, title != null ? title : "");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
                    Intent chooser = Intent.createChooser(shareIntent,
                            title != null && !title.isEmpty() ? title : "Share via");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(chooser);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void reloadApp() {
            runOnUiThread(() -> {
                if (webView != null) {
                    if (!isNetworkAvailable()) {
                        Toast.makeText(MainActivity.this, "इंटरनेट कनेक्शन उपलब्ध नहीं है। कृपया इंटरनेट चालू करें।",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    isErrorState = false;
                    String defaultUrl = getSharedPreferences("eformx_prefs", MODE_PRIVATE)
                            .getString("redirect_url", "https://eformx.com/");
                    applySmartCacheStrategy(defaultUrl);
                    if (!hasLoadedAnyPageSuccessfully) {
                        webView.clearHistory();
                    }
                    webView.loadUrl(defaultUrl);
                }
            });
        }

        @android.webkit.JavascriptInterface
        public String getFcmToken() {
            return androidBridge != null ? androidBridge.getFcmToken() : "";
        }

        @android.webkit.JavascriptInterface
        public String getDeviceId() {
            return androidBridge != null ? androidBridge.getDeviceId() : "";
        }

        @android.webkit.JavascriptInterface
        public String getDeviceInfo() {
            return androidBridge != null ? androidBridge.getDeviceInfo() : "{}";
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
                    return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
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

    private long lastPresencePingTime = 0;

    private void registerNetworkCallback() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onLost(Network network) {
                        runOnUiThread(() -> {
                            if (webView != null) {
                                webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                            }
                        });
                    }

                    @Override
                    public void onAvailable(Network network) {
                        runOnUiThread(() -> {
                            if (webView != null) {
                                webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                            }
                            if (errorOverlay != null && errorOverlay.getVisibility() == View.VISIBLE) {
                                hideErrorOverlay();
                            }
                        });

                        long now = System.currentTimeMillis();
                        if (now - lastPresencePingTime > 5000) {
                            lastPresencePingTime = now;
                            MyFirebaseMessagingService.sendNetworkPresencePing(MainActivity.this, "data_connected");
                        }
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
        if (loadingWatchdogHandler != null) {
            loadingWatchdogHandler.removeCallbacksAndMessages(null);
        }
        if (androidBridge != null) {
            androidBridge.cleanup();
        }
        unregisterNetworkCallback();
        super.onDestroy();
    }

    public void showExitConfirmationDialog() {
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

    private void handleBackAction() {
        if (isErrorState || (errorOverlay != null && errorOverlay.getVisibility() == View.VISIBLE)) {
            showExitConfirmationDialog();
            return;
        }
        String currentUrl = (webView != null) ? webView.getUrl() : null;
        if (currentUrl != null && (currentUrl.startsWith("data:") || currentUrl.contains("error")
                || currentUrl.contains("android=exit"))) {
            showExitConfirmationDialog();
            return;
        }
        if (!hasLoadedAnyPageSuccessfully) {
            showExitConfirmationDialog();
            return;
        }
        navigateBackOrExit();
    }

    private void navigateBackOrExit() {
        if (webView == null || !webView.canGoBack()) {
            showExitConfirmationDialog();
            return;
        }

        try {
            android.webkit.WebBackForwardList history = webView.copyBackForwardList();
            int currentIndex = history.getCurrentIndex();
            int targetStep = 0;

            for (int i = currentIndex - 1; i >= 0; i--) {
                android.webkit.WebHistoryItem item = history.getItemAtIndex(i);
                if (item != null) {
                    String url = item.getUrl();
                    if (url != null && !url.startsWith("data:") && !url.contains("error")
                            && !url.contains("about:blank")) {
                        targetStep = i - currentIndex;
                        break;
                    }
                }
            }

            if (targetStep < 0 && webView.canGoBackOrForward(targetStep)) {
                isErrorState = false;
                webView.goBackOrForward(targetStep);
            } else {
                showExitConfirmationDialog();
            }
        } catch (Exception e) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                showExitConfirmationDialog();
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyStatusBarAppearance();
        checkGooglePlayAppUpdate();
    }

    public void checkGooglePlayAppUpdate() {
        try {
            if (appUpdateManager == null) {
                appUpdateManager = AppUpdateManagerFactory.create(this);
            }
            com.google.android.gms.tasks.Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                9001);
                    } catch (Exception ignored) {
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            MyFirebaseMessagingService.muteSoundOnly(this);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackAction();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void checkAndRequestAutoStartPermission() {
        try {
            String manufacturer = Build.MANUFACTURER.toLowerCase();
            Intent intent = new Intent();

            if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
                intent.setComponent(new android.content.ComponentName("com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if (manufacturer.contains("oppo")) {
                intent.setComponent(new android.content.ComponentName("com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if (manufacturer.contains("vivo")) {
                intent.setComponent(new android.content.ComponentName("com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
                intent.setComponent(new android.content.ComponentName("com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            } else if (manufacturer.contains("letv")) {
                intent.setComponent(new android.content.ComponentName("com.letv.android.letvsafe",
                        "com.letv.android.letvsafe.AutobootManageActivity"));
            } else if (manufacturer.contains("asus")) {
                intent.setComponent(new android.content.ComponentName("com.asus.mobilemanager",
                        "com.asus.mobilemanager.entry.FunctionActivity"));
            }

            if (intent.getComponent() != null && getPackageManager()
                    .queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY).size() > 0) {
                startActivity(intent);
            }
        } catch (Exception e) {
            android.util.Log.e("AutoStart", "Unable to open Auto-Start settings: " + e.getMessage());
        }
    }

    public void openNotificationSettings() {
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
            }
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
            } catch (Exception ex) {
                android.util.Log.e("Settings", "Unable to open notification settings: " + ex.getMessage());
            }
        }
    }

    public void requestNotificationPermissionExplicit() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(
                        android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
            }
            openNotificationSettings();
        } else {
            Toast.makeText(this, "Notification permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    public void checkAndRequestAllPermissions() {
        try {
            SharedPreferences prefs = getSharedPreferences(SecureConfig.getPrefsName(), MODE_PRIVATE);
            if (prefs.getBoolean("has_prompted_main_permissions", false)) {
                return;
            }
            prefs.edit().putBoolean("has_prompted_main_permissions", true).apply();

            java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!NotificationManagerCompat.from(this).areNotificationsEnabled() ||
                        checkSelfPermission(
                                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS);
                }
            }

            if (checkSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (checkSelfPermission(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (!permissionsToRequest.isEmpty()) {
                locationPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
            }
        } catch (Exception e) {
            android.util.Log.e("Permissions", "Error checking/requesting permissions: " + e.getMessage());
        }
    }

    private void applyStatusBarAppearance() {
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(),
                getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.show(WindowInsetsCompat.Type.statusBars());
            insetsController.setAppearanceLightStatusBars(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                insetsController.setAppearanceLightNavigationBars(true);
            }
        }
    }
}
