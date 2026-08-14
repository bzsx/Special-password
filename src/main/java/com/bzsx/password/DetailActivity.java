package com.bzsx.password;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class DetailActivity extends AppCompatActivity {

    public static Bitmap sPreviousPageBitmap = null;

    private static final String PREFS_NAME = "detail_theme_prefs";
    private static final String KEY_THEME_MODE = "detail_theme_mode";
    private static final String MODE_LIGHT = "LIGHT";
    private static final String MODE_DARK = "DARK";

    private long passwordId;
    private DatabaseHelper dbHelper;
    private String plainPassword;
    private String currentThemeMode;

    private TextView tvName, tvAccount, tvPassword;
    private View rootLayout;
    private ImageView ivPreviousPage;
    private int lastSwipeEdge = BackEventCompat.EDGE_LEFT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentThemeMode = prefs.getString(KEY_THEME_MODE, MODE_LIGHT);

        if (MODE_DARK.equals(currentThemeMode)) {
            setTheme(R.style.Theme_PasswordApp_DetailDark);
        } else {
            setTheme(R.style.Theme_PasswordApp_DetailLight);
        }

        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_detail);

        android.content.res.TypedArray ta2 = getTheme().obtainStyledAttributes(
            new int[]{com.google.android.material.R.attr.colorSurface});
        int windowSurfaceColor = ta2.getColor(0, 0xFFF5F5F5);
        ta2.recycle();
        getWindow().setBackgroundDrawable(new ColorDrawable(windowSurfaceColor));

        rootLayout = findViewById(R.id.root_layout);
        ivPreviousPage = findViewById(R.id.iv_previous_page);

        View rootContainer = findViewById(R.id.root_container);
        if (rootContainer != null) {
            android.content.res.TypedArray ta = getTheme().obtainStyledAttributes(
                new int[]{com.google.android.material.R.attr.colorSurface});
            int containerSurfaceColor = ta.getColor(0, 0xFFF5F5F5);
            ta.recycle();
            rootContainer.setBackgroundColor(containerSurfaceColor);
        }

        if (sPreviousPageBitmap != null && !sPreviousPageBitmap.isRecycled()) {
            ivPreviousPage.setImageBitmap(sPreviousPageBitmap);
        }

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        passwordId = getIntent().getLongExtra("password_id", -1);
        dbHelper = new DatabaseHelper(this);

        tvName = findViewById(R.id.tv_detail_name);
        tvAccount = findViewById(R.id.tv_detail_account);
        tvPassword = findViewById(R.id.tv_detail_password);
        MaterialButton btnCopy = findViewById(R.id.btn_copy_password);

        TextView tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        tvToolbarTitle.setText("加载中...");

        applyThemeColors();
        BackgroundUtil.applyBackground(this, rootLayout);
        loadPasswordAsync();

        btnCopy.setOnClickListener(v -> copyPassword());
        registerBackCallback();
    }

    private void registerBackCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {

            @RequiresApi(34)
            @Override
            public void handleOnBackStarted(BackEventCompat backEvent) {
                if (ivPreviousPage.getDrawable() != null) {
                    ivPreviousPage.setVisibility(View.VISIBLE);
                }
                lastSwipeEdge = backEvent.getSwipeEdge();
            }

            @RequiresApi(34)
            @Override
            public void handleOnBackProgressed(BackEventCompat backEvent) {
                float progress = backEvent.getProgress();
                float scale = 1f - (0.10f * progress);
                rootLayout.setTranslationX(0f);
                rootLayout.setScaleX(scale);
                rootLayout.setScaleY(scale);
                rootLayout.setAlpha(1f - progress * 0.20f);
            }

            @Override
            public void handleOnBackPressed() {
                finish();
            }

            @RequiresApi(34)
            @Override
            public void handleOnBackCancelled() {
                rootLayout.setTranslationX(0f);
                rootLayout.setScaleX(1f);
                rootLayout.setScaleY(1f);
                rootLayout.setAlpha(1f);
                ivPreviousPage.setVisibility(View.GONE);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void loadPasswordAsync() {
        new Thread(() -> {
            PasswordEntry entry = dbHelper.getPassword(passwordId);
            if (entry == null) {
                runOnUiThread(() -> {
                    TextView tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
                    tvToolbarTitle.setText("详情");
                });
                return;
            }

            String masterPassword = SessionManager.getInstance().getMasterPassword(DetailActivity.this);
            String decrypted = null;
            try {
                if (masterPassword != null) {
                    decrypted = CryptoUtil.decrypt(entry.getEncryptedPassword(), masterPassword);
                }
            } catch (Exception e) {
                // 解密失败
            }

            final String finalDecrypted = decrypted;
            final String website = entry.getWebsite();
            runOnUiThread(() -> {
                TextView tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
                tvToolbarTitle.setText(entry.getName());
                tvName.setText(entry.getName());
                tvAccount.setText(entry.getAccount());
                if (finalDecrypted != null) {
                    plainPassword = finalDecrypted;
                    tvPassword.setText(finalDecrypted);
                } else {
                    tvPassword.setText("解密失败");
                    Toast.makeText(DetailActivity.this, "解密失败", Toast.LENGTH_SHORT).show();
                }

                TextView tvWebsite = findViewById(R.id.tv_detail_website);
                if (tvWebsite != null) {
                    if (website != null && !website.isEmpty()) {
                        tvWebsite.setText(website);
                    } else {
                        tvWebsite.setText("未设置（自动填充将匹配名称）");
                        tvWebsite.setAlpha(0.5f);
                    }
                }
            });
        }).start();
    }

    private void copyPassword() {
        if (plainPassword != null) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("password", plainPassword);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.password_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void applyThemeColors() {
        int themeColor = ThemeColorManager.getThemeColor(this);

        View root = findViewById(R.id.root_layout);
        MaterialCardView cardName = findViewById(R.id.card_name);
        MaterialCardView cardAccount = findViewById(R.id.card_account);
        MaterialCardView cardPassword = findViewById(R.id.card_password);

        TextView labelName = findViewById(R.id.label_name);
        TextView labelAccount = findViewById(R.id.label_account);
        TextView labelPassword = findViewById(R.id.label_password);

        TextView tvDetailName = findViewById(R.id.tv_detail_name);
        TextView tvDetailAccount = findViewById(R.id.tv_detail_account);
        TextView tvDetailPassword = findViewById(R.id.tv_detail_password);

        MaterialButton btnCopy = findViewById(R.id.btn_copy_password);

        if (root != null) root.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        int cardBg = 0xFFF5F5F5;
        if (cardName != null) cardName.setCardBackgroundColor(cardBg);
        if (cardAccount != null) cardAccount.setCardBackgroundColor(cardBg);
        if (cardPassword != null) cardPassword.setCardBackgroundColor(cardBg);

        int labelColor = 0xFF757575;
        if (labelName != null) labelName.setTextColor(labelColor);
        if (labelAccount != null) labelAccount.setTextColor(labelColor);
        if (labelPassword != null) labelPassword.setTextColor(labelColor);

        if (tvDetailName != null) tvDetailName.setTextColor(themeColor);
        if (tvDetailAccount != null) tvDetailAccount.setTextColor(themeColor);
        if (tvDetailPassword != null) tvDetailPassword.setTextColor(themeColor);

        if (btnCopy != null) {
            btnCopy.setTextColor(android.graphics.Color.WHITE);
            btnCopy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(themeColor));
        }

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            toolbar.setTitleTextColor(themeColor);
        }

        getWindow().setStatusBarColor(themeColor);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.zoom_exit);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}