package com.example.odsk00238061;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class TextToSpeechActivity extends AppCompatActivity  {

    int camFacing = CameraSelector.LENS_FACING_BACK;
    private ImageAnalysis imageAnalysis;
    private Intent intentRecognizer;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ObjectDetector objectDetector;
    private ProcessCameraProvider cameraProvider;
    private SpeechRecognizer speechRecognizer;
    private TextRecognizer textRecognizer;
    private static final int TOUCH_DURATION_THRESHOLD = 3000;
    private String extractedText = "";
    private Handler handler = new Handler();
    private Runnable longTouchRunnable;
    private PreviewView previewView;
    private Speaker speaker;
    private Context context;
    private boolean readText = false;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},PackageManager.PERMISSION_GRANTED);
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if(ContextCompat.checkSelfPermission(TextToSpeechActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    activityResultLauncher.launch(Manifest.permission.CAMERA);
                } else{
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
                    speaker.speakText("Text to speech is open, hold your device over the piece of text.");
                }
            }
        });
    }

    public void setupCameraAndBindPreview(ProcessCameraProvider cameraProvider, Context context) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(camFacing).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new ImageAnalysis.Analyzer() {
                    @OptIn(markerClass = ExperimentalGetImage.class) @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        Image image = imageProxy.getImage();
                        if (image != null) {
                            InputImage inputImage = InputImage.fromMediaImage(image, rotationDegrees);
                            detectTextFromImage(inputImage, imageProxy);
                        }
                    }

                });
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,imageAnalysis, preview);
    }

    public void detectTextFromImage(InputImage inputImage, ImageProxy imageProxy) {
        if (readText) {
            textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text visionText) {
                            extractedText = visionText.getText();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("TextToSpeechActivity", "onFailure: " + e.getMessage());
                            speaker.speakText(e.getMessage());
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<Text>() {
                        @Override
                        public void onComplete(@NonNull Task<Text> task) {
                            imageProxy.close();
                            if(extractedText.isEmpty()){
                                speaker.speakText("No text detected, please try again.");
                            } else {
                                speaker.speakText(extractedText);
                            }
                            readText = false;
                        }
                    });

        } else{
            imageProxy.close();
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
                if(matches != null){
                    if(matches.contains("read")){
                        readText = true;
                    } else {
                        ProjectHelper.handleCommands(matches, TextToSpeechActivity.this, speaker, context);
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
}
