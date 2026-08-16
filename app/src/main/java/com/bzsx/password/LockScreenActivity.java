package com.bzsx.password;

import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.textfield.TextInputLayout;

public class LockScreenActivity extends AppCompatActivity {

    private static final String PREF_NAME = "master_password_pref";
    private static final String KEY_HASH = "master_password_hash";

    // 首次使用验证
    private static final String FIRST_USE_PREF_NAME = "first_use_prefs";
    private static final String KEY_VERIFIED = "verified";

    // 系统锁屏请求码
    private static final int REQ_SYSTEM_UNLOCK = 1001;  // 用系统锁屏解锁进入
    private static final int REQ_SYSTEM_FORGOT = 1002;  // 忘记密码时验证本人

    private TextView tvTitle;
    private TextInputLayout tilPassword, tilConfirm;
    private EditText etPassword, etConfirm;
    private Button btnAction;
    private com.google.android.material.button.MaterialButton btnSystemUnlock;
    private TextView tvError;
    private TextView tvForgot;
    private TextView tvSwitchMode;

    private boolean isFirstTime;
    private boolean isSystemMode;
    private boolean isTemporaryPassword; // 是否在"系统锁屏模式下次临时用主密码"状态

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiThemeManager.applyNightMode(this);
        // 初始化全局异常捕获，防止闪退
        CrashHandler.getInstance().init(this);

        super.onCreate(savedInstanceState);

        // 首次使用验证
        if (!isFirstUseVerified()) {
            showFirstTimeVerifyDialog();
            return;
        }

        setContentView(R.layout.activity_lock_screen);

        applyThemeColor();
        BackgroundUtil.applyBackground(this, findViewById(R.id.root_layout));

        tvTitle = findViewById(R.id.tv_lock_title);
        tilPassword = findViewById(R.id.til_master_password);
        tilConfirm = findViewById(R.id.til_confirm_password);
        etPassword = findViewById(R.id.et_master_password);
        etConfirm = findViewById(R.id.et_confirm_password);
        btnAction = findViewById(R.id.btn_lock_action);
        btnSystemUnlock = findViewById(R.id.btn_system_unlock);
        tvError = findViewById(R.id.tv_lock_error);
        tvForgot = findViewById(R.id.tv_forgot);
        tvSwitchMode = findViewById(R.id.tv_switch_mode);

        isFirstTime = isFirstTime();

        if (isFirstTime) {
            // 首次使用：始终设置主密码（作为兜底解锁，即使之后选系统锁屏）
            setupFirstTimeUI();
        } else {
            // 非首次：根据解锁方式分流
            routeUnlockMode();
        }

        btnAction.setOnClickListener(v -> handleAction());
        btnSystemUnlock.setOnClickListener(v ->
                requestSystemUnlock("验证锁屏", "请验证手机锁屏后进入", REQ_SYSTEM_UNLOCK));
        tvForgot.setOnClickListener(v -> onForgotClick());
        tvSwitchMode.setOnClickListener(v -> {
            // 全局解锁方式只能在"关于"页切换。这里根据当前界面分支：
            // 系统锁屏界面 -> 临时用主密码；临时主密码界面 -> 返回系统锁屏
            if (isTemporaryPassword) {
                onSwitchBackToSystem();
            } else {
                onSwitchModeClick();
            }
        });
    }

    /**
     * 根据保存的解锁方式加载对应界面。
     */
    private void routeUnlockMode() {
        isSystemMode = UnlockPrefs.isSystemMode(this);

        if (isSystemMode && UnlockPrefs.hasSystemLock(this)) {
            // 系统锁屏模式且设备有锁屏：显示"验证手机锁屏"界面，并自动拉起一次验证
            setupSystemUnlockUI();
            // 界面渲染完成后自动弹系统锁屏验证
            btnSystemUnlock.postDelayed(() ->
                    requestSystemUnlock("验证锁屏", "请验证手机锁屏后进入", REQ_SYSTEM_UNLOCK), 300);
        } else {
            if (isSystemMode) {
                // 选了系统锁屏但设备没锁屏：回退到全局主密码
                UnlockPrefs.setMode(this, UnlockPrefs.MODE_PASSWORD);
                isSystemMode = false;
                Toast.makeText(this, "未检测到系统锁屏，已切换为主密码", Toast.LENGTH_SHORT).show();
            }
            setupPasswordUI(false);
        }
    }

    /**
     * 系统锁屏解锁界面：隐藏主密码输入，显示"验证手机锁屏"按钮。
     */
    private void setupSystemUnlockUI() {
        isSystemMode = true;
        isTemporaryPassword = false;
        tvTitle.setText("请验证手机锁屏");
        // 隐藏主密码输入相关
        tilPassword.setVisibility(View.GONE);
        tilConfirm.setVisibility(View.GONE);
        etPassword.setVisibility(View.GONE);
        etConfirm.setVisibility(View.GONE);
        btnAction.setVisibility(View.GONE);
        // 显示系统验证按钮
        btnSystemUnlock.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
        // 显示"切换主密码"入口 + "忘记密码"
        tvSwitchMode.setText("切换为主密码解锁");
        tvSwitchMode.setVisibility(View.VISIBLE);
        tvForgot.setVisibility(View.VISIBLE);
    }

    /**
     * 首次使用：设置主密码界面。
     */
    private void setupFirstTimeUI() {
        isSystemMode = false;
        isTemporaryPassword = false;
        tvTitle.setText(R.string.set_master_password);
        tilPassword.setVisibility(View.VISIBLE);
        etPassword.setVisibility(View.VISIBLE);
        tilConfirm.setVisibility(View.VISIBLE);
        etConfirm.setVisibility(View.VISIBLE);
        btnAction.setVisibility(View.VISIBLE);
        btnSystemUnlock.setVisibility(View.GONE);
        tvSwitchMode.setVisibility(View.GONE);
        tvForgot.setVisibility(View.GONE);
        btnAction.setText(R.string.confirm);
    }

    /**
     * 非首次：输入主密码界面。
     * @param temporary true=系统锁屏模式下次临时用主密码（保留返回系统锁屏入口）
     */
    private void setupPasswordUI(boolean temporary) {
        isTemporaryPassword = temporary;
        tvTitle.setText(temporary ? "本次使用主密码解锁" : getString(R.string.enter_master_password));
        tilPassword.setVisibility(View.VISIBLE);
        etPassword.setVisibility(View.VISIBLE);
        tilConfirm.setVisibility(View.GONE);
        etConfirm.setVisibility(View.GONE);
        btnAction.setVisibility(View.VISIBLE);
        btnSystemUnlock.setVisibility(View.GONE);
        btnAction.setText(R.string.unlock);
        tvForgot.setVisibility(View.VISIBLE);
        if (temporary) {
            // 临时场景：提供"返回系统锁屏"入口
            tvSwitchMode.setText("返回系统锁屏验证");
            tvSwitchMode.setVisibility(View.VISIBLE);
        } else {
            tvSwitchMode.setVisibility(View.GONE);
        }
    }

    /**
     * 临时场景下的入口点击：返回系统锁屏验证界面。
     */
    private void onSwitchBackToSystem() {
        setupSystemUnlockUI();
        etPassword.setText("");
        Toast.makeText(this, "已返回系统锁屏验证", Toast.LENGTH_SHORT).show();
    }

    private boolean isFirstUseVerified() {
        SharedPreferences prefs = getSharedPreferences(FIRST_USE_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_VERIFIED, false);
    }

    private boolean isFirstTime() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return TextUtils.isEmpty(prefs.getString(KEY_HASH, null));
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void enterApp(String password) {
        // 把主密码存入 SessionManager（供解密数据用）
        if (password != null) {
            SessionManager.getInstance().setMasterPassword(password, this);
        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void handleAction() {
        tvError.setVisibility(View.GONE);
        String password = etPassword.getText().toString().trim();

        if (password.length() < 6) {
            showError(getString(R.string.master_password_error));
            return;
        }

        if (isFirstTime) {
            String confirm = etConfirm.getText().toString().trim();
            if (!password.equals(confirm)) {
                showError(getString(R.string.master_password_mismatch));
                return;
            }
            setNewPassword(password);
        } else {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String storedHash = prefs.getString(KEY_HASH, "");
            if (CryptoUtil.verifyPassword(password, storedHash)) {
                enterApp(password);
            } else {
                showError(getString(R.string.master_password_wrong));
                etPassword.setText("");
            }
        }
    }

    /**
     * 保存新主密码并进入。
     */
    private void setNewPassword(String password) {
        String hash = CryptoUtil.hashPassword(password);
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_HASH, hash).apply();
        enterApp(password);
    }

    /**
     * 忘记密码：验证系统锁屏本人，成功后清除数据重设主密码。
     */
    private void onForgotClick() {
        if (!UnlockPrefs.hasSystemLock(this)) {
            showForgotFallbackDialog();
            return;
        }
        requestSystemUnlock("验证本人", "忘记主密码，请验证手机锁屏", REQ_SYSTEM_FORGOT);
    }

    /**
     * 系统锁屏界面下的"临时用主密码"入口。
     * 仅本次进入临时用主密码验证，不改变全局解锁方式（全局只能在"关于"页切换）。
     */
    private void onSwitchModeClick() {
        setupPasswordUI(true);
        Toast.makeText(this, "本次用主密码解锁（不会改变设置）", Toast.LENGTH_SHORT).show();
        etPassword.setText("");
        etPassword.requestFocus();
    }

    /**
     * 忘记密码的兜底：设备无系统锁屏，只能清除数据。
     */
    private void showForgotFallbackDialog() {
        new AlertDialog.Builder(this, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog)
                .setTitle("无法验证")
                .setMessage("设备未设置系统锁屏。\n\n若忘记主密码且无法验证手机锁屏，只能清除应用数据后重新设置。\n\n是否前往系统设置开启锁屏？")
                .setPositiveButton("去设置", (d, w) -> startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)))
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 弹出系统锁屏验证（KeyguardManager，直接是系统密码界面）。
     */
    private void requestSystemUnlock(String title, String desc, int requestCode) {
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null || !km.isDeviceSecure()) {
            if (requestCode == REQ_SYSTEM_FORGOT) {
                showForgotFallbackDialog();
            }
            return;
        }
        Intent intent = km.createConfirmDeviceCredentialIntent(title, desc);
        if (intent != null) {
            try {
                startActivityForResult(intent, requestCode);
            } catch (Exception e) {
                if (requestCode == REQ_SYSTEM_FORGOT) {
                    showForgotFallbackDialog();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_SYSTEM_UNLOCK) {
                // 系统锁屏解锁成功：用缓存的明文主密码进入
                String cached = SessionManager.getInstance().getMasterPassword(this);
                if (cached != null && !cached.isEmpty()) {
                    SessionManager.getInstance().setMasterPassword(cached, this);
                    enterApp(cached);
                } else {
                    // 缓存无主密码：提示用主密码或忘记密码重置
                    Toast.makeText(this, "主密码未缓存，请输入主密码或使用'忘记密码'重置", Toast.LENGTH_LONG).show();
                }
            } else if (requestCode == REQ_SYSTEM_FORGOT) {
                // 忘记密码且验证本人成功：清除数据重设主密码
                confirmResetPassword();
            }
        }
    }

    /**
     * 确认清除数据并重设主密码（确认弹窗）。
     */
    private void confirmResetPassword() {
        new AlertDialog.Builder(this, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog)
                .setTitle("重置主密码")
                .setMessage("验证成功。由于旧密码无法找回，重置主密码将清除所有已保存的密码数据。\n\n是否继续？")
                .setPositiveButton("继续", (d, w) -> eraseAllAndReset())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 清除所有密码数据 + 主密码哈希，进入重设主密码。
     */
    private void eraseAllAndReset() {
        // 清除数据库密码数据
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("passwords", null, null);
            db.close();
        } catch (Exception ignored) {}

        // 清除主密码哈希，进入首次设置
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_HASH).apply();

        Toast.makeText(this, "数据已清除，请设置新的主密码", Toast.LENGTH_SHORT).show();
        // 刷新界面进入首次设置
        isFirstTime = true;
        setupFirstTimeUI();
        etPassword.setText("");
        etConfirm.setText("");
        etPassword.requestFocus();
    }

    private void showFirstTimeVerifyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_first_time_verify, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        TextView tvQqGroup = dialogView.findViewById(R.id.tv_qq_group);
        tvQqGroup.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("QQ群号", "241333711");
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "QQ群号已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });

        TextView tvBilibili = dialogView.findViewById(R.id.tv_bilibili_link);
        tvBilibili.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://space.bilibili.com/3546612747995937?spm_id_from=333.337.0.0"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show();
            }
        });

        // ---- 倒计时逻辑：10秒后才可进入 ----
        TextView tvCountdown = dialogView.findViewById(R.id.tv_countdown);
        Button btnEnter = dialogView.findViewById(R.id.btn_enter);

        // 初始：禁用进入按钮
        btnEnter.setEnabled(false);
        btnEnter.setAlpha(0.5f);

        final long totalSeconds = 10;
        final long startTime = System.currentTimeMillis();
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                long remaining = totalSeconds - elapsed;
                if (remaining < 0) remaining = 0;
                tvCountdown.setText(remaining + " 秒");
                if (remaining > 0) {
                    handler.postDelayed(this, 200);
                } else {
                    btnEnter.setEnabled(true);
                    btnEnter.setAlpha(1f);
                }
            }
        };
        handler.post(countdownRunnable);

        btnEnter.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(FIRST_USE_PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_VERIFIED, true).apply();
            dialog.dismiss();
            recreate();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85f),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void applyThemeColor() {
        int themeColor = ThemeColorManager.getThemeColor(this);

        if (btnAction != null) {
            btnAction.setTextColor(android.graphics.Color.WHITE);
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(themeColor));
            ShapeAppearanceModel shapeAppearance = ShapeAppearanceModel.builder()
                    .setAllCornerSizes(new com.google.android.material.shape.AbsoluteCornerSize(28f))
                    .build();
            MaterialShapeDrawable backgroundDrawable = new MaterialShapeDrawable(shapeAppearance);
            backgroundDrawable.setFillColor(android.content.res.ColorStateList.valueOf(themeColor));
            btnAction.setBackground(backgroundDrawable);
        }

        if (btnSystemUnlock != null) {
            btnSystemUnlock.setTextColor(android.graphics.Color.WHITE);
            btnSystemUnlock.setBackgroundTintList(android.content.res.ColorStateList.valueOf(themeColor));
        }

        getWindow().setStatusBarColor(themeColor);

        if (tilPassword != null) {
            tilPassword.setBoxStrokeColor(themeColor);
            tilPassword.setHintTextColor(android.content.res.ColorStateList.valueOf(themeColor));
        }
        if (tilConfirm != null) {
            tilConfirm.setBoxStrokeColor(themeColor);
            tilConfirm.setHintTextColor(android.content.res.ColorStateList.valueOf(themeColor));
        }
    }
}