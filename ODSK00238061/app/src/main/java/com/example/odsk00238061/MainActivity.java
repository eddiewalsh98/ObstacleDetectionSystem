package com.example.odsk00238061;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.odsk00238061.utils.Artist;
import com.example.odsk00238061.utils.BoundingBox;
import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.Speaker;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {

    private BlockingQueue<DetectedObject> objectQueue;
    private Speaker speaker;
    private Artist artist;
    private Thread speakerThread;
    private Thread artistThread;
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
        previewView = findViewById(R.id.cameraPreview);
        objectQueue = new LinkedBlockingQueue<>();
        speaker = new Speaker(this,  10, objectQueue, previewView);
        artist = new Artist(this, previewView, objectQueue);
        speaker.onInit(0);
        speakerThread = new Thread(speaker);
        artistThread = new Thread(artist);
        speakerThread.start();
        artistThread.start();
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

        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .build();

        objectDetector = ObjectDetection.getClient(options);
    }

    private void BindPreview(ProcessCameraProvider CameraProvider, Context context)
    {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(camFacing).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                                                        .setTargetResolution(targetResolution)
                                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                        .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new ImageAnalysis.Analyzer() {
                    @ExperimentalGetImage @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        Image image = imageProxy.getImage();
                        if(image != null){
                            InputImage inputImage = InputImage.fromMediaImage(image, rotationDegrees);
                            objectDetector.process(inputImage)
                                    .addOnSuccessListener(detectedObjects -> {
                                        for (DetectedObject object : detectedObjects) {
                                            try {
                                                if(ProjectHelper.checkDetectedObjectHasValidConfidence(object)){
                                                    objectQueue.put(object);
                                                }
                                            } catch (InterruptedException e) {
                                                Log.e("Object Queue", e.getMessage());
                                            }
                                        }
                                        imageProxy.close();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Object Detection", "Object Detection Failed: " + e.getMessage());
                                        imageProxy.close();
                                    });
                        }
                    }
                });
        CameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,imageAnalysis, preview);
    }
}