package com.example.clickme;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.LinearLayout;
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

    private final String URL = "http://admin:ms1234@10.0.0.3:80/ipcam/mjpeg.cgi";
    private RequestQueue queue;
    private final String URL_ROOT = "http://10.0.0.100:5000/";

    private MinimizedActivityService MAS;
    private MainActivityViewModel MAVM;
    private MjpegView mv;

    private VideoViewFragmentViewModel VVFVM;
    private MjpegViewService MVS;
    private MjpegInputStream result;
    private Boolean accidentallyMinimized = true;
    private Intent serviceIntent1;
    private Intent serviceIntent2;
    private Boolean stopped = false;
    private Statistics statistics = new Statistics();
    private int statisticCounter = 0;

    public ProgressBar distraction_level_bar;
    public TextView distraction_label;
    public TextView distraction_value;
    public Button calibration_button;
    public Button statistics_button;
    public Button minimize_button;
    public ImageButton break_button;
    public ImageView stop_image;
    public ImageView warning_image;
    public LinearLayout lll;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_view, container, false);
        mv = view.findViewById(R.id.surface_view);

        distraction_level_bar = view.findViewById(R.id.distraction_level_bar);
        distraction_label = view.findViewById(R.id.distraction_label);
        distraction_value = view.findViewById(R.id.distraction_value);
        calibration_button = view.findViewById(R.id.calibration_button);
        statistics_button = view.findViewById(R.id.statistics_button);
        minimize_button = view.findViewById(R.id.minimize_button);
        break_button = view.findViewById(R.id.break_button);
        stop_image = view.findViewById(R.id.stop_image);
        warning_image = view.findViewById(R.id.warning_image);
        lll = view.findViewById(R.id.lll);

        distraction_label.setTextColor(Color.GREEN);
        distraction_value.setTextColor(Color.GREEN);
        distraction_level_bar.getProgressDrawable().setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
        statistics_button.setEnabled(false);

        queue = Volley.newRequestQueue(getActivity());

        calibration_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               if(MVS != null){

                   calibration_button.setText("calibration");
                   if(MVS.calibrationMode == 0) MVS.calibrationMode = 1;
                   else
                       if(MVS.calibrationMode == 1) MVS.calibrationMode = 2;

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

        VVFVM.getBinder().observe(this, new Observer<MjpegViewService.MyBinder>() {
            @Override
            public void onChanged(@Nullable MjpegViewService.MyBinder myBinder) {
                if(myBinder != null){
                    MVS = myBinder.getService();
                    new DoRead().execute(URL);
                }
                else{
                    MVS = null;
                }
            }
        });

        VVFVM.getIsProgressUpdating().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable final Boolean aBoolean) {
                final Handler handler = new Handler();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if(MVS != null) {
                            if (aBoolean) {

                                if(MVS.calibrationMode == 0)
                                    if (!break_button.isEnabled()){ //ak dokoncilo kalibraciu
                                    if(!MVS.isPaused) minimize_button.setEnabled(true);
                                    break_button.setEnabled(true);
                                    break_button.setImageResource(R.drawable.coffee);
                                    if(MVS.isPaused) statistics_button.setEnabled(true);
                                }
                                setProgress(MVS.globalDistraction);
                                    if(!MVS.isPaused && MVS.calibrationMode == 0){ //zapis statistiky iba ak nie je pauza alebo kalibracia
                                        makeStatistic(MVS.globalDistraction, MVS.phoneDistraction, MVS.coffeeDistraction, MVS.headTiltedFactor + MVS.eyesClosedFactor);
                                    }

                                handler.postDelayed(this, 200);
                            }
                        }
                    }
                };
                handler.postDelayed(runnable, 200);
            }
        });

        MAVM = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        MAVM.getBinder().observe(this, new Observer<MinimizedActivityService.MyBinder>() {
            @Override
            public void onChanged(@Nullable MinimizedActivityService.MyBinder myBinder) {
                if(myBinder != null){
                    MAS = myBinder.getService();
                }
                else{
                    MAS = null;
                }
            }
        });

        MAVM.getIsProgressUpdating().observe(this, new Observer<Boolean>() {
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
                                if (MAS.hiden) {
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
                                        if (!mv.minimized)
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
        MAVM.setIsUpdating(false);
        VVFVM.setIsUpdating(true);

        clearStatistics();

        return view;
    }

    public void makeStatistic(int globalDistraction, int phoneDistraction, int coffeeDistraction, int drowsinessLevel){

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

    public void sendStatistics(){

        JSONObject statistic = new JSONObject();
        try {
            statistic.put("timestamp", System.currentTimeMillis()/1000);
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

        StringRequest sendStatisticsRequest = new StringRequest(Request.Method.POST, URL_ROOT + "upload_data",
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

    public void clearStatistics(){
        StringRequest clearStatisticsRequest = new StringRequest(Request.Method.GET, URL_ROOT + "clear_stats",
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

    public void setProgress(int distraction){

        if(distraction > 100) distraction = 100;

        if(mv.minimized){

            MAS.distraction_level_bar.setMax(100);
            MAS.distraction_level_bar.setProgress(distraction);
            MAS.distraction_value.setText(String.valueOf(distraction / 10.0));
            int color = Color.GREEN;

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
            int color = Color.GREEN;

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

    public void minimize(){
        mv.minimized = true;
        MAVM.setIsUpdating(true);
        MAS.resumeInterface();
    }

    public void moveMainActivityToFront(){
        Intent mainActivity = new Intent(getActivity(), MainActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(mainActivity);
        mv.minimized = false;
        MAVM.setIsUpdating(false);
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
        if(MAS != null && !MAS.hiden){
            MAS.hideTopPanel();
        }
    }

    @Override
    public void onDestroy() {

        stopped = true;
        MVS.mRun = false;
        getActivity().unbindService(VVFVM.getServiceConnection());
        getActivity().unbindService(MAVM.getServiceConnection());
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

    public void startService1(){
        serviceIntent1 = new Intent(getActivity(), MinimizedActivityService.class);
        getActivity().startService(serviceIntent1);
        getActivity().bindService(serviceIntent1, MAVM.getServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    public void startService2(){
        serviceIntent2 = new Intent(getActivity(), MjpegViewService.class);
        getActivity().startService(serviceIntent2);
        getActivity().bindService(serviceIntent2, VVFVM.getServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {

        protected MjpegInputStream doInBackground(String... Url) {
            //TODO: if camera has authentication deal with it and don't just not work

            try {
                java.net.URL url = new URL(Url[0]); // here is your URL path
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(150 );
                conn.setConnectTimeout(150);
                int responseCode = conn.getResponseCode();

                if (responseCode == 401) {
                    return null;
                }
                return new MjpegInputStream(conn.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream res) {

            if (res != null) {
                mv.mIn = res;
                result = res;
                VVFVM.setIsUpdating(true);

                if(mv.mIn != null) {
                    MVS.mIn = result;
                    mv.mRun = true;
                    MVS.mRun = true;
                    MVS.mSurfaceHolder = mv.holder;
                    MVS.setSurfaceSize(788, 443);
                    MVS.startPretendLongRunningTask();

                    MVS.setDisplayMode(MjpegView.SIZE_BEST_FIT);
                }


            }

        }
    }
}
