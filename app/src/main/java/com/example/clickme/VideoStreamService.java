package com.example.clickme;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.net.HttpURLConnection;
import java.net.URL;

public class VideoStreamService extends Service {

    private IBinder mBinder = new VideoStreamService.MyBinder();
    private Handler mHandler;
    private static final String URL = "http://admin:ms1234@10.0.0.3:80/ipcam/mjpeg.cgi";
    private MjpegInputStream result;
    private Boolean mIsPaused = true;
    private Boolean isNotSet = true;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean isNotSet() {
        return isNotSet;
    }

    public void setResult(){
        isNotSet = false;
    }

    public class MyBinder extends Binder {
        VideoStreamService getService(){
            return VideoStreamService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    public void startPretendLongRunningTask(){
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(result == null) {
                    new DoRead().execute(URL);
                }
                mHandler.postDelayed(this, 200);
            }
        };
        mHandler.postDelayed(runnable, 200);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {

        protected MjpegInputStream doInBackground(String... Url) {
            //TODO: if camera has authentication deal with it and don't just not work

            System.out.println("////////////////////////////////////////////////// 1. Sending http request");
            try {
                java.net.URL url = new URL(Url[0]); // here is your URL path
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(150 /* milliseconds */);
                conn.setConnectTimeout(150 /* milliseconds */);
                int responseCode = conn.getResponseCode();

                System.out.println("////////////////////////////////////////////////// 2. Request finished, status = " + responseCode);
                if (responseCode == 401) {
                    return null;
                }
                return new MjpegInputStream(conn.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("////////////////////////////////////////////////// Request failed-ClientProtocolException");
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            if (result != null) {
                System.out.println("////////////////////////////////////////////////// MAME RESULT!!!!");
                setResult(result);
            }
        }
    }

    public void setResult(MjpegInputStream res){
        result = res;
    }

    public MjpegInputStream getResult(){
        return result;
    }
}
