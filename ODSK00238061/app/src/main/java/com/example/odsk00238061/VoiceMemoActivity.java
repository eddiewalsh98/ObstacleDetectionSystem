package com.example.odsk00238061;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
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
    private MediaPlayer mediaPlayer;
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        BeginVoiceMemo();
    }

    public void BeginVoiceMemo() {
        startRecordingMemo();
    }

    public void startRecordingMemo() {
        speaker.speakText("Memo recording starting now");
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

                    if(matches.contains("detect objects")) {
                        Intent intent = new Intent(VoiceMemoActivity.this, ObjectDetectionActivity.class);
                        speaker.Destroy();
                        startActivity(intent);

                    } else if(matches.contains("text to speech")){

                        Intent intent = new Intent(VoiceMemoActivity.this, TextToSpeechActivity.class);
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

                        startRecordingMemo();
                        speaker.speakText("Memo recorded successfully");

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

}
