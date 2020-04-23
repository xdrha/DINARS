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

import com.android.volley.RequestQueue;

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
    private Boolean accidentallyMinimized = true;
    private Intent serviceIntent1;
    private Intent serviceIntent2;
    private Boolean stopped = false;

    public ProgressBar distraction_level_bar;
    public TextView distraction_label;
    public TextView distraction_value;
    public Button capture_button;
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
        capture_button = view.findViewById(R.id.capture_button);
        statistics_button = view.findViewById(R.id.statistics_button);
        minimize_button = view.findViewById(R.id.minimize_button);
        break_button = view.findViewById(R.id.break_button);
        stop_image = view.findViewById(R.id.stop_image);
        warning_image = view.findViewById(R.id.warning_image);
        lll = view.findViewById(R.id.lll);

        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*queue = Volley.newRequestQueue(getActivity());

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

                queue.add(checkAvailabilityRequest);*/
               if(MVS != null){
                   if(MVS.calibrationMode == 0) MVS.calibrationMode = 1;
                   else
                       if(MVS.calibrationMode == 1) MVS.calibrationMode = 2;
               }

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
                accidentallyMinimized = false;
                minimize();
                getActivity().moveTaskToBack(false);
                getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
            }
        });

        statistics_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                System.out.println("mv height " + mv.getHeight());
                System.out.println("mv width " + mv.getWidth());

                System.out.println("lll height " + lll.getHeight());
                System.out.println("lll width " + lll.getWidth());

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
                                setProgress(MVS.globalDistraction);
                                handler.postDelayed(this, 100);
                            }
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

        return view;
    }

    public void setProgress(int distraction){
        distraction_level_bar.setMax(100);
        distraction_level_bar.setProgress(distraction);
        distraction_value.setText(String.valueOf(distraction / 10.0));
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
        System.out.println("////////////////////////// VVF DESTROY DONE");
        MVS.mRun = false;
        getActivity().unbindService(VVFVM.getServiceConnection());
        getActivity().unbindService(MAVM.getServiceConnection());
        getActivity().finishAffinity();
        System.exit(0);
        super.onDestroy();
    }

    @Override
    public void onStop() {
        getActivity().moveTaskToBack(false);
        getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
        System.out.println("////////////////////////// VVF STOP DONE");
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
