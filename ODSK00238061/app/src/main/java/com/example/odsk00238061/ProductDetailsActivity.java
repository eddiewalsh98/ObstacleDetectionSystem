package com.example.odsk00238061;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
            TextToSpeech tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        if(productDescription != null){
                            speaker.speakText("The product name is " + productName + ". Would you like to hear the description?");
                        } else{
                            speaker.speakText("The product name is " + productName + ". Unfortunately, there is no description available for this product.");

                        }
                    }
                }
            });

            hasExecuted = true;
        }
    }

    public void getProductDetails(String upc) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getResources().getString(R.string.BASE_URL) + upc
                    + "&formatted=y&key="
                    + getResources().getString(R.string.API_KEY);

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
                if(matches != null) {
                    if(matches.contains("yes") && productDescription != null){
                        speaker.speakText(productDescription);
                    } else if(matches.contains("yes") && productDescription == null){
                        speaker.speakText("Unfortunately, there is no description available for this product.");
                    } else {
                        ProjectHelper.handleCommands(matches, ProductDetailsActivity.this , speaker, context);
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
    }


}