package com.bzsx.password;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 开屏页（Splash）
 * 打字机效果：先打英文 Special Password，光标左移回行首，再补中文 神奇的密码 ·，
 * 最终一整行显示：神奇的密码 · Special Password。
 * 下方 slogan 与底部署名由透明渐变浮现。右上角灰色"跳过"可随时跳过，进入锁屏页。
 */
public class SplashActivity extends AppCompatActivity {

    // 播放开关的 SharedPreferences
    private static final String PREFS_NAME = "splash_prefs";
    private static final String KEY_ENABLED = "splash_enabled";

    // 完整标题串（中文在前 · 英文在后）
    private static final String CHINESE = "神奇的密码 · ";
    private static final String ENGLISH = "Special Password";

    // 时间（毫秒）
    private static final int CHAR_INTERVAL_EN = 125;   // 英文每字间隔
    private static final int CURSOR_MOVE_DURATION = 450; // 光标左移时长
    private static final int CHAR_INTERVAL_CN = 300;   // 中文每字间隔
    private static final int SLOGAN_DELAY = 250;       // slogan 开始延时（中文打完后）
    private static final int SLOGAN_DURATION = 1200;   // slogan 渐变时长
    private static final int MADE_BY_DELAY = 600;      // 署名开始延时（中文打完后）
    private static final int TOTAL_DURATION = 7300;    // 总时长（自动跳转）

    private TextView tvMainTitle;
    private TextView tvCursor;
    private TextView tvSlogan;
    private TextView tvMadeBy;
    private View rootView;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable blinkRunnable = new Runnable() {
        private boolean visible = true;
        @Override
        public void run() {
            if (isSkipped) return;
            tvCursor.setAlpha(visible ? 1f : 0.12f);
            visible = !visible;
            handler.postDelayed(this, 260);
        }
    };

    private int typedCn = 0;   // 已打出的中文字段字符数（"神奇的密码 · "共8字符）
    private int typedEn = 0;   // 已打出的英文字母数
    private boolean isSkipped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiThemeManager.applyNightMode(this);
        super.onCreate(savedInstanceState);

        // 如果用户关闭了开屏，直接进锁屏页
        if (!isSplashEnabled(this)) {
            goToLock();
            return;
        }

        setContentView(R.layout.activity_splash);

        tvMainTitle = findViewById(R.id.tv_main_title);
        tvCursor = findViewById(R.id.tv_cursor);
        tvSlogan = findViewById(R.id.tv_slogan);
        tvMadeBy = findViewById(R.id.tv_madeby);
        rootView = findViewById(R.id.splash_root);

        // 初始：文字为空（英文阶段从 0 开始打字）
        typedEn = 0;
        typedCn = 0;
        renderText();

        // 光标初始不可见，等开始打字
        tvCursor.setVisibility(View.INVISIBLE);

        // 右上角跳过
        findViewById(R.id.tv_skip).setOnClickListener(v -> skip());

        startEnglishTyping();
        autoGoToLock();
    }

    /**
     * 渲染主标题：文字内容始终是"当前真实已显示的部分"（无透明占位），
     * TextView 高度可变、父容器居中，因此文字块始终实时居中。
     * - 英文阶段：只显示已打出的英文
     * - 中文阶段：中文逐个补在前、英文固定在后
     */
    private void renderText() {
        String text;
        if (typedEn < ENGLISH.length()) {
            // 英文打字阶段：仅显示英文已打出部分
            text = ENGLISH.substring(0, typedEn);
        } else {
            // 中文阶段：中文在前 + 英文在后
            text = CHINESE.substring(0, typedCn) + ENGLISH;
        }
        tvMainTitle.setText(text);
    }

    /**
     * 计算光标应处的 x 位置（相对主标题 TextView 左边缘）。
     * - 英文阶段：光标紧跟已显示英文末尾
     * - 中文阶段：光标紧跟已显示中文末尾（英文在右侧，保持不换行）
     */
    private float cursorX() {
        if (typedEn < ENGLISH.length()) {
            return tvMainTitle.getPaint().measureText(ENGLISH.substring(0, typedEn));
        }
        return tvMainTitle.getPaint().measureText(CHINESE.substring(0, typedCn));
    }

    /**
     * 阶段1：英文打字机。
     */
    private void startEnglishTyping() {
        tvCursor.setVisibility(View.VISIBLE);
        // 光标先放在文末（英文末尾）
        positionCursor(cursorX());
        blinkCursor();

        Runnable typeEn = new Runnable() {
            @Override
            public void run() {
                if (isSkipped) return;
                if (typedEn < ENGLISH.length()) {
                    typedEn++;
                    renderText();
                    positionCursor(cursorX());
                    handler.postDelayed(this, CHAR_INTERVAL_EN);
                } else {
                    // 英文打完，开始光标左移
                    moveCursorBackToStart();
                }
            }
        };
        handler.postDelayed(typeEn, 400);
    }

    /**
     * 阶段2：光标从左移回行首（此时英文整行显示在右，中文占位透明在左）。
     */
    private void moveCursorBackToStart() {
        // 英文已完成：光标当前在英文末尾 = 英文全宽，左移回行首（x=0）
        float startX = tvMainTitle.getPaint().measureText(ENGLISH);
        float endX = 0f;
        ValueAnimator anim = ValueAnimator.ofFloat(startX, endX);
        anim.setDuration(CURSOR_MOVE_DURATION);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> tvCursor.setTranslationX((Float) a.getAnimatedValue()));
        anim.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
            @Override public void onAnimationEnd(Animator animation) {
                if (!isSkipped) startChineseTyping();
            }
        });
        anim.start();
    }

    /**
     * 阶段3：中文打字机（在行首逐个补出中文）。
     */
    private void startChineseTyping() {
        // 进入中文阶段前，确保英文已全部显示
        typedEn = ENGLISH.length();
        renderText();

        Runnable typeCn = new Runnable() {
            @Override
            public void run() {
                if (isSkipped) return;
                if (typedCn < CHINESE.length()) {
                    typedCn++;
                    renderText();
                    positionCursor(cursorX());
                    handler.postDelayed(this, CHAR_INTERVAL_CN);
                } else {
                    // 中文打完：光标直接消失，再开始下方文字浮现
                    tvCursor.setVisibility(View.GONE);
                    blinkHandlerRemove();
                    startBottomReveal();
                }
            }
        };
        handler.postDelayed(typeCn, 100);
    }

    /**
     * 阶段4：slogan 与署名渐变浮现。
     */
    private void startBottomReveal() {
        // slogan：淡入 + 轻微上浮
        startFade(tvSlogan, SLOGAN_DELAY, SLOGAN_DURATION, 16f);
        // 署名：仅淡入
        startFade(tvMadeBy, MADE_BY_DELAY, 800, 0f);
    }

    private void startFade(View v, long startDelay, long duration, float riseDistance) {
        v.setAlpha(0f);
        if (riseDistance > 0) v.setTranslationY(riseDistance);
        v.postDelayed(() -> {
            if (isSkipped) return;
            AnimatorSet set = new AnimatorSet();
            ObjectAnimator fade = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f);
            fade.setDuration(duration);
            if (riseDistance > 0) {
                ObjectAnimator rise = ObjectAnimator.ofFloat(v, "translationY", riseDistance, 0f);
                rise.setDuration(duration);
                set.playTogether(fade, rise);
            } else {
                set.play(fade);
            }
            set.setInterpolator(new DecelerateInterpolator());
            set.start();
        }, startDelay);
    }

    /**
     * 将光标精确定位到主标题某字符位置（用 translationX 覆盖，不移入布局流）。
     */
    private void positionCursor(float x) {
        tvCursor.setTranslationX(x);
    }

    /**
     * 光标绿色闪烁。
     */
    private void blinkCursor() {
        tvCursor.setAlpha(1f);
        handler.postDelayed(blinkRunnable, 300);
    }

    /**
     * 停止光标闪烁。
     */
    private void blinkHandlerRemove() {
        handler.removeCallbacks(blinkRunnable);
    }

    /**
     * 跳过：取消所有动画，立即进锁屏页。
     */
    private void skip() {
        isSkipped = true;
        handler.removeCallbacksAndMessages(null);
        goToLock();
    }

    private void goToLock() {
        Intent intent = new Intent(this, LockScreenActivity.class);
        startActivity(intent);
        finish();
        // 淡入锁屏页过渡
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 自动跳转（总时长到）。
     */
    private void autoGoToLock() {
        handler.postDelayed(() -> {
            if (isSkipped) return;
            isSkipped = true;
            // 整体淡出
            ObjectAnimator fade = ObjectAnimator.ofFloat(rootView, "alpha", 1f, 0f);
            fade.setDuration(400);
            fade.addListener(new Animator.AnimatorListener() {
                @Override public void onAnimationStart(Animator animation) {}
                @Override public void onAnimationCancel(Animator animation) {}
                @Override public void onAnimationRepeat(Animator animation) {}
                @Override public void onAnimationEnd(Animator animation) {
                    goToLock();
                }
            });
            fade.start();
        }, TOTAL_DURATION);
    }

    /**
     * 开屏开关读取。
     */
    public static boolean isSplashEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public static void setSplashEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 若是从锁屏返回（用户关闭开屏直接进入的场景），此 Activity 已 finish，不处理
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
