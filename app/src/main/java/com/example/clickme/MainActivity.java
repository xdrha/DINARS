package com.example.clickme;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.android.volley.RequestQueue;


public class MainActivity extends AppCompatActivity {

    RequestQueue queue;
    public final static String URL_ROOT = "http://192.168.0.129:5000/";

    MinimizedActivityService MAS;
    MainActivityViewModel MAVM;
    private SectionsStatePagerAdapter secondStatePagerAdapter;
    private ViewPager mViewPager;


   /* @Override
    protected void onResume()
    {
        super.onResume();
        //overridePendingTransition(R.anim.slide_from_top,R.anim.slide_in_top);

        if(!OpenCVLoader.initDebug()){

        }
        else{

        }
    }*/

    /*public void startService(){
        Intent serviceIntent = new Intent(MainActivity.this, MinimizedActivityService.class);
        startService(serviceIntent);

        bindService();
    }

    public void bindService(){
        Intent serviceIntent = new Intent(MainActivity.this, MinimizedActivityService.class);
        bindService(serviceIntent, MAVM.getServiceConnection(), Context.BIND_AUTO_CREATE);
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
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        /*if(OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(), "yess zbehlo to", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(), "kurva kokot pes", Toast.LENGTH_LONG).show();
        }*/

        secondStatePagerAdapter = new SectionsStatePagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);
        setViewPager(0);

        /*VideoCapture capture = new VideoCapture("http://admin:ms1234@10.0.0.3:80/ipcam/mjpeg.cgi?dummy=param.mjpg");

        if (capture.isOpened()) {
            System.out.println("Video is captured");}
        else{
            System.out.println("kokotina");
        }*/



        /*final ProgressBar distraction_level_bar = findViewById(R.id.distraction_level_bar);
        final TextView distraction_label = findViewById(R.id.distraction_label);
        final TextView distraction_value = findViewById(R.id.distraction_value);

        final Button capture_button = findViewById(R.id.capture_button);
        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                queue = Volley.newRequestQueue(MainActivity.this);

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
                                Toast.makeText(MainActivity.this, "Server unavailable " + error.getMessage(), Toast.LENGTH_LONG).show();
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

        final MjpegView video_viewer = findViewById(R.id.video_screen);
        video_viewer.setMode(MjpegView.MODE_STRETCH);
        video_viewer.setAdjustHeight(true);

        final ImageButton break_button = findViewById(R.id.break_button);
        break_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                video_viewer.setUrl("http://admin:ms1234@10.0.0.3:80/ipcam/mjpeg.cgi");
                video_viewer.startStream();
            }
        });

        final ImageView stop_image = findViewById(R.id.stop_image);
        final ImageView warning_image = findViewById(R.id.warning_image);

        final ImageButton maximize_button = findViewById(R.id.maximize_button);
        maximize_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.moveTaskToBack(true);
                overridePendingTransition( R.anim.slide_in_up, R.anim.slide_out_up);
                MAS.resumeInterface();
            }
        });


        final Button stop = findViewById(R.id.statistics_button);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                toggleUpdates();

                System.out.println("distraction_label height" + distraction_label.getGravity());
                System.out.println("distraction_label width" + distraction_label.getTextSize());

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

                video_viewer.stopStream();
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
                                    MAVM.setIsUpdating(false);
                                }
                                MAS.distraction_level_bar.setProgress(MAS.getProgress());
                                MAS.distraction_level_bar.setMax(MAS.getMaxValue());
                                distraction_level_bar.setProgress(MAS.getProgress());
                                distraction_level_bar.setMax(MAS.getMaxValue());
                                handler.postDelayed(this, 100);
                            }

                        }
                        else{
                            handler.removeCallbacks(this);
                        }
                    }
                };

                if(aBoolean){
                    distraction_value.setText("HAHA");
                    handler.postDelayed(runnable, 100);
                }
                else{
                    if (MAS.getProgress() == MAS.getMaxValue()){
                        distraction_value.setText("NIE");
                    }
                    else{
                        distraction_value.setText("KOKOTINA");
                    }
                }

            }
        });

        //TODO create Service
        //Intent serviceIntent = new Intent(MainActivity.this, MinimizedActivityService.class);
        //bindService(serviceIntent, SConn,Context.BIND_AUTO_CREATE);

        startService();*/

    }

    private void setupViewPager(ViewPager viewPager){
        SectionsStatePagerAdapter adapter = new SectionsStatePagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new VideoViewFragment(), "fragment1") ;
        viewPager.setAdapter(adapter);
    }

    public void setViewPager(int i){
        mViewPager.setCurrentItem(i);
    }

}
