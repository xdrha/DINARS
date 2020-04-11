package com.example.clickme;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;


public class MainActivity extends AppCompatActivity {

    private ViewPager mViewPager;

    @Override
    protected void onResume()
    {
        super.onResume();
        overridePendingTransition(R.anim.slide_from_top,R.anim.slide_in_top);

        Toast.makeText(this, "RESUME DONE", Toast.LENGTH_SHORT).show();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "DESTROY DONE", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Toast.makeText(this, "STOP DONE", Toast.LENGTH_SHORT).show();
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

        Toast.makeText(this, "ON CREATE", Toast.LENGTH_SHORT).show();


        if(OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(), "OpenCV initialization successful", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(), "ERROR: in OpenCV initialization", Toast.LENGTH_LONG).show();
        }

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
