package com.example.odsk00238061;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.RectangleOverlayView;
import com.example.odsk00238061.utils.Speaker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;


public class MainActivity extends AppCompatActivity  {

    private static final int TOUCH_DURATION_THRESHOLD = 3000; // 3 seconds
    private final Handler handler = new Handler();
    private Runnable longTouchRunnable;
    private SpeechRecognizer speechRecognizer;
    private MediaPlayer mediaPlayer;
    private Intent intentRecognizer;
    private Speaker speaker;
    private Context context;

    private RelativeLayout relativeLayout;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        relativeLayout = findViewById(R.id.layout);
        context = this;
        relativeLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startLongTouchTimer();
                        break;
                    case MotionEvent.ACTION_UP:
                        cancelLongTouchTimer();
                        break;
                }
                return true;
            }
        });
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},PackageManager.PERMISSION_GRANTED);
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speaker = new Speaker(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        speaker.speakText("Welcome to the ODS application. Hold down on the screen to speak and say 'help' to get a list of commands.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSpeechRecognition();
    }

    private void startSpeechRecognition() {
        // Start listening for speech continuously
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                speaker.setRunning(false);
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {
                stopSpeechRecognition();
            }

            @Override
            public void onError(int error) {
                stopSpeechRecognition();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(matches != null){
                    if(matches.contains("text to speech")){
                        Intent intent = new Intent(MainActivity.this, TextToSpeechActivity.class);
                        speaker.Destroy();
                        startActivity(intent);
                    } else if(matches.contains("help")) {
                        speaker.speakText("To use the application, you can say 'text to speech' to convert text to speech, " +
                                "'read' to read text from the camera, 'battery life' to check your battery level, " +
                                "'record voice memo' to record a 20 second voice memo, 'play record memo' to play the recorded memo, " +
                                "and 'detect objects' to detect objects in the camera view. Hold down on the screen to speak.");

                    } else if(matches.contains("battery life")) {
                        float batteryLevel = ProjectHelper.getBatteryLevel(context);
                        speaker.speakText("Your battery level is " + batteryLevel + " percent");

                    } else if(matches.contains("record voice memo")) {
                        Intent intent = new Intent(MainActivity.this, VoiceMemoActivity.class);
                        speaker.Destroy();
                        startActivity(intent);

                    } else if(matches.contains("play record memo")) {
                        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
                        File memoDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                        File file = new File(memoDirectory, "ODSRecordingFile" + ".mp3");

                        try {
                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setDataSource(file.getPath());
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                        } catch (IOException e) {
                            Log.d("PlayMemo", e.getMessage());
                            speaker.speakText("I'm sorry, I couldn't play the memo. Please try again");
                        }

                    } else if(matches.contains("obstacles")) {
                        Intent intent = new Intent(MainActivity.this, ObjectDetectionActivity.class);
                        speaker.Destroy();
                        startActivity(intent);
                    }
                    else {
                        speaker.speakText("I'm sorry, I didn't get that. Please try again");

                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
        speechRecognizer.startListening(intentRecognizer);
    }

    private void stopSpeechRecognition() {
        // Stop listening for speech
        speechRecognizer.stopListening();
        speaker.setRunning(true);
    }
    private void startLongTouchTimer() {
        if (longTouchRunnable == null) {
            longTouchRunnable = new Runnable() {
                @Override
                public void run() {
                    startSpeechRecognition();
                }
            };
            handler.postDelayed(longTouchRunnable, TOUCH_DURATION_THRESHOLD);
        }
    }

    private void cancelLongTouchTimer() {
        if (longTouchRunnable != null) {
            handler.removeCallbacks(longTouchRunnable);
            longTouchRunnable = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopSpeechRecognition();
        speaker.Destroy();
    }
}
