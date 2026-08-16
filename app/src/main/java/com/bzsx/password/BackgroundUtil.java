package com.bzsx.password;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

import java.io.File;

public class BackgroundUtil {

    private static final String PREFS_BG = "background_prefs";
    private static final String KEY_BG_ENABLED = "background_enabled";
    private static final String KEY_BG_ALPHA = "background_alpha";
    private static final String BG_IMAGE_FILENAME = "app_background.png";

    public static boolean isBackgroundEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_BG, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_BG_ENABLED, false);
    }

    public static int getBackgroundAlpha(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_BG, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_BG_ALPHA, 80);
    }

    public static File getBgImageFile(Context context) {
        return new File(context.getFilesDir(), BG_IMAGE_FILENAME);
    }

    public static void applyBackground(Context context, View rootView) {
        if (rootView == null) return;
        if (!isBackgroundEnabled(context)) return;

        File bgFile = getBgImageFile(context);
        if (!bgFile.exists()) return;

        Bitmap bitmap = BitmapFactory.decodeFile(bgFile.getAbsolutePath());
        if (bitmap != null) {
            int alpha = getBackgroundAlpha(context);
            BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
            drawable.setAlpha((int) (alpha * 2.55f));
            rootView.setBackground(drawable);
        }
    }
}