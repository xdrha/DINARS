package com.example.clickme;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
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

import java.net.HttpURLConnection;
import java.net.URL;

public class VideoViewFragment extends Fragment {

    private static final String URL = "http://admin:ms1234@10.0.0.3:80/ipcam/mjpeg.cgi";
    private RequestQueue queue;
    public final static String URL_ROOT = "http://192.168.0.129:5000/";

    private MinimizedActivityService MAS;
    private MainActivityViewModel MAVM;
    private MjpegView mv;

    private VideoViewFragmentViewModel VVFVM;
    private MjpegViewService MVS;
    private MjpegInputStream result;

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
        final ImageButton minimize_button = view.findViewById(R.id.minimize_button);
        final ImageView stop_image = view.findViewById(R.id.stop_image);
        final ImageView warning_image = view.findViewById(R.id.warning_image);
        final LinearLayout lll = view.findViewById(R.id.lll);

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
                if(MVS.isPaused) {
                    MVS.isPaused = false;
                }
                else{
                    MVS.isPaused = true;
                }
            }
        });


        minimize_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mv.minimized = true;
                MAVM.setIsUpdating(true);
                getActivity().moveTaskToBack(false);
                getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
                MAS.resumeInterface();
            }
        });

        statistics_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                System.out.println("mv height " + mv.getHeight());
                System.out.println("mv width " + mv.getWidth());

                System.out.println("distraction_label height " + distraction_label.getHeight());
                System.out.println("distraction_label textsize " + distraction_label.getWidth());

                System.out.println("distraction_level_bar height " + distraction_level_bar.getHeight());
                System.out.println("distraction_level_bar width " + distraction_level_bar.getWidth());

                System.out.println("distraction_value height " + distraction_value.getHeight());
                System.out.println("distraction_value width " + distraction_value.getWidth());

                System.out.println("warning_image height " + warning_image.getHeight());
                System.out.println("warning_image width " + warning_image.getWidth());

                System.out.println("stop_image height " + stop_image.getHeight());
                System.out.println("stop_image width " + stop_image.getWidth());

                System.out.println("maximize height " + minimize_button.getHeight());
                System.out.println("maximize width " + minimize_button.getWidth());
            }
        });

        VVFVM = ViewModelProviders.of(this).get(VideoViewFragmentViewModel.class);

        VVFVM.getBinder().observe(this, new Observer<MjpegViewService.MyBinder>() {
            @Override
            public void onChanged(@Nullable MjpegViewService.MyBinder myBinder) {
                if(myBinder != null){
                    MVS = myBinder.getService();
                    System.out.println("////////////////// NIE JE NULL DOPICEE");
                    new DoRead().execute(URL);
                }
                else{
                    MVS = null;
                    System.out.println("////////////////// JE NULL DOPICEE");
                }
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

                            System.out.println("////////////////////// CHECK");
                            if(MAS.maximize){
                                //vypni service, zapni main
                                System.out.println("////////////////////// TOTO  SA VYKONA");
                                MAS.maximize = false;
                                handler.removeCallbacks(this);
                                moveMainActivityToFront();
                            }
                            else{
                                handler.postDelayed(this, 200);
                            }
                        }
                    }
                };
                handler.postDelayed(runnable, 200);
            }
        });

        startService1();
        startService2();

        return view;

    }

    public void moveMainActivityToFront(){
        Toast.makeText(getContext(), "MAKING MOVING MAIN ACTIVITY TO FRONT", Toast.LENGTH_SHORT).show();
        Intent mainActivity = new Intent(getActivity(), MainActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(mainActivity);
    }

    public void onPause(){
        super.onPause();
        //mv.stopPlayback();
        System.out.println("////////////////////////////////// PAUSOL SOM TO");
        MVS.isMinimized = true;
    }

    public void onResume(){
        super.onResume();
        if(MVS != null) {
            MVS.isMinimized = false;
        }
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
        Intent serviceIntent = new Intent(getActivity(), MjpegViewService.class);
        getActivity().startService(serviceIntent);

        bindService2();
    }

    public void bindService2(){
        Intent serviceIntent = new Intent(getActivity(), MjpegViewService.class);
        getActivity().bindService(serviceIntent, VVFVM.getServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {

        protected MjpegInputStream doInBackground(String... Url) {
            //TODO: if camera has authentication deal with it and don't just not work

            System.out.println("////////////////////////////////////////////////// 1. Sending http request");
            try {
                java.net.URL url = new URL(Url[0]); // here is your URL path
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(150 );
                conn.setConnectTimeout(150);
                int responseCode = conn.getResponseCode();

                System.out.println("////////////////////////////////////////////////// 2. Request finished, status = " + responseCode);
                if (responseCode == 401) {
                    return null;
                }
                return new MjpegInputStream(conn.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("////////////////////////////////////////////////// Request failed-ClientProtocolException");
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream res) {


            if (res != null) {
                System.out.println("////////////////////////////////////////////////// OKEY HOTOVKA");
                mv.mIn = res;
                result = res;
                VVFVM.setIsUpdating(true);

                if(mv.mIn != null) {
                    MVS.mIn = result;
                    mv.mRun = true;
                    MVS.mRun = true;
                    System.out.println("////////////////////////////////////////////////// Input stream nie je null");
                    MVS.mSurfaceHolder = mv.holder;
                    MVS.setSurfaceSize(803, 476);
                    MVS.startPretendLongRunningTask();

                    System.out.println("////////////////////////////////////////////////// zacak konat");
                    MVS.setDisplayMode(MjpegView.SIZE_BEST_FIT);

                    MVS.showFps(true);

                }


            }

        }
    }
}
