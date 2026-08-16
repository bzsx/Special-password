package com.bzsx.password;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * 解锁方式管理工具。
 * 支持两种解锁方式：
 *  - password：使用应用内主密码（默认）
 *  - system  ：使用系统锁屏（PIN / 图案 / 密码 / 生物识别）
 */
public class UnlockPrefs {

    private static final String PREFS_NAME = "unlock_prefs";
    private static final String KEY_MODE = "unlock_mode";

    public static final String MODE_PASSWORD = "password";
    public static final String MODE_SYSTEM = "system";

    private UnlockPrefs() {}

    /** 当前解锁方式（默认主密码） */
    public static String getMode(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MODE, MODE_PASSWORD);
    }

    /** 设置解锁方式 */
    public static void setMode(Context context, String mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_MODE, mode).apply();
    }

    /** 当前是否为系统锁屏模式 */
    public static boolean isSystemMode(Context context) {
        return MODE_SYSTEM.equals(getMode(context));
    }

    /** 设备是否已设置系统锁屏（PIN/图案/密码/生物识别） */
    public static boolean hasSystemLock(Context context) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && km.isDeviceSecure();
    }
}
