package com.example.clickme;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class VideoViewFragmentViewModel extends ViewModel {

    private MutableLiveData<Boolean> isProcessing = new MutableLiveData<>();
    private MutableLiveData<MJpegViewService.MyBinder> mJpegViewServiceBinder = new MutableLiveData<>();
    private MutableLiveData<Boolean> isActivityHidden = new MutableLiveData<>();
    private MutableLiveData<Boolean> isBeepNeeded = new MutableLiveData<>();
    private MutableLiveData<MinimizedActivityService.MyBinder> minimizedActivityBinder = new MutableLiveData<>();

    public LiveData<Boolean> getIsVideoProcessing(){
        return isProcessing;
    }

    public LiveData<Boolean> getIsBeepNeeded(){
        return isBeepNeeded;
    }

    public LiveData<MJpegViewService.MyBinder> getMJVSBinder(){
        return mJpegViewServiceBinder;
    }
    public LiveData<Boolean> getIsHidden(){
        return isActivityHidden;
    }

    public LiveData<MinimizedActivityService.MyBinder> getMASBinder(){
        return minimizedActivityBinder;
    }


    private ServiceConnection MJVSConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MJpegViewService.MyBinder binder = (MJpegViewService.MyBinder) iBinder;
            mJpegViewServiceBinder.postValue(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mJpegViewServiceBinder.postValue(null);
        }
    };

    private ServiceConnection MASConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MinimizedActivityService.MyBinder binder = (MinimizedActivityService.MyBinder) iBinder;
            minimizedActivityBinder.postValue(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            minimizedActivityBinder.postValue(null);
        }
    };

    public ServiceConnection getMJVSConnection(){
        return MJVSConnection;
    }

    public void setIsProcessing(Boolean result){
        isProcessing.postValue(result);
    }

    public void setIsBeepNeeded(Boolean result){
        isBeepNeeded.postValue(result);
    }

    public ServiceConnection getMASConnection(){
        return MASConnection;
    }

    public void setIsHidden(Boolean isUpdating){
        isActivityHidden.postValue(isUpdating);
    }
}
