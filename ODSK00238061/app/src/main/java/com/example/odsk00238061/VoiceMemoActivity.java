package com.example.odsk00238061;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.Speaker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class VoiceMemoActivity extends AppCompatActivity {
    private MediaRecorder mediaRecorder;
    private Intent intentRecognizer;
    private SpeechRecognizer speechRecognizer;
    private Speaker speaker;
    private final Handler handler = new Handler();
    private static final int TOUCH_DURATION_THRESHOLD = 3000;
    private Runnable longTouchRunnable;
    private RelativeLayout relativeLayout;
    private Context context;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        speaker = new Speaker(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_voicememo);
        relativeLayout = findViewById(R.id.layout);
        relativeLayout.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startLongTouchTimer();
                    break;
                case MotionEvent.ACTION_UP:
                    cancelLongTouchTimer();
                    break;
            }
            return true;
        });

        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        TextToSpeech tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                speaker.speakText("The voice recording will began after the beep. Hold the screen to stop recording");
                BeginVoiceMemo();
            }
        });

    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        BeginVoiceMemo();
//    }

    public void BeginVoiceMemo() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Start recording voice memo after the delay
                startRecordingMemo();
            }
        }, 6000);
    }

    public void startRecordingMemo() {

        playBeep();

        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());

        File memoDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);

        File file = new File(memoDirectory, "ODSRecordingFile" + ".mp3");

        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(file.getPath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            Log.d("RecordMemo", e.getMessage());
            speaker.speakText("I'm sorry, I couldn't record the memo. Please try again");
        }
    }

    public void stopRecordingMemo() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
    }

    private void startLongTouchTimer() {
        if (longTouchRunnable == null) {
            longTouchRunnable = new Runnable() {
                @Override
                public void run() {
                    if(mediaRecorder != null) {
                        stopRecordingMemo();
                        speaker.speakText("Memo recorded successfully");
                    }

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
                if(matches != null) {
                    ProjectHelper.handleCommands(matches, VoiceMemoActivity.this, speaker, context);
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

    private void playBeep() {
        // Initialize ToneGenerator with the beep tone type and volume
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); // Adjust volume as needed

        // Play the beep sound for a short duration
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);

        // Delay for a short period to allow the beep sound to play before recording starts
        try {
            Thread.sleep(1000); // Adjust the delay duration as needed

        } catch (InterruptedException e) {

            e.printStackTrace();

        }

        // Release resources
        toneGenerator.release();
    }
}
