package com.android.watermarkdemo.utils;

import android.util.Size;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraUtils {

    //获取最大图像宽高 或 最大预览宽高
    public static Size getMaxSize(List<android.util.Size> sizes, int maxWidth, float rate) {
        Collections.sort(sizes, new CompareSizesByArea());

        for (android.util.Size size : sizes) {
            if (size.getWidth() <= maxWidth && equalRate(size, rate)) {
                return size;
            }
        }
        return sizes.get(0);
    }

    //计算size宽高比例是否近似于rate
    private static boolean equalRate(Size size, float rate) {
        float r = (float) (size.getWidth()) / (float) (size.getHeight());
        return Math.abs(r - rate) <= 0.03;
    }

    //比较两个Size的大小基于它们的area
    private static class CompareSizesByArea implements Comparator<Size> {
        public int compare(Size lhs, Size rhs) {
            if (lhs.getWidth() == rhs.getWidth()) {
                return 0;
            } else if (lhs.getWidth() > rhs.getWidth()) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
