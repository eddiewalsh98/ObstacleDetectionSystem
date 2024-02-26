//package com.example.odsk00238061.utils;
//
//import android.Manifest;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.media.AudioFormat;
//import android.media.AudioRecord;
//import android.media.MediaRecorder;
//
//import androidx.core.app.ActivityCompat;
//
//public class HollySpeechRecognizer {
//    private static final String wakeWord = "hey holly";
//
//    private static final int SAMPLE_RATE = 16000;
//    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
//    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
//    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
//
//    private boolean isRunning = false;
//    private AudioRecord audioRecord;
//    private Thread recordingThread;
//    private HollySpeechRecognizerListener listener;
//    private Context context;
//
//    public HollySpeechRecognizer(HollySpeechRecognizerListener listener, Context context) {
//        this.listener = listener;
//        this.context = context;
//    }
//
//    public void startDetection() {
//        if (!isRunning) {
//            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
//                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
//                audioRecord.startRecording();
//                isRunning = true;
//                recordingThread = new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        detectWakeWord();
//                    }
//                });
//                recordingThread.start();
//            }
//        }
//    }
//
//    public void stopDetection() {
//        if (isRunning) {
//            isRunning = false;
//            audioRecord.stop();
//            audioRecord.release();
//            audioRecord = null;
//        }
//    }
//
//    private void detectWakeWord() {
//        short[] buffer = new short[BUFFER_SIZE];
//        while (isRunning) {
//            int numSamplesRead = audioRecord.read(buffer, 0, BUFFER_SIZE);
//            if (numSamplesRead > 0) {
//                // Perform wake word detection on the audio data in the buffer
//                if (isWakeWordDetected(buffer)) {
//                    listener.onWakeWordDetected();
//                }
//            } else {
//                // Log.e(TAG, "Error reading audio data from microphone");
//                stopDetection();
//            }
//        }
//    }
//
//    private boolean isWakeWordDetected(short[] audioData) {
//        int wakeWordLength = wakeWord.length();
//        int audioDataLength = audioData.length;
//
//        // Iterate over the audio data to search for the wake word pattern
//        for (int i = 0; i <= audioDataLength - wakeWordLength; i++) {
//            boolean isMatch = true;
//            for (int j = 0; j < wakeWordLength; j++) {
//                // Compare audio data with wake word pattern
//                if (audioData[i + j] != (short) wakeWord.charAt(j)) {
//                    isMatch = false;
//                    break;
//                }
//            }
//            if (isMatch) {
//                // Wake word detected
//                return true;
//            }
//        }
//
//        // Wake word not detected
//        return false;
//    }
//
//    public interface HollySpeechRecognizerListener {
//        void onWakeWordDetected();
//    }
//}
