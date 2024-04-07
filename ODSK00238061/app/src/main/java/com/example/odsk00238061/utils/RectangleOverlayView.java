package com.example.odsk00238061.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.camera.core.processing.SurfaceProcessorNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RectangleOverlayView extends View {
    private Paint paint;
    private Paint textPaint;
    private int left, top, right, bottom;
    private String text = "";
    private List<Integer> Colours = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE);

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

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30);
    }

    // Method to update the position and size of the rectangle
    public void updateRect(Rect boundingBox, String text) {

        if(!text.equals(this.text)){
            changeRectangleColor();
        }

        this.left = boundingBox.left;
        this.top = boundingBox.top;
        this.right = boundingBox.right;
        this.bottom = boundingBox.bottom;
        this.text = text;
        invalidate(); // Trigger a redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(left, top, right, bottom, paint);
        if (!text.isEmpty()) {
            float textWidth = textPaint.measureText(text);
            float x = left + (right - left - textWidth) / 2;
            float y = top + (bottom - top) / 2;
            canvas.drawText(text, x, y, textPaint);
        }

        //canvas.restore();
    }

    private void changeRectangleColor() {
        int randomIndex = (int) (Math.random() * Colours.size());
        int newColor = Colours.get(randomIndex);

        while (newColor == paint.getColor()){
            randomIndex = (int) (Math.random() * Colours.size());
            newColor = Colours.get(randomIndex);
        }

        paint.setColor(newColor);
    }
}

