package com.bzsx.password;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

public class SettingsFragment extends Fragment {

    // 背景图片相关
    private static final String PREFS_BG = "background_prefs";

    // 主题色相关
    private TextView tvThemeColorPreview;
    private static final String KEY_BG_ENABLED = "background_enabled";
    private static final String KEY_BG_ALPHA = "background_alpha";
    private static final String BG_IMAGE_FILENAME = "app_background.png";

    // 检查更新 URL
    private static final String CHECK_UPDATE_URL = "https://bzsx.lhx520.icu/check_version.php";

    // 背景图片相关视图
    private SeekBar seekBarAlpha;
    private TextView tvAlphaValue;
    private TextView tvBgStatus;
    private LinearLayout layoutBgOptions;
    private LinearLayout layoutDisableBg;
    private View rootView;

    private File bgImageFile;
    private ExecutorService executorService;
    private Handler mainHandler;

    // 加载弹窗
    private ProgressDialog loadingDialog;

    // ======================== 在线更新下载相关 ========================
    // 下载 APK 的地址：服务器 check_version.php 返回的 url 字段优先，
    // 若接口没返回 url，则使用下面这个默认下载地址（可自行修改成你的 APK 直链）
    private static final String DEFAULT_APK_URL = "https://bzsx.lhx520.icu/神奇的密码_1.3.3.apk";

    // 当前要下载的地址、APK 文件、进度 UI
    private AlertDialog downloadDialog;
    private ProgressBar downloadProgressBar;
    private TextView downloadStatusText;
    private boolean downloading;   // 防止重复下载
    private boolean downloadCancelled; // 是否被取消
    private String apkUrl = "";    // 服务器返回的下载地址

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bgImageFile = new File(requireContext().getFilesDir(), BG_IMAGE_FILENAME);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        // 设置版本号
        TextView tvVersion = rootView.findViewById(R.id.tv_version);
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            tvVersion.setText("版本 " + versionName);
        } catch (Exception e) {
            tvVersion.setText("版本 未知");
        }

        // GitHub 项目链接（系统自动优先用GitHub App，未安装则浏览器）
        TextView tvGithub = rootView.findViewById(R.id.tv_github);
        tvGithub.setOnClickListener(v ->
                openWeb("https://github.com/bzsx/bzsx_password", "无法打开GitHub"));

        // 更新日志
        TextView tvChangelog = rootView.findViewById(R.id.tv_changelog);
        tvChangelog.setOnClickListener(v -> showChangelogDialog());

        // 联系作者
        TextView tvContact = rootView.findViewById(R.id.tv_contact);
        tvContact.setOnClickListener(v -> showContactDialog());

        // 直接检查更新按钮
        MaterialButton btnDirectCheckUpdate = rootView.findViewById(R.id.btn_direct_check_update);
        btnDirectCheckUpdate.setOnClickListener(v -> performDirectUpdateCheck());

        // === 开屏动画开关 ===
        com.google.android.material.materialswitch.MaterialSwitch switchSplash = rootView.findViewById(R.id.switch_splash);
        switchSplash.setChecked(SplashActivity.isSplashEnabled(requireContext()));
        switchSplash.setOnCheckedChangeListener((buttonView, isChecked) ->
                SplashActivity.setSplashEnabled(requireContext(), isChecked));

        // === 深色模式 ===
        TextView tvDarkModeValue = rootView.findViewById(R.id.tv_dark_mode_value);
        updateDarkModeLabel(tvDarkModeValue);
        rootView.findViewById(R.id.layout_dark_mode).setOnClickListener(v ->
                showDarkModeDialog(tvDarkModeValue));

        // === 主题色自定义 ===
        LinearLayout layoutThemeColor = rootView.findViewById(R.id.layout_theme_color);
        tvThemeColorPreview = rootView.findViewById(R.id.tv_theme_color_preview);
        int currentColor = ThemeColorManager.getThemeColor(requireContext());
        tvThemeColorPreview.setBackgroundColor(currentColor);
        layoutThemeColor.setOnClickListener(v -> showColorPickerDialog());

        // === 解锁方式 ===
        TextView tvUnlockModeValue = rootView.findViewById(R.id.tv_unlock_mode_value);
        tvUnlockModeValue.setText(UnlockPrefs.isSystemMode(requireContext()) ? "系统锁屏" : "主密码");
        LinearLayout layoutUnlockMode = rootView.findViewById(R.id.layout_unlock_mode);
        layoutUnlockMode.setOnClickListener(v -> showUnlockModeDialog(tvUnlockModeValue));

        // === 修改密码 ===
        LinearLayout layoutChangePassword = rootView.findViewById(R.id.layout_change_password);
        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // === 添加背景图片 ===
        LinearLayout layoutSelectBg = rootView.findViewById(R.id.layout_select_bg);
        layoutSelectBg.setOnClickListener(v -> selectBackgroundImage());

        // === 关闭使用背景图片 ===
        layoutDisableBg = rootView.findViewById(R.id.layout_disable_bg);
        layoutDisableBg.setOnClickListener(v -> disableBackgroundWithConfirm());

        // === 背景相关选项 ===
        layoutBgOptions = rootView.findViewById(R.id.layout_bg_options);
        seekBarAlpha = rootView.findViewById(R.id.seekbar_alpha);
        tvAlphaValue = rootView.findViewById(R.id.tv_alpha_value);
        tvBgStatus = rootView.findViewById(R.id.tv_bg_status);

        restoreBackgroundState();

        seekBarAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int alpha = progress + 1;
                tvAlphaValue.setText(alpha + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 仅保存透明度并即时应用背景，不 recreate 整页，避免页面刷新
                int alpha = seekBar.getProgress() + 1;
                saveBgAlpha(alpha);
                View root = requireActivity().findViewById(R.id.root_layout);
                BackgroundUtil.applyBackground(requireContext(), root);
            }
        });

        return rootView;
    }

    // ======================== 直接检查更新 ========================

    private void performDirectUpdateCheck() {
        String currentVersion;
        try {
            currentVersion = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            currentVersion = "1.0";
        }

        // 显示加载弹窗（安卓原生转圈 + 文字）
        showLoadingDialog();

        String finalCurrentVersion = currentVersion;

        executorService.execute(() -> {
            String latestVersion = null;
            String errorMessage = null;

            try {
                URL url = new URL(CHECK_UPDATE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    String responseJson = sb.toString().trim();
                    JSONObject jsonObject = new JSONObject(responseJson);
                    latestVersion = jsonObject.optString("version", null);
                    // 读取下载地址 url 字段（服务器返回的 APK 下载链接）
                    apkUrl = jsonObject.optString("url", "");
                } else {
                    errorMessage = "服务器返回 " + connection.getResponseCode();
                }
                connection.disconnect();
            } catch (Exception e) {
                errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "网络连接失败";
                }
            }

            final String finalLatestVersion = latestVersion;
            final String finalErrorMessage = errorMessage;
            final String finalApkUrl = apkUrl;

            mainHandler.post(() -> {
                // 关闭加载弹窗
                dismissLoadingDialog();

                if (finalLatestVersion != null && !finalLatestVersion.isEmpty()) {
                    // 纯数字比大小：比较当前版本和服务器版本
                    int compareResult = compareVersions(finalCurrentVersion, finalLatestVersion);

                    if (compareResult > 0) {
                        // 本机版本 > 服务器版本（本机比服务器新）
                        new AlertDialog.Builder(requireContext())
                                .setTitle("检查更新")
                                .setMessage("你个作者拿新版本忽悠我呢。\n\n当前版本：" + finalCurrentVersion + "\n服务器最新版本：" + finalLatestVersion)
                                .setPositiveButton("知道了", null)
                                .show();
                    } else if (compareResult == 0) {
                        // 本机版本 == 服务器版本（已是最新）
                        new AlertDialog.Builder(requireContext())
                                .setTitle("检查更新")
                                .setMessage("你已经是最新版本了。\n\n当前版本：" + finalCurrentVersion + "\n服务器版本：" + finalLatestVersion)
                                .setPositiveButton("好的", null)
                                .show();
                    } else {
                        // 本机版本 < 服务器版本（发现新版本）
                        new AlertDialog.Builder(requireContext())
                                .setTitle("发现新版本")
                                .setMessage("最新版本：" + finalLatestVersion + "\n你当前的版本：" + finalCurrentVersion + "\n\n是否立即下载并更新？")
                                .setPositiveButton("立即更新", (dialog, which) -> {
                                    String downloadUrl = (finalApkUrl != null && !finalApkUrl.isEmpty())
                                            ? finalApkUrl : DEFAULT_APK_URL;
                                    checkInstallPermissionAndDownload(downloadUrl);
                                })
                                .setNegativeButton("稍后", null)
                                .setNeutralButton("前往官网", (dialog, which) -> {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bzsx.lhx520.icu/password.html"));
                                    startActivity(intent);
                                })
                                .show();
                    }
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("检查失败")
                            .setMessage("无法获取最新版本信息。" + (finalErrorMessage != null ? "（" + finalErrorMessage + "）" : ""))
                            .setPositiveButton("好的", null)
                            .show();
                }
            });
        });
    }

    /**
     * 纯数字版本号比较
     * 支持 "1.3"、"1.3.1" 这种多点号版本号
     * 返回：current > target 返回正数，相等返回 0，current < target 返回负数
     */
    private int compareVersions(String current, String target) {
        String[] curParts = current.split("\\.");
        String[] tarParts = target.split("\\.");
        int maxLen = Math.max(curParts.length, tarParts.length);
        for (int i = 0; i < maxLen; i++) {
            int cur = i < curParts.length ? parseIntSafe(curParts[i]) : 0;
            int tar = i < tarParts.length ? parseIntSafe(tarParts[i]) : 0;
            if (cur != tar) {
                return cur - tar;
            }
        }
        return 0;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 显示加载弹窗（安卓原生转圈 + 文字）
    private void showLoadingDialog() {
        try {
            if (loadingDialog == null) {
                loadingDialog = new ProgressDialog(requireContext());
                loadingDialog.setMessage("正在校验...");
                loadingDialog.setCancelable(false); // 不允许取消，防止误关
                // 安卓原生环形转圈样式
                loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            }
            loadingDialog.show();
        } catch (Exception e) {
            // 弹窗失败忽略，不影响功能
        }
    }

    // 关闭加载弹窗
    private void dismissLoadingDialog() {
        try {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    // ======================== 在线更新：下载并安装 APK ========================

    /**
     * 检查是否有"安装未知来源应用"权限，有则直接下载，没有则引导去设置页开启。
     */
    private void checkInstallPermissionAndDownload(String url) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean canInstall = requireContext().getPackageManager()
                    .canRequestPackageInstalls();
            if (!canInstall) {
                // 提醒用户去开启"允许安装未知来源应用"
                new AlertDialog.Builder(requireContext())
                        .setTitle("需要安装权限")
                        .setMessage("更新需要允许本应用安装未知来源的应用，请前往系统设置开启后再试。")
                        .setPositiveButton("去开启", (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + requireContext().getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return;
            }
        }
        startDownload(url);
    }

    /**
     * 弹出带进度条、文件大小、实时网速的下载对话框，并在后台下载 APK。
     */
    private void startDownload(String url) {
        if (downloading) {
            Toast.makeText(requireContext(), "正在下载中，请稍候...", Toast.LENGTH_SHORT).show();
            return;
        }
        downloading = true;
        downloadCancelled = false;

        // 构造下载进度对话框
        ProgressBar progressBar = new ProgressBar(requireContext(), null,
                android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        downloadProgressBar = progressBar;

        downloadStatusText = new TextView(requireContext());
        downloadStatusText.setTextSize(14);
        downloadStatusText.setPadding(8, 16, 8, 8);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 8);
        layout.addView(progressBar);
        layout.addView(downloadStatusText);

        downloadDialog = new AlertDialog.Builder(requireContext())
                .setTitle("正在下载更新")
                .setView(layout)
                .setCancelable(false)
                .setNegativeButton("取消下载", (d, w) -> {
                    downloadCancelled = true;
                })
                .create();
        downloadDialog.show();

        // 后台下载
        executorService.execute(() -> {
            String apkFileName = "update_" + System.currentTimeMillis() + ".apk";
            File apkFile = new File(requireContext().getFilesDir(), apkFileName);
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL downloadUrl = new URL(url);
                connection = (HttpURLConnection) downloadUrl.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    mainHandler.post(() -> {
                        if (downloadDialog != null && downloadDialog.isShowing()) {
                            downloadDialog.dismiss();
                        }
                        downloading = false;
                        Toast.makeText(requireContext(), "下载失败：服务器返回 " + responseCode, Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // 文件总大小（服务器未返回时用 -1）
                long totalSize = connection.getContentLengthLong();
                // 存到字段供 UI 使用
                final long fileTotal = totalSize;

                input = connection.getInputStream();
                output = new FileOutputStream(apkFile);

                byte[] buffer = new byte[8192];
                long downloaded = 0;
                long lastTime = System.currentTimeMillis();
                long lastBytes = 0;
                long lastSpeed = 0;
                int len;

                while ((len = input.read(buffer)) != -1) {
                    if (downloadCancelled) {
                        break;
                    }
                    output.write(buffer, 0, len);
                    downloaded += len;

                    // 每 300ms 刷新一次进度 / 文件大小 / 网速
                    long now = System.currentTimeMillis();
                    if (now - lastTime >= 300) {
                        long speed = (downloaded - lastBytes) * 1000 / (now - lastTime); // 字节/秒
                        lastSpeed = speed;
                        lastTime = now;
                        lastBytes = downloaded;

                        final long fileDownloaded = downloaded;
                        final long fileSpeed = speed;
                        updateDownloadUI(fileDownloaded, fileTotal, fileSpeed);
                    }
                }
                output.flush();

                if (downloadCancelled) {
                    mainHandler.post(() -> {
                        if (downloadDialog != null && downloadDialog.isShowing()) {
                            downloadDialog.dismiss();
                        }
                        downloading = false;
                        apkFile.delete();
                        Toast.makeText(requireContext(), "下载已取消", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 下载完成
                final File finalApk = apkFile;
                mainHandler.post(() -> {
                    if (downloadDialog != null && downloadDialog.isShowing()) {
                        downloadDialog.dismiss();
                    }
                    downloading = false;
                    installApk(finalApk);
                });
            } catch (final Exception e) {
                mainHandler.post(() -> {
                    if (downloadDialog != null && downloadDialog.isShowing()) {
                        downloadDialog.dismiss();
                    }
                    downloading = false;
                    apkFile.delete();
                    Toast.makeText(requireContext(), "下载失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                } catch (IOException ignored) {}
                if (connection != null) connection.disconnect();
            }
        });
    }

    /**
     * 刷新下载进度对话框：进度条百分比、已下载/总大小、实时网速。
     */
    private void updateDownloadUI(long downloaded, long total, long speed) {
        mainHandler.post(() -> {
            if (downloadDialog == null || !downloadDialog.isShowing()) return;
            int percent = 0;
            if (total > 0) {
                percent = (int) (downloaded * 100 / total);
            }
            downloadProgressBar.setProgress(percent);

            String totalText = total > 0 ? formatFileSize(total) : "未知大小";
            String text = "已下载：" + formatFileSize(downloaded) + " / " + totalText + "\n" +
                    (percent > 0 ? "进度：" + percent + "%\n" : "") +
                    "网速：" + formatSpeed(speed);
            downloadStatusText.setText(text);
        });
    }

    /**
     * 使用 FileProvider 打开 APK，触发系统安装界面。
     */
    private void installApk(File apkFile) {
        try {
            Uri apkUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法打开安装界面：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 字节数转可读大小，如 1.5 MB。
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    /**
     * 网速（字节/秒）转可读形式，如 1.2 MB/s。
     */
    private String formatSpeed(long bytesPerSec) {
        if (bytesPerSec <= 0) return "0 B/s";
        String[] units = {"B/s", "KB/s", "MB/s"};
        int unitIndex = 0;
        double size = bytesPerSec;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    @Override
    public void onDestroy() {
        dismissLoadingDialog();
        downloadCancelled = true;
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // ======================== 外观（深色模式/动态取色） ========================

    private void updateDarkModeLabel(TextView tv) {
        int mode = UiThemeManager.getThemeMode(requireContext());
        String label;
        switch (mode) {
            case UiThemeManager.MODE_LIGHT: label = "浅色"; break;
            case UiThemeManager.MODE_DARK: label = "深色"; break;
            default: label = "跟随系统"; break;
        }
        tv.setText(label);
    }

    private void showDarkModeDialog(TextView tvDarkModeValue) {
        int current = UiThemeManager.getThemeMode(requireContext());
        int checked;
        switch (current) {
            case UiThemeManager.MODE_LIGHT: checked = 1; break;
            case UiThemeManager.MODE_DARK: checked = 2; break;
            default: checked = 0; break;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("深色模式")
                .setSingleChoiceItems(new String[]{"跟随系统", "浅色", "深色"}, checked, (d, which) -> {
                    int mode;
                    switch (which) {
                        case 1: mode = UiThemeManager.MODE_LIGHT; break;
                        case 2: mode = UiThemeManager.MODE_DARK; break;
                        default: mode = UiThemeManager.MODE_FOLLOW_SYSTEM; break;
                    }
                    UiThemeManager.setThemeMode(requireContext(), mode);
                    UiThemeManager.applyNightMode(requireContext());
                    updateDarkModeLabel(tvDarkModeValue);
                    d.dismiss();
                    // 重建界面以应用新主题
                    requireActivity().recreate();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ======================== 解锁方式 ========================

    private void showUnlockModeDialog(TextView tvUnlockModeValue) {
        String current = UnlockPrefs.getMode(requireContext());
        int checked = UnlockPrefs.MODE_SYSTEM.equals(current) ? 1 : 0;
        new AlertDialog.Builder(requireContext())
                .setTitle("解锁方式")
                .setSingleChoiceItems(new String[]{"主密码", "系统锁屏密码"}, checked, (d, which) -> {
                    if (which == 0) {
                        UnlockPrefs.setMode(requireContext(), UnlockPrefs.MODE_PASSWORD);
                        tvUnlockModeValue.setText("主密码");
                        Toast.makeText(requireContext(), "已切换为主密码解锁", Toast.LENGTH_SHORT).show();
                    } else {
                        if (UnlockPrefs.hasSystemLock(requireContext())) {
                            UnlockPrefs.setMode(requireContext(), UnlockPrefs.MODE_SYSTEM);
                            tvUnlockModeValue.setText("系统锁屏");
                            Toast.makeText(requireContext(), "已切换为系统锁屏解锁", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "设备未设置系统锁屏，无法切换", Toast.LENGTH_LONG).show();
                        }
                    }
                    d.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ======================== 联系作者 ========================

    private void showContactDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_contact, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("联系作者")
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // 第一行：B站主页（优先唤起B站App进入空间页，未装或失败则回退浏览器）
        dialogView.findViewById(R.id.tv_contact_bili_link).setOnClickListener(v ->
                goToBilibiliSpace("3546612747995937"));

        // 官方网站（点击打开，走浏览器）
        dialogView.findViewById(R.id.tv_contact_website).setOnClickListener(v ->
                openWeb("https://bzsx.lhx520.icu/password.html", "无法打开网站"));

        // 第二行：QQ群（点击复制群号，可在QQ内搜索群号加入）
        final String qqGroup = "241333711";
        dialogView.findViewById(R.id.tv_contact_qq).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("QQ群号", qqGroup));
            Toast.makeText(requireContext(), "QQ群号已复制，请在QQ中搜索群号 241333711 加入", Toast.LENGTH_LONG).show();
        });

        // 第三行：邮箱（点击发邮件）
        dialogView.findViewById(R.id.tv_contact_email).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:Gary_cong0605@outlook.com"));
            startActivity(intent);
        });

        // 关闭键
        dialogView.findViewById(R.id.btn_contact_close).setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // 修复弹窗宽度
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.9f),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    /**
     * 打开网页链接。系统会按已安装应用自动匹配对应的手机App（如 B站/GitHub App），
     * 未安装则回退到默认浏览器打开。
     */
    private void openWeb(String url, String errorMsg) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 跳转到指定 UID 的 B站 个人空间：
     * 优先用 bilibili:// scheme 唤起 B站 App；未安装 / scheme 无效 / 唤起失败则回退浏览器。
     */
    private void goToBilibiliSpace(String uid) {
        final String biliPackage = "tv.danmaku.bili";
        final String uidStr = uid;

        // 1. 检查 B站 App 是否已安装
        PackageManager pm = requireContext().getPackageManager();
        boolean installed;
        try {
            pm.getPackageInfo(biliPackage, 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }

        // 2. 已安装：尝试用 scheme 唤起
        if (installed) {
            Intent appIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("bilibili://space/" + uidStr));
            // 检查 scheme 是否能被处理
            if (appIntent.resolveActivity(pm) != null) {
                try {
                    startActivity(appIntent);
                    return;
                } catch (Exception e) {
                    // 唤起失败，继续走网页回退
                }
            }
        }

        // 3. 回退：浏览器打开 B站 空间网页
        openWeb("https://space.bilibili.com/" + uidStr, "无法打开B站主页");
    }

    // ======================== 修改密码 ========================

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        builder.setTitle("修改主密码");

        TextView etOldPassword = dialogView.findViewById(R.id.et_old_password);
        TextView etNewPassword = dialogView.findViewById(R.id.et_new_password);
        TextView etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);

        AlertDialog dialog = builder.setPositiveButton("修改", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String oldPwd = etOldPassword.getText().toString().trim();
                String newPwd = etNewPassword.getText().toString().trim();
                String confirmPwd = etConfirmPassword.getText().toString().trim();

                SharedPreferences prefs = requireContext().getSharedPreferences("master_password_pref", Context.MODE_PRIVATE);
                String storedHash = prefs.getString("master_password_hash", "");
                if (!CryptoUtil.verifyPassword(oldPwd, storedHash)) {
                    Toast.makeText(requireContext(), "原密码错误", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newPwd.length() < 6) {
                    Toast.makeText(requireContext(), "新密码长度不能少于6位", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newPwd.equals(confirmPwd)) {
                    Toast.makeText(requireContext(), "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }

                String newHash = CryptoUtil.hashPassword(newPwd);
                prefs.edit().putString("master_password_hash", newHash).apply();
                // 同步更新缓存的明文主密码，保证系统锁屏解锁能正确解密数据
                SessionManager.getInstance().setMasterPassword(newPwd, requireContext());

                Toast.makeText(requireContext(), "主密码修改成功！", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    // ======================== 背景图片 ========================

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    CropDialog cropDialog = new CropDialog(requireContext(), uri);
                    cropDialog.setCropListener(new CropDialog.CropListener() {
                        @Override
                        public void onCropped(File croppedFile) {
                            saveCroppedImage(Uri.fromFile(croppedFile));
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
                    cropDialog.show();
                }
            });

    private void selectBackgroundImage() {
        imagePickerLauncher.launch("image/*");
    }

    private void saveCroppedImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            FileOutputStream fos = new FileOutputStream(bgImageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            SharedPreferences bgPrefs = requireContext().getSharedPreferences(PREFS_BG, Context.MODE_PRIVATE);
            bgPrefs.edit()
                    .putBoolean(KEY_BG_ENABLED, true)
                    .putInt(KEY_BG_ALPHA, 80)
                    .apply();

            Toast.makeText(requireContext(), "背景图片已设置", Toast.LENGTH_SHORT).show();
            requireActivity().recreate();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "保存背景图片失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void disableBackground() {
        SharedPreferences bgPrefs = requireContext().getSharedPreferences(PREFS_BG, Context.MODE_PRIVATE);
        bgPrefs.edit().putBoolean(KEY_BG_ENABLED, false).apply();
        if (bgImageFile.exists()) {
            bgImageFile.delete();
        }
        requireActivity().recreate();
    }

    private void disableBackgroundWithConfirm() {
        new AlertDialog.Builder(requireContext())
                .setTitle("关闭背景图片")
                .setMessage("确定要关闭使用背景图片吗？")
                .setPositiveButton("确定", (d, w) -> disableBackground())
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean isBgEnabled() {
        SharedPreferences bgPrefs = requireContext().getSharedPreferences(PREFS_BG, Context.MODE_PRIVATE);
        return bgPrefs.getBoolean(KEY_BG_ENABLED, false);
    }

    private void saveBgAlpha(int alpha) {
        SharedPreferences bgPrefs = requireContext().getSharedPreferences(PREFS_BG, Context.MODE_PRIVATE);
        bgPrefs.edit().putInt(KEY_BG_ALPHA, alpha).apply();
    }

    private void restoreBackgroundState() {
        boolean enabled = isBgEnabled();
        if (enabled && bgImageFile.exists()) {
            tvBgStatus.setText("已启用");
            int colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, 0);
            tvBgStatus.setTextColor(colorPrimary);
            layoutBgOptions.setVisibility(View.VISIBLE);
            layoutDisableBg.setVisibility(View.VISIBLE);
            int alpha = getBgAlpha();
            seekBarAlpha.setProgress(alpha - 1);
            tvAlphaValue.setText(alpha + "%");
        } else {
            tvBgStatus.setText("未设置");
            int colorOnSurfaceVariant = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, 0);
            tvBgStatus.setTextColor(colorOnSurfaceVariant);
            layoutBgOptions.setVisibility(View.GONE);
            layoutDisableBg.setVisibility(View.GONE);
        }
    }

    private int getBgAlpha() {
        SharedPreferences bgPrefs = requireContext().getSharedPreferences(PREFS_BG, Context.MODE_PRIVATE);
        return bgPrefs.getInt(KEY_BG_ALPHA, 80);
    }

    public static File getBgImageFile(Context context) {
        return new File(context.getFilesDir(), BG_IMAGE_FILENAME);
    }

    public static boolean isBackgroundEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_BG, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_BG_ENABLED, false);
    }

    public static int getBackgroundAlpha(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_BG, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_BG_ALPHA, 80);
    }

    // ======================== 主题色自定义 ========================

    private void showColorPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_color_picker, null);
        builder.setView(dialogView);
        builder.setTitle("选择主题色");

        LinearLayout layoutPresetColors = dialogView.findViewById(R.id.layout_preset_colors);
        SeekBar seekBarRed = dialogView.findViewById(R.id.seekbar_red);
        SeekBar seekBarGreen = dialogView.findViewById(R.id.seekbar_green);
        SeekBar seekBarBlue = dialogView.findViewById(R.id.seekbar_blue);
        TextView tvRedValue = dialogView.findViewById(R.id.tv_red_value);
        TextView tvGreenValue = dialogView.findViewById(R.id.tv_green_value);
        TextView tvBlueValue = dialogView.findViewById(R.id.tv_blue_value);
        TextView tvColorPreview = dialogView.findViewById(R.id.tv_color_preview);

        final int[] currentColor = {ThemeColorManager.getThemeColor(requireContext())};

        tvColorPreview.setBackgroundColor(currentColor[0]);
        tvColorPreview.setText(String.format("#%06X", 0xFFFFFF & currentColor[0]));
        seekBarRed.setProgress((currentColor[0] >> 16) & 0xFF);
        seekBarGreen.setProgress((currentColor[0] >> 8) & 0xFF);
        seekBarBlue.setProgress(currentColor[0] & 0xFF);
        tvRedValue.setText(String.valueOf(seekBarRed.getProgress()));
        tvGreenValue.setText(String.valueOf(seekBarGreen.getProgress()));
        tvBlueValue.setText(String.valueOf(seekBarBlue.getProgress()));

        for (int i = 0; i < ThemeColorManager.PRESET_COLORS.length; i++) {
            final int color = ThemeColorManager.PRESET_COLORS[i];
            View colorView = new View(requireContext());
            int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, getResources().getDisplayMetrics());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(8, 0, 8, 0);
            colorView.setLayoutParams(params);
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            drawable.setColor(color);
            drawable.setCornerRadius(8);
            colorView.setBackground(drawable);

            final int index = i;
            colorView.setOnClickListener(v -> {
                int c = ThemeColorManager.PRESET_COLORS[index];
                seekBarRed.setProgress((c >> 16) & 0xFF);
                seekBarGreen.setProgress((c >> 8) & 0xFF);
                seekBarBlue.setProgress(c & 0xFF);
                currentColor[0] = c;
                tvColorPreview.setBackgroundColor(c);
                tvColorPreview.setText(String.format("#%06X", 0xFFFFFF & c));
            });
            layoutPresetColors.addView(colorView);
        }

        SeekBar.OnSeekBarChangeListener rgbListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int r = seekBarRed.getProgress();
                int g = seekBarGreen.getProgress();
                int b = seekBarBlue.getProgress();
                currentColor[0] = 0xFF000000 | (r << 16) | (g << 8) | b;
                tvColorPreview.setBackgroundColor(currentColor[0]);
                tvColorPreview.setText(String.format("#%06X", 0xFFFFFF & currentColor[0]));
                tvRedValue.setText(String.valueOf(r));
                tvGreenValue.setText(String.valueOf(g));
                tvBlueValue.setText(String.valueOf(b));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekBarRed.setOnSeekBarChangeListener(rgbListener);
        seekBarGreen.setOnSeekBarChangeListener(rgbListener);
        seekBarBlue.setOnSeekBarChangeListener(rgbListener);

        AlertDialog dialog = builder.setPositiveButton("应用", (d, w) -> {
            boolean isPreset = false;
            for (int i = 0; i < ThemeColorManager.PRESET_COLORS.length; i++) {
                if (ThemeColorManager.PRESET_COLORS[i] == currentColor[0]) {
                    ThemeColorManager.setThemeColor(requireContext(), currentColor[0]);
                    isPreset = true;
                    break;
                }
            }
            if (!isPreset) {
                ThemeColorManager.setCustomColor(requireContext(), currentColor[0]);
            }
            tvThemeColorPreview.setBackgroundColor(currentColor[0]);
            requireActivity().recreate();
        })
        .setNegativeButton("取消", null)
        .create();

        dialog.show();
    }

    // ======================== 更新日志 ========================

    private void showChangelogDialog() {
        String changelog = readChangelogFromAssets();

        TextView textView = new TextView(requireContext());
        textView.setText(changelog);
        textView.setTextSize(14);
        textView.setTextIsSelectable(true);
        textView.setPadding(48, 16, 48, 8);
        textView.setLineSpacing(6f, 1f);

        new AlertDialog.Builder(requireContext())
                .setTitle("更新日志")
                .setView(textView)
                .setPositiveButton("知道了", null)
                .show();
    }

    private String readChangelogFromAssets() {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = requireContext().getAssets().open("CHANGELOG.md");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# ")) continue;
                if (line.startsWith("## ")) {
                    sb.append("▎").append(line.substring(3)).append("\n");
                } else {
                    sb.append(line).append("\n");
                }
            }
            reader.close();
        } catch (IOException e) {
            sb.append("无法读取更新日志文件");
        }
        return sb.toString();
    }
}