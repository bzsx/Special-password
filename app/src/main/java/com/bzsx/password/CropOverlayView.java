package com.bzsx.password;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

public class CropOverlayView extends View {

    private Paint borderPaint;
    private Paint maskPaint;
    private Paint cornerPaint;
    private float cropLeft, cropTop, cropRight, cropBottom;
    private Path clipPath;
    private int strokeColor = Color.WHITE;

    public CropOverlayView(Context context) {
        super(context);
        init();
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.argb(160, 0, 0, 0));

        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(6f);

        clipPath = new Path();
    }

    /**
     * 设置裁剪框的位置（相对于View的坐标）
     */
    public void setCropRect(float left, float top, float right, float bottom) {
        this.cropLeft = left;
        this.cropTop = top;
        this.cropRight = right;
        this.cropBottom = bottom;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        // 绘制半透明蒙层（裁剪框外部变暗）
        // 用4个矩形覆盖裁剪框外区域
        // 上方
        canvas.drawRect(0, 0, w, cropTop, maskPaint);
        // 下方
        canvas.drawRect(0, cropBottom, w, h, maskPaint);
        // 左方
        canvas.drawRect(0, cropTop, cropLeft, cropBottom, maskPaint);
        // 右方
        canvas.drawRect(cropRight, cropTop, w, cropBottom, maskPaint);

        // 绘制裁剪框边框
        canvas.drawRect(cropLeft, cropTop, cropRight, cropBottom, borderPaint);

        // 绘制4个角标
        float cornerLen = 40f;
        // 左上角
        canvas.drawLine(cropLeft, cropTop, cropLeft + cornerLen, cropTop, cornerPaint);
        canvas.drawLine(cropLeft, cropTop, cropLeft, cropTop + cornerLen, cornerPaint);
        // 右上角
        canvas.drawLine(cropRight, cropTop, cropRight - cornerLen, cropTop, cornerPaint);
        canvas.drawLine(cropRight, cropTop, cropRight, cropTop + cornerLen, cornerPaint);
        // 左下角
        canvas.drawLine(cropLeft, cropBottom, cropLeft + cornerLen, cropBottom, cornerPaint);
        canvas.drawLine(cropLeft, cropBottom, cropLeft, cropBottom - cornerLen, cornerPaint);
        // 右下角
        canvas.drawLine(cropRight, cropBottom, cropRight - cornerLen, cropBottom, cornerPaint);
        canvas.drawLine(cropRight, cropBottom, cropRight, cropBottom - cornerLen, cornerPaint);
    }

    public float getCropLeft() { return cropLeft; }
    public float getCropTop() { return cropTop; }
    public float getCropRight() { return cropRight; }
    public float getCropBottom() { return cropBottom; }
}
