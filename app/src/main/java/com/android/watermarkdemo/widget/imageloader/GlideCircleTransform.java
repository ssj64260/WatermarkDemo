package com.android.watermarkdemo.widget.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

/**
 * Glide控件 画圆型类
 */
public class GlideCircleTransform extends BitmapTransformation {

    private float borderThickness = 0;//像素px
    private int red = 0;
    private int green = 0;
    private int blue = 0;
    private int alpha = 0;

    public GlideCircleTransform setColor(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        return this;
    }

    public GlideCircleTransform setBorderThickness(int px) {
        this.borderThickness = px;
        return this;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        return circleCrop(pool, toTransform);
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {

    }

    private Bitmap circleCrop(BitmapPool pool, Bitmap source) {
        if (source == null) return null;

        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);

        Bitmap result = pool.get(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(result);

        float r = size / 2f;
        if (alpha != 0) {
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.rgb(red, green, blue));
            bgPaint.setAntiAlias(true);
            canvas.drawCircle(r, r, r, bgPaint);
        }

        Paint paint = new Paint();
        paint.setShader(new BitmapShader(squared, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
        paint.setAntiAlias(true);
        canvas.drawCircle(r, r, r - borderThickness, paint);
        return result;
    }

}