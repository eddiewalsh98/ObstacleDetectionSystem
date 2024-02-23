package com.example.odsk00238061.utils;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.widget.RelativeLayout;

import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.objects.DetectedObject;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class Artist implements Runnable {
    private BlockingQueue<DetectedObject> objectQueue;
    private PreviewView preview;
    private Context context;
    private ConcurrentHashMap<Integer, BoundingBox> boundingboxDictionary;

    public Artist(Context context, PreviewView preview, BlockingQueue<DetectedObject> objectQueue) {
        this.context = context;
        this.preview = preview;
        this.boundingboxDictionary = new ConcurrentHashMap<>();
        this.objectQueue = objectQueue;
    }

    @Override
    public void run() {
        try{
            while(true){
                DetectedObject detectedObject = objectQueue.take();
                addBoundingBox(detectedObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt(); // Restore interrupted state
            Log.d("Artist Run", e.getMessage());
        }
    }

    public void startUpdatingBoundingBoxes()
    {
        // Start a separate thread for updating bounding boxes periodically
        Thread updateThread = new Thread(() -> {
            try {
                while (true) {
                    updateBoundingBoxes();
                    Thread.sleep(1000); // Adjust the interval as needed
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt(); // Restore interrupted state
                Log.d("Update Thread", e.getMessage());
            }
        });
        updateThread.start();
    }

    private void addBoundingBox(DetectedObject object) {
        BoundingBox boundingBox = new BoundingBox(context);
        boundingBox.setLocation(object.getBoundingBox());
        boundingBox.setBoundingBoxID(object.getTrackingId());
        boundingBox.setLabel(ProjectHelper.getLabelWithHighestConfidence(object.getLabels()));
        preview.addView(boundingBox);
        boundingboxDictionary.put(object.getTrackingId(), boundingBox);
    }

    private void RemoveBoundingBoxFromView(int trackingId) {
        BoundingBox boundingBox = boundingboxDictionary.get(trackingId);
        preview.removeView(boundingBox);
    }

    private void RemoveBoundingBoxCompletely(int trackingId) {
        BoundingBox boundingBox = boundingboxDictionary.get(trackingId);
        RemoveBoundingBoxFromView(trackingId);
        boundingboxDictionary.remove(trackingId);
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
