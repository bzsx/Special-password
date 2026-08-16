package com.bzsx.password;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * UI 深浅色模式管理器：跟随系统 / 浅色 / 深色。
 */
public class UiThemeManager {

    private static final String PREFS_NAME = "ui_theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int MODE_FOLLOW_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private UiThemeManager() {}

    public static int getThemeMode(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_THEME_MODE, MODE_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(Context context, int mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public static void applyNightMode(Context context) {
        int mode = getThemeMode(context);
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}