package com.example.clickme;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;


public class MainActivityViewModel extends ViewModel {

    private MutableLiveData<Boolean> mIsProgressUpdating = new MutableLiveData<>();
    private MutableLiveData<MinimizedActivityService.MyBinder> myBinder = new MutableLiveData<>();

    public LiveData<Boolean> getIsProgressUpdating(){
        return mIsProgressUpdating;
    }

    public LiveData<MinimizedActivityService.MyBinder> getBinder(){
        return myBinder;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MinimizedActivityService.MyBinder binder = (MinimizedActivityService.MyBinder) iBinder;
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

    public void setIsUpdating(Boolean isUpdating){
        mIsProgressUpdating.postValue(isUpdating);
    }
}
