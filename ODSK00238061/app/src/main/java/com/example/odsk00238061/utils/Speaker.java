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
    private final int threshold = 25;
    private Obstacle potentialObstacle;


    public Speaker(Context context) {
        this.context = context;
        this.textToSpeech = new TextToSpeech(context, this);
        this.potentialObstacle = new Obstacle();
    }

    public Speaker(Context context, PreviewView previewView) {
        this.context = context;
        this.view = previewView;
        this.potentialObstacle = new Obstacle();
    }


    @Override
    public void run() {
            while (isRunning) {
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

    public void Destroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
