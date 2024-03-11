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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
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
    private Intent intentRecognizer;
    private BlockingQueue<DetectedObject> objectQueue;
    private Speaker speaker;
    private Thread speakerThread;
    private RectangleOverlayView rectangleOverlayView;
    private PreviewView previewView;
    int camFacing = CameraSelector.LENS_FACING_BACK;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ObjectDetector objectDetector;
    private ProcessCameraProvider cameraProvider;
    private Context context;
    private Size targetResolution = new Size(1280, 720);

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
                                BindPreview(cameraProvider, context);
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                                Log.e("CameraX Camera Provider", e.getMessage());
                            }
                        }, ContextCompat.getMainExecutor(MainActivity.this));
                    } else {
                        Toast.makeText(MainActivity.this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LocalModel localModel =
                new LocalModel.Builder()
                        .setAssetFilePath("2.tflite")
                        .build();

        CustomObjectDetectorOptions customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.5f)
                        .setMaxPerObjectLabelCount(1)
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
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},PackageManager.PERMISSION_GRANTED);
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        objectQueue = new LinkedBlockingQueue<>();
        speaker = new Speaker(this,  5, objectQueue, previewView);
        speaker.onInit(0);
        speakerThread = new Thread(speaker);
        speakerThread.start();

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    activityResultLauncher.launch(Manifest.permission.CAMERA);
                } else{
                    BindPreview(cameraProvider, this);
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace(); // Handle exceptions as needed
                Log.e("CamerX Camera Provider", e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void BindPreview(ProcessCameraProvider CameraProvider, Context context) {
        float aspectRatio = (float) previewView.getWidth() / previewView.getHeight();

        Preview preview = new Preview.Builder()
                                     .setTargetResolution(new Size(1920, 1920))
                                     .build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(camFacing).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1920, 1920))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new ImageAnalysis.Analyzer() {
                    @ExperimentalGetImage
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        Image image = imageProxy.getImage();
                        if (image != null) {
                            InputImage inputImage = InputImage.fromMediaImage(image, imageProxy.getImageInfo().getRotationDegrees());
                            float inputImageAspectRatio = (float) inputImage.getWidth() / inputImage.getHeight();
                            Task<List<DetectedObject>> task = objectDetector.process(inputImage);
                            Log.d("Object Detection", "Preview | W" + previewView.getWidth() + "| H" + previewView.getHeight() + "| AR" + aspectRatio);
                            Log.d("Object Detection", "Input Image | W" + inputImage.getWidth() + "| H" + inputImage.getHeight() + "| AR" + aspectRatio);
                            Log.d("Object Detection", "Image Proxy | W" + imageProxy.getWidth() + "| H" + imageProxy.getHeight() + "| AR" + aspectRatio);

                            task.addOnSuccessListener(
                                            new OnSuccessListener<List<DetectedObject>>() {
                                                @Override
                                                public void onSuccess(List<DetectedObject> detectedObjects) {
                                                    if(!detectedObjects.isEmpty()){
                                                        objectQueue.addAll(detectedObjects);
                                                        Matrix mappingMatrix = ProjectHelper.getMappingMatrix(imageProxy, previewView);
                                                        for(DetectedObject object : detectedObjects){
                                                            //Rect adjustedBoundingBox = adjustBoundingBox(object.getBoundingBox(),inputImageAspectRatio ,aspectRatio);
                                                            Rect boundingBox = ProjectHelper.mapBoundingBox(object.getBoundingBox(), mappingMatrix);

                                                            rectangleOverlayView.updateRect(boundingBox);
                                                            rectangleOverlayView.invalidate();
                                                        }
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
                                                    image.close();
                                                }
                                            }
                                    );

                        }
                    }
                });

        CameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speaker.setRunning(false);
        stopSpeechRecognition();
        objectDetector.close();
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

            }

            @Override
            public void onError(int error) {

            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(matches != null){
                    if(matches.contains("text to speech")){
                        //speaker.speakText("Hello Eddie, your speech recognition is working fine");
                        Intent intent = new Intent(MainActivity.this, TextToSpeechActivity.class);
                        speaker.Destroy();
                        startActivity(intent);

                    } else if(matches.contains("help")) {
                        speaker.speakText("You can say 'read' to read text from the camera " +
                                "or 'detect objects' to detect objects from the camera");
                    } else if(matches.contains("battery life")) {
                        float batteryLevel = ProjectHelper.getBatteryLevel(context);
                        speaker.speakText("Your battery level is " + batteryLevel + " percent");
                    }
                    else {
                        speaker.speakText("I'm sorry, I didn't get that. Please try again");
                        Thread newSpeakerThread = new Thread(speaker);
                        newSpeakerThread.start();
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
    }
    private void startLongTouchTimer() {
        if (longTouchRunnable == null) {
            longTouchRunnable = new Runnable() {
                @Override
                public void run() {
                    // Perform speech-to-text activation
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
        objectQueue.clear();
        objectDetector.close();
    }
}
