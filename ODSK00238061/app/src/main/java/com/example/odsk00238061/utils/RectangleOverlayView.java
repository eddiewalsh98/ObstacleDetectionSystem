package com.example.odsk00238061.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class RectangleOverlayView extends View {
    private Paint paint;
    private int left, top, right, bottom;

    public RectangleOverlayView(Context context) {
        super(context);
        init();
    }

    public RectangleOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
    }

    // Method to update the position and size of the rectangle
    public void updateRect(Rect boundingBox) {
        this.left = boundingBox.left;
        this.top = boundingBox.top;
        this.right = boundingBox.right;
        this.bottom = boundingBox.bottom;
        invalidate(); // Trigger a redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(left, top, right, bottom, paint);
    }
}