package com.bzsx.password;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static CrashHandler instance;
    private Context context;

    private CrashHandler() {}

    public static synchronized CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // 把堆栈转成字符串
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        // 不用 Toast，直接弹窗
        // 注意：这里要用主线程 Handler 执行，否则可能在非主线程抛异常
        new Handler(Looper.getMainLooper()).post(() -> showErrorDialog(stackTrace));

        // 保持进程存活，不闪退（不要 killProcess / System.exit）
    }

    private void showErrorDialog(String errorInfo) {
        // 构建对话框内容
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_error, null);
        TextView tvError = dialogView.findViewById(R.id.tv_error_info);
        tvError.setText(errorInfo);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("错误了，错误信息如下")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("复制信息", (dialog, which) -> {
                    ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("error", errorInfo));
                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("知道了", (dialog, which) -> dialog.dismiss());

        // 当 UI 需要 Context 时，需要 Activity 类型的 Context
        // 如果使用 applicationContext 弹窗，需要加 type 适配
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }
        dialog.show();
    }
}