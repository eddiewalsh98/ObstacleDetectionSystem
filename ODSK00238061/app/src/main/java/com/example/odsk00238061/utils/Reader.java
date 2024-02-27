package com.example.odsk00238061.utils;

import android.content.Context;
import android.util.Log;

import com.google.mlkit.vision.objects.DetectedObject;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class Reader implements Runnable{

    Speaker speaker;
    private String lastDetectedText;
    private boolean isRunning = true;
    private BlockingQueue<String> detectedTextQueue;

    public Reader(BlockingQueue<String> detectedTextQueue, Context context){
        this.detectedTextQueue = detectedTextQueue;
        speaker = new Speaker(context);
    }


    @Override
    public void run() {
        try {
            while(isRunning){

            }
        } catch(Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt(); // Restore interrupted state
            Log.d("Speaker Run", e.getMessage());
        }
    }
}
