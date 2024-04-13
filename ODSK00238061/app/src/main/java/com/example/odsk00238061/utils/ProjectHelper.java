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
        if(matches.contains("text-to-speech") || matches.contains("text to speech")
                || matches.contains("speech") || matches.contains("text")){

            Intent intent = new Intent(activity, TextToSpeechActivity.class);
            speaker.Destroy();
            activity.startActivity(intent);

        } else if(matches.contains("detect obstacles") || matches.contains("obstacles") || matches.contains("detect")) {

            Intent intent = new Intent(activity, ObjectDetectionActivity.class);
            speaker.Destroy();
            activity.startActivity(intent);

        } else if(matches.contains("scan barcode") || matches.contains("barcode") || matches.contains("scan")) {

            Intent intent = new Intent(activity, BarcodeScannerActivity.class);
            speaker.Destroy();
            activity.startActivity(intent);

        } else if(matches.contains("battery")) {

            float batteryLevel = getBatteryLevel(context);
            speaker.speakText("The current battery life is " + batteryLevel + " percent");

        } else if(matches.contains("record memo") || matches.contains("record")) {

            Intent intent = new Intent(activity, VoiceMemoActivity.class);
            speaker.Destroy();
            activity.startActivity(intent);

        } else if(matches.contains("play memo") || matches.contains("play")) {

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
                "Text to speech, Detect obstacles, Scan barcode, Battery, Record memo, Play memo";
    }


    public static void initProductDetailsCommandHandler(Speaker speaker, Context context, Product product){
        if(!product.isTitleAvailable() && !product.isDescriptionAvailable()){
            speaker.speakText("I'm sorry, I couldn't find any information about this product.");
            return;
        }

        if(product.isFood()){
            if(product.isIngredientsAvailable() && product.isNutritionFactsAvailable()) {
                speaker.speakText("The product name is " + product.getTitle() + ". " +
                                  "To get the ingredients, say 'ingredients'. " +
                                  "To get the nutrition facts, say 'nutrition'.");
            } else {
                speaker.speakText("The product name is " + product.getTitle() + ". " +
                                  "Some of the product details may not be available.");
            }

        } else {
            if(product.isDescriptionAvailable()){
                speaker.speakText("The product name is " + product.getTitle() + ". " +
                                  "To get the description, say 'description'.");
            } else {
                speaker.speakText("The product name is " + product.getTitle() + ".");
            }
        }
    }

    public static void ProductDetailsCommandHandler(ArrayList<String> matches, Activity activity, Speaker speaker, Context context, Product product) {
        if(matches.contains("ingredients")){
            if(product.isIngredientsAvailable()){
                speaker.speakText("The ingredients include " + product.getIngredients());
            } else {
                speaker.speakText("Unfortunately, there are no ingredients available for this product.");
            }
        } else if(matches.contains("nutrition")){
            if(product.isNutritionFactsAvailable()){
                speaker.speakText("The nutrition facts include " + product.getNutritionFacts());
            } else {
                speaker.speakText("Unfortunately, there are no nutrition facts available for this product.");
            }
        } else if(matches.contains("description")){
            if(product.isDescriptionAvailable()){
                speaker.speakText(product.getDescription());
            } else {
                speaker.speakText("Unfortunately, there is no description available for this product.");
            }
        } else if(matches.contains("product name")) {
            if(product.isTitleAvailable()){
                speaker.speakText("The product name is " + product.getTitle());
            } else {
                speaker.speakText("Unfortunately, there is no product name available for this product.");
            }
        } else if(matches.contains("repeat")){
            initProductDetailsCommandHandler(speaker, context, product);

        } else {
            handleCommands(matches, activity, speaker, context);
        }
    }


    public static InputImage imageProxyToInputImage(ImageProxy imageProxy, int imageSize) {
        Bitmap bitmap = imageProxy.toBitmap();
        bitmap = ProjectHelper.resizeBitmap(bitmap, imageSize);

        return InputImage.fromBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());
    }

}
