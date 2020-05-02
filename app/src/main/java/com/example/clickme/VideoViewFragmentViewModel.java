package com.example.clickme;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class VideoViewFragmentViewModel extends ViewModel {

    private MutableLiveData<Boolean> isProcessing = new MutableLiveData<>();
    private MutableLiveData<MJpegViewService.MyBinder> myBinder = new MutableLiveData<>();

    public LiveData<Boolean> getIsVideoProcessing(){
        return isProcessing;
    }

    public LiveData<MJpegViewService.MyBinder> getBinder(){
        return myBinder;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MJpegViewService.MyBinder binder = (MJpegViewService.MyBinder) iBinder;
            myBinder.postValue(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            myBinder.postValue(null);
        }
    };

    public ServiceConnection getServiceConnection(){
        return serviceConnection;
    }

    public void setIsProcessing(Boolean result){
        isProcessing.postValue(result);
    }
}
