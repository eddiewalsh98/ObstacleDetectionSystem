package com.example.odsk00238061.utils;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.objects.DetectedObject;

import java.util.List;

public class ProjectHelper {
    @NonNull
    public static String getLabelWithHighestConfidence(List<DetectedObject.Label> labels){

        String maxLabel = "Obstacle";
        double maxConfidence = Double.MIN_VALUE;

        if(!labels.isEmpty()){
            for (DetectedObject.Label label : labels) {
                if (label.getConfidence() > maxConfidence) {
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

    public static boolean checkDetectedObjectHasValidConfidence(DetectedObject object){
        return object.getLabels().stream()
                .anyMatch(label -> label.getConfidence() != 0.4);
    }


}
