package com.example.clickme;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class vvf extends Fragment {

    // Declare variables
//        VideoView videoview;
//    MainActivity main = new MainActivity();
    private static final String TAG = "MjpegActivity";

    private MjpegView mv;
    // Physical display width and height.
    private static int displayWidth = 0;
    private static int displayHeight = 0;

    // Video URL
//    public String path = main.Path;
//    String VideoURL = path + "Video1.mp4";
//        String VideoURL = "http://192.168.43.1:8080";

    //sample public cam
    String URL = "http://admin:ms1234@10.0.0.3:80/ipcam/mjpeg.cgi";
//    String URL = "http://192.168.43.134:5432/XMLParser/Video1.mp4";
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

//        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.activity_main, container, false);
        View view = inflater.inflate(R.layout.fragment_video_view, container, false);
//        return view;

        mv = view.findViewById(R.id.surface_view);
//        mv = new MjpegView(this.getContext());
        System.out.println("///////////////////////// nasiel too");
        //new DoRead().execute(URL);
        return view;
    }

    public void onPause(){
        super.onPause();
        mv.stopPlayback();
    }

    public void onResume(){
        super.onResume();
    }
}