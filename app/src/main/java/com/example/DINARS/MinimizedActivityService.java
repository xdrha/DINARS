package com.example.DINARS;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
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
import android.widget.Toast;


public class MinimizedActivityService extends Service{

    private LinearLayout LL;
    private WindowManager wm;
    public TextView distraction_label;
    public ProgressBar distraction_level_bar;
    public TextView distraction_value;
    public ImageView warning_image;
    public ImageView stop_image;
    private ImageButton close_button;
    private WindowManager.LayoutParams params;

    private IBinder mBinder = new MinimizedActivityService.MyBinder();

    public Boolean hidden = false;
    private Boolean added = false;

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

        try {
            Thread.sleep(420);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        LL = new LinearLayout(this);
        LL.setOrientation(LinearLayout.HORIZONTAL);

        LL.setPadding(5,5,5,5);
        LL.setBackgroundColor(0xaf000000);
        LL.setGravity(Gravity.CENTER);
        LL.setAlpha(1f);

        distraction_label = new TextView(this);
        distraction_label.setLayoutParams(new LinearLayout.LayoutParams(178, 41));
        distraction_label.setText("Distraction level");
        distraction_label.setGravity(Gravity.CENTER);
        distraction_label.setTextSize(18);

        distraction_level_bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        distraction_level_bar.setMax(10);
        distraction_level_bar.setScaleY(2);
        distraction_level_bar.setProgress(4);
        distraction_level_bar.setLayoutParams(new LinearLayout.LayoutParams(454,41));

        distraction_value = new TextView(this);
        distraction_value.setLayoutParams(new LinearLayout.LayoutParams(93, 41));
        distraction_value.setText("  0.0");
        distraction_value.setGravity(Gravity.CENTER);
        distraction_value.setTextSize(25);

        warning_image = new ImageView(this);
        warning_image.setLayoutParams(new LinearLayout.LayoutParams(93, 41));
        warning_image.setImageResource(R.drawable.warning);

        stop_image = new ImageView(this);
        stop_image.setLayoutParams(new LinearLayout.LayoutParams(94, 41));
        stop_image.setImageResource(R.drawable.stop);

        close_button = new ImageButton(this);
        close_button.setLayoutParams(new LinearLayout.LayoutParams(102, 41));
        close_button.setBackgroundColor(Color.TRANSPARENT);
        close_button.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);

        LL.addView(distraction_label);
        LL.addView(distraction_level_bar);
        LL.addView(distraction_value);
        LL.addView(warning_image);
        LL.addView(stop_image);
        LL.addView(close_button);

        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSPARENT);

        params.gravity = Gravity.TOP;
        params.x = 0;
        params.y = 0;
        params.width = 1024;
        params.height = 51;

        close_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Application is still running on background.", Toast.LENGTH_SHORT).show();
                hideTopPanel();
            }
        });

    }

    public void hideTopPanel(){
        hidden = true;
        if(added) {
            wm.removeView(LL);
            added = false;
        }
    }

    @Override
    public void onDestroy() {
        if(added) {
            wm.removeView(LL);
            added = false;
        }
        super.onDestroy();
    }

    public void resumeInterface(){
        hidden = false;
        if(!added) {
            wm.addView(LL, params);
            added = true;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent){
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }


}
