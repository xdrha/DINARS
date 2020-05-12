package com.example.DINARS;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoViewFragment extends Fragment {

    private RequestQueue queue;

    private VideoViewFragmentViewModel VVFVM;
    private MJpegViewService MVS;
    private MinimizedActivityService MAS;
    private Boolean accidentallyMinimized = true;
    private Intent serviceIntent1;
    private Intent serviceIntent2;
    private Boolean stopped = false;
    private Statistics statistics = new Statistics();
    private int statisticCounter = 0;

    private ProgressBar distraction_level_bar;
    private TextView distraction_label;
    private TextView distraction_value;
    private Button calibration_button;
    private Button statistics_button;
    private Button minimize_button;
    private ImageButton break_button;
    private ImageView stop_image;
    private ImageView warning_image;
    private MJpegView MV;

    private MediaPlayer beepPlayer;
    private int beepStatus = 0;
    private int beepCounter = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_view, container, false);
        MV = view.findViewById(R.id.surface_view);

        distraction_level_bar = view.findViewById(R.id.distraction_level_bar);
        distraction_label = view.findViewById(R.id.distraction_label);
        distraction_value = view.findViewById(R.id.distraction_value);
        calibration_button = view.findViewById(R.id.calibration_button);
        statistics_button = view.findViewById(R.id.statistics_button);
        minimize_button = view.findViewById(R.id.minimize_button);
        break_button = view.findViewById(R.id.break_button);
        stop_image = view.findViewById(R.id.stop_image);
        warning_image = view.findViewById(R.id.warning_image);

        distraction_label.setTextColor(Color.GREEN);
        distraction_value.setTextColor(Color.GREEN);
        distraction_level_bar.getProgressDrawable().setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
        statistics_button.setEnabled(false);

        queue = Volley.newRequestQueue(getActivity());

        beepPlayer = MediaPlayer.create(getContext(), R.raw.beep_long);

        calibration_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               if(MVS != null){

                   if(MVS.calibrationMode == 0) MVS.calibrationMode = 1;
                   else
                       if(MVS.calibrationMode == 1) {
                           MVS.calibrationMode = 2;
                           calibration_button.setEnabled(false);
                       }

                   break_button.setEnabled(false);
                   break_button.setImageResource(R.drawable.coffee_disabled);
                   minimize_button.setEnabled(false);
                   statistics_button.setEnabled(false);
               }

            }

        });

        break_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(MVS.isPaused) {
                    MVS.isPaused = false;
                    minimize_button.setEnabled(true);
                    statistics_button.setEnabled(false);
                }
                else{
                    MVS.isPaused = true;
                    minimize_button.setEnabled(false);
                    statistics_button.setEnabled(true);
                }
            }
        });


        minimize_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accidentallyMinimized = false;
                minimize();
                getActivity().moveTaskToBack(false);
                getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
            }
        });

        statistics_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                accidentallyMinimized = false;

                Intent stats = new Intent(getActivity(), StatisticsActivity.class);
                getActivity().startActivity(stats);
            }
        });

        VVFVM = ViewModelProviders.of(this).get(VideoViewFragmentViewModel.class);

        VVFVM.getMJVSBinder().observe(this, new Observer<MJpegViewService.MyBinder>() {
            @Override
            public void onChanged(@Nullable MJpegViewService.MyBinder myBinder) {
                if(myBinder != null){
                    MVS = myBinder.getService();
                    new DoRead().execute(getResources().getString(R.string.urlCam));
                }
                else MVS = null;
            }
        });

        VVFVM.getMASBinder().observe(this, new Observer<MinimizedActivityService.MyBinder>(){
            @Override
            public void onChanged(@Nullable MinimizedActivityService.MyBinder myBinder) {
                if(myBinder != null) MAS = myBinder.getService();
                else MAS = null;
            }
        });

        VVFVM.getIsVideoProcessing().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable final Boolean aBoolean) {
                final Handler handler = new Handler();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if(MVS != null) {
                            if (aBoolean) {

                                if(MVS.calibrationMode == 0) {
                                    if (!break_button.isEnabled()) { //ak dokoncilo kalibraciu
                                        if (!MVS.isPaused)
                                            minimize_button.setEnabled(true);
                                        break_button.setEnabled(true);
                                        calibration_button.setEnabled(true);
                                        break_button.setImageResource(R.drawable.coffee);
                                        if (MVS.isPaused)
                                            statistics_button.setEnabled(true);
                                    }
                                    if (!MVS.isPaused) { //zapis statistiky iba ak nie je pauza alebo kalibracia
                                        setProgress(MVS.globalDistraction);
                                        makeStatistic(MVS.globalDistraction, MVS.phoneDistraction, MVS.coffeeDistraction, MVS.headTiltedFactor + MVS.eyesClosedFactor);
                                    }
                                }
                                if(MVS.calibrationMode == 1){
                                    calibration_button.setEnabled(true);
                                }
                                handler.postDelayed(this, 200);
                            }
                        }
                    }
                };
                handler.postDelayed(runnable, 200);
            }
        });

        VVFVM.getIsBeepNeeded().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable final Boolean aBoolean) {
                final Handler handler = new Handler();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (aBoolean) {
                            if(MVS != null) {

                                if(MVS.globalDistraction >= 50){ //urci beep status
                                    if(beepStatus == 0) {
                                        beepStatus = 1;
                                        beepCounter = 10;
                                    }
                                }
                                else{
                                    if(beepStatus > 0) {
                                        beepStatus = 0;
                                        beepCounter = 10;
                                    }
                                }

                                if (beepStatus > 0 && MVS.calibrationMode == 0 && !MVS.isPaused) {
                                    if (MVS.globalDistraction >= 80) {
                                        if(beepCounter >= 3) beepCounter = 0;
                                    }
                                    else{
                                        if(beepCounter >= 10) beepCounter = 0;
                                    }
                                    if(beepCounter == 0) {
                                        beepPlayer.start();
                                    }
                                    beepCounter++;
                                }
                                handler.postDelayed(this, 200);
                            }
                        }
                    }
                };
                handler.postDelayed(runnable, 200);
            }
        });

        VVFVM.getIsHidden().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable final Boolean aBoolean) {

                final Handler handler = new Handler();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        if(stopped){
                            handler.removeCallbacks(this);
                        }
                        else {

                            if (aBoolean) {
                                if (MAS.hidden) {
                                    //vypni service, zapni main
                                    moveMainActivityToFront();
                                    handler.removeCallbacks(this);
                                } else {
                                    handler.postDelayed(this, 200);
                                }

                            } else {
                                try {
                                    if (!getActivity().getWindow().getDecorView().getRootView().isShown()) {
                                        if (accidentallyMinimized) {
                                            handler.removeCallbacks(this);
                                            minimize();
                                        }
                                    } else {
                                        if (!MV.minimized)
                                            handler.postDelayed(this, 200);
                                    }
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                };
                handler.postDelayed(runnable, 200);
            }
        });

        startService1();
        startService2();
        VVFVM.setIsHidden(false);
        VVFVM.setIsProcessing(true);
        VVFVM.setIsBeepNeeded(true);

        clearStatistics();

        return view;
    }

    private void makeStatistic(int globalDistraction, int phoneDistraction, int coffeeDistraction, int drowsinessLevel){

        if(globalDistraction > 100) globalDistraction = 100;

        statistics.newStatistic(globalDistraction / 10.0, phoneDistraction / 10.0, coffeeDistraction / 10.0, drowsinessLevel / 10.0, statisticCounter);

        if(statisticCounter == 4){
            sendStatistics();
            statistics.init();
            statisticCounter = 0;
        }
        else{
            statisticCounter++;
        }
    }

    private void sendStatistics(){

        JSONObject statistic = new JSONObject();
        try {
            double avgGD = 0;
            double avgPD = 0;
            double avgCD = 0;
            double avgDL = 0;

            for(int i = 0; i < 5; i++){
                avgGD += statistics.getGlobalDistraction(i);
                avgPD += statistics.getPhoneDistraction(i);
                avgCD += statistics.getCoffeeDistraction(i);
                avgDL += statistics.getDrowsinessLevel(i);
            }

            statistic.put("globalDistraction", avgGD / 5);
            statistic.put("phoneDistraction", avgPD / 5);
            statistic.put("coffeeDistraction", avgCD / 5);
            statistic.put("drowsinessLevel", avgDL / 5);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String requestBody = statistic.toString();

        StringRequest sendStatisticsRequest = new StringRequest(Request.Method.POST, getResources().getString(R.string.urlServer) + "upload_data",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody(){
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    return null;
                }
            }
        };

        sendStatisticsRequest.setRetryPolicy(new DefaultRetryPolicy(
                1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(sendStatisticsRequest);

    }

    private void clearStatistics(){
        StringRequest clearStatisticsRequest = new StringRequest(Request.Method.GET, getResources().getString(R.string.urlServer) + "clear_stats",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });

        clearStatisticsRequest.setRetryPolicy(new DefaultRetryPolicy(
                100,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(clearStatisticsRequest);
    }

    private void doBeep(int distraction){
        if(distraction >= 50){
            if(beepStatus == 0) {
                beepStatus = 1;
            }
        }
        else{
            if(beepStatus > 0) {
                beepStatus = 0;
            }
        }
    }

    private void setProgress(int distraction){

        doBeep(distraction);

        if(distraction > 100) distraction = 100;

        int color;

        if(MV.minimized){

            MAS.distraction_level_bar.setMax(100);
            MAS.distraction_level_bar.setProgress(distraction);
            MAS.distraction_value.setText(String.valueOf(distraction / 10.0));
            color = Color.GREEN;

            if(distraction >= 50 && distraction < 80){
                color = Color.YELLOW;
                MAS.warning_image.setVisibility(View.VISIBLE);
                MAS.stop_image.setVisibility(View.INVISIBLE);
            }
            else{
                if(distraction >= 80){
                    color = Color.RED;
                    MAS.warning_image.setVisibility(View.VISIBLE);
                    MAS.stop_image.setVisibility(View.VISIBLE);
                }
                else{ // <50
                    MAS.warning_image.setVisibility(View.INVISIBLE);
                    MAS.stop_image.setVisibility(View.INVISIBLE);
                }
            }
            MAS.distraction_label.setTextColor(color);
            MAS.distraction_value.setTextColor(color);
            MAS.distraction_level_bar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        else {
            distraction_level_bar.setMax(100);
            distraction_level_bar.setProgress(distraction);
            distraction_value.setText(String.valueOf(distraction / 10.0));
            color = Color.GREEN;

            if (distraction >= 50 && distraction < 80) {
                warning_image.setVisibility(View.VISIBLE);
                stop_image.setVisibility(View.INVISIBLE);
                color = Color.YELLOW;
            }
            else {
                if (distraction >= 80) {
                    warning_image.setVisibility(View.VISIBLE);
                    stop_image.setVisibility(View.VISIBLE);
                    color = Color.RED;
                }
                else { // <50
                    warning_image.setVisibility(View.INVISIBLE);
                    stop_image.setVisibility(View.INVISIBLE);
                }
            }
            distraction_label.setTextColor(color);
            distraction_value.setTextColor(color);
            distraction_level_bar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    private void minimize(){
        MV.minimized = true;
        VVFVM.setIsHidden(true);
        MAS.resumeInterface();
    }

    private void moveMainActivityToFront(){
        Intent mainActivity = new Intent(getActivity(), MainActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(mainActivity);
        MV.minimized = false;
        VVFVM.setIsHidden(false);
    }

    public void onPause(){
        super.onPause();
        MVS.isMinimized = true;
    }

    public void onResume(){
        super.onResume();
        accidentallyMinimized = true;
        if(MVS != null) {
            MVS.isMinimized = false;
        }
        if(MAS != null && !MAS.hidden){
            MAS.hideTopPanel();
        }
    }

    @Override
    public void onDestroy() {
        stopped = true;
        MVS.mRun = false;
        getActivity().unbindService(VVFVM.getMJVSConnection());
        getActivity().unbindService(VVFVM.getMASConnection());
        getActivity().finishAffinity();
        System.exit(0);
        super.onDestroy();
    }

    @Override
    public void onStop() {
        if(accidentallyMinimized) {
            getActivity().moveTaskToBack(false);
            getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
        }
        super.onStop();
    }

    private void startService1(){
        serviceIntent1 = new Intent(getActivity(), MinimizedActivityService.class);
        getActivity().startService(serviceIntent1);
        getActivity().bindService(serviceIntent1, VVFVM.getMASConnection(), Context.BIND_AUTO_CREATE);
    }

    private void startService2(){
        serviceIntent2 = new Intent(getActivity(), MJpegViewService.class);
        getActivity().startService(serviceIntent2);
        getActivity().bindService(serviceIntent2, VVFVM.getMJVSConnection(), Context.BIND_AUTO_CREATE);
    }

    private class DoRead extends AsyncTask<String, Void, MJpegInputStream> {

        protected MJpegInputStream doInBackground(String... Url) {
            //TODO: if camera has authentication deal with it and don't just not work

            try {
                java.net.URL url = new URL(Url[0]); // here is your URL path
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(550 );
                conn.setConnectTimeout(550);
                int responseCode = conn.getResponseCode();

                if (responseCode == 401) {
                    return null;
                }
                return new MJpegInputStream(conn.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MJpegInputStream res) {

            if (res != null) {
                VVFVM.setIsProcessing(true);

                MVS.mIn = res;
                MVS.mRun = true;
                MVS.mSurfaceHolder = MV.holder;
                MVS.setSurfaceSize(788, 443);
                MVS.setDisplayMode(MJpegView.SIZE_BEST_FIT);
                MVS.startPretendLongRunningTask();
            }

        }
    }
}
