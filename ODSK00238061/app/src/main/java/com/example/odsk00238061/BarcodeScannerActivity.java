package com.example.odsk00238061;

import static com.example.odsk00238061.utils.ProjectHelper.*;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.Speaker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BarcodeScannerActivity extends AppCompatActivity {
    private Intent intentRecognizer;
    private SpeechRecognizer speechRecognizer;
    private static final int TOUCH_DURATION_THRESHOLD = 3000;
    private Handler handler = new Handler();
    private Runnable longTouchRunnable;
    private Speaker speaker;
    private int camFacing = CameraSelector.LENS_FACING_BACK;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private Context context;
    private BarcodeScanner barcodeScanner;
    private boolean getBarcode = true;

    private String barcode_UPC = "";

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
                                setupCameraAndBindPreview(cameraProvider, context);
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                                Log.e("CameraX Camera Provider", e.getMessage());
                            }
                        }, ContextCompat.getMainExecutor(BarcodeScannerActivity.this));
                    } else {
                        Toast.makeText(BarcodeScannerActivity.this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcodescanner);
        vibrate(this, 1000);
        previewView = findViewById(R.id.cameraPreview);
        context = this;
        speaker = new Speaker(this);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, PackageManager.PERMISSION_GRANTED);
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E)
                        .build();

        barcodeScanner = BarcodeScanning.getClient(options);

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

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (ContextCompat.checkSelfPermission(BarcodeScannerActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    activityResultLauncher.launch(Manifest.permission.CAMERA);
                } else {
                    setupCameraAndBindPreview(cameraProvider, this);
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace(); // Handle exceptions as needed
                Log.e("CameraX Camera Provider", e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));

        TextToSpeech tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    speaker.speakText("Barcode scanner is open, hold your device over the barcode.");
                }
            }
        });
    }

    private void setupCameraAndBindPreview(ProcessCameraProvider cameraProvider, Context context) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(camFacing).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), new ImageAnalysis.Analyzer() {
            @ExperimentalGetImage
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                Image image = imageProxy.getImage();
                if (image != null) {
                    InputImage inputImage = InputImage.fromMediaImage(image, rotationDegrees);
                    detectBarcodeFromImage(inputImage, imageProxy);
                }
            }
        });
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    public void detectBarcodeFromImage(InputImage inputImage, ImageProxy imageProxy) {
    if (getBarcode) {
        barcodeScanner.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        if (!barcodes.isEmpty()) {
                            getBarcode = false;
                            vibrate(BarcodeScannerActivity.this, 1000);
                            Intent intent = new Intent(BarcodeScannerActivity.this, ProductDetailsActivity.class);
                            intent.putExtra("barcode", barcodes.get(0).getRawValue());
                            startActivity(intent);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("BarcodeScanner", e.getMessage());
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Barcode>> task) {
                        imageProxy.close();
                    }
                });
        }
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


    private void startSpeechRecognition() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

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
                if (matches != null) {
                     ProjectHelper.handleCommands(matches, BarcodeScannerActivity.this,
                                                  speaker, context);
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
}
