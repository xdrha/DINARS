package com.example.clickme;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

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

    //TODO OpenCV variables
    Mat mat = new Mat();
    Mat descriptors2,descriptors1;
    FeatureDetector detector;
    DescriptorExtractor descriptor;
    MatOfKeyPoint keypoints1,keypoints2;
    DescriptorMatcher matcher;
    Mat img1;
    Scalar RED = new Scalar(255, 0, 0);
    Scalar GREEN = new Scalar(0, 255, 0);

    public void initializeOpenCVDependencies() throws IOException{
        detector = FeatureDetector.create(FeatureDetector.ORB);
        descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        img1 = new Mat();
        AssetManager assetManager = getAssets();
        InputStream istr = assetManager.open("hand.jpeg");
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        Utils.bitmapToMat(bitmap, img1);
        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGB2GRAY);
        img1.convertTo(img1, 0); //converting the image to match with the type of the cameras image
        descriptors1 = new Mat();
        keypoints1 = new MatOfKeyPoint();
        detector.detect(img1, keypoints1);
        descriptor.compute(img1, keypoints1, descriptors1);
        System.out.println("//////////////////////////////////////// PODARILO SA INICIALIZOVAT");
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

    public Mat recognize(Mat aInputFrame) {

        Imgproc.cvtColor(aInputFrame, aInputFrame, Imgproc.COLOR_RGB2GRAY);
        descriptors2 = new Mat();
        keypoints2 = new MatOfKeyPoint();
        detector.detect(aInputFrame, keypoints2);
        descriptor.compute(aInputFrame, keypoints2, descriptors2);

        // Matching
        MatOfDMatch matches = new MatOfDMatch();
        if (img1.type() == aInputFrame.type()) {
            matcher.match(descriptors1, descriptors2, matches);
        } else {
            System.out.println("////////////////// NEZHODLI SA TYPY :((");
            return aInputFrame;
        }
        List<DMatch> matchesList = matches.toList();

        Double max_dist = 0.0;
        Double min_dist = 100.0;

        for (int i = 0; i < matchesList.size(); i++) {
            Double dist = (double) matchesList.get(i).distance;
            if (dist < min_dist)
                min_dist = dist;
            if (dist > max_dist)
                max_dist = dist;
        }

        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (int i = 0; i < matchesList.size(); i++) {
            if (matchesList.get(i).distance <= (1.5 * min_dist)) {
                good_matches.addLast(matchesList.get(i));
            }
        }

        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(good_matches);
        Mat outputImg = new Mat();
        MatOfByte drawnMatches = new MatOfByte();
        if (aInputFrame.empty() || aInputFrame.cols() < 1 || aInputFrame.rows() < 1) {
            return aInputFrame;
        }
        Features2d.drawMatches(img1, keypoints1, aInputFrame, keypoints2, goodMatches, outputImg, GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
        Imgproc.resize(outputImg, outputImg, aInputFrame.size());

        return outputImg;
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
                                    //
                                    //TODO: tu sa bude diat rozpoznavanie veci atd.
                                    //
                                    Utils.bitmapToMat(bm,mat);
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
