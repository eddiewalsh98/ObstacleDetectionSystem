package com.example.odsk00238061;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
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
import com.example.odsk00238061.utils.Product;
import com.example.odsk00238061.utils.ProjectHelper;
import com.example.odsk00238061.utils.Speaker;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailsActivity extends AppCompatActivity {

    private Context context;
    private SpeechRecognizer speechRecognizer;
    private Intent intentRecognizer;
    private static final int TOUCH_DURATION_THRESHOLD = 3000;
    private Handler handler = new Handler();
    private boolean hasExecuted = false;
    private Product product;
    private Runnable longTouchRunnable;
    private String productName = "";
    private String productDescription = "";
    private TextView productNameTextView;
    private TextView productDescriptionTextView;
    private ImageView productImageView;
    private Speaker speaker;
    private RelativeLayout relativeLayout;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);
        relativeLayout = findViewById(R.id.layout);

        Intent intent = getIntent();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String upc = intent.getStringExtra("barcode");
        product = new Product();

        context = this;

        productNameTextView = findViewById(R.id.productNameTextView);
        productDescriptionTextView = findViewById(R.id.productDescriptionTextView);
        productImageView = findViewById(R.id.productImageView);

        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        getProductDetails(upc);

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
                        ProjectHelper.initProductDetailsCommandHandler(speaker, context, product);
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

                                JSONObject productJ = products.getJSONObject(0);

                                String prodTitle = productJ.optString("title");
                                String prodCategory = productJ.optString("category");
                                String prodDescription = productJ.optString("description");
                                String prodIngredients = productJ.optString("ingredients");

                                if(prodTitle.trim().toLowerCase().equals(prodDescription
                                                   .trim().toLowerCase())){
                                    prodDescription = "";
                                }

                                product.setTitle(prodTitle);
                                product.setCategory(prodCategory);
                                product.setIngredients(prodIngredients);


                                JSONArray imagesArray = productJ.getJSONArray("images");
                                List<String> imagesList = new ArrayList<>();
                                for (int i = 0; i < imagesArray.length(); i++) {
                                    imagesList.add(imagesArray.getString(i));
                                }

                                product.setImages(imagesList);

                                if(!(product.getImages().isEmpty())){
                                    Picasso.get().load(product.getImages().get(0))
                                            .into(productImageView);
                                } else {
                                    productImageView.setImageResource(R.drawable.placeholder_image);
                                }

                                productNameTextView.setText(product.getTitle());

                                if(product.isDescriptionAvailable()) {
                                    productDescriptionTextView.setText(product.getDescription());
                                } else {
                                    productDescriptionTextView.setText("No description available");
                                }

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
                    ProjectHelper.ProductDetailsCommandHandler(matches, ProductDetailsActivity.this, speaker, context, product);
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