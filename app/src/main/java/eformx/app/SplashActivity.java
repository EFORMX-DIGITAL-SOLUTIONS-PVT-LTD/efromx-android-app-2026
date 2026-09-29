package eformx.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.provider.Settings;
import android.net.Uri;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashActivity extends Activity {

    private int currentSlide = 0;
    private HorizontalScrollView scrollView;
    private LinearLayout slidesLayout;
    private LinearLayout dotsLayout;
    private ProgressRingView progressRingView;
    private android.os.Handler splashHandler;
    private Runnable splashRunnable;

    private final String[][] slideTitles = {
        {"रियल एस्टेट, ऑनलाइन फॉर्म\nऔर IT सेवाएँ", "एक ही जगह"},
        {"रियल एस्टेट सेवाएँ", "प्रॉपर्टी खरीदें व बेचें"},
        {"IT एवं सॉफ्टवेयर सेवाएँ", "वेब और ऐप विकास"},
        {"ज़रूरी अनुमतियाँ", "और उनके लाभ"}
    };

    private final String[] slideSubtitles = {
        "आपकी ज़रूरतों के लिए डिजिटल समाधान\nअब और भी आसान, भरोसेमंद और तेज़।",
        "प्लॉट, मकान, दुकान और प्रॉपर्टी वेरिफिकेशन\nअब सुरक्षित, पारदर्शी और सबसे आसान।",
        "वेबसाइट, मोबाइल ऐप, बिलिंग और कस्टम सॉफ्टवेयर\nआपके व्यापार को दें आधुनिक डिजिटल पहचान।",
        "बिना किसी रुकावट के बेहतरीन सेवाओं के लिए\nनिम्नलिखित अनुमतियां देना आवश्यक है।"
    };

    private final String[] drawableNames = {
        "onboarding_slide1_form",
        "onboarding_slide2_speed",
        "onboarding_slide3_cafe",
        "onboarding_slide3_cafe"
    };

    private FrameLayout splashRootLayout;
    private volatile String redirectUrl = null;
    private final java.util.concurrent.atomic.AtomicBoolean hasTransitioned = new java.util.concurrent.atomic.AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isNetworkConnected()) {
            showNoInternetSplashView();
            return;
        }

        startSplashFlow();
    }

    private boolean isNetworkConnected() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.net.Network network = cm.getActiveNetwork();
                    if (network != null) {
                        android.net.NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                        return capabilities != null && (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                                || capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                                || capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
                    }
                } else {
                    android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    return activeNetwork != null && activeNetwork.isConnected();
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void showNoInternetSplashView() {
        try {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(Color.TRANSPARENT);

            FrameLayout root = new FrameLayout(this);
            root.setBackgroundColor(Color.parseColor("#F8FAFC"));

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(32));

            TextView iconTv = new TextView(this);
            iconTv.setText("📡");
            iconTv.setTextSize(48);
            iconTv.setGravity(Gravity.CENTER);
            card.addView(iconTv);

            TextView titleTv = new TextView(this);
            titleTv.setText("इंटरनेट कनेक्शन आवश्यक है");
            titleTv.setTextSize(20);
            titleTv.setTextColor(Color.parseColor("#0F172A"));
            titleTv.setTypeface(Typeface.DEFAULT_BOLD);
            titleTv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            titleParams.topMargin = dpToPx(16);
            card.addView(titleTv, titleParams);

            TextView msgTv = new TextView(this);
            msgTv.setText("ऐप शुरू करने के लिए कृपया अपने फोन का मोबाइल डेटा या वाई-फ़ाई ऑन करें।");
            msgTv.setTextSize(14);
            msgTv.setTextColor(Color.parseColor("#64748B"));
            msgTv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            msgParams.topMargin = dpToPx(8);
            card.addView(msgTv, msgParams);

            TextView retryBtn = new TextView(this);
            retryBtn.setText("पुनः प्रयास करें (Retry)");
            retryBtn.setTextSize(16);
            retryBtn.setTextColor(Color.WHITE);
            retryBtn.setTypeface(Typeface.DEFAULT_BOLD);
            retryBtn.setGravity(Gravity.CENTER);
            retryBtn.setPadding(dpToPx(32), dpToPx(14), dpToPx(32), dpToPx(14));

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(Color.parseColor("#0052FF"));
            btnBg.setCornerRadius(dpToPx(12));
            retryBtn.setBackground(btnBg);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnParams.topMargin = dpToPx(28);
            card.addView(retryBtn, btnParams);

            retryBtn.setOnClickListener(v -> {
                if (isNetworkConnected()) {
                    startSplashFlow();
                } else {
                    android.widget.Toast.makeText(SplashActivity.this, "इंटरनेट अभी भी बंद है। कृपया डेटा ऑन करें।", android.widget.Toast.LENGTH_SHORT).show();
                }
            });

            TextView settingsBtn = new TextView(this);
            settingsBtn.setText("नेटवर्क सेटिंग्स खोलें");
            settingsBtn.setTextSize(14);
            settingsBtn.setTextColor(Color.parseColor("#0052FF"));
            settingsBtn.setGravity(Gravity.CENTER);
            settingsBtn.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
            LinearLayout.LayoutParams setParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            setParams.topMargin = dpToPx(12);
            card.addView(settingsBtn, setParams);

            settingsBtn.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } catch (Exception e) {
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } catch (Exception ignored) {}
                }
            });

            FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.gravity = Gravity.CENTER;
            cardParams.setMargins(dpToPx(24), 0, dpToPx(24), 0);
            root.addView(card, cardParams);

            setContentView(root);
        } catch (Exception e) {
            startSplashFlow();
        }
    }

    private void startSplashFlow() {
        checkAppInstallApi();
        showBrandingSplashScreen();

        splashHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        splashRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;
            if (splashRootLayout != null) {
                try {
                    splashRootLayout.animate()
                            .alpha(0f)
                            .setDuration(250)
                            .start();
                } catch (Exception ignored) {}
            }
            transitionFromSplash();
        };
        splashHandler.postDelayed(splashRunnable, 1600);

        splashHandler.postDelayed(this::transitionFromSplash, 2200);
    }

    private void transitionFromSplash() {
        if (hasTransitioned.getAndSet(true)) {
            return;
        }
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }

        SharedPreferences prefs = getSharedPreferences(SecureConfig.getPrefsName(), MODE_PRIVATE);
        boolean hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false);
        if (getIntent().getBooleanExtra("reset_onboarding", false)) {
            hasSeenOnboarding = false;
        }

        if (hasSeenOnboarding) {
            launchMainActivity();
        } else {
            setupOnboardingUi();
        }
    }

    private void showBrandingSplashScreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.show(WindowInsetsCompat.Type.statusBars());
            insetsController.setAppearanceLightStatusBars(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                insetsController.setAppearanceLightNavigationBars(true);
            }
        }

        splashRootLayout = new FrameLayout(this);
        splashRootLayout.setBackgroundColor(Color.parseColor("#F4F8FF"));
        splashRootLayout.setClickable(true);
        splashRootLayout.setFocusable(true);
        splashRootLayout.setOnClickListener(v -> transitionFromSplash());

        AmbientBackgroundView ambientBgView = new AmbientBackgroundView(this);
        splashRootLayout.addView(ambientBgView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ambientBgView.setAlpha(0.6f);
        ambientBgView.animate()
                .alpha(1.0f)
                .setDuration(1200)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .start();

        BottomWaveView waveView = new BottomWaveView(this);
        FrameLayout.LayoutParams waveParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(210));
        waveParams.gravity = Gravity.BOTTOM;
        splashRootLayout.addView(waveView, waveParams);

        waveView.setTranslationY(dpToPx(60));
        waveView.animate()
                .translationY(0f)
                .setDuration(800)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        LinearLayout centerLayout = new LinearLayout(this);
        centerLayout.setOrientation(LinearLayout.VERTICAL);
        centerLayout.setGravity(Gravity.CENTER);

        FrameLayout logoWrapper = new FrameLayout(this);
        int logoSize = dpToPx(110);
        LinearLayout.LayoutParams logoWrapperParams = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoWrapperParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoWrapperParams.bottomMargin = dpToPx(20);

        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setShape(GradientDrawable.OVAL);
        logoBg.setColor(Color.WHITE);
        logoWrapper.setBackground(logoBg);
        logoWrapper.setElevation(dpToPx(3));

        ImageView logoImageView = new ImageView(this);
        logoImageView.setImageResource(R.mipmap.ic_launcher);
        int logoPadding = dpToPx(8);
        logoImageView.setPadding(logoPadding, logoPadding, logoPadding, logoPadding);
        logoWrapper.addView(logoImageView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        centerLayout.addView(logoWrapper, logoWrapperParams);

        logoWrapper.setAlpha(0f);
        logoWrapper.setScaleX(0.3f);
        logoWrapper.setScaleY(0.3f);
        logoWrapper.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(750)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                .start();

        TextView titleTv = new TextView(this);
        titleTv.setText("eFormX");
        titleTv.setTextSize(32);
        titleTv.setTextColor(Color.parseColor("#0F172A"));
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.gravity = Gravity.CENTER_HORIZONTAL;
        titleParams.bottomMargin = dpToPx(4);
        centerLayout.addView(titleTv, titleParams);

        titleTv.setAlpha(0f);
        titleTv.setTranslationY(dpToPx(35));
        titleTv.animate()
                .alpha(1.0f)
                .translationY(0f)
                .setStartDelay(220)
                .setDuration(600)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        TextView taglineTv = new TextView(this);
        taglineTv.setText("Digital Solutions");
        taglineTv.setTextSize(14);
        taglineTv.setTextColor(Color.parseColor("#64748B"));
        taglineTv.setGravity(Gravity.CENTER);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            taglineTv.setLetterSpacing(0.06f);
        }
        LinearLayout.LayoutParams taglineParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        taglineParams.gravity = Gravity.CENTER_HORIZONTAL;
        centerLayout.addView(taglineTv, taglineParams);

        taglineTv.setAlpha(0f);
        taglineTv.setTranslationY(dpToPx(25));
        taglineTv.animate()
                .alpha(1.0f)
                .translationY(0f)
                .setStartDelay(380)
                .setDuration(600)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setIndeterminate(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0052FF")));
        }
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dpToPx(34), dpToPx(34));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = dpToPx(36);
        centerLayout.addView(progressBar, progressParams);

        progressBar.setAlpha(0f);
        progressBar.animate()
                .alpha(1.0f)
                .setStartDelay(550)
                .setDuration(500)
                .start();

        FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.gravity = Gravity.CENTER;
        splashRootLayout.addView(centerLayout, centerParams);

        setContentView(splashRootLayout);
    }

    private void setupOnboardingUi() {
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.show(WindowInsetsCompat.Type.statusBars());
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(false);
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#F4F8FF"));

        // 1. Top Side Background Ambient Graphics (Translucent Orbs & Waves)
        AmbientBackgroundView ambientBgView = new AmbientBackgroundView(this);
        rootLayout.addView(ambientBgView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 2. Bottom Decorative Wavy Curve View
        BottomWaveView waveView = new BottomWaveView(this);
        FrameLayout.LayoutParams waveParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(220));
        waveParams.gravity = Gravity.BOTTOM;
        rootLayout.addView(waveView, waveParams);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Top Header Bar with Skip Button
        FrameLayout topBar = new FrameLayout(this);
        LinearLayout.LayoutParams topBarParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topBarParams.setMargins(dpToPx(16), dpToPx(4), dpToPx(16), dpToPx(4));
        topBar.setLayoutParams(topBarParams);

        TextView skipBtn = new TextView(this);
        skipBtn.setText("Skip ➔");
        skipBtn.setTextSize(13);
        skipBtn.setTextColor(Color.parseColor("#475569"));
        skipBtn.setTypeface(Typeface.DEFAULT_BOLD);
        skipBtn.setPadding(dpToPx(14), dpToPx(6), dpToPx(14), dpToPx(6));
        GradientDrawable skipBg = new GradientDrawable();
        skipBg.setColor(Color.parseColor("#E2E8F0"));
        skipBg.setCornerRadius(dpToPx(14));
        skipBtn.setBackground(skipBg);
        FrameLayout.LayoutParams skipLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        skipLp.gravity = Gravity.END;
        topBar.addView(skipBtn, skipLp);
        skipBtn.setOnClickListener(v -> proceedAfterPermission());

        contentLayout.addView(topBar);

        // Center HorizontalScrollView Area for 4 Slides
        scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        slidesLayout = new LinearLayout(this);
        slidesLayout.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(slidesLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        for (int i = 0; i < 4; i++) {
            slidesLayout.addView(createSlideView(i));
        }

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);

        // Bottom Controls Container (Large Prominent Dots + Red Progress Ring + Arrow Button)
        LinearLayout bottomLayout = new LinearLayout(this);
        bottomLayout.setOrientation(LinearLayout.VERTICAL);
        bottomLayout.setGravity(Gravity.CENTER);
        bottomLayout.setPadding(0, 0, 0, 0);

        // Prominent Dot Indicators
        dotsLayout = new LinearLayout(this);
        dotsLayout.setOrientation(LinearLayout.HORIZONTAL);
        dotsLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dotsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dotsParams.bottomMargin = dpToPx(12);

        // Progress Ring & Floating Button Container
        FrameLayout progressButtonWrapper = new FrameLayout(this);
        int wrapperSize = dpToPx(80);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(wrapperSize, wrapperSize);
        wrapperParams.gravity = Gravity.CENTER;

        // Custom Red Progress Ring View
        progressRingView = new ProgressRingView(this);
        FrameLayout.LayoutParams ringParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        progressButtonWrapper.addView(progressRingView, ringParams);

        // Inner Circular White Arrow Button (60dp size)
        FrameLayout nextBtnContainer = new FrameLayout(this);
        int innerBtnSize = dpToPx(60);
        FrameLayout.LayoutParams innerBtnParams = new FrameLayout.LayoutParams(innerBtnSize, innerBtnSize);
        innerBtnParams.gravity = Gravity.CENTER;

        GradientDrawable nextBtnBg = new GradientDrawable();
        nextBtnBg.setShape(GradientDrawable.OVAL);
        nextBtnBg.setColor(Color.WHITE);
        nextBtnContainer.setBackground(nextBtnBg);
        nextBtnContainer.setElevation(dpToPx(8));

        TextView nextArrow = new TextView(this);
        nextArrow.setText("➔");
        nextArrow.setTextSize(26);
        nextArrow.setTextColor(Color.parseColor("#0052FF"));
        nextArrow.setTypeface(Typeface.DEFAULT_BOLD);
        nextArrow.setGravity(Gravity.CENTER);
        nextBtnContainer.addView(nextArrow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        progressButtonWrapper.addView(nextBtnContainer, innerBtnParams);

        bottomLayout.addView(dotsLayout, dotsParams);
        bottomLayout.addView(progressButtonWrapper, wrapperParams);

        contentLayout.addView(scrollView, scrollParams);
        contentLayout.addView(bottomLayout, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        rootLayout.addView(contentLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            int topPad = Math.max(insets.top, dpToPx(24)) + dpToPx(6);
            int bottomPad = Math.max(insets.bottom, dpToPx(16)) + dpToPx(12);
            contentLayout.setPadding(0, topPad, 0, bottomPad);

            FrameLayout.LayoutParams waveLp = (FrameLayout.LayoutParams) waveView.getLayoutParams();
            if (waveLp != null) {
                waveLp.height = bottomPad + dpToPx(180);
                waveView.setLayoutParams(waveLp);
            }
            return windowInsets;
        });

        setContentView(rootLayout);

        // Touch Gesture Snap Controller
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            private float startX = 0f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float endX = event.getRawX();
                        float deltaX = endX - startX;
                        int threshold = screenWidth / 8;
                        if (deltaX < -threshold && currentSlide < 3) {
                            currentSlide++;
                        } else if (deltaX > threshold && currentSlide > 0) {
                            currentSlide--;
                        }
                        scrollView.post(() -> scrollView.smoothScrollTo(currentSlide * screenWidth, 0));
                        updateDots(currentSlide);
                        progressRingView.setProgressDirect((currentSlide + 1) / 4.0f);
                        return true;
                }
                return false;
            }
        });

        // Track real-time progress while scrolling
        scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            int pageWidth = getResources().getDisplayMetrics().widthPixels;
            if (pageWidth > 0) {
                float floatPos = (float) scrollX / pageWidth;
                float progress = Math.min(1.0f, Math.max(0.25f, (floatPos + 1.0f) / 4.0f));
                progressRingView.setProgressDirect(progress);
            }
        });

        updateDots(0);
        progressRingView.setProgressDirect(1.0f / 4.0f);

        View.OnClickListener finishOnboarding = v -> {
            java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS);
                }
            }

            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (!permissionsToRequest.isEmpty()) {
                requestPermissions(permissionsToRequest.toArray(new String[0]), 101);
            } else {
                proceedAfterPermission();
            }
        };

        nextBtnContainer.setOnClickListener(v -> {
            if (currentSlide < 3) {
                currentSlide++;
                int pageWidth = getResources().getDisplayMetrics().widthPixels;
                scrollView.smoothScrollTo(currentSlide * pageWidth, 0);
                updateDots(currentSlide);
                progressRingView.setProgressDirect((currentSlide + 1) / 4.0f);
            } else {
                finishOnboarding.onClick(v);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    private View createSlideView(int index) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        LinearLayout slideContainer = new LinearLayout(this);
        slideContainer.setOrientation(LinearLayout.VERTICAL);
        slideContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        slideContainer.setPadding(dpToPx(16), 0, dpToPx(16), 0);
        slideContainer.setLayoutParams(new LinearLayout.LayoutParams(
                screenWidth, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Top Logo Header Badge
        LinearLayout logoHeader = new LinearLayout(this);
        logoHeader.setOrientation(LinearLayout.VERTICAL);
        logoHeader.setGravity(Gravity.CENTER);
        logoHeader.setPadding(0, 0, 0, dpToPx(4));

        TextView logoTitleTv = new TextView(this);
        logoTitleTv.setText("EX EFORMX");
        logoTitleTv.setTextSize(22);
        logoTitleTv.setTextColor(Color.parseColor("#0052FF"));
        logoTitleTv.setTypeface(Typeface.DEFAULT_BOLD);
        logoTitleTv.setGravity(Gravity.CENTER);

        // Sub Tagline: ── भारत का 1ONE DIGITAL CAFE ──
        LinearLayout taglineLayout = new LinearLayout(this);
        taglineLayout.setOrientation(LinearLayout.HORIZONTAL);
        taglineLayout.setGravity(Gravity.CENTER);

        TextView tagLeft = new TextView(this);
        tagLeft.setText("──  भारत का ");
        tagLeft.setTextSize(11);
        tagLeft.setTextColor(Color.parseColor("#1E293B"));
        tagLeft.setTypeface(Typeface.DEFAULT_BOLD);

        TextView tagPill = new TextView(this);
        tagPill.setText("1ONE");
        tagPill.setTextSize(10);
        tagPill.setTextColor(Color.WHITE);
        tagPill.setTypeface(Typeface.DEFAULT_BOLD);
        tagPill.setPadding(dpToPx(5), dpToPx(1), dpToPx(5), dpToPx(1));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(Color.parseColor("#FF6B00"));
        pillBg.setCornerRadius(dpToPx(10));
        tagPill.setBackground(pillBg);

        TextView tagRight = new TextView(this);
        tagRight.setText(" DIGITAL CAFE  ──");
        tagRight.setTextSize(11);
        tagRight.setTextColor(Color.parseColor("#0052FF"));
        tagRight.setTypeface(Typeface.DEFAULT_BOLD);

        taglineLayout.addView(tagLeft);
        taglineLayout.addView(tagPill);
        taglineLayout.addView(tagRight);

        logoHeader.addView(logoTitleTv);
        logoHeader.addView(taglineLayout);
        slideContainer.addView(logoHeader);

        // 2. Main Title (Navy & Orange)
        TextView titleNavyTv = new TextView(this);
        titleNavyTv.setText(slideTitles[index][0]);
        titleNavyTv.setTextSize(18);
        titleNavyTv.setTextColor(Color.parseColor("#0D1B2A"));
        titleNavyTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleNavyTv.setGravity(Gravity.CENTER);

        TextView titleOrangeTv = new TextView(this);
        titleOrangeTv.setText(slideTitles[index][1]);
        titleOrangeTv.setTextSize(18);
        titleOrangeTv.setTextColor(Color.parseColor("#FF6B00"));
        titleOrangeTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleOrangeTv.setGravity(Gravity.CENTER);

        slideContainer.addView(titleNavyTv);
        slideContainer.addView(titleOrangeTv);

        // 3. Subtitle
        TextView subtitleTv = new TextView(this);
        subtitleTv.setText(slideSubtitles[index]);
        subtitleTv.setTextSize(12);
        subtitleTv.setTextColor(Color.parseColor("#64748B"));
        subtitleTv.setGravity(Gravity.CENTER);
        subtitleTv.setLineSpacing(dpToPx(2), 1.0f);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dpToPx(2);
        subParams.bottomMargin = dpToPx(6);
        slideContainer.addView(subtitleTv, subParams);

        // 4. Middle Graphic Card
        View cardGraphicView = createCardGraphicForSlide(index);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        cardParams.leftMargin = dpToPx(4);
        cardParams.rightMargin = dpToPx(4);
        cardParams.bottomMargin = dpToPx(12);
        slideContainer.addView(cardGraphicView, cardParams);

        return slideContainer;
    }

    private View createCardGraphicForSlide(int slideIndex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(dpToPx(20));
        cardBg.setStroke(dpToPx(1), Color.parseColor("#E2E8F0"));
        card.setBackground(cardBg);
        card.setElevation(dpToPx(6));

        if (slideIndex == 0) {
            // Header: Stars + भारत का 1ONE DIGITAL CAFE + Tagline
            LinearLayout headerLayout = new LinearLayout(this);
            headerLayout.setOrientation(LinearLayout.VERTICAL);
            headerLayout.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            headerParams.bottomMargin = dpToPx(6);
            headerLayout.setLayoutParams(headerParams);

            TextView starsTv = new TextView(this);
            starsTv.setText("⭐⭐⭐⭐⭐");
            starsTv.setTextSize(11);
            starsTv.setGravity(Gravity.CENTER);
            headerLayout.addView(starsTv);

            LinearLayout cafeRow = new LinearLayout(this);
            cafeRow.setOrientation(LinearLayout.HORIZONTAL);
            cafeRow.setGravity(Gravity.CENTER);
            cafeRow.setPadding(0, dpToPx(2), 0, dpToPx(2));

            TextView cafeLeft = new TextView(this);
            cafeLeft.setText("भारत का ");
            cafeLeft.setTextSize(12);
            cafeLeft.setTextColor(Color.parseColor("#0052FF"));
            cafeLeft.setTypeface(Typeface.DEFAULT_BOLD);

            TextView cafePill = new TextView(this);
            cafePill.setText("1ONE");
            cafePill.setTextSize(9);
            cafePill.setTextColor(Color.WHITE);
            cafePill.setTypeface(Typeface.DEFAULT_BOLD);
            cafePill.setPadding(dpToPx(4), dpToPx(1), dpToPx(4), dpToPx(1));
            GradientDrawable cafePillBg = new GradientDrawable();
            cafePillBg.setColor(Color.parseColor("#FF6B00"));
            cafePillBg.setCornerRadius(dpToPx(6));
            cafePill.setBackground(cafePillBg);

            TextView cafeRight = new TextView(this);
            cafeRight.setText(" DIGITAL CAFE");
            cafeRight.setTextSize(12);
            cafeRight.setTextColor(Color.parseColor("#0052FF"));
            cafeRight.setTypeface(Typeface.DEFAULT_BOLD);

            cafeRow.addView(cafeLeft);
            cafeRow.addView(cafePill);
            cafeRow.addView(cafeRight);
            headerLayout.addView(cafeRow);

            TextView audienceTv = new TextView(this);
            audienceTv.setText("लोगों के लिए  •  व्यवसाय के लिए  •  बेहतर भारत के लिए");
            audienceTv.setTextSize(10);
            audienceTv.setTextColor(Color.parseColor("#64748B"));
            audienceTv.setGravity(Gravity.CENTER);
            headerLayout.addView(audienceTv);

            card.addView(headerLayout);

            // Generated 3D HD Illustration Image View
            ImageView illustrationImg = new ImageView(this);
            illustrationImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
            illustrationImg.setAdjustViewBounds(true);
            illustrationImg.setImageResource(R.drawable.onboarding_slide1_form);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
            imgParams.gravity = Gravity.CENTER;
            imgParams.bottomMargin = dpToPx(6);
            card.addView(illustrationImg, imgParams);

            // Chips Row: ⚡ तेज़ | ✔ सुविधाजनक | 🛡️ विश्वसनीय
            LinearLayout chipsLayout = new LinearLayout(this);
            chipsLayout.setOrientation(LinearLayout.HORIZONTAL);
            chipsLayout.setGravity(Gravity.CENTER);

            chipsLayout.addView(createChip("⚡  तेज़"));
            chipsLayout.addView(createChip("✔  सुविधाजनक"));
            chipsLayout.addView(createChip("🛡️  विश्वसनीय"));

            card.addView(chipsLayout);
        } else if (slideIndex == 1) {
            // Generated 3D HD Illustration Image View (Real Estate)
            ImageView illustrationImg = new ImageView(this);
            illustrationImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
            illustrationImg.setAdjustViewBounds(true);
            illustrationImg.setImageResource(R.drawable.onboarding_slide2_speed);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
            imgParams.gravity = Gravity.CENTER;
            imgParams.bottomMargin = dpToPx(6);
            card.addView(illustrationImg, imgParams);

            // 4 Grid Badges: प्रॉपर्टी खरीद-बिक्री, दस्तावेज़ वेरिफिकेशन, आसान रेंटल सेवा, 100% सुरक्षित डील
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER);

            row1.addView(createGridItem("🏠", "प्रॉपर्टी खरीद-बिक्री"));
            row1.addView(createGridItem("📑", "दस्तावेज़ वेरिफिकेशन"));

            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setGravity(Gravity.CENTER);
            row2.setPadding(0, dpToPx(6), 0, 0);

            row2.addView(createGridItem("🔑", "आसान रेंटल सेवा"));
            row2.addView(createGridItem("🛡️", "100% सुरक्षित डील"));

            card.addView(row1);
            card.addView(row2);
        } else if (slideIndex == 2) {
            // 4 Service Icons Row
            LinearLayout iconRow = new LinearLayout(this);
            iconRow.setOrientation(LinearLayout.HORIZONTAL);
            iconRow.setGravity(Gravity.CENTER);

            iconRow.addView(createIconBadge("💻", "वेबसाइट"));
            iconRow.addView(createIconBadge("📱", "मोबाइल ऐप"));
            iconRow.addView(createIconBadge("⚙️", "सॉफ्टवेयर"));
            iconRow.addView(createIconBadge("☁️", "क्लाउड"));

            card.addView(iconRow);

            // Generated 3D HD Illustration Image View (IT Services)
            ImageView illustrationImg = new ImageView(this);
            illustrationImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
            illustrationImg.setAdjustViewBounds(true);
            illustrationImg.setImageResource(R.drawable.onboarding_slide3_cafe);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
            imgParams.gravity = Gravity.CENTER;
            imgParams.topMargin = dpToPx(4);
            imgParams.bottomMargin = dpToPx(4);
            card.addView(illustrationImg, imgParams);

            // Tricolor Banner Ribbon
            TextView ribbonTv = new TextView(this);
            ribbonTv.setText("──  वेबसाइट  •  मोबाइल ऐप  •  कस्टम सॉफ्टवेयर  ──");
            ribbonTv.setTextSize(12);
            ribbonTv.setTextColor(Color.parseColor("#0052FF"));
            ribbonTv.setTypeface(Typeface.DEFAULT_BOLD);
            ribbonTv.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams ribbonParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            card.addView(ribbonTv, ribbonParams);
        } else {
            // Slide 4: Permissions & Benefits Card with Interactive Allow Buttons
            LinearLayout permList = new LinearLayout(this);
            permList.setOrientation(LinearLayout.VERTICAL);
            permList.setGravity(Gravity.CENTER_VERTICAL);
            permList.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));

            boolean hasNotification = NotificationManagerCompat.from(this).areNotificationsEnabled();

            boolean hasLocation = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                                  checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;

            final boolean finalHasNotify = hasNotification;
            permList.addView(createPermissionRow("🔔", "नोटिफिकेशन (Notifications)", "कॉल अलर्ट और अपडेट तुरंत पाने के लिए।", hasNotification, v -> {
                if (!finalHasNotify) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
                    } else {
                        openNotificationSettings();
                    }
                }
            }));

            final boolean finalHasLoc = hasLocation;
            permList.addView(createPermissionRow("📍", "स्थान (GPS Location)", "स्थान सेवाओं के लिए लोकेशन उपयोग करने के लिए।", hasLocation, v -> {
                if (!finalHasLoc) {
                    requestPermissions(new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 103);
                }
            }));

            boolean hasAutoStart = getSharedPreferences("eformx_prefs", MODE_PRIVATE).getBoolean("has_prompted_autostart", false);
            permList.addView(createPermissionRow("⚡", "ऑटो-स्टार्ट (Auto-Start)", "ऐप बंद होने पर भी कॉल और अलर्ट बजने के लिए।", hasAutoStart, v -> {
                forceOpenAutoStartSettings();
            }));

            // Prominent "Allow All Permissions / अनुमति दें ➔" Action Button inside card
            TextView allowAllBtn = new TextView(this);
            boolean allGranted = finalHasNotify && finalHasLoc;
            allowAllBtn.setText(allGranted ? "Continue to App  ➔" : "Allow All Permissions (अनुमति दें)  ➔");
            allowAllBtn.setTextSize(13);
            allowAllBtn.setTextColor(Color.WHITE);
            allowAllBtn.setTypeface(Typeface.DEFAULT_BOLD);
            allowAllBtn.setGravity(Gravity.CENTER);
            allowAllBtn.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(Color.parseColor(allGranted ? "#059669" : "#0052FF"));
            btnBg.setCornerRadius(dpToPx(14));
            allowAllBtn.setBackground(btnBg);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnParams.topMargin = dpToPx(8);
            allowAllBtn.setLayoutParams(btnParams);

            allowAllBtn.setOnClickListener(v -> {
                java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS);
                    }
                }
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
                }
                if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                }
                if (!permissionsToRequest.isEmpty()) {
                    requestPermissions(permissionsToRequest.toArray(new String[0]), 101);
                } else {
                    proceedAfterPermission();
                }
            });

            android.widget.ScrollView permScroll = new android.widget.ScrollView(this);
            permScroll.setVerticalScrollBarEnabled(false);
            permScroll.setFillViewport(true);
            permScroll.addView(permList);
            card.addView(permScroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
            card.addView(allowAllBtn);
        }

        return card;
    }

    private View createChip(String text) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(11);
        chip.setTextColor(Color.parseColor("#1E293B"));
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setPadding(dpToPx(8), dpToPx(5), dpToPx(8), dpToPx(5));

        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(Color.parseColor("#F1F5F9"));
        chipBg.setCornerRadius(dpToPx(14));
        chipBg.setStroke(dpToPx(1), Color.parseColor("#E2E8F0"));
        chip.setBackground(chipBg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dpToPx(3);
        params.rightMargin = dpToPx(3);
        chip.setLayoutParams(params);
        return chip;
    }

    private View createGridItem(String icon, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));

        GradientDrawable itemBg = new GradientDrawable();
        itemBg.setColor(Color.parseColor("#F8FAFC"));
        itemBg.setCornerRadius(dpToPx(10));
        itemBg.setStroke(dpToPx(1), Color.parseColor("#E2E8F0"));
        item.setBackground(itemBg);

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(13);

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextSize(11);
        labelTv.setTextColor(Color.parseColor("#1E293B"));
        labelTv.setTypeface(Typeface.DEFAULT_BOLD);
        labelTv.setPadding(dpToPx(5), 0, 0, 0);

        item.addView(iconTv);
        item.addView(labelTv);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        params.leftMargin = dpToPx(3);
        params.rightMargin = dpToPx(3);
        item.setLayoutParams(params);
        return item;
    }

    private View createIconBadge(String icon, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(20);
        iconTv.setGravity(Gravity.CENTER);

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextSize(10);
        labelTv.setTextColor(Color.parseColor("#475569"));
        labelTv.setGravity(Gravity.CENTER);

        item.addView(iconTv);
        item.addView(labelTv);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        item.setLayoutParams(params);
        return item;
    }

    private View createPermissionRow(String icon, String title, String benefit, boolean isGranted, View.OnClickListener onAllowClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(Color.parseColor("#F8FAFC"));
        rowBg.setCornerRadius(dpToPx(12));
        rowBg.setStroke(dpToPx(1), Color.parseColor("#E2E8F0"));
        row.setBackground(rowBg);

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(20);
        iconTv.setGravity(Gravity.CENTER);

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setPadding(dpToPx(10), 0, dpToPx(6), 0);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextSize(12);
        titleTv.setTextColor(Color.parseColor("#0D1B2A"));
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);

        TextView benefitTv = new TextView(this);
        benefitTv.setText(benefit);
        benefitTv.setTextSize(11);
        benefitTv.setTextColor(Color.parseColor("#64748B"));

        textContainer.addView(titleTv);
        textContainer.addView(benefitTv);

        TextView actionBtn = new TextView(this);
        actionBtn.setTextSize(11);
        actionBtn.setTypeface(Typeface.DEFAULT_BOLD);
        actionBtn.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));

        GradientDrawable btnBg = new GradientDrawable();
        if (isGranted) {
            actionBtn.setText("✔ Allowed");
            actionBtn.setTextColor(Color.parseColor("#059669"));
            btnBg.setColor(Color.parseColor("#E6F4EA"));
            btnBg.setCornerRadius(dpToPx(10));
        } else {
            actionBtn.setText("Allow >");
            actionBtn.setTextColor(Color.parseColor("#FF6B00"));
            btnBg.setColor(Color.parseColor("#FFF3E0"));
            btnBg.setCornerRadius(dpToPx(10));
            btnBg.setStroke(dpToPx(1), Color.parseColor("#FFE0B2"));
            actionBtn.setOnClickListener(onAllowClick);
        }
        actionBtn.setBackground(btnBg);

        row.addView(iconTv);
        row.addView(textContainer, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(actionBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dpToPx(4);
        params.bottomMargin = dpToPx(4);
        row.setLayoutParams(params);
        return row;
    }

    private void updateDots(int position) {
        dotsLayout.removeAllViews();
        for (int i = 0; i < 4; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams dParam;
            GradientDrawable dBg = new GradientDrawable();
            if (i == position) {
                dParam = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(7));
                dBg.setCornerRadius(dpToPx(4));
                dBg.setColor(Color.WHITE);
            } else {
                dParam = new LinearLayout.LayoutParams(dpToPx(7), dpToPx(7));
                dBg.setCornerRadius(dpToPx(4));
                dBg.setColor(Color.parseColor("#99FFFFFF"));
            }
            dParam.leftMargin = dpToPx(4);
            dParam.rightMargin = dpToPx(4);
            dot.setBackground(dBg);
            dotsLayout.addView(dot, dParam);
        }
    }

    private void proceedAfterPermission() {
        SharedPreferences prefs = getSharedPreferences("eformx_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("has_seen_onboarding", true).apply();
        launchMainActivity();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            proceedAfterPermission();
        } else if (requestCode == 102 || requestCode == 103) {
            refreshSlide4();
        }
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
            } catch (Exception ignored) {}
        }
    }

    private void refreshSlide4() {
        if (slidesLayout != null && slidesLayout.getChildCount() > 3) {
            slidesLayout.removeViewAt(3);
            View newSlide = createSlideView(3);
            slidesLayout.addView(newSlide, 3);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentSlide == 3 && slidesLayout != null) {
            refreshSlide4();
        }
    }

    private void checkAndRequestAutoStartPermission() {
        SharedPreferences prefs = getSharedPreferences("eformx_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("has_prompted_autostart", false)) {
            return;
        }
        forceOpenAutoStartSettings();
    }

    private void forceOpenAutoStartSettings() {
        try {
            SharedPreferences prefs = getSharedPreferences("eformx_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("has_prompted_autostart", true).apply();

            String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
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

    private void launchMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        String urlToLoad = (redirectUrl != null && !redirectUrl.isEmpty())
                ? redirectUrl
                : getSharedPreferences("eformx_prefs", MODE_PRIVATE).getString("redirect_url", null);
        if (urlToLoad != null && !urlToLoad.isEmpty()) {
            intent.putExtra("target_url", urlToLoad);
        }
        startActivity(intent);
        finish();
    }

    private void checkAppInstallApi() {
        new Thread(() -> {
            java.net.HttpURLConnection conn = null;
            try {
                java.net.URL url = new java.net.URL(SecureConfig.getInstallApiUrl());
                conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader in = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    org.json.JSONObject json = new org.json.JSONObject(response.toString());
                    if (json.optBoolean("status", false) || json.optInt("status_code", 0) == 200) {
                        org.json.JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            SharedPreferences prefs = getSharedPreferences("eformx_prefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();

                            // 1. Parse redirect_url
                            if (data.has("redirect_url")) {
                                String urlString = data.optString("redirect_url", "").trim();
                                if (!urlString.isEmpty()) {
                                    redirectUrl = urlString;
                                    editor.putString("redirect_url", redirectUrl);
                                }
                            }

                            // 2. Parse version & compare with saved version
                            if (data.has("version")) {
                                String apiVersion = data.optString("version", "").trim();
                                if (!apiVersion.isEmpty()) {
                                    String savedVersion = prefs.getString("api_server_version", "");
                                    if (savedVersion.isEmpty()) {
                                        // First time saving
                                        editor.putString("api_server_version", apiVersion);
                                    } else if (!savedVersion.equalsIgnoreCase(apiVersion)) {
                                        // Version changed! Mark cache flush needed
                                        editor.putString("api_server_version", apiVersion);
                                        editor.putBoolean("cache_flush_needed", true);
                                    }
                                }
                            }

                            // 3. Parse catche_url array (whitelist for caching)
                            if (data.has("catche_url")) {
                                org.json.JSONArray cacheArray = data.optJSONArray("catche_url");
                                if (cacheArray != null) {
                                    java.util.HashSet<String> cacheSet = new java.util.HashSet<>();
                                    for (int i = 0; i < cacheArray.length(); i++) {
                                        String cUrl = cacheArray.optString(i, "").trim();
                                        if (!cUrl.isEmpty()) {
                                            cacheSet.add(cUrl);
                                        }
                                    }
                                    if (redirectUrl != null && !redirectUrl.trim().isEmpty()) {
                                        cacheSet.add(redirectUrl.trim());
                                    }
                                    editor.putStringSet("catche_url_whitelist", cacheSet);
                                    editor.putString("catche_url_json", cacheArray.toString());
                                }
                            }

                            editor.commit();
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("SplashActivity", "Error checking install app API: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // Custom Red Circular Progress Ring View around Next Button
    private class ProgressRingView extends View {
        private Paint trackPaint;
        private Paint progressPaint;
        private float progress = 0.25f;

        public ProgressRingView(Context context) {
            super(context);
            init();
        }

        private void init() {
            trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeWidth(dpToPx(3.5f));
            trackPaint.setColor(Color.parseColor("#40FFFFFF")); // Translucent white track

            progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            progressPaint.setStyle(Paint.Style.STROKE);
            progressPaint.setStrokeWidth(dpToPx(4.5f));
            progressPaint.setColor(Color.parseColor("#FF3B30")); // Vibrant Bright Red Ring
            progressPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        public void setProgressDirect(float newProgress) {
            this.progress = newProgress;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float strokeWidth = dpToPx(4.5f);
            float halfStroke = strokeWidth / 2f;
            RectF rect = new RectF(
                    halfStroke, halfStroke,
                    getWidth() - halfStroke, getHeight() - halfStroke
            );
            canvas.drawOval(rect, trackPaint);
            float sweepAngle = progress * 360f;
            canvas.drawArc(rect, -90, sweepAngle, false, progressPaint);
        }
    }

    // Top Side Ambient Background View (Translucent Blue & Orange Glowing Orbs)
    private class AmbientBackgroundView extends View {
        private Paint paint1;
        private Paint paint2;

        public AmbientBackgroundView(Context context) {
            super(context);
            paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint1.setColor(Color.parseColor("#E0EDFF")); // Translucent soft blue

            paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint2.setColor(Color.parseColor("#FFF0E6")); // Translucent soft orange
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();

            // Top-Left Soft Blue Circle
            canvas.drawCircle(dpToPx(20), dpToPx(40), dpToPx(140), paint1);

            // Top-Right Soft Orange Circle
            canvas.drawCircle(w - dpToPx(10), dpToPx(100), dpToPx(120), paint2);
        }
    }

    // Custom Canvas View for Drawing Blue Wavy Gradient Curve at Bottom
    private class BottomWaveView extends View {
        private Paint wavePaint;

        public BottomWaveView(Context context) {
            super(context);
            init();
        }

        private void init() {
            wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wavePaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            android.graphics.LinearGradient gradient = new android.graphics.LinearGradient(
                    0, 0, w, h,
                    Color.parseColor("#0052FF"),
                    Color.parseColor("#0A3EB8"),
                    android.graphics.Shader.TileMode.CLAMP
            );
            wavePaint.setShader(gradient);

            Path path = new Path();
            path.moveTo(0, h * 0.28f);
            path.cubicTo(w * 0.28f, h * 0.08f, w * 0.68f, h * 0.38f, w, h * 0.18f);
            path.lineTo(w, h);
            path.lineTo(0, h);
            path.close();

            canvas.drawPath(path, wavePaint);
        }
    }
}
