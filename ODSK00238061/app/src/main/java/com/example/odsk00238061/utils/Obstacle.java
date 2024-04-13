package com.example.odsk00238061.utils;

import android.graphics.Rect;
import android.view.View;

import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.objects.DetectedObject;

import java.util.List;

public class Obstacle {

    private Integer obstacleID;

    private String obstacleName;

    private String obstacleLocation;

    private Integer obstacleOccurence;

    private Rect obstacleRect;

    private Boolean isObstacle;

    private PreviewView previewView;

    public Obstacle(){ }

    public Obstacle(DetectedObject object, Rect boundingBox, int previewWidth){
        if(object.getLabels().isEmpty()) {
            this.obstacleName = "Unknown Obstacle";
        } else {
            this.obstacleName = getLabelWithHighestConfidence(object.getLabels());
        }
        this.obstacleID = object.getTrackingId();
        this.obstacleRect = object.getBoundingBox();
        this.obstacleOccurence = 0;
        this.obstacleLocation = calculateObstacleLocation(obstacleRect, previewWidth);
        this.isObstacle = true;
    }

    public Obstacle(Integer obstacleID, String obstacleName, String obstacleLocation, Integer obstacleOccurence){
        this.obstacleID = obstacleID;
        this.obstacleName = obstacleName;
        this.obstacleLocation = obstacleLocation;
        this.obstacleOccurence = obstacleOccurence;
        isObstacle = false;
    }

    public Integer getObstacleID(){
        return obstacleID;
    }

    public void setObstacleID(Integer obstacleID){
        this.obstacleID = obstacleID;
    }

    public String getObstacleName(){
        return obstacleName;
    }

    public void setObstacleName(String obstacleName){
        this.obstacleName = obstacleName;
    }

    public String getObstacleLocation(){
        return obstacleLocation;
    }

    public void setObstacleLocation(String obstacleLocation){
        this.obstacleLocation = obstacleLocation;
    }

    public void setObstacleLocation(Rect obstacle) {
        this.obstacleRect = obstacle;
    }

    public Rect getObstacleRect() {
        return obstacleRect;
    }

    public Integer getObstacleOccurence(){
        return obstacleOccurence;
    }

    public void incrementObstacleOccurence(){
        this.obstacleOccurence++;
    }

    public Boolean getIsObstacle(){
        return isObstacle;
    }

    public void setIsObstacle(Boolean isObstacle){
        this.isObstacle = isObstacle;
    }

    public void setPreviewView(PreviewView view) { this.previewView = view; }

    public String getLabelWithHighestConfidence(List<DetectedObject.Label> labels) {
        String maxLabel = "";
        float maxConfidence = Float.MIN_VALUE; // Initialize to the smallest possible value
        for (DetectedObject.Label label : labels) {
            float confidence = label.getConfidence();
            if (confidence > maxConfidence) {
                maxConfidence = confidence;
                maxLabel = label.getText();
            }
        }
        return maxLabel;
    }

    private String calculateObstacleLocation(Rect rect, int pWidth) {
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
}
