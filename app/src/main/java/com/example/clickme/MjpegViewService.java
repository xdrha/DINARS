package com.example.clickme;

import android.app.Service;
import android.content.Context;
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

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
    public Boolean isMinimized = false;
    public Boolean isPaused = false;
    public Dnn dnn;



    //TODO OpenCV variables
    Mat mat = new Mat();

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;

    public void initializeOpenCVDependencies() throws IOException{

        try {
            // load cascade file from application resources
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (mJavaDetector.empty()) {
                System.out.println("Failed to load cascade classifier");
                mJavaDetector = null;
            } else
                System.out.println("Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to load cascade. Exception thrown: " + e);
        }
        System.out.println("//////////////////////////////////////// PODARILO SA INICIALIZOVAT");

        mGray = new Mat();
        mRgba = new Mat();
    }


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
        try {
            initializeOpenCVDependencies();
        } catch (IOException e) {
            System.out.println("//////////////////////////////////////// NEPODARILO SA INICIALIZOVAT");
            e.printStackTrace();
        }
    }

    public class MyBinder extends Binder {
        MjpegViewService getService(){
            return MjpegViewService.this;
        }
    }

    private Rect destRect(int bmw, int bmh) {

        /*bmw = 1280 / bmh = 720*/

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


    public Mat recognize(Mat inputFrame) {

        mRgba = inputFrame;
        Imgproc.cvtColor(inputFrame, mGray, Imgproc.COLOR_RGB2GRAY);
        //mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            //mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        /*else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);
        }*/
        else {
            System.out.println("Detection method is not selected!");
        }

        org.opencv.core.Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        return mRgba;
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
                                    if(bm != null) {
                                        //
                                        //TODO: tu sa bude diat rozpoznavanie veci atd.
                                        //
                                        Utils.bitmapToMat(bm, mat);
                                        mat = recognize(mat);
                                        Utils.matToBitmap(mat, bm);

                                        if (!isMinimized && c != null) {
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
                                    }
                                } catch (Exception e) {
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
        mHandler.postDelayed(runnable, 10);
    }

    public void stop(){
        stopSelf();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

}
