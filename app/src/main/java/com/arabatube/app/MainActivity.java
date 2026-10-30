package com.arabatube.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Telefon ekranı: aracın ekranına arama/link gönderir. */
public class MainActivity extends AppCompatActivity {

    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private TextView status;
    private EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = dp(20);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("ArabaTube");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#E53935"));
        col.addView(title);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, dp(6), 0, dp(16));
        col.addView(status);

        input = new EditText(this);
        input.setHint("Ara veya YouTube linki yapıştır");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_GO);
        input.setOnEditorActionListener((v, actionId, e) -> {
            boolean enter = e != null && e.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && e.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                submit();
                return true;
            }
            return false;
        });
        col.addView(input);

        Button send = new Button(this);
        send.setText("Arabada aç");
        send.setOnClickListener(v -> submit());
        col.addView(send);

        Button home = new Button(this);
        home.setText("Araçta YouTube ana sayfası");
        home.setOnClickListener(v -> deliver(TubeCarActivity.HOME));
        col.addView(home);

        TextView help = new TextView(this);
        help.setTextSize(14);
        help.setPadding(0, dp(24), 0, 0);
        help.setText(
                "İPUCU: YouTube uygulamasında bir videoda Paylaş → ArabaTube seçersen video direkt araç ekranında açılır.\n\n"
                + "KURULUM\n"
                + "1) Android Auto → en alttaki 'Sürüm'e 10 kez dokun → geliştirici modu açılır.\n"
                + "2) Sağ üst ⋮ → Geliştirici ayarları → 'Bilinmeyen kaynaklar' aç.\n"
                + "3) Android Auto ayarları → Başlatıcıyı özelleştir → ArabaTube'u işaretle.\n"
                + "4) Hâlâ görünmüyorsa uygulamayı bilgisayardan şu komutla kur:\n"
                + "   adb install -r -i com.android.vending ArabaTube.apk\n\n"
                + "⚠ Videoyu sadece araç park halindeyken izle.");
        col.addView(help);

        ScrollView sv = new ScrollView(this);
        sv.addView(col);
        setContentView(sv);

        handleShare(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleShare(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void handleShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (text == null) return;
        Matcher m = URL.matcher(text);
        if (m.find()) deliver(m.group());
        else {
            input.setText(text);
            submit();
        }
    }

    private void submit() {
        String q = input.getText().toString().trim();
        if (q.isEmpty()) return;
        Matcher m = URL.matcher(q);
        String url = m.find() ? m.group()
                : "https://m.youtube.com/results?search_query="
                  + encode(q);
        deliver(url);
    }

    private static String encode(String q) {
        try {
            return URLEncoder.encode(q, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return q;
        }
    }

    private void deliver(String url) {
        boolean now = CarBridge.send(url);
        Toast.makeText(this,
                now ? "Araç ekranında açıldı" : "Araç ekranı kapalı — ArabaTube açılınca yüklenecek",
                Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void updateStatus() {
        boolean on = CarBridge.isCarConnected();
        status.setText(on ? "● Araç ekranı bağlı" : "○ Araç ekranı kapalı (Android Auto'da ArabaTube'u aç)");
        status.setTextColor(on ? Color.parseColor("#43A047") : Color.GRAY);
        status.setGravity(Gravity.START);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
