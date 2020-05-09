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

public class MJpegViewService extends Service {
    public SurfaceHolder mSurfaceHolder;
    private IBinder mBinder = new MJpegViewService.MyBinder();
    private Handler mHandler;
    public MJpegInputStream mIn = null;
    public boolean mRun = false;
    private boolean surfaceDone = false;
    private int dispWidth;
    private int dispHeight;
    private int displayMode;
    public Boolean isMinimized = false;
    public Boolean isPaused = false;
    public Boolean afterCalibrationBreak = false;
    public Boolean faceNotFound = false;

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
    public int eyesClosedFactor = 0;
    public int headTiltedFactor = 0;
    public int decisionMatrix[][] = {{0,0},{0,0},{0,0},{0,0},{0,0}};
    public int eyesClosedArray[] = {0,0,0,0,0,0,0,0,0,0};
    public int headTiltedArray[] = {0,0,0,0,0,0,0,0,0,0};

    public int phoneMaxHeight = 0;
    public int phoneMaxWidth = 0;
    public int coffeeMaxHeight = 0;
    public int coffeeMaxWidth = 0;

    public int phoneMinHeight = 1000;
    public int phoneMinWidth = 1000;
    public int coffeeMinHeight = 1000;
    public int coffeeMinWidth = 1000;

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

            InputStream isPhone = getResources().openRawResource(R.raw.cascade_phone);
            InputStream isCoffee = getResources().openRawResource(R.raw.haarcascade_coffee);
            InputStream isFace = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            InputStream isEye = getResources().openRawResource(R.raw.haarcascade_eye);

            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            phoneCascadeFile = new File(cascadeDir, "haarcascade_phone.xml");
            coffeeCascadeFile = new File(cascadeDir, "haarcascade_coffee.xml");
            faceCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            eyeCascadeFile = new File(cascadeDir, "haarcascade_eye.xml");

            if ((coffeeDetector = newCascadeClassifier(coffeeCascadeFile, isCoffee)).empty()) {
                coffeeDetector = null;
            }

            if ((phoneDetector = newCascadeClassifier(phoneCascadeFile, isPhone)).empty()) {
                phoneDetector = null;
            }

            if ((faceDetector = newCascadeClassifier(faceCascadeFile, isFace)).empty()) {
                faceDetector = null;
            }

            if ((eyeDetector = newCascadeClassifier(eyeCascadeFile, isEye)).empty()) {
                eyeDetector = null;
            }

            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
        }

        mGray = new Mat();
        mRgba = new Mat();
        mat = new Mat();
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

    public class MyBinder extends Binder {
        MJpegViewService getService(){
            return MJpegViewService.this;
        }
    }

    private Rect destRect(int bmw, int bmh) {

        /*bmw = 1280 / bmh = 720*/

        int tempx;
        int tempy;

        if (displayMode == MJpegView.SIZE_BEST_FIT) {
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
        else{
            if (displayMode == MJpegView.SIZE_STANDARD) {
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            else{
                if (displayMode == MJpegView.SIZE_FULLSCREEN){
                    return new Rect(0, 0, dispWidth, dispHeight);
                }
            }
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

        for(int i = 0; i < 5; i++){
            countPhones += decisionMatrix[i][0];
            countCoffees += decisionMatrix[i][1];
        }

        for(int i = 0; i < 4; i++){
            decisionMatrix[i][0] = decisionMatrix[i + 1][0];
            decisionMatrix[i][1] = decisionMatrix[i + 1][1];
        }

        if(decisionMatrix[4][0] == 0 && decisionMatrix[4][1] == 0) return 0; //ked nic nepride nema co vykreslit :(
        if(countPhones <= 2 && countCoffees <= 2) return 0;
        if(countCoffees > countPhones && decisionMatrix[3][1] == 1) return 2;
        else
            if(decisionMatrix[3][0] == 1) return 1;

        return 0;
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

    public int computeDrowsinessFactor(int[] array){
        //ak prva polka je == 0 -> vrat sucet druhej
        //ak prva polka je > 0 && druha polka > 0 -> vrat sucet

        int firstSecondCount = 0;
        int secondSecondCount = 0;

        for(int i = 0; i < 10; i++){
            if(i < 5) firstSecondCount += array[i];
            else secondSecondCount += array[i];
        }
        for(int i = 0; i < 9; i++)
            array[i] = array[i + 1];

        if(firstSecondCount > 0 && secondSecondCount > 0) return (firstSecondCount + secondSecondCount) * 10;
        else return secondSecondCount * 10;
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

                        afterCalibrationBreak = true;
                        if (faceFound) {
                            //ma okuliare???

                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage("Do you wear glasses?").setTitle("Eyes not found!");

                            builder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //vynuluj eyesclosed
                                    for(int i = 0; i < 10; i++) eyesClosedArray[i] = 0;
                                    eyesClosedFactor = 0;
                                    wearingGlasses = true;
                                    afterCalibrationBreak = false;
                                    calibrationMode = 0;
                                }
                            });
                            builder.setNegativeButton("no (restart calibration)", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    calibrationMode = 1;
                                    afterCalibrationBreak = false;
                                    progressRectangle = 0;
                                    Imgproc.rectangle(mRgba, new Point(420, 460), new Point(420 + progressRectangle, 480), new Scalar(255, 255, 0), -1);
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                        } else {

                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage("Calibration not successful!");

                            builder.setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    calibrationMode = 1;
                                    afterCalibrationBreak = false;
                                    progressRectangle = 0;
                                    Imgproc.rectangle(mRgba, new Point(420, 460), new Point(420 + progressRectangle, 480), new Scalar(255, 255, 0), -1);
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            dialog.setCanceledOnTouchOutside(false);
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
                                afterCalibrationBreak = true;
                                Imgproc.rectangle(mRgba, new Point(420, 460), new Point(420 + progressRectangle, 480), new Scalar(255, 255, 0), -1);

                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setMessage("Calibration done!");

                                builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        calibrationMode = 0;
                                        afterCalibrationBreak = false;
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
                                new Size(100, 100), new Size(400, 400));

                    org.opencv.core.Rect[] facesArray;

                    if (!faces.empty()) {

                        faceNotFound = false;
                        headTiltedArray[9] = 0;

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
                            eyeDetector.detectMultiScale(faceMat, eyes, 1.5, 10, 0, // TODO: objdetect.CV_HAAR_SCALE_IMAGE (1.5, 10 eye cascade)
                                    new Size(10, 10), new Size(100, 100));

                        if (!eyes.empty()) {

                            eyesClosedArray[9] = 0;

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
                                    Imgproc.rectangle(mRgba, eyesArray[i].tl(), eyesArray[i].br(), FACE_RECT_COLOR, 3);
                                }
                            }
                        }
                        else{
                            if(!wearingGlasses)
                                eyesClosedArray[9] = 1;
                        }
                    } else {
                        faceNotFound = true;
                        headTiltedArray[9] = 1;
                    }

                    eyesClosedFactor = computeDrowsinessFactor(eyesClosedArray);
                    headTiltedFactor = computeDrowsinessFactor(headTiltedArray);

                    //endregion

                    //region object detection region

                    if (phoneDetector != null)
                        phoneDetector.detectMultiScale(mGray.submat(new org.opencv.core.Rect(0, 200, mGray.width(), 520)),
                                phones, 1.5, 400, 0, // TODO: objdetect.CV_HAAR_SCALE_IMAGE (2, 20 phone cascade)
                                new Size(150, 150), new Size(250, 250));

                    /*if (coffeeDetector != null)
                        coffeeDetector.detectMultiScale(mGray.submat(new org.opencv.core.Rect(0, 200, mGray.width(), 520)),
                                coffees, 2, 20, 0, // TODO: objdetect.CV_HAAR_SCALE_IMAGE (2, 20 coffee cascade)
                                new Size(150, 150), new Size(250, 250));*/


                    if (!phones.empty()) decisionMatrix[4][0] = 1;
                    else decisionMatrix[4][0] = 0;

                    if (!coffees.empty()) decisionMatrix[4][1] = 1;
                    else decisionMatrix[4][1] = 0;

                    int decision = makeDecision();

                    if (decision == 1) {
                        coffeeDistraction = 0;
                        phoneDistraction = 50;
                        org.opencv.core.Rect[] objectArray;

                        if(decisionMatrix[4][0] == 1) objectArray = phones.toArray();
                        else objectArray = coffees.toArray();

                        System.out.println("////////////// velkost telefonu: " + objectArray[0].height + ", " + objectArray[0].width);

                        if(objectArray[0].height > phoneMaxHeight) phoneMaxHeight = objectArray[0].height;
                        if(objectArray[0].width > phoneMaxWidth) phoneMaxWidth = objectArray[0].width;
                        if(objectArray[0].height < phoneMinHeight) phoneMinHeight = objectArray[0].height;
                        if(objectArray[0].width < phoneMinWidth) phoneMinWidth = objectArray[0].width;

                        Imgproc.rectangle(mRgba, new Point(objectArray[0].x, objectArray[0].y + 200),
                                new Point(objectArray[0].x + objectArray[0].width, objectArray[0].y + 200 + objectArray[0].width), OBJECT_RECT_COLOR, 3);
                        Imgproc.putText(mRgba, "phone", new Point(objectArray[0].x, objectArray[0].y - 3 + 200), Core.FONT_HERSHEY_SIMPLEX, 1, OBJECT_RECT_COLOR, 2);
                    } else {
                        if (decision == 2) {
                            phoneDistraction = 0;
                            coffeeDistraction = 50;
                            org.opencv.core.Rect[] objectArray;

                            if(decisionMatrix[4][1] == 1) objectArray = coffees.toArray();
                            else objectArray = phones.toArray();

                            System.out.println("////////////// velkost kavy: " + objectArray[0].height + ", " + objectArray[0].width);

                            if(objectArray[0].height > coffeeMaxHeight) coffeeMaxHeight = objectArray[0].height;
                            if(objectArray[0].width > coffeeMaxWidth) coffeeMaxWidth = objectArray[0].width;
                            if(objectArray[0].height < coffeeMinHeight) coffeeMinHeight = objectArray[0].height;
                            if(objectArray[0].width < coffeeMinWidth) coffeeMinWidth = objectArray[0].width;

                            Imgproc.rectangle(mRgba, objectArray[0].tl(), objectArray[0].br(), OBJECT_RECT_COLOR, 3);
                            Imgproc.putText(mRgba, "coffee", new Point(objectArray[0].x, objectArray[0].y - 3), Core.FONT_HERSHEY_SIMPLEX, 1, OBJECT_RECT_COLOR, 2);
                        } else {
                            if (phoneDistraction > 0) phoneDistraction -= 5;
                            if (coffeeDistraction > 0) coffeeDistraction -= 5;
                        }
                    }
                    //endregion
                }
            }
        }

        System.out.println("/////////////////// max coffee dimensions: " + coffeeMaxHeight + coffeeMaxWidth);
        System.out.println("/////////////////// max phone dimensions: " + phoneMaxHeight + phoneMaxWidth);
        System.out.println("/////////////////// min coffee dimensions: " + coffeeMinHeight + coffeeMinWidth);
        System.out.println("/////////////////// min phone dimensions: " + phoneMinHeight + phoneMinWidth);

        return mRgba;
    }

    public void computeDistractionLevel(){
        globalDistraction = phoneDistraction + coffeeDistraction + headTiltedFactor + eyesClosedFactor;
    }

    public Mat makePausedScreen(Mat inputFrame){
        mRgba = inputFrame;
        Mat roi = mRgba.clone();
        double opacity = 0.7;

        Imgproc.rectangle(roi, new Point(0,0), new Point(mRgba.width(), mRgba.height()), new Scalar(0,0,0),-1);
        Core.addWeighted(roi,opacity, mRgba, 1-opacity,0, mRgba);
        Imgproc.putText(mRgba, "PAUSED", new Point(mRgba.width() / 2 - 90, mRgba.height() / 2 - 10), Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(200,200,200), 3);

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
                    if (surfaceDone) {
                        try {
                            synchronized (mSurfaceHolder) {
                                try {

                                    bm = mIn.readMJpegFrame();

                                    if(bm != null && !afterCalibrationBreak) {
                                        //
                                        //TODO: tu sa bude diat rozpoznavanie veci atd.
                                        //
                                        if(!isPaused || calibrationMode != 0) {
                                            Utils.bitmapToMat(bm, mat);
                                            mat = recognize(mat);
                                            Utils.matToBitmap(mat, bm);

                                            computeDistractionLevel();
                                        }
                                        else{
                                            Utils.bitmapToMat(bm, mat);
                                            mat = makePausedScreen(mat);
                                            Utils.matToBitmap(mat, bm);
                                        }

                                        c = mSurfaceHolder.lockCanvas();
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
                    mHandler.postDelayed(this, 20);
                }
                else{
                    mHandler.removeCallbacks(this);
                }
            }
        };
        mHandler.postDelayed(runnable, 20);
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

}
