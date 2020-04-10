package com.example.clickme;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.RequestQueue;

import org.opencv.android.OpenCVLoader;


public class MainActivity extends AppCompatActivity {

    RequestQueue queue;
    public final static String URL_ROOT = "http://192.168.0.129:5000/";

    MinimizedActivityService MAS;
    MainActivityViewModel MAVM;
    private SectionsStatePagerAdapter sectionsStatePagerAdapter;
    private ViewPager mViewPager;


    @Override
    protected void onResume()
    {
        super.onResume();
        overridePendingTransition(R.anim.slide_from_top,R.anim.slide_in_top);

        if(!OpenCVLoader.initDebug()){

        }
        else{

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        System.out.println("dpHeight" + dpHeight);
        System.out.println("dpWidth" + dpWidth);


        if(OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(), "OpenCV initialization successful", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(), "ERROR: in OpenCV initialization", Toast.LENGTH_LONG).show();
        }

        sectionsStatePagerAdapter = new SectionsStatePagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.container);
        setupViewPager(mViewPager);
        setViewPager(0);

    }

    private void setupViewPager(ViewPager viewPager){
        SectionsStatePagerAdapter adapter = new SectionsStatePagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new VideoViewFragment(), "fragment1") ;
        viewPager.setAdapter(adapter);
    }

    public void setViewPager(int i){
        mViewPager.setCurrentItem(i);
    }

}
