package com.example.odsk00238061.utils;

import android.graphics.Rect;
import android.view.View;

public class Obstacle {

    private Integer obstacleID;

    private String obstacleName;

    private String obstacleLocation;

    private Integer obstacleOccurence;

    private Boolean isObstacle;

    public Obstacle(){ }

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
                // Handle the case when the rect is exactly at the right edge
                // Or beyond the right edge of the preview view
                // This might depend on your specific requirements
                return "Right Ahead";
            }
        }
    }
}
