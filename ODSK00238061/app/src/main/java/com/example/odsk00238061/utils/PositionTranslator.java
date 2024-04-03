package com.example.odsk00238061.utils;

import android.graphics.Rect;

public class PositionTranslator {
    private final int sourceImageWidth;
    private final int sourceImageHeight;
    private final int targetViewWidth;
    private final int targetViewHeight;
    private final int phoneRotation;

    public PositionTranslator(int sourceImageWidth, int sourceImageHeight, int targetViewWidth, int targetViewHeight, int phoneRotation) {
        this.sourceImageWidth = sourceImageWidth;
        this.sourceImageHeight = sourceImageHeight;
        this.targetViewWidth = targetViewWidth;
        this.targetViewHeight = targetViewHeight;
        this.phoneRotation = phoneRotation;
    }

    public Rect translateBoundingBox(Rect boundingBox) {
        // Calculate scaling factors
        float scaleX = (float) targetViewWidth / sourceImageWidth;
        float scaleY = (float) targetViewHeight / sourceImageHeight;

        // Translate bounding box coordinates
        int left = (int) (boundingBox.left * scaleX);
        int top = (int) (boundingBox.top * scaleY);
        int right = (int) (boundingBox.right * scaleX);
        int bottom = (int) (boundingBox.bottom * scaleY);

        // Adjust for phone rotation
        switch (phoneRotation) {
            case 90:
                return new Rect(top, targetViewWidth - right, bottom, targetViewWidth - left);
            case 180:
                return new Rect(targetViewWidth - right, targetViewHeight - bottom, targetViewWidth - left, targetViewHeight - top);
            case 270:
                return new Rect(targetViewHeight - bottom, left, targetViewHeight - top, right);
            default:
                return new Rect(left, top, right, bottom);
        }
    }
}
