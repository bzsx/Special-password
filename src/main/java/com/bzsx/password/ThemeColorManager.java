package com.bzsx.password;
import android.content.Context;
import android.content.SharedPreferences;


/**
 * 主题色管理工具类
 * 支持预选颜色和手动调色，持久化保存用户选择的主题色
 * 不影响背景色，仅改变主色调（标题栏、按钮、选中态等）
 */
public class ThemeColorManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_COLOR = "theme_color";
    private static final String KEY_CUSTOM_COLOR = "custom_theme_color";

    /** 预选颜色列表（名称, 颜色值） */
    public static final String[] PRESET_NAMES = {"绿色", "蓝色", "紫色", "红色", "橙色", "青色"};
    public static final int[] PRESET_COLORS = {
            0xFF2E7D32, // 绿色
            0xFF1565C0, // 蓝色
            0xFF7B1FA2, // 紫色
            0xFFC62828, // 红色
            0xFFE65100, // 橙色
            0xFF00695C  // 青色
    };

    /** 获取当前主题色（默认绿色） */
    public static int getThemeColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean useCustom = prefs.getBoolean("use_custom_color", false);
        if (useCustom) {
            return prefs.getInt(KEY_CUSTOM_COLOR, 0xFF2E7D32);
        }
        return prefs.getInt(KEY_THEME_COLOR, 0xFF2E7D32);
    }

    /** 设置预选主题色 */
    public static void setThemeColor(Context context, int color) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_THEME_COLOR, color)
                .putBoolean("use_custom_color", false)
                .apply();
    }

    /** 设置自定义颜色 */
    public static void setCustomColor(Context context, int color) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_CUSTOM_COLOR, color)
                .putBoolean("use_custom_color", true)
                .apply();
    }
}
