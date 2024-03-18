package com.example.odsk00238061;

import static androidx.camera.core.AspectRatio.RATIO_4_3;

import android.Manifest;
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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
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

import com.example.odsk00238061.utils.Obstacle;
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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

public class ObjectDetectionActivity extends AppCompatActivity {
    private static final int TOUCH_DURATION_THRESHOLD = 3000; // 3 seconds
    private final Handler handler = new Handler();
    private Runnable longTouchRunnable;
    private SpeechRecognizer speechRecognizer;
    private Intent intentRecognizer;
    //private BlockingQueue<DetectedObject> objectQueue;
    private Speaker speaker;
    private Thread speakerThread;
    private RectangleOverlayView rectangleOverlayView;
    private PreviewView previewView;
    int camFacing = CameraSelector.LENS_FACING_BACK;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ObjectDetector objectDetector;
    private ProcessCameraProvider cameraProvider;
    private Context context;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private CameraSelector cameraSelector;

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        // Camera permission is granted, proceed with setting up camera
                        cameraProviderFuture.addListener(() -> {
                            try {
                                cameraProvider = cameraProviderFuture.get();
                                BindPreview(cameraProvider);
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                                Log.e("CameraX Camera Provider", e.getMessage());
                            }
                        }, ContextCompat.getMainExecutor(ObjectDetectionActivity.this));
                    } else {
                        Toast.makeText(ObjectDetectionActivity.this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_detection);
        LocalModel localModel =
                new LocalModel.Builder()
                        .setAssetFilePath("1.tflite")
                        .build();

        CustomObjectDetectorOptions customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.7f)
                        .setMaxPerObjectLabelCount(3)
                        .build();

        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
        previewView = findViewById(R.id.cameraPreview);
        context = this;
        rectangleOverlayView = findViewById(R.id.rectangle_overlay);
        previewView.setOnTouchListener(new View.OnTouchListener() {
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
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, PackageManager.PERMISSION_GRANTED);
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speaker = new Speaker(this, previewView);
        speakerThread = new Thread(speaker);
        speakerThread.start();

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if(ContextCompat.checkSelfPermission(ObjectDetectionActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    activityResultLauncher.launch(Manifest.permission.CAMERA);
                } else{
                    BindPreview(cameraProvider);
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace(); // Handle exceptions as needed
                Log.e("CamerX Camera Provider", e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void BindPreview(ProcessCameraProvider CameraProvider) {
        speaker.speakText("Please hold phone in front of you to detect obstacles");
        Size targetResolution = new Size(360,800 );

        preview = new Preview.Builder()
                //.setTargetResolution(targetResolution)
                .build();


        cameraSelector = new CameraSelector.Builder()
                                           .requireLensFacing(camFacing)
                                           .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        imageAnalysis = new ImageAnalysis.Builder()
                //.setTargetResolution(targetResolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new ImageAnalysis.Analyzer() {
                    @ExperimentalGetImage
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        Image image = imageProxy.getImage();
                        Log.d("ImageProxy", imageProxy.getWidth() + "x" + imageProxy.getHeight());
                        if (image != null) {
                            InputImage inputImage = InputImage.fromMediaImage(image, imageProxy.getImageInfo().getRotationDegrees());

                            Task<List<DetectedObject>> task = objectDetector.process(inputImage);
                            task.addOnSuccessListener(
                                            new OnSuccessListener<List<DetectedObject>>() {
                                                @Override
                                                public void onSuccess(List<DetectedObject> detectedObjects) {
                                                    if(!detectedObjects.isEmpty()){
                                                        DetectedObject object = detectedObjects.get(0);
                                                        Obstacle obstacle = new Obstacle(object);
                                                        speaker.processDetectedObject(obstacle);
                                                        Matrix mappingMatrix = ProjectHelper.getMappingMatrix(imageProxy, previewView);
                                                        Rect boundingBox = ProjectHelper.mapBoundingBox(object.getBoundingBox(), mappingMatrix);
                                                        rectangleOverlayView.updateRect(boundingBox);
                                                        rectangleOverlayView.invalidate();

                                                    }
                                                }
                                            }
                                    )
                                    .addOnFailureListener(
                                            new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e("Object Detection", e.getMessage());
                                                }
                                            }
                                    )
                                    .addOnCompleteListener(
                                            new OnCompleteListener<List<DetectedObject>>() {
                                                @Override
                                                public void onComplete(@NonNull Task<List<DetectedObject>> task) {
                                                    imageProxy.close();
                                                }
                                            }
                                    );

                        }
                    }
                });

        CameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
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
                if(matches != null) {
                    if(matches.contains("stop detecting")) {
                        Intent intent = new Intent(ObjectDetectionActivity.this, MainActivity.class);
                        speaker.Destroy();
                        startActivity(intent);
                    }
                    else if(matches.contains("text to speech")){
                        speaker.speakText("To continue to a different task, please say 'stop detecting'");

                    } else if(matches.contains("help")) {
                        speaker.speakText("To continue to a different task, please say 'stop detecting'");
                    } else if(matches.contains("battery life")) {
                        speaker.speakText("To continue to a different task, please say 'stop detecting'");

                    } else if(matches.contains("record voice memo")) {
                        speaker.speakText("To continue to a different task, please say 'stop detecting'");

                    } else if(matches.contains("play record memo")) {
                        speaker.speakText("To continue to a different task, please say 'stop detecting'");
                    }
                    else {
                        speaker.speakText("I'm sorry, I didn't get that. Please try again");
                        speaker.setRunning(true);
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
        speaker.setRunning(false);
        stopSpeechRecognition();
        objectDetector.close();
    }

}
