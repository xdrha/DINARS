package com.example.clickme;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MjpegViewService extends Service {
    public SurfaceHolder mSurfaceHolder;
    private IBinder mBinder = new MjpegViewService.MyBinder();
    private Handler mHandler;
    public MjpegInputStream mIn = null;
    public boolean mRun = false;
    private boolean surfaceDone = false;
    private int dispWidth;
    private int dispHeight;
    private int displayMode;
    public Boolean isMinimized = false;
    public Boolean isPaused = false;
    public CountDownTimer eyesClosedTimer = null;

    //TODO calibration variables
    public int calibrationMode = 0;
    public Boolean faceFound = false;
    public Boolean eyesFound = false;
    public Boolean wearingGlasses = false;
    public int progressRectangle = 0;


    //TODO OpenCV variables

    private Mat mRgba;
    private Mat mGray;
    private Mat mat;

    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private static final Scalar OBJECT_RECT_COLOR = new Scalar(255, 0, 0, 255);
    private static final Scalar EYE_RECT_COLOR = new Scalar(0, 0, 255, 255);

    private File phoneCascadeFile;
    private File faceCascadeFile;
    private File eyeCascadeFile;
    private File coffeeCascadeFile;
    private CascadeClassifier coffeeDetector;
    private CascadeClassifier phoneDetector;
    private CascadeClassifier faceDetector;
    private CascadeClassifier eyeDetector;

    //TODO distraction meter variables
    public int globalDistraction = 0;
    public int phoneDistraction = 0;
    public int coffeeDistraction = 0;
    public Boolean phoneUsage = false;
    public double tick = 1;
    public int decisionMatrix[][] = {{0,0},{0,0},{0,0}};

    private CascadeClassifier newCascadeClassifier(File mCascadeFile, InputStream is) throws IOException{
        FileOutputStream os = new FileOutputStream(mCascadeFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();

        return new CascadeClassifier(mCascadeFile.getAbsolutePath());
    }

    public void initializeOpenCVDependencies(){

        try {
            // load cascade file from application resources

            InputStream isPhone = getResources().openRawResource(R.raw.haarcascade_phone);
            InputStream isCoffee = getResources().openRawResource(R.raw.haarcascade_coffee);
            InputStream isFace = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            InputStream isEye = getResources().openRawResource(R.raw.haarcascade_eye);

            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            phoneCascadeFile = new File(cascadeDir, "haarcascade_phone.xml");
            coffeeCascadeFile = new File(cascadeDir, "haarcascade_coffee.xml");
            faceCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            eyeCascadeFile = new File(cascadeDir, "haarcascade_eye.xml");

            if ((coffeeDetector = newCascadeClassifier(coffeeCascadeFile, isCoffee)).empty()) {
                System.out.println("///////////////////////////// Failed to load coffee cascade classifier");
                coffeeDetector = null;
            }


            if ((phoneDetector = newCascadeClassifier(phoneCascadeFile, isPhone)).empty()) {
                System.out.println("///////////////////////////// Failed to load phone cascade classifier");
                phoneDetector = null;
            }

            if ((faceDetector = newCascadeClassifier(faceCascadeFile, isFace)).empty()) {
                System.out.println("///////////////////////////// Failed to load face cascade classifier");
                faceDetector = null;
            }

            if ((eyeDetector = newCascadeClassifier(eyeCascadeFile, isEye)).empty()) {
                System.out.println("///////////////////////////// Failed to load eye cascade classifier");
                eyeDetector = null;
            }

            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("//////////////////////////////// Failed to load cascade. Exception thrown: " + e);
        }

        mGray = new Mat();
        mRgba = new Mat();
    }

    public void setDisplayMode(int s) {
        displayMode = s;
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
        initializeOpenCVDependencies();
    }

    public void reStartEyesClosedTimer(){
        if(eyesClosedTimer != null) {
            eyesClosedTimer.cancel();
        }
        globalDistraction /= tick;
        tick = 1;
        //ak po 2 sekundach najde tvar ale nenajde aspon jedno oko, tak ma asi zavrete oci, alebo cumi do mobilu
        eyesClosedTimer = new CountDownTimer(2000, 100) {

            @Override
            public void onTick(long l) {
                globalDistraction = (int)((phoneDistraction + coffeeDistraction) * tick);
                tick += 0.1;
                if(globalDistraction >= 100){
                    globalDistraction = 100;
                    cancel();
                }

            }

            @Override
            public void onFinish() {
                //vodic zaspal
                System.err.println("\n//////////////// VODIC ZASPAL\n");
            }
        }.start();
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

    public int makeDecision(){
        int countPhones = 0;
        int countCoffees = 0;

        for(int i = 0; i < 3; i++){
            countPhones += decisionMatrix[i][0];
            countCoffees += decisionMatrix[i][1];
        }

        if(countPhones == 0 && countCoffees == 0) return 0;
        if(countCoffees > countPhones) return 2;

        return 1;
    }

    public void drawCalibrationMode(){
        Mat roi = mRgba.clone();
        double opacity = 0.7;
        Imgproc.rectangle(roi, new Point(0,0), new Point(420, roi.height()), new Scalar(0,0,0),-1);
        Imgproc.rectangle(roi, new Point(860,0), new Point(roi.width(), roi.height()), new Scalar(0,0,0),-1);
        Imgproc.rectangle(roi, new Point(420,0), new Point(860, 30), new Scalar(0,0,0),-1);
        Imgproc.rectangle(roi, new Point(420,400), new Point(860, roi.height()), new Scalar(0,0,0),-1);
        Core.addWeighted(roi,opacity, mRgba, 1-opacity,0, mRgba);

        Imgproc.rectangle(mRgba, new Point(424,34), new Point(856, 396), new Scalar(255,255,0,255),5);
    }

    public void findFaceAndEyes(){

        MatOfRect faces = new MatOfRect();
        MatOfRect eyes = new MatOfRect();

        if (faceDetector != null)
            faceDetector.detectMultiScale(mGray.submat(new org.opencv.core.Rect(420, 30, 440, 370)), faces, 1.1, 2, 0, // TODO: objdetect.CV_HAAR_SCALE_IMAGE (1.1, 2 face cascade)
                    new Size(100, 100), new Size(900, 900));

        org.opencv.core.Rect[] facesArray;

        if (!faces.empty()) {

            facesArray = faces.toArray();
            if (facesArray.length != 0) {
                facesArray[0].x += 420;
                facesArray[0].y += 30;
                Imgproc.rectangle(mRgba, new Point(facesArray[0].x, facesArray[0].y),
                        new Point(facesArray[0].x + facesArray[0].width, facesArray[0].y + facesArray[0].height),
                        new Scalar(255,255,0), 3);

                if (!faceFound) faceFound = true;
            }

            Mat faceMat = mGray.submat(facesArray[0]);
            if (eyeDetector != null)
                eyeDetector.detectMultiScale(faceMat, eyes, 1.5, 10, 0, // TODO: objdetect.CV_HAAR_SCALE_IMAGE (1.5, 10 phone cascade)
                        new Size(10, 10), new Size(100, 100));

            if (!eyes.empty()) {

                org.opencv.core.Rect[] eyesArray = eyes.toArray();

                if (eyesArray.length != 0) {
                    for (int i = 0; i < eyesArray.length; i++) {

                        eyesArray[i].x += facesArray[0].x;
                        eyesArray[i].y += facesArray[0].y;
                        Boolean temp = false;
                        if (i > 0) {
                            for (int j = 0; j < i; j++) {
                                if (eyesArray[i].x <= eyesArray[j].x + eyesArray[j].width && eyesArray[j].x <= eyesArray[i].x + eyesArray[i].width)
                                    temp = true;
                                if (eyesArray[j].x <= eyesArray[i].x + eyesArray[i].width && eyesArray[i].x <= eyesArray[j].x + eyesArray[j].width)
                                    temp = true;
                            }
                            if (temp) continue;
                        }
                        Imgproc.rectangle(mRgba, eyesArray[i].tl(), eyesArray[i].br(), new Scalar(255,255,0), 3);
                        if (!eyesFound) eyesFound = true;
                    }
                }
            }
        }
    }

    public Mat recognize(Mat inputFrame) {

        mRgba = inputFrame;
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY);

        //TODO calibration mode

        if (calibrationMode == 1) {
            faceFound = false;
            eyesFound = false;
            wearingGlasses = false;

            drawCalibrationMode();

            Imgproc.putText(mRgba, "1. Sit down normally", new Point(10, 50), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255), 3);
            Imgproc.putText(mRgba, "2. Move the camera", new Point(10, 150), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255), 3);
            Imgproc.putText(mRgba, "  ( your head should", new Point(10, 200), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255), 3);
            Imgproc.putText(mRgba, "  be in the middle of", new Point(10, 250), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255), 3);
            Imgproc.putText(mRgba, "  the yellow frame )", new Point(10, 300), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255), 3);
            Imgproc.putText(mRgba, "3. Press CALIBRATION", new Point(10, 400), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255), 3);
            Imgproc.putText(mRgba, "   button again", new Point(10, 450), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255), 3);
        }
        else {
            if (calibrationMode == 2 || calibrationMode == 3) {
                 drawCalibrationMode();
                Imgproc.putText(mRgba, "Calibration progress", new Point(420, 450), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255), 2);
                Imgproc.rectangle(mRgba, new Point(420, 460), new Point(860,480), new Scalar(200,200,200), -1);

                if (calibrationMode != 3) {
                    progressRectangle = 0;
                    calibrationMode = 3;
                }
                if (calibrationMode == 3) {

                    if (progressRectangle >= 440) { ////skoncil cas na kalibraciu

                        isPaused = true;
                        if (faceFound) {
                            //ma okuliare???

                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage("Do you wear glasses?").setTitle("Eyes not found!");

                            builder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User clicked OK button
                                    wearingGlasses = true;
                                    isPaused = false;
                                    calibrationMode = 0;
                                }
                            });
                            builder.setNegativeButton("no (restart calibration)", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    calibrationMode = 1;
                                    isPaused = false;
                                    progressRectangle = 0;
                                    Imgproc.rectangle(mRgba, new Point(420, 460), new Point(420 + progressRectangle, 480), new Scalar(255, 255, 0), -1);
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            dialog.show();
                        } else {

                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage("Calibration not successful!");

                            builder.setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    calibrationMode = 1;
                                    isPaused = false;
                                    progressRectangle = 0;
                                    Imgproc.rectangle(mRgba, new Point(420, 460), new Point(420 + progressRectangle, 480), new Scalar(255, 255, 0), -1);
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            dialog.show();
                        }
                    } else {

                        progressRectangle += 20;

                        findFaceAndEyes();

                        if (faceFound) {
                            Imgproc.putText(mRgba, ">> Face found", new Point(880, 50), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 0), 2);

                            if (eyesFound) {
                                Imgproc.putText(mRgba, ">> Eyes found", new Point(880, 100), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 0), 2);
                                progressRectangle = 440;
                                calibrationMode = 0;
                                isPaused = true;
                                Imgproc.rectangle(mRgba, new Point(420, 460), new Point(420 + progressRectangle, 480), new Scalar(255, 255, 0), -1);

                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setMessage("Calibration done!");

                                builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        calibrationMode = 0;
                                        isPaused = false;
                                    }
                                });

                                AlertDialog dialog = builder.create();
                                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                dialog.show();
                            }
                        }

                        Imgproc.rectangle(mRgba, new Point(420, 460), new Point(420 + progressRectangle, 480), new Scalar(255, 255, 0), -1);
                        return mRgba;
                    }
                }

            } else {
                if (calibrationMode == 0) {

                    MatOfRect phones = new MatOfRect();
                    MatOfRect faces = new MatOfRect();
                    MatOfRect eyes = new MatOfRect();
                    MatOfRect coffees = new MatOfRect();

                    //region face detection region

                    if (faceDetector != null)
                        faceDetector.detectMultiScale(mGray.submat(new org.opencv.core.Rect(420, 30, 440, 370)), faces, 1.1, 2, 0, // TODO: objdetect.CV_HAAR_SCALE_IMAGE (1.1, 2 face cascade)
                                new Size(100, 100), new Size(900, 900));

                    org.opencv.core.Rect[] facesArray;

                    if (!faces.empty()) {

                        facesArray = faces.toArray();
                        if (facesArray.length != 0) {
                            facesArray[0].x += 420;
                            facesArray[0].y += 30;
                            Imgproc.rectangle(mRgba, new Point(facesArray[0].x, facesArray[0].y),
                                    new Point(facesArray[0].x + facesArray[0].width, facesArray[0].y + facesArray[0].height),
                                    FACE_RECT_COLOR, 3);
                        }

                        Mat faceMat = mGray.submat(facesArray[0]);
                        if (eyeDetector != null)
                            eyeDetector.detectMultiScale(faceMat, eyes, 1.5, 10, 0, // TODO: objdetect.CV_HAAR_SCALE_IMAGE (1.5, 10 phone cascade)
                                    new Size(10, 10), new Size(100, 100));

                        if (!eyes.empty()) {

                            reStartEyesClosedTimer();

                            org.opencv.core.Rect[] eyesArray = eyes.toArray();

                            if (eyesArray.length != 0) {
                                for (int i = 0; i < eyesArray.length; i++) {

                                    eyesArray[i].x += facesArray[0].x;
                                    eyesArray[i].y += facesArray[0].y;
                                    Boolean temp = false;
                                    if (i > 0) {
                                        for (int j = 0; j < i; j++) {
                                            if (eyesArray[i].x <= eyesArray[j].x + eyesArray[j].width && eyesArray[j].x <= eyesArray[i].x + eyesArray[i].width)
                                                temp = true;
                                            if (eyesArray[j].x <= eyesArray[i].x + eyesArray[i].width && eyesArray[i].x <= eyesArray[j].x + eyesArray[j].width)
                                                temp = true;
                                        }
                                        if (temp) continue;
                                    }
                                    Imgproc.rectangle(mRgba, eyesArray[i].tl(), eyesArray[i].br(), EYE_RECT_COLOR, 3);
                                }
                            }
                        }
                    } else {
                        reStartEyesClosedTimer(); // ked nenajde hlavu tak nemozem povedat ze zaspal
                    }

                    //endregion

                    //region object detection region

                    if (phoneDetector != null)
                        phoneDetector.detectMultiScale(mGray, phones, 2, 15, 0, // TODO: objdetect.CV_HAAR_SCALE_IMAGE (2, 15 phone cascade)
                                new Size(100, 100), new Size(500, 500));

                    if (coffeeDetector != null)
                        coffeeDetector.detectMultiScale(mGray, coffees, 2, 15, 0, // TODO: objdetect.CV_HAAR_SCALE_IMAGE (2, 15 coffee cascade)
                                new Size(100, 100), new Size(500, 500));


                    if (!phones.empty()) decisionMatrix[2][0] = 1;
                    else decisionMatrix[2][0] = 0;

                    if (!coffees.empty()) decisionMatrix[2][1] = 1;
                    else decisionMatrix[2][1] = 0;


                    int decision = makeDecision();

                    if (decision == 1) {
                        coffeeDistraction = 0;
                        phoneDistraction = 50;

                        org.opencv.core.Rect[] phonesArray = phones.toArray();
                        Imgproc.rectangle(mRgba, phonesArray[0].tl(), phonesArray[0].br(), OBJECT_RECT_COLOR, 3);
                        Imgproc.putText(mRgba, "phone", new Point(phonesArray[0].x, phonesArray[0].y - 3), Core.FONT_HERSHEY_SIMPLEX, 1, OBJECT_RECT_COLOR, 2);
                    } else {
                        if (decision == 2) {
                            phoneDistraction = 0;
                            coffeeDistraction = 50;

                            org.opencv.core.Rect[] coffeesArray = coffees.toArray();
                            Imgproc.rectangle(mRgba, coffeesArray[0].tl(), coffeesArray[0].br(), OBJECT_RECT_COLOR, 3);
                            Imgproc.putText(mRgba, "coffee", new Point(coffeesArray[0].x, coffeesArray[0].y - 3), Core.FONT_HERSHEY_SIMPLEX, 1, OBJECT_RECT_COLOR, 2);
                        } else {
                            if (phoneDistraction > 0) phoneDistraction -= 5;
                            if (coffeeDistraction > 0) coffeeDistraction -= 5;
                        }
                    }

                    //endregion
                    return mRgba;
                }
            }
        }

        return mRgba;

    }


    public void startPretendLongRunningTask() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                surfaceDone = true;
                Bitmap bm;
                Rect destRect;
                Canvas c = null;
                Paint p = new Paint();
                if (mRun) {
                    if (surfaceDone ) {
                        try {

                            synchronized (mSurfaceHolder) {
                                try {

                                    bm = mIn.readMjpegFrame();
                                    if(bm != null && !isPaused) {
                                        c = mSurfaceHolder.lockCanvas();
                                        //
                                        //TODO: tu sa bude diat rozpoznavanie veci atd.
                                        //
                                        mat = new Mat();
                                        Utils.bitmapToMat(bm, mat);
                                        mat = recognize(mat);
                                        Utils.matToBitmap(mat, bm);

                                        if (!isMinimized && c != null) {
                                            destRect = destRect(bm.getWidth(), bm.getHeight());
                                            c.drawColor(Color.BLACK);
                                            c.drawBitmap(bm, null, destRect, p);
                                        }
                                    }
                                    else
                                        c = null;
                                } catch (Exception e) {
                                    e.getStackTrace();
                                }
                            }
                        } finally {
                            if (c != null) {
                                mSurfaceHolder.unlockCanvasAndPost(c);
                            }
                        }
                    }
                    mHandler.postDelayed(this, 100);
                }
                else{
                    mHandler.removeCallbacks(this);
                }
            }
        };
        mHandler.postDelayed(runnable, 100);
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
