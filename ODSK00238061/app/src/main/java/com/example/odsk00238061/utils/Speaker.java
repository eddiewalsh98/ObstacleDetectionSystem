package com.example.odsk00238061.utils;

import android.content.Context;
import android.graphics.Rect;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.camera.view.PreviewView;
import java.util.Locale;


public class Speaker implements Runnable,TextToSpeech.OnInitListener {
    private boolean isRunning = true;
    private long lastSpeechTime = 0;
    private static final long MIN_SPEECH_DELAY = 4000;
    private TextToSpeech textToSpeech;
    private PreviewView view;
    private final Context context;
    //private ConcurrentHashMap<Integer, Obstacle> obstacleDictionary;
    private final int threshold = 25;
    //private BlockingQueue<DetectedObject> objectQueue;
    private Obstacle potentialObstacle;

    //private Timer timer;

    // Add a function that converts a detected object to an obstacle
    // Have a function that checks the location of an obstacle to verify its
    // in a similar proximity of the previous obstacle

    public Speaker(Context context) {
        this.context = context;
        this.textToSpeech = new TextToSpeech(context, this);
      //  this.objectQueue = new PriorityBlockingQueue<>();
        this.potentialObstacle = new Obstacle();
    }

    public Speaker(Context context, PreviewView previewView) {
        this.context = context;
        this.view = previewView;
        this.potentialObstacle = new Obstacle();
    }

//    public Speaker(Context context, int threshold, BlockingQueue<DetectedObject> objectQueue, PreviewView previewView) {
//        this.context = context;
//        this.view = previewView;
//        this.obstacleDictionary = new ConcurrentHashMap<>();
//        this.threshold = threshold;
//        //this.objectQueue = objectQueue; // Initialize the objectQueue
//        this.textToSpeech = new TextToSpeech(context, this);
//        this.timer = new Timer();
//        this.timer.scheduleAtFixedRate(new CheckThresholdTask(), 0, 1000);
//    }


    @Override
    public void run() {
            while (isRunning) { // Check the flag in the loop condition
                checkOccurenceOfObstacle();
            }
    }

    public void checkOccurenceOfObstacle() {
        if(potentialObstacle.getObstacleOccurence() != null) {
            long currentTime = System.currentTimeMillis();
            if (potentialObstacle.getObstacleOccurence() > threshold && currentTime - lastSpeechTime > MIN_SPEECH_DELAY) {
                String location = ProjectHelper.calculateObstacleLocation(potentialObstacle.getObstacleRect(), view.getWidth());
                String obstacle = potentialObstacle.getObstacleName();
                speakText(obstacle + " located at " + location);
                potentialObstacle = new Obstacle();
                lastSpeechTime = currentTime;
            }
        }
    }

    public void processDetectedObject(Obstacle newObstacle) {
        //run my checks
        //if they are the same with similar location than increment
        // if it hits our threshold than we speak and equal our 'potential object' to a new object
        // if it is not the same object than we speak and equal our 'potential object' to a new object
        if(potentialObstacle != null) {
            if(newObstacle.getObstacleName().equals(potentialObstacle.getObstacleName())) {
                potentialObstacle.incrementObstacleOccurence();
                potentialObstacle.getObstacleRect().set(newObstacle.getObstacleRect());
            } else {
                potentialObstacle = newObstacle;
            }
        } else {
            potentialObstacle = newObstacle;
        }
    }

    public boolean compareObstaclesLocation(Rect newLocation) {
        // Calculate the center coordinates of each rectangle

        double proximityThreshold = calculateProximityThreshold(potentialObstacle.getObstacleRect(), newLocation, 10);

        int nLocationCenterX = newLocation.centerX();
        int nLocationCenterY = newLocation.centerY();
        int oLocationCenterX = potentialObstacle.getObstacleRect().centerX();
        int oL2CenterY = potentialObstacle.getObstacleRect().centerY();

        // Calculate the distance between the centers of the rectangles
        double distance = calculateDistance(nLocationCenterX, nLocationCenterY, oLocationCenterX, oL2CenterY);

        // Compare the distance to the proximity threshold
        return distance <= proximityThreshold;
    }

    private static double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static double calculateAverageSize(Rect rect1, Rect rect2) {
        // Calculate the area of each rectangle
        int area1 = rect1.width() * rect1.height();
        int area2 = rect2.width() * rect2.height();

        // Calculate the average size
        return (area1 + area2) / 2.0;
    }

    public static double calculateProximityThreshold(Rect rect1, Rect rect2, double thresholdPercentage) {
        double averageSize = calculateAverageSize(rect1, rect2);
        // Calculate the threshold as a percentage of the average size
        return averageSize * thresholdPercentage / 100.0;
    }


    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {
            int langResult = textToSpeech.setLanguage(Locale.US);
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("Speaker", "Language is not supported");
            }
        } else {
            Log.e("Speaker", "Initialization failed");
        }
    }

    public void speakText(String text) {
        try{
            if (textToSpeech == null) {
                textToSpeech = new TextToSpeech(context, this);
            }
            if (textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            Log.d("SpeakText", "Text Successfully Spoken");
        } catch(Exception e){
            Log.d("SpeakText", e.getLocalizedMessage());
        }
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

//    public void addObstacle(DetectedObject object) {
//        try{
//            String objectLocation = ProjectHelper.calculateObstacleLocation(object.getBoundingBox(), view.getWidth());
//            String obstacleLabel = object.getLabels().get(0).getText();
//            Obstacle obstacle = new Obstacle(object.getTrackingId(), obstacleLabel ,objectLocation, 0);
//            obstacleDictionary.put(obstacle.getObstacleID(), obstacle);
//            Log.d("AddObstacle", obstacle.getObstacleName() + " added to the dictionary!");
//        } catch(Exception e){
//            Log.d("AddObstacle", e.getMessage());
//        }
//    }

//    public void obstacleOccurenceTexttoSpeech(Obstacle obstacle){
//        try{
//            if (obstacle != null && obstacle.getObstacleOccurence() != null) {
//                if(obstacle.getObstacleOccurence() > threshold){
//                    speakText(obstacle.getObstacleName() + " located at " + obstacle.getObstacleLocation());
//                    Log.d("Obstacle", obstacle.getObstacleName() + " added to the dictionary!");
//                }
//            }
//        } catch(Exception e){
//            Log.d("ObstacleOccurenceTexttoSpeech", e.getMessage());
//        }
//    }

//    public void removeObstacleFromDisctionary(int id){
//        try{
//            obstacleDictionary.remove(id);
//        } catch(Exception e){
//            Log.d("RemoveObstacle", e.getMessage());
//        }
//    }

//    private void processDetectedObject(DetectedObject detectedObject) {
//        try{
//            if(obstacleDictionary.containsKey(detectedObject.getTrackingId())){
//                Obstacle obstacle = obstacleDictionary.get(detectedObject.getTrackingId());
//                obstacle.incrementObstacleOccurence();
//                obstacleDictionary.put(obstacle.getObstacleID(), obstacle);
//            } else {
//                addObstacle(detectedObject);
//            }
//        } catch(Exception e){
//            Log.d("ProcessDetectedObject", e.getMessage());
//        }
//    }

    public void Destroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

//    private class CheckThresholdTask extends TimerTask {
//        @Override
//        public void run() {
//            // Iterate through the obstacle dictionary and check each obstacle's occurrence
//            for (Obstacle obstacle : obstacleDictionary.values()) {
//                if (obstacle.getObstacleOccurence() > threshold) {
//                    // If the occurrence exceeds the threshold, trigger text-to-speech
//                    obstacleOccurenceTexttoSpeech(obstacle);
//                    removeObstacleFromDisctionary(obstacle.getObstacleID());
//                    Log.d("Obstacle", obstacle.getObstacleName() + " removed from the dictionary!");
//                }
//            }
//        }
//    }
}
