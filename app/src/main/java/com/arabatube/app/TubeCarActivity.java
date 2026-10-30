package com.arabatube.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;

import java.util.Locale;

/** Android Auto ekranında çalışan YouTube görünümü. */
public class TubeCarActivity extends CarActivity {

    static final String HOME = "https://m.youtube.com/";
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;

    private final Handler main = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    private WebView web;
    private LinearLayout bar;
    private FrameLayout fullscreenHolder;
    private TextView barToggle;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private float zoom;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setIgnoreConfigChanges(0xFFFFFFFF);

        // Android Auto'nun kendi başlık çubuğunu ve menü düğmesini gizle → daha fazla alan
        CarUiController ui = getCarUiController();
        ui.getStatusBarController().hideAppHeader();
        ui.getMenuController().hideMenuButton();

        prefs = getSharedPreferences("arabatube", Context.MODE_PRIVATE);
        zoom = prefs.getFloat("zoom", 1.0f);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);

        bar = buildToolbar();
        column.addView(bar, new LinearLayout.LayoutParams(MATCH, dp(52)));

        web = new WebView(this);
        setupWebView(web);
        column.addView(web, new LinearLayout.LayoutParams(MATCH, 0, 1f));
        root.addView(column, new FrameLayout.LayoutParams(MATCH, MATCH));

        // YouTube'un tam ekran düğmesi video görünümünü buraya taşır
        fullscreenHolder = new FrameLayout(this);
        fullscreenHolder.setBackgroundColor(Color.BLACK);
        fullscreenHolder.setVisibility(View.GONE);
        root.addView(fullscreenHolder, new FrameLayout.LayoutParams(MATCH, MATCH));

        // Araç çubuğu gizliyken geri getirmek için küçük düğme
        barToggle = makeButton("☰", v -> setBarVisible(true));
        barToggle.setVisibility(View.GONE);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(dp(48), dp(48));
        tlp.gravity = Gravity.TOP | Gravity.END;
        tlp.setMargins(0, dp(6), dp(6), 0);
        root.addView(barToggle, tlp);

        setContentView(root);

        web.loadUrl(HOME);
        CarBridge.setListener(url -> main.post(() -> {
            exitFullscreen();
            if (web != null) web.loadUrl(url);
        }));
    }

    // ---------- WebView ----------

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView w) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        // "; wv" etiketi kaldırılınca YouTube "uygulamada aç" ısrarını azaltıyor
        s.setUserAgentString(s.getUserAgentString().replace("; wv", ""));

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(w, true);

        w.setBackgroundColor(Color.BLACK);
        w.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String scheme = u.getScheme() == null ? "" : u.getScheme();
                // intent://, vnd.youtube:// gibi uygulama linklerini yut; sadece web kalsın
                return !(scheme.equals("http") || scheme.equals("https"));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                applyZoom();
            }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                enterFullscreen(view, callback);
            }

            @Override
            public void onHideCustomView() {
                exitFullscreen();
            }
        });
    }

    private void js(String code) {
        if (web != null) web.evaluateJavascript(code, null);
    }

    private void applyZoom() {
        js("document.documentElement.style.zoom='" + String.format(Locale.US, "%.2f", zoom) + "';");
    }

    private void changeZoom(float delta) {
        zoom = Math.max(0.5f, Math.min(2.5f, zoom + delta));
        prefs.edit().putFloat("zoom", zoom).apply();
        applyZoom();
    }

    // ---------- Tam ekran ----------

    private void enterFullscreen(View view, WebChromeClient.CustomViewCallback cb) {
        if (customView != null) {
            cb.onCustomViewHidden();
            return;
        }
        customView = view;
        customCallback = cb;
        fullscreenHolder.addView(view, new FrameLayout.LayoutParams(MATCH, MATCH));
        fullscreenHolder.setVisibility(View.VISIBLE);
    }

    private void exitFullscreen() {
        if (customView == null) return;
        fullscreenHolder.removeView(customView);
        fullscreenHolder.setVisibility(View.GONE);
        customView = null;
        if (customCallback != null) customCallback.onCustomViewHidden();
        customCallback = null;
    }

    // ---------- Araç çubuğu ----------

    private LinearLayout buildToolbar() {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setBackgroundColor(Color.parseColor("#141414"));
        b.setPadding(dp(6), 0, dp(6), 0);

        addBtn(b, "◀", v -> goBack());
        addBtn(b, "⌂", v -> { exitFullscreen(); web.loadUrl(HOME); });
        addBtn(b, "⟳", v -> web.reload());
        addBtn(b, "⏯", v -> js("(function(){var v=document.querySelector('video');"
                + "if(v){if(v.paused)v.play();else v.pause();}})();"));
        addBtn(b, "−", v -> changeZoom(-0.1f));
        addBtn(b, "+", v -> changeZoom(0.1f));

        View spacer = new View(this);
        b.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView title = new TextView(this);
        title.setText("ArabaTube");
        title.setTextColor(Color.parseColor("#E53935"));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(16);
        title.setPadding(dp(8), 0, dp(12), 0);
        b.addView(title);

        addBtn(b, "⤢", v -> setBarVisible(false));
        return b;
    }

    private void addBtn(LinearLayout parent, String label, View.OnClickListener l) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(42));
        lp.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(makeButton(label, l), lp);
    }

    private TextView makeButton(String label, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.WHITE);
        t.setTextSize(20);
        t.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2A2A2A"));
        bg.setCornerRadius(dp(10));
        t.setBackground(bg);
        t.setClickable(true);
        t.setOnClickListener(l);
        return t;
    }

    private void setBarVisible(boolean visible) {
        bar.setVisibility(visible ? View.VISIBLE : View.GONE);
        barToggle.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private void goBack() {
        if (customView != null) exitFullscreen();
        else if (web.canGoBack()) web.goBack();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ---------- Yaşam döngüsü ----------

    @Override
    public void onBackPressed() {
        if (customView != null) exitFullscreen();
        else if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        CarBridge.setListener(null);
        exitFullscreen();
        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
