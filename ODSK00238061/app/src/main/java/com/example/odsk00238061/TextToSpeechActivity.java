package com.example.odsk00238061;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

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
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.odsk00238061.utils.ImageAnalysisUtil;
import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.Speaker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class TextToSpeechActivity extends AppCompatActivity  {

    int camFacing = CameraSelector.LENS_FACING_BACK;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ObjectDetector objectDetector;
    private ProcessCameraProvider cameraProvider;
    TextRecognizer textRecognizer;
    private static final int TOUCH_DURATION_THRESHOLD = 3000;
    private Handler handler = new Handler();
    private Runnable longTouchRunnable;
    private PreviewView previewView;
    private Speaker speaker;

    Context context;

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
                        }, ContextCompat.getMainExecutor(TextToSpeechActivity.this));
                    } else {
                        Toast.makeText(TextToSpeechActivity.this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textspeech);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        context = this;
        previewView = findViewById(R.id.textCameraView);
        speaker = new Speaker(this);
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

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if(ContextCompat.checkSelfPermission(TextToSpeechActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
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

    public void BindPreview(ProcessCameraProvider cameraProvider, Context context){
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(camFacing).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new ImageAnalysis.Analyzer() {
                    @ExperimentalGetImage
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        Image image = imageProxy.getImage();
                        if(image != null){
                            InputImage inputImage = InputImage.fromMediaImage(image, rotationDegrees);
                            Task<Text> result =
                                    textRecognizer.process(inputImage)
                                            .addOnSuccessListener(new OnSuccessListener<Text>() {
                                                @Override
                                                public void onSuccess(Text visionText) {
                                                    String text = visionText.getText();
                                                    speaker.speakText(text);
                                                }
                                            })
                                            .addOnFailureListener(
                                                    new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            // Task failed with an exception
                                                            // ...
                                                        }
                                                    });
                        }
                    }
                });
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,imageAnalysis, preview);
    }




    private void startLongTouchTimer() {
        if (longTouchRunnable == null) {
            longTouchRunnable = new Runnable() {
                @Override
                public void run() {

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
}
