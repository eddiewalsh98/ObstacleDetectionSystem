package com.example.odsk00238061.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.objects.DetectedObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class Speaker implements Runnable, TextToSpeech.OnInitListener {

    private boolean isRunning = true;
    private static final String FILENAME = "directions.txt";
    private TextToSpeech textToSpeech;
    private PreviewView view;
    private Context context;
    private ConcurrentHashMap<Integer, Obstacle> obstacleDictionary;
    private int threshold;
    private BlockingQueue<DetectedObject> objectQueue;
    private Timer timer;

    public Speaker(Context context, int threshold, BlockingQueue<DetectedObject> objectQueue, PreviewView previewView) {
        this.context = context;
        this.view = previewView;
        this.obstacleDictionary = new ConcurrentHashMap<>();
        this.threshold = threshold;
        this.objectQueue = objectQueue; // Initialize the objectQueue
        textToSpeech = new TextToSpeech(context, this);
        timer = new Timer();
        timer.scheduleAtFixedRate(new CheckThresholdTask(), 0, 1000);
        createFileIfNotExists(context);
    }

    @Override
    public void run() {
        try {
            while (isRunning) { // Check the flag in the loop condition
                if(!objectQueue.isEmpty()){
                    DetectedObject detectedObject = objectQueue.take();
                    processDetectedObject(detectedObject);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt(); // Restore interrupted state
            Log.d("Speaker Run", e.getMessage());
        }
    }

    @Override
    public void onInit(int status) {
        int langResult = textToSpeech.setLanguage(Locale.US);
    }

    public void speakText(String text) {
        try{
            if (textToSpeech == null) {
                textToSpeech = new TextToSpeech(context, this);
            }
            if (textToSpeech != null && textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            writeCommandToFile(text);
            Log.d("SpeakText", "Text Successfully Spoken");
        } catch(Exception e){
            Log.d("SpeakText", e.getMessage());
        }
    }

    public void setThreshold(int limit){
        this.threshold = limit;
    }
    public void stop() {
        textToSpeech.stop();
        textToSpeech.shutdown();
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public void addObstacle(DetectedObject object) {
        try{
            String objectLocation = ProjectHelper.calculateObstacleLocation(object.getBoundingBox(), view.getWidth());
            String obstacleLabel = ProjectHelper.getLabelWithHighestConfidence(object.getLabels());
            Obstacle obstacle = new Obstacle(object.getTrackingId(), obstacleLabel ,objectLocation, 0);
            obstacleDictionary.put(obstacle.getObstacleID(), obstacle);
            Log.d("AddObstacle", obstacle.getObstacleName() + " added to the dictionary!");
        } catch(Exception e){
            Log.d("AddObstacle", e.getMessage());
        }
    }

    public void obstacleOccurenceTexttoSpeech(Obstacle obstacle){
        try{
            if(obstacle.getObstacleOccurence() > threshold){
                speakText(obstacle.getObstacleName() + " located at " + obstacle.getObstacleLocation());
                Log.d("Obstacle", obstacle.getObstacleName() + " added to the dictionary!");
            }
        } catch(Exception e){
            Log.d("ObstacleOccurenceTexttoSpeech", e.getMessage());
        }
    }

    public void removeObstacleFromDisctionary(int id){
        try{
            obstacleDictionary.remove(id);
        } catch(Exception e){
            Log.d("RemoveObstacle", e.getMessage());
        }
    }

    private void processDetectedObject(DetectedObject detectedObject) {
        try{
            if(obstacleDictionary.containsKey(detectedObject.getTrackingId())){
                Obstacle obstacle = obstacleDictionary.get(detectedObject.getTrackingId());
                obstacle.incrementObstacleOccurence();
                obstacleDictionary.put(obstacle.getObstacleID(), obstacle);
            } else {
                addObstacle(detectedObject);
            }
        } catch(Exception e){
            Log.d("ProcessDetectedObject", e.getMessage());
        }
    }

    public void writeCommandToFile(String command) {
        try {
            FileWriter writer = new FileWriter(FILENAME, true);
            writer.write(command + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle file writing error
        }
    }

    private void createFileIfNotExists(Context context) {
        File file = new File(context.getFilesDir(), "directions.txt");
        Log.d("File", "File Path: " + file.getAbsolutePath());
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("CreateFile", e.getMessage());
            }
        }
    }

    private class CheckThresholdTask extends TimerTask {
        @Override
        public void run() {
            // Iterate through the obstacle dictionary and check each obstacle's occurrence
            for (Obstacle obstacle : obstacleDictionary.values()) {
                if (obstacle.getObstacleOccurence() > threshold) {
                    // If the occurrence exceeds the threshold, trigger text-to-speech
                    obstacleOccurenceTexttoSpeech(obstacle);
                    removeObstacleFromDisctionary(obstacle.getObstacleID());
                    Log.d("Obstacle", obstacle.getObstacleName() + " removed from the dictionary!");
                }
            }
        }
    }
}
