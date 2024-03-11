package com.example.odsk00238061.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class BoundingBox extends View {

    private float scaleFactor = 1.0f;
    private Integer boundingboxID;
    private Rect location;
    private String label;
    private Paint boxpaint;
    private Paint textpaint;

    public BoundingBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BoundingBox(Context context) {
        super(context);
        init();
    }
    public void init(){
        boxpaint = new Paint();
        boxpaint.setColor(Color.BLACK);
        boxpaint.setStrokeWidth(10f);
        boxpaint.setStyle(Paint.Style.STROKE);

        textpaint = new Paint();
        textpaint.setColor(Color.BLACK);
        textpaint.setTextSize(50f);
        textpaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(checkLocationandLabel()){
            canvas.drawRect(location.left, location.top, location.right, location.bottom, boxpaint);
            canvas.drawText(label, location.centerX(),location.centerY(), textpaint);
        }
    }

    public boolean checkLocationandLabel(){
        if(this.location != null && this.label != null){
            return true;
        }
        return false;
    }

    public Rect getLocation() {
        return location;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setBoundingBoxID(Integer id) {
        this.boundingboxID = id;
    }

    public Integer getBoundingBoxID() {
        return boundingboxID;
    }

    public float scale(float imagePixel) {
        return imagePixel * scaleFactor;
    }

}
