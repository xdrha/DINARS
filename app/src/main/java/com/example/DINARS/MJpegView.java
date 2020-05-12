package com.example.DINARS;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MJpegView extends SurfaceView implements SurfaceHolder.Callback {

    public final static int SIZE_STANDARD   = 1;
    public final static int SIZE_BEST_FIT   = 4;
    public final static int SIZE_FULLSCREEN = 8;

    public Boolean minimized = false;
    public SurfaceHolder holder;

    public void init() {
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
    }

    public MJpegView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
        //nikdy sa nezmeni
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public MJpegView(Context context) {
        super(context);
        init();
    }

    public void surfaceCreated(SurfaceHolder holder) {

    }

}