package com.example.odsk00238061.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ProjectHelper {
    @NonNull
    public static String getLabelWithHighestConfidence(List<DetectedObject.Label> labels){

        String maxLabel = "";
        double maxConfidence = Double.MIN_VALUE;

        if(!labels.isEmpty()){
            for (DetectedObject.Label label : labels) {
                if (label.getConfidence() > maxConfidence && !label.getText().isEmpty()) {
                    maxConfidence = label.getConfidence();
                    maxLabel = label.getText();
                }
            }
        }
        return maxLabel;
    }

    public static String calculateObstacleLocation(Rect rect, int pWidth){
        int previewWidth = pWidth;
        int rectCenterX = rect.centerX();

        int leftThreshold = previewWidth / 4;
        int rightThreshold = 3 * previewWidth / 4;

        if (rectCenterX < leftThreshold) {
            if (rectCenterX < previewWidth / 8) {
                return "Far Left";
            } else {
                return "Left Ahead";
            }
        } else if (rectCenterX < previewWidth / 2) {
            if (rectCenterX < previewWidth * 3 / 8) {
                return "Center Left";
            } else {
                return "Center Ahead";
            }
        } else if (rectCenterX == previewWidth / 2) {
            return "Center";
        } else if (rectCenterX < rightThreshold) {
            if (rectCenterX < previewWidth * 5 / 8) {
                return "Center Right";
            } else {
                return "Right Ahead";
            }
        } else {
            if (rectCenterX < previewWidth * 7 / 8) {
                return "Far Right";
            } else {
                return "Right Ahead";
            }
        }
    }

    public static Matrix getMappingMatrix(ImageProxy imageProxy, PreviewView previewView) {
        Rect cropRect = imageProxy.getCropRect();
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        Matrix matrix = new Matrix();

        // A float array of the source vertices (crop rect) in clockwise order.
        float[] source = {
                cropRect.left,
                cropRect.top,
                cropRect.right,
                cropRect.top,
                cropRect.right,
                cropRect.bottom,
                cropRect.left,
                cropRect.bottom
        };

        // A float array of the destination vertices in clockwise order.
        float[] destination = {
                0f,
                0f,
                previewView.getWidth(),
                0f,
                previewView.getWidth(),
                previewView.getHeight(),
                0f,
                previewView.getHeight()
        };

        // The destination vertexes need to be shifted based on rotation degrees.
        // The rotation degree represents the clockwise rotation needed to correct
        // the image.

        // Each vertex is represented by 2 float numbers in the vertices array.
        int vertexSize = 2;
        // The destination needs to be shifted 1 vertex for every 90° rotation.
        int shiftOffset = rotationDegrees / 90 * vertexSize;
        float[] tempArray = destination.clone();
        for (int toIndex = 0; toIndex < source.length; toIndex++) {
            int fromIndex = (toIndex + shiftOffset) % source.length;
            destination[toIndex] = tempArray[fromIndex];
        }
        matrix.setPolyToPoly(source, 0, destination, 0, 4);
        return matrix;
    }

    public static Rect mapBoundingBox(Rect boundingBox, Matrix mappingMatrix) {
        // Create arrays to hold the coordinates of the bounding box
        float[] src = {boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom};
        float[] dst = new float[4];

        // Apply the transformation matrix to map the bounding box coordinates
        mappingMatrix.mapPoints(dst, src);

        // Create a new Rect object with the mapped coordinates and return it
        return new Rect((int) dst[0], (int) dst[1], (int) dst[2], (int) dst[3]);
    }

    public static float getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = (level / (float) scale) * 100;
        return batteryPct;
    }

    public static Rect updateBoundingBoxBasedOnPreview(Rect boundingbox, PreviewView view, InputImage inputImage) {
        float scaleX = (float) view.getWidth() / inputImage.getWidth();
        float scaleY = (float) view.getHeight() / inputImage.getHeight();

        int scaledLeft = (int) (boundingbox.left * scaleX);
        int scaledTop = (int) (boundingbox.top * scaleY);
        int scaledRight = (int) (boundingbox.right * scaleX);
        int scaledBottom = (int) (boundingbox.bottom * scaleY);

        // Create a new Rect with scaled coordinates
        return new Rect(scaledLeft, scaledTop, scaledRight, scaledBottom);
    }

}
