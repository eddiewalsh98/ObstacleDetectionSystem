package com.example.odsk00238061.utils;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.example.odsk00238061.BarcodeScannerActivity;
import com.example.odsk00238061.ObjectDetectionActivity;
import com.example.odsk00238061.TextToSpeechActivity;
import com.example.odsk00238061.VoiceMemoActivity;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ProjectHelper {

    public static String calculateObstacleLocation(Rect rect, int pWidth){
        int previewWidth = pWidth;
        int rectCenterX = rect.centerX();

        int leftThreshold = previewWidth / 4;
        int rightThreshold = 3 * previewWidth / 4;

        if (rectCenterX < leftThreshold) {
            if (rectCenterX < previewWidth / 8) {
                return "Far Left";
            } else {
                return "Left Ahead";
            }
        } else if (rectCenterX < previewWidth / 2) {
            if (rectCenterX < previewWidth * 3 / 8) {
                return "Center Left";
            } else {
                return "Center Ahead";
            }
        } else if (rectCenterX == previewWidth / 2) {
            return "Center";
        } else if (rectCenterX < rightThreshold) {
            if (rectCenterX < previewWidth * 5 / 8) {
                return "Center Right";
            } else {
                return "Right Ahead";
            }
        } else {
            if (rectCenterX < previewWidth * 7 / 8) {
                return "Far Right";
            } else {
                return "Right Ahead";
            }
        }
    }

    public static float getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = (level / (float) scale) * 100;
        return batteryPct;
    }

    public static void vibrate(Context context, long milliseconds) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(milliseconds);
            }
        }
    }

    public static Bitmap resizeBitmap(Bitmap getBitmap, int maxSize) {
        int width = getBitmap.getWidth();
        int height = getBitmap.getHeight();
        double x;

        if (width >= height && width > maxSize) {
            x = width / height;
            width = maxSize;
            height = (int) (maxSize / x);
        } else if (height >= width && height > maxSize) {
            x = height / width;
            height = maxSize;
            width = (int) (maxSize / x);
        }
        return Bitmap.createScaledBitmap(getBitmap, width, height, false);
    }

    public static void handleCommands(ArrayList<String> matches, Activity activity, Speaker speaker, Context context){
        if(matches.contains("text to speech")){

            Intent intent = new Intent(activity, TextToSpeechActivity.class);
            speaker.Destroy();
            activity.startActivity(intent);

        } else if(matches.contains("detect obstacles")){

            Intent intent = new Intent(activity, ObjectDetectionActivity.class);
            speaker.Destroy();
            activity.startActivity(intent);

        } else if(matches.contains("scan barcode")){

            Intent intent = new Intent(activity, BarcodeScannerActivity.class);
            speaker.Destroy();
            activity.startActivity(intent);

        } else if(matches.contains("settings")) {

            speaker.speakText("settings are currently under construction");

        } else if(matches.contains("record memo")) {

            Intent intent = new Intent(activity, VoiceMemoActivity.class);
            speaker.Destroy();
            activity.startActivity(intent);

        } else if(matches.contains("play memo")){

            PlayLatestMemo(context, speaker);

        } else if(matches.contains("help")) {

            speaker.speakText(getHelpMessage());

        } else {
            speaker.speakText("I'm sorry, I didn't understand that. Please try again");
        }
    }

    private static void PlayLatestMemo(Context context, Speaker speaker){
        ContextWrapper contextWrapper = new ContextWrapper(context);
        File memoDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(memoDirectory, "ODSRecordingFile" + ".mp3");

        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            speaker.speakText("I'm sorry, I couldn't play the memo. Please try again");
        }
    }

    private static String getHelpMessage() {
        return "You can say the following commands: \n" +
                "Text to speech, Detect obstacles, Scan barcode, Settings, Record memo, Play memo";
    }

}
