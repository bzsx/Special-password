package com.bzsx.password;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class CropDialog extends Dialog {

    private Uri sourceUri;
    private Bitmap sourceBitmap;
    private ImageView ivCropImage;
    private View cropOverlay;
    private TextView tvCancel, tvConfirm;
    private CropOverlayView cropOverlayView;

    // 图片变换
    private Matrix imageMatrix = new Matrix();
    private float totalScale = 1f;
    private float offsetX = 0f, offsetY = 0f;
    private float baseScale = 1f;

    // 手势
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float lastFocusX, lastFocusY;
    private boolean isScaling = false;

    // 屏幕宽高
    private int screenW, screenH;

    public CropDialog(Context context, Uri sourceUri) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.sourceUri = sourceUri;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        if (win != null) {
            win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }

        setContentView(R.layout.dialog_crop_background);

        // 获取屏幕尺寸
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;

        ivCropImage = findViewById(R.id.iv_crop_image);
        cropOverlay = findViewById(R.id.view_crop_overlay);
        tvCancel = findViewById(R.id.tv_crop_cancel);
        tvConfirm = findViewById(R.id.tv_crop_confirm);

        // 加载图片
        loadSourceImage();

        // 初始化裁剪覆盖层（用CropOverlayView替换View）
        ViewGroup overlayParent = (ViewGroup) cropOverlay.getParent();
        int index = overlayParent.indexOfChild(cropOverlay);
        overlayParent.removeView(cropOverlay);
        cropOverlayView = new CropOverlayView(getContext());
        cropOverlayView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        overlayParent.addView(cropOverlayView, index);

        // 设置裁剪框——正好是整个屏幕区域（按屏幕宽高比）
        cropOverlayView.setCropRect(0, 0, screenW, screenH);

        // 手势
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        ivCropImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        isScaling = true;
                        lastFocusX = (event.getX(0) + event.getX(1)) / 2;
                        lastFocusY = (event.getY(0) + event.getY(1)) / 2;
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        isScaling = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (!isScaling && event.getPointerCount() == 1) {
                            // 单指拖动
                            offsetX += event.getX() - lastFocusX;
                            offsetY += event.getY() - lastFocusY;
                            lastFocusX = event.getX();
                            lastFocusY = event.getY();
                            applyTransform();
                        } else if (event.getPointerCount() >= 2) {
                            // 双指移动（缩放时一起移动）
                            float cx = (event.getX(0) + event.getX(1)) / 2;
                            float cy = (event.getY(0) + event.getY(1)) / 2;
                            if (lastFocusX != 0 && lastFocusY != 0) {
                                offsetX += cx - lastFocusX;
                                offsetY += cy - lastFocusY;
                            }
                            lastFocusX = cx;
                            lastFocusY = cy;
                            applyTransform();
                        }
                        break;
                    case MotionEvent.ACTION_DOWN:
                        lastFocusX = event.getX();
                        lastFocusY = event.getY();
                        break;
                }
                return true;
            }
        });

        tvCancel.setOnClickListener(v -> dismiss());
        tvConfirm.setOnClickListener(v -> doCrop());
    }

    private void loadSourceImage() {
        try {
            // 先读取图片尺寸，计算合适的初始缩放
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(
                    getContext().getContentResolver().openInputStream(sourceUri),
                    null, opts);
            int imgW = opts.outWidth;
            int imgH = opts.outHeight;

            // 采样加载
            int sample = 1;
            while (imgW / sample > screenW * 2 && imgH / sample > screenH * 2) {
                sample *= 2;
            }
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            sourceBitmap = BitmapFactory.decodeStream(
                    getContext().getContentResolver().openInputStream(sourceUri),
                    null, opts);

            if (sourceBitmap == null) {
                Toast.makeText(getContext(), "无法加载图片", Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }

            // 计算初始缩放——让图片至少填满屏幕
            float scaleX = (float) screenW / sourceBitmap.getWidth();
            float scaleY = (float) screenH / sourceBitmap.getHeight();
            baseScale = Math.max(scaleX, scaleY);
            totalScale = baseScale;

            // 居中显示
            offsetX = (screenW - sourceBitmap.getWidth() * totalScale) / 2;
            offsetY = (screenH - sourceBitmap.getHeight() * totalScale) / 2;

            ivCropImage.setImageBitmap(sourceBitmap);
            applyTransform();

        } catch (Exception e) {
            Toast.makeText(getContext(), "加载图片失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            dismiss();
        }
    }

    private void applyTransform() {
        Matrix m = new Matrix();
        m.postTranslate(offsetX, offsetY);
        m.postScale(totalScale, totalScale, offsetX, offsetY);
        // 实际缩放中心应该是图片中心
        // 改用标准矩阵
        Matrix matrix = new Matrix();
        matrix.postTranslate(offsetX, offsetY);
        matrix.postScale(totalScale, totalScale, offsetX, offsetY);
        ivCropImage.setImageMatrix(matrix);
    }

    private void doCrop() {
        if (sourceBitmap == null) return;

        try {
            // 计算图片在原始坐标系中的裁剪区域
            // 屏幕裁剪区域：[0, 0, screenW, screenH] 对应到图片上的区域
            float imgLeft = -offsetX / totalScale;
            float imgTop = -offsetY / totalScale;
            float imgRight = imgLeft + screenW / totalScale;
            float imgBottom = imgTop + screenH / totalScale;

            // 限制到图片边界
            imgLeft = Math.max(0, imgLeft);
            imgTop = Math.max(0, imgTop);
            imgRight = Math.min(sourceBitmap.getWidth(), imgRight);
            imgBottom = Math.min(sourceBitmap.getHeight(), imgBottom);

            int cropW = (int) (imgRight - imgLeft);
            int cropH = (int) (imgBottom - imgTop);
            if (cropW <= 0 || cropH <= 0) {
                Toast.makeText(getContext(), "裁剪区域无效", Toast.LENGTH_SHORT).show();
                return;
            }

            // 裁剪
            Bitmap cropped = Bitmap.createBitmap(sourceBitmap,
                    (int) imgLeft, (int) imgTop, cropW, cropH);

            // 保存到缓存文件
            File cacheFile = new File(getContext().getCacheDir(), "crop_temp.png");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            cropped.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            cropped.recycle();

            // 回调
            if (listener != null) {
                listener.onCropped(cacheFile);
            }
            dismiss();

        } catch (Exception e) {
            Toast.makeText(getContext(), "裁剪失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (listener != null) {
            listener.onCancel();
        }
        super.onBackPressed();
    }

    @Override
    public void dismiss() {
        if (sourceBitmap != null && !sourceBitmap.isRecycled()) {
            sourceBitmap.recycle();
            sourceBitmap = null;
        }
        super.dismiss();
    }

    // ============== 手势 ==============

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            totalScale *= scale;
            // 限制缩放范围
            totalScale = Math.max(baseScale * 0.5f, Math.min(totalScale, baseScale * 5f));
            applyTransform();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // 双击复位
            totalScale = baseScale;
            offsetX = (screenW - sourceBitmap.getWidth() * totalScale) / 2;
            offsetY = (screenH - sourceBitmap.getHeight() * totalScale) / 2;
            applyTransform();
            return true;
        }
    }

    // ============== 回调 ==============

    private CropListener listener;

    public void setCropListener(CropListener listener) {
        this.listener = listener;
    }

    public interface CropListener {
        void onCropped(File croppedFile);
        void onCancel();
    }
}
