package com.example.clickme;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class VideoViewFragment extends Fragment {

    private static final String URL = "http://admin:ms1234@10.0.0.3:80/ipcam/mjpeg.cgi";
    private RequestQueue queue;
    public final static String URL_ROOT = "http://192.168.0.129:5000/";

    private MinimizedActivityService MAS;
    private MainActivityViewModel MAVM;
    private MjpegView mv;

    private VideoViewFragmentViewModel VVFVM;
    private VideoStreamService VSS;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_view, container, false);
        mv = view.findViewById(R.id.surface_view);

        final ProgressBar distraction_level_bar = view.findViewById(R.id.distraction_level_bar);
        final TextView distraction_label = view.findViewById(R.id.distraction_label);
        final TextView distraction_value = view.findViewById(R.id.distraction_value);
        final Button capture_button = view.findViewById(R.id.capture_button);
        final Button statistics_button = view.findViewById(R.id.statistics_button);
        final ImageButton break_button = view.findViewById(R.id.break_button);
        final ImageButton maximize_button = view.findViewById(R.id.maximize_button);
        final ImageView stop_image = view.findViewById(R.id.stop_image);
        final ImageView warning_image = view.findViewById(R.id.warning_image);

        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                queue = Volley.newRequestQueue(getActivity());

                StringRequest checkAvailabilityRequest = new StringRequest(Request.Method.GET, URL_ROOT + "send_something",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                String nieco = "niccc";
                                capture_button.setText(nieco);
                                JSONObject jsonObject = null;
                                try {
                                    jsonObject = new JSONObject(response);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    nieco = (String) jsonObject.get("response");
                                    capture_button.setText(nieco);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(getActivity(), "Server unavailable " + error.getMessage(), Toast.LENGTH_LONG).show();
                                capture_button.setText("KOKOTINA");
                            }
                        });

                checkAvailabilityRequest.setRetryPolicy(new DefaultRetryPolicy(
                        1000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                queue.add(checkAvailabilityRequest);
            }

        });

        break_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

            }
        });


        maximize_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mv.minimized = true;
                getActivity().moveTaskToBack(true);
                getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
                MAS.resumeInterface();
            }
        });

        statistics_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //toggleUpdates();
                if(VSS != null){
                    VSS.startPretendLongRunningTask();
                    VVFVM.setIsUpdating(true);
                }

                System.out.println("distraction_label height" + mv.getHeight());
                System.out.println("distraction_label width" + mv.getWidth());

                System.out.println("distraction_level_bar height" + distraction_level_bar.getHeight());
                System.out.println("distraction_level_bar width" + distraction_level_bar.getWidth());

                System.out.println("distraction_value height" + distraction_value.getGravity());
                System.out.println("distraction_value width" + distraction_value.getTextSize());

                System.out.println("warning_image height" + warning_image.getHeight());
                System.out.println("warning_image width" + warning_image.getWidth());

                System.out.println("stop_image height" + stop_image.getHeight());
                System.out.println("stop_image width" + stop_image.getWidth());

                System.out.println("maximize height" + maximize_button.getHeight());
                System.out.println("maximize width" + maximize_button.getWidth());
            }
        });

        VVFVM = ViewModelProviders.of(this).get(VideoViewFragmentViewModel.class);

        VVFVM.getBinder().observe(this, new Observer<VideoStreamService.MyBinder>() {
            @Override
            public void onChanged(@Nullable VideoStreamService.MyBinder myBinder) {
                if(myBinder != null){
                    VSS = myBinder.getService();
                }
                else{
                    VSS = null;
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
                        if(aBoolean){
                            if(VVFVM.getBinder().getValue() != null){
                                if(VSS.getResult() != null && VSS.isNotSet()) {
                                    VSS.setResult();
                                    System.out.println("////////////////////////// toto sa vykonalo huraaaaaa");
                                    mv.setSource(VSS.getResult());
                                    mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
                                    mv.showFps(true);
                                    VVFVM.setIsUpdating(false);
                                    handler.postDelayed(this, 100);
                                }
                                else{
                                    handler.postDelayed(this, 100);
                                    System.out.println("////////////////////////// najprv nie");
                                }
                            }
                            else{
                                System.out.println("////////////////////////// mega strasna vec");
                            }
                        }
                        else{
                            System.out.println("////////////////////////// kokotina ale strasna....");
                            VVFVM.setIsUpdating(false);
                            handler.removeCallbacks(this);
                        }
                    }
                };
                handler.postDelayed(runnable, 100);
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
                        if(aBoolean){
                            if(MAVM.getBinder().getValue() != null){
                                if(MAS.getProgress() == MAS.getMaxValue()){
                                    System.out.println("/////////////////////// ZASTAVI TO....");
                                    MAVM.setIsUpdating(false);
                                }
                                System.out.println("/////////////////////// VYKRESLUJE....");
                                MAS.distraction_level_bar.setProgress(MAS.getProgress());
                                MAS.distraction_level_bar.setMax(MAS.getMaxValue());
                                distraction_level_bar.setProgress(MAS.getProgress());
                                distraction_level_bar.setMax(MAS.getMaxValue());
                                handler.postDelayed(this, 100);
                            }

                        }
                        else{
                            System.out.println("/////////////////////// FALSE....");
                            handler.removeCallbacks(this);
                        }
                    }
                };

                if(aBoolean){
                    distraction_value.setText("HAHA");
                    handler.postDelayed(runnable, 100);
                    System.out.println("/////////////////////// SETUJE NA HAHA....");
                }
                else{
                    if (MAS.getProgress() == MAS.getMaxValue()){
                        distraction_value.setText("NIE");
                        System.out.println("/////////////////////// SETUJE NA NIE....");
                    }
                    else{
                        distraction_value.setText("KOKOTINA");
                        System.out.println("/////////////////////// SETUJE NA KOKOTINA....");
                    }
                }

            }
        });

        startService1();
        startService2();

        //new DoRead().execute(URL);
        return view;

    }

    public void onPause(){
        super.onPause();
        //mv.stopPlayback();
        mv.minimized = true;
    }

    public void onResume(){
        super.onResume();
        mv.minimized = false;
    }

    public void startService1(){
        Intent serviceIntent = new Intent(getActivity(), MinimizedActivityService.class);
        getActivity().startService(serviceIntent);

        bindService1();
    }

    public void bindService1(){
        Intent serviceIntent = new Intent(getActivity(), MinimizedActivityService.class);
        getActivity().bindService(serviceIntent, MAVM.getServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    public void startService2(){
        Intent serviceIntent = new Intent(getActivity(), VideoStreamService.class);
        getActivity().startService(serviceIntent);

        bindService2();
    }

    public void bindService2(){
        Intent serviceIntent = new Intent(getActivity(), VideoStreamService.class);
        getActivity().bindService(serviceIntent, VVFVM.getServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    private void toggleUpdates(){
        if(MAS != null){
            if(MAS.getProgress() == MAS.getMaxValue()){
                MAS.resetTask();
            }
            else{
                if(MAS.getIsPaused()){
                    MAS.unPause();
                    MAVM.setIsUpdating(true);
                }
                else{
                    MAS.pause();
                    MAVM.setIsUpdating(false);
                }
            }
        }
    }




    /*public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {

        protected MjpegInputStream doInBackground(String... Url) {
            //TODO: if camera has authentication deal with it and don't just not work

            System.out.println("//////////////////////////////////////////////////1. Sending http request");
            try {
                java.net.URL url = new URL(Url[0]); // here is your URL path
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(150 );
                conn.setConnectTimeout(150);
                int responseCode = conn.getResponseCode();

                System.out.println("//////////////////////////////////////////////////2. Request finished, status = " + responseCode);
                if (responseCode == 401) {
                    return null;
                }
                return new MjpegInputStream(conn.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("//////////////////////////////////////////////////Request failed-ClientProtocolException");
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            if (result != null) {
                System.out.println("//////////////////////////////////////////////////tu sa nieco stale deje");
                mv.setSource(result);
                mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
                mv.showFps(true);
            }
        }
    }*/
}
