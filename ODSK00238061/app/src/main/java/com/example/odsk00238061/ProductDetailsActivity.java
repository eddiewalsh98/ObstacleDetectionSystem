package com.example.odsk00238061;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.Speaker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ProductDetailsActivity extends AppCompatActivity {

    private Context context;
    private Intent intentRecognizer;
    private SpeechRecognizer speechRecognizer;
    private static final int TOUCH_DURATION_THRESHOLD = 3000;
    private Handler handler = new Handler();
    private boolean hasExecuted = false;
    private Runnable longTouchRunnable;
    private String productName = "";
    private String productDescription = "";
    private TextView productNameTextView;
    private TextView productDescriptionTextView;
    private Speaker speaker;
    private RelativeLayout relativeLayout;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);
        Intent intent = getIntent();
        String upc = intent.getStringExtra("barcode");
        context = this;
        productNameTextView = findViewById(R.id.productNameTextView);
        productDescriptionTextView = findViewById(R.id.productDescriptionTextView);
        getProductDetails(upc);
        relativeLayout = findViewById(R.id.layout);
        speaker = new Speaker(this);
        relativeLayout.setOnTouchListener(new View.OnTouchListener() {
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

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executeOnStartLogic();
            }
        }, 3000);
    }

    private void executeOnStartLogic() {
        if (!hasExecuted) {
            if (productName != "" && productDescription != "" && speaker != null) {
                speaker.speakText("The product name is " + productName);
                speaker.speakText("The ingredients include " + productDescription);
            } else {
                if (speaker != null) {

                } else {
                    Log.e("YourActivity", "Speaker is not initialized");
                }
            }
            // Set the flag to true to ensure this logic is executed only once
            hasExecuted = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(productName != "") {
            speaker.speakText("The product name is" + productName);
            speaker.speakText("The incredients include " + productDescription);
        } else {
            speaker.speakText("Sorry, I could not find the product details");
        }
    }

    public void getProductDetails(String upc) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.barcodelookup.com/v3/products?barcode=" + upc + "&formatted=y&key=cy0xfgm8a8y6u0ytpb50snsf17o21c";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            JSONArray products = jsonResponse.getJSONArray("products");
                            if (products.length() > 0) {
                                JSONObject product = products.getJSONObject(0);
                                productName = product.optString("title");
                                productDescription = product.optString("ingredients");
                                productNameTextView.setText(productName);
                                productDescriptionTextView.setText(productDescription);
                            } else {

                            }
                        } catch (JSONException e) {
                            Log.d("ProductDetailsActivity", "Error: " + e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ProductDetailsActivity", "Error: " + error.getMessage());
            }
        });
        queue.add(stringRequest);
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

                if(matches.contains("yes") || matches.contains("repeat")) {
                    if(productName != "") {
                        speaker.speakText("The product name is" + productName);
                        speaker.speakText("The incredients include " + productDescription);
                    } else {
                        speaker.speakText("Sorry, I could not find the product details");
                    }
                } else if(matches.contains("no")) {
                    speaker.speakText("Okay, let me know if you need any help");
                } else if(matches.contains("detect obstacles")){
                    Intent intent = new Intent(ProductDetailsActivity.this, ObjectDetectionActivity.class);
                    speaker.Destroy();
                    startActivity(intent);
                } else if(matches.contains("text to speech")){
                    Intent intent = new Intent(ProductDetailsActivity.this, TextToSpeechActivity.class);
                    speaker.Destroy();
                    startActivity(intent);
                } else if(matches.contains("barcode")) {
                    Intent intent = new Intent(ProductDetailsActivity.this, BarcodeScannerActivity.class);
                    speaker.Destroy();
                    startActivity(intent);
                } else if(matches.contains("battery life")) {
                    float batteryLevel = ProjectHelper.getBatteryLevel(context);
                    speaker.speakText("Your battery level is " + batteryLevel + " percent");
                } else if(matches.contains("help")) {
                    speaker.speakText("You can say 'read' to read text from the camera " +
                            "or 'detect objects' to detect objects from the camera");
                } else if(matches.contains("record voice memo")) {
                    Intent intent = new Intent(ProductDetailsActivity.this, VoiceMemoActivity.class);
                    speaker.Destroy();
                    startActivity(intent);
                } else {
                    speaker.speakText("Sorry, I did not understand that");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }


}