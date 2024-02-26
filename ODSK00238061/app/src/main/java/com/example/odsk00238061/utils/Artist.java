package com.example.odsk00238061.utils;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.widget.RelativeLayout;

import androidx.camera.view.PreviewView;

import com.example.odsk00238061.MainActivity;
import com.google.mlkit.vision.objects.DetectedObject;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;


public class Artist implements Runnable {
    private BlockingQueue<DetectedObject> objectQueue;
    private PreviewView preview;
    private Context context;
    private Handler mainHandler;
    private ConcurrentHashMap<Integer, BoundingBox> boundingboxDictionary;

    public Artist(MainActivity context, PreviewView previewView, BlockingQueue<DetectedObject> objectQueue, android.os.Handler mainHandler) {
        this.context = context;
        this.preview = previewView;
        this.boundingboxDictionary = new ConcurrentHashMap<>();
        this.objectQueue = objectQueue;
        this.mainHandler = mainHandler;
    }

    @Override
    public void run() {
        try{
            while(!Thread.currentThread().isInterrupted()){
                if(!objectQueue.isEmpty()){
                    //DetectedObject detectedObject = objectQueue.take();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            while(!Thread.currentThread().isInterrupted()){

                                Log.d("Artist Run", "In run thread");

                            try {
                                DetectedObject detectedObject = objectQueue.take();
                                addBoundingBox(detectedObject);
                                startUpdatingBoundingBoxes();
                            } catch (InterruptedException e) {
                                Log.d("Artist Run", e.getMessage());
                               Thread.currentThread().interrupt();
                            }
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt(); // Restore interrupted state
            Log.d("Artist Run", e.getMessage());
        }
    }

    public void startUpdatingBoundingBoxes()
    {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                updateBoundingBoxes();
            }
        });
    }

    private void addBoundingBox(DetectedObject object) {
        BoundingBox boundingBox = new BoundingBox(context);
        if(object != null){
            boundingBox.setLocation(object.getBoundingBox());
            boundingBox.setLocation(object.getBoundingBox());
            boundingBox.setBoundingBoxID(object.getTrackingId());
            boundingBox.setLabel(ProjectHelper.getLabelWithHighestConfidence(object.getLabels()));
            if(preview != null) {
                Log.d("Artist", "Before Adding Bounding Box To Preview");
                preview.addView(boundingBox);
                Log.d("Artist", "After Adding Bounding Box To Preview");
                boundingboxDictionary.put(object.getTrackingId(), boundingBox);
            } else {
                Log.e("Artist", "PreviewView is null");
            }
        } else{
            Log.e("Artist", "Detected Object is null");
        }
    }

    private void RemoveBoundingBoxCompletely(int trackingId) {
        if (boundingboxDictionary != null) {
            BoundingBox boundingBox = boundingboxDictionary.get(trackingId);
            if (preview != null) {
                preview.removeView(boundingBox);
            }
        } else {
            Log.e("Artist", "PreviewView is null");
        }
    }


    private void updateBoundingBoxes() {
        // Add new bounding boxes
        for (DetectedObject detectedObject : objectQueue) {
            int trackingId = detectedObject.getTrackingId();
            if (!boundingboxDictionary.containsKey(trackingId)) {
                addBoundingBox(detectedObject);
            }
        }

        // Remove bounding boxes that are not present in the detected objects
        Set<Integer> trackingIdsToRemove = new HashSet<>();
        for (int trackingId : boundingboxDictionary.keySet()) {
            boolean found = false;
            for (DetectedObject detectedObject : objectQueue) {
                if (detectedObject.getTrackingId() == trackingId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                trackingIdsToRemove.add(trackingId);
            }
        }

        for (int trackingId : trackingIdsToRemove) {
            RemoveBoundingBoxCompletely(trackingId);
        }

        // Update bounding box locations
        for (DetectedObject detectedObject : objectQueue) {
            int trackingId = detectedObject.getTrackingId();
            if (boundingboxDictionary.containsKey(trackingId)) {
                BoundingBox boundingBox = boundingboxDictionary.get(trackingId);
                Rect boundingBoxRect = detectedObject.getBoundingBox();
                boundingBox.setLocation(boundingBoxRect);
            }
        }
    }
}
