package com.example.odsk00238061;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;

import com.example.odsk00238061.utils.Speaker;

import java.util.Locale;

public class TextToSpeechActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private PreviewView previewView;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textspeech);
        previewView = findViewById(R.id.textCameraView);
        textToSpeech = new TextToSpeech(this, this);
        onInit(0);
        readText("Welcome to the text to speech activity!");

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US);
        }
    }

    public void readText(String text) {
        try{
            if (textToSpeech != null && textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            Log.d("SpeakText", "Text Successfully Spoken");
        } catch(Exception e){
            Log.d("SpeakText", e.getMessage());
        }
    }
}
