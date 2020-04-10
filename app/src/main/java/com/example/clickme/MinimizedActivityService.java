package com.example.clickme;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


public class MinimizedActivityService extends Service{

    public LinearLayout LL;
    public WindowManager wm;
    public TextView distraction_label;
    public ProgressBar distraction_level_bar;
    public TextView distraction_value;
    public ImageView warning_image;
    public ImageView stop_image;
    public ImageButton maximize_button;
    private WindowManager.LayoutParams params;

    private IBinder mBinder = new MinimizedActivityService.MyBinder();
    private Handler mHandler;
    private int mProgress, mMaxValue;
    private boolean mIsPaused;

    public class MyBinder extends Binder {
        MinimizedActivityService getService(){
            return MinimizedActivityService.this;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mProgress = 0;
        mIsPaused = true;
        mMaxValue = 5000;

        try {
            Thread.sleep(420);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        LL = new LinearLayout(this);
        LL.setOrientation(LinearLayout.HORIZONTAL);
        LL.setPadding(10,10,10,10);
        LL.setBackgroundColor(0xbf222222);
        LL.setGravity(Gravity.CENTER);
        LL.setAlpha(1f);

        distraction_label = new TextView(this);
        distraction_label.setLayoutParams(new LinearLayout.LayoutParams(215, 47));
        distraction_label.setText("Distraction level");
        distraction_label.setGravity(Gravity.CENTER);
        distraction_label.setTextSize(18);

        distraction_level_bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        distraction_level_bar.setMax(10);
        distraction_level_bar.setScaleY(2);
        distraction_level_bar.setProgress(4);
        distraction_level_bar.setLayoutParams(new LinearLayout.LayoutParams(666,47));

        distraction_value = new TextView(this);
        distraction_value.setLayoutParams(new LinearLayout.LayoutParams(101, 47));
        distraction_value.setText("  0.0");
        distraction_value.setGravity(Gravity.CENTER);
        distraction_value.setTextSize(30);

        warning_image = new ImageView(this);
        warning_image.setLayoutParams(new LinearLayout.LayoutParams(89, 47));
        warning_image.setImageResource(R.drawable.warning);

        stop_image = new ImageView(this);
        stop_image.setLayoutParams(new LinearLayout.LayoutParams(90, 47));
        stop_image.setImageResource(R.drawable.stop);

        maximize_button = new ImageButton(this);
        maximize_button.setLayoutParams(new LinearLayout.LayoutParams(93, 47));
        maximize_button.setImageResource(android.R.drawable.arrow_down_float);

        LL.addView(distraction_label);
        LL.addView(distraction_level_bar);
        LL.addView(distraction_value);
        LL.addView(warning_image);
        LL.addView(stop_image);
        LL.addView(maximize_button);

        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSPARENT);

        params.gravity = Gravity.TOP;
        params.x = 0;
        params.y = 0;
        params.width = 1280;
        params.height = 73;

        maximize_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent main_activity = new Intent(MinimizedActivityService.this, MainActivity.class);
                main_activity.setAction(Intent.ACTION_MAIN);
                main_activity.addCategory(Intent.CATEGORY_LAUNCHER);
                main_activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(main_activity);
                onDestroy();
            }
        });

    }

    @Override
    public void onDestroy() {
        wm.removeView(LL);
        super.onDestroy();
    }

    public void resumeInterface(){

        wm.addView(LL, params);
    }

    public void startPretendLongRunningTask(){
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(mProgress >= mMaxValue || mIsPaused){
                    mHandler.removeCallbacks(this);
                    pause();
                }
                else{
                    mProgress += 100;
                    mHandler.postDelayed(this, 100);
                }
            }
        };
        mHandler.postDelayed(runnable, 100);
    }

    public void pause(){
        mIsPaused = true;
    }

    public void unPause(){
        mIsPaused = false;
        startPretendLongRunningTask();
    }
    public Boolean getIsPaused(){
        return mIsPaused;
    }
    public int getProgress(){
        return mProgress;
    }
    public int getMaxValue(){
        return mMaxValue;
    }
    public void resetTask(){
        mProgress = 0;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent){
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }


}
