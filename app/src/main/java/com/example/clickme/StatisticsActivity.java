package com.example.clickme;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private RequestQueue queue;
    public final String URL_ROOT = "http://10.0.0.100:5000/";

    private Button overall_button;
    private Button phone_button;
    private Button coffee_button;
    private Button drowsiness_button;
    private TextView average_label;
    private ProgressBar average_progress;
    private TextView average_value;
    private TextView danger_value;
    private ImageView graphView;

    private double avgOverall = 0;
    private double avgPhone = 0;
    private double avgCoffee = 0;
    private double avgDrowsiness = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        overall_button = findViewById(R.id.overall_button);
        phone_button = findViewById(R.id.phone_button);
        coffee_button = findViewById(R.id.coffee_button);
        drowsiness_button = findViewById(R.id.drowsiness_button);
        average_label = findViewById(R.id.average_label);
        average_progress = findViewById(R.id.average_level_bar);
        average_value = findViewById(R.id.average_value);
        danger_value = findViewById(R.id.danger_value);
        graphView = findViewById(R.id.graph_view);

        average_progress.setMax(1000);

        overall_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAverageValues("overall");
                loadGraph("overall");
            }
        });

        phone_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAverageValues("phone");
                loadGraph("phone");
            }
        });

        coffee_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAverageValues("coffee");
                loadGraph("coffee");
            }
        });

        drowsiness_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAverageValues("drowsiness");
                loadGraph("drowsiness");
            }
        });


        queue = Volley.newRequestQueue(this);

        checkAvailability();

    }

    public void finishActivity(){
        this.finish();
    }

    public void showErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setMessage("Server is not available!");

        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finishActivity();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void checkAvailability(){

        queue = Volley.newRequestQueue(this);

        StringRequest checkAvailabilityRequest = new StringRequest(Request.Method.GET, URL_ROOT + "check_availability",
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        loadStatistics();
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        showErrorDialog();
                    }
                });

        checkAvailabilityRequest.setRetryPolicy(new DefaultRetryPolicy(
                250,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(checkAvailabilityRequest);
    }

    public void loadGraph(String type){
        Map<String, String> parameters = new HashMap<>();
        parameters.put("type", type);

        graphView.setImageDrawable(LoadImage(buildURI(URL_ROOT + "get_graph", parameters)));
    }

    public static String buildURI(String url, Map<String, String> params) {

        Uri.Builder builder = Uri.parse(url).buildUpon();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        return builder.build().toString();
    }

    public void loadStatistics(){

        StringRequest loadStatsRequest = new StringRequest(Request.Method.GET, URL_ROOT + "load_graph_data",
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        JSONObject jsonObject;
                        try {
                            jsonObject = new JSONObject(response);
                            avgOverall = Double.parseDouble((String) jsonObject.get("avg_overall"));
                            avgPhone = Double.parseDouble((String) jsonObject.get("avg_phone"));
                            avgCoffee = Double.parseDouble((String) jsonObject.get("avg_coffee"));
                            avgDrowsiness = Double.parseDouble((String) jsonObject.get("avg_drowsiness"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        setAverageValues("overall");
                        loadGraph("overall");
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        showErrorDialog();
                    }
                });

        loadStatsRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(loadStatsRequest);
    }

    public void setColor(double value){

        average_progress.setProgress((int)(value * 100));
        int color;

        if(value >= 5){
            color = Color.RED;
            danger_value.setText("Danger level: VERY HIGH");
        }
        else{
            if(value >= 2){
                color = Color.YELLOW;
                danger_value.setText("Danger level: HIGH");
            }
            else{
                color = Color.GREEN;
                danger_value.setText("Danger level: LOW");
            }
        }
        average_label.setTextColor(color);
        average_value.setTextColor(color);
        average_progress.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        danger_value.setTextColor(color);
    }

    public void setAverageValues(String type){

        if(type == "overall"){
            average_value.setText(String.valueOf(avgOverall));
            setColor(avgOverall);
        }
        if(type == "phone"){
            average_value.setText(String.valueOf(avgPhone));
            setColor(avgPhone);
        }
        if(type == "coffee"){
            average_value.setText(String.valueOf(avgCoffee));
            setColor(avgCoffee);
        }
        if(type == "drowsiness"){
            average_value.setText(String.valueOf(avgDrowsiness));
            setColor(avgDrowsiness);
        }
    }

    public static Drawable LoadImage(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            return d;
        } catch (Exception e) {
            return null;
        }
    }
}
