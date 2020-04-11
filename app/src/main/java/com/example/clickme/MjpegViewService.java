package com.example.clickme;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;

import java.io.IOException;

public class MjpegViewService extends Service {
    public SurfaceHolder mSurfaceHolder;
    private int frameCounter = 0;
    private long start;
    private Bitmap ovl;
    private IBinder mBinder = new MjpegViewService.MyBinder();
    private Handler mHandler;
    public MjpegInputStream mIn = null;
    private boolean showFps = false;
    public boolean mRun = false;
    private boolean surfaceDone = false;
    private Paint overlayPaint;
    private int overlayTextColor;
    private int overlayBackgroundColor;
    private int ovlPos;
    private int dispWidth;
    private int dispHeight;
    private int displayMode;
    public Boolean minimized = false;
    public SurfaceHolder holder;

    public void setDisplayMode(int s) {
        displayMode = s;
    }

    public void showFps(boolean b) {
        showFps = b;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    public class MyBinder extends Binder {
        MjpegViewService getService(){
            return MjpegViewService.this;
        }
    }

    /*public MjpegViewService(SurfaceHolder surfaceHolder, Context context) {
        mSurfaceHolder = surfaceHolder;
    }*/

    private Rect destRect(int bmw, int bmh) {
        int tempx;
        int tempy;
        if (displayMode == MjpegView.SIZE_STANDARD) {
            tempx = (dispWidth / 2) - (bmw / 2);
            tempy = (dispHeight / 2) - (bmh / 2);
            return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
        }
        if (displayMode == MjpegView.SIZE_BEST_FIT) {
            float bmasp = (float) bmw / (float) bmh;
            bmw = dispWidth;
            bmh = (int) (dispWidth / bmasp);
            if (bmh > dispHeight) {
                bmh = dispHeight;
                bmw = (int) (dispHeight * bmasp);
            }
            tempx = (dispWidth / 2) - (bmw / 2);
            tempy = (dispHeight / 2) - (bmh / 2);
            return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
        }
        if (displayMode == MjpegView.SIZE_FULLSCREEN){
            return new Rect(0, 0, dispWidth, dispHeight);
        }
        return null;
    }

    public void setSurfaceSize(int width, int height) {
        synchronized(mSurfaceHolder) {
            dispWidth = width;
            dispHeight = height;
        }
    }

    private Bitmap makeFpsOverlay(Paint p, String text) {
        Rect b = new Rect();
        p.getTextBounds(text, 0, text.length(), b);
        int bwidth  = b.width()+2;
        int bheight = b.height()+2;
        Bitmap bm = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        p.setColor(overlayBackgroundColor);
        c.drawRect(0, 0, bwidth, bheight, p);
        p.setColor(overlayTextColor);
        c.drawText(text, -b.left+1, (bheight/2)-((p.ascent()+p.descent())/2)+1, p);
        return bm;
    }

    public void startPretendLongRunningTask() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                surfaceDone = true;
                start = System.currentTimeMillis();
                PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
                Bitmap bm;
                int width;
                int height;
                Rect destRect;
                Canvas c = null;
                Paint p = new Paint();
                String fps;
                if (mRun) {
                    if (surfaceDone) {
                        try {
                            c = mSurfaceHolder.lockCanvas();
                            synchronized (mSurfaceHolder) {
                                try {

                                    bm = mIn.readMjpegFrame();
                                    if (!minimized && c != null) {
                                        destRect = destRect(bm.getWidth(), bm.getHeight());
                                        c.drawColor(Color.BLACK);
                                        c.drawBitmap(bm, null, destRect, p);
                                        if (showFps) {
                                            p.setXfermode(mode);
                                            if (ovl != null) {
                                                height = ((ovlPos & 1) == 1) ? destRect.top : destRect.bottom - ovl.getHeight();
                                                width = ((ovlPos & 8) == 8) ? destRect.left : destRect.right - ovl.getWidth();
                                                c.drawBitmap(ovl, width, height, null);
                                            }
                                            p.setXfermode(null);
                                            frameCounter++;
                                            if ((System.currentTimeMillis() - start) >= 1000) {
                                                fps = String.valueOf(frameCounter) + " fps";
                                                frameCounter = 0;
                                                start = System.currentTimeMillis();
                                                ovl = makeFpsOverlay(overlayPaint, fps);
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    e.getStackTrace();
                                    //Log.d(TAG, "catch IOException hit in run", e);
                                }
                            }
                        } finally {
                            if (c != null) {
                                mSurfaceHolder.unlockCanvasAndPost(c);
                            }
                        }
                    }
                    mHandler.postDelayed(this, 10);
                }
                else{
                    mHandler.removeCallbacks(this);
                }
            }
        };
        System.out.print("/////////////////////////////////////// POST DELAYED 100");
        mHandler.postDelayed(runnable, 10);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

}
