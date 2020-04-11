package com.example.clickme;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class VideoViewFragmentViewModel extends ViewModel {

    private MutableLiveData<Boolean> mIsResultNull = new MutableLiveData<>();
    private MutableLiveData<MjpegViewService.MyBinder> myBinder = new MutableLiveData<>();

    public LiveData<Boolean> getIsProgressUpdating(){
        return mIsResultNull;
    }

    public LiveData<MjpegViewService.MyBinder> getBinder(){
        return myBinder;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MjpegViewService.MyBinder binder = (MjpegViewService.MyBinder) iBinder;
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

    public void setIsUpdating(Boolean result){
        mIsResultNull.postValue(result);
    }
}
