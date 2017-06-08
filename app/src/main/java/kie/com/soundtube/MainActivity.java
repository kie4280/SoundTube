package kie.com.soundtube;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import kie.com.soundtube.MediaPlayerService.*;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements SearchFragment.OnFragmentInteractionListener, VideoFragment.OnFragmentInteractionListener {

    public static Handler UiHandler = null;
    public MediaPlayerService mediaService;
    private boolean servicebound = false;
    private Intent playIntent;
    public static boolean netConncted = false;

    VideoFragment videoFragment;
    SearchFragment searchFragment;
    ViewPager viewPager;
    ConnectivityManager connectmgr;

    SlidingUpPanelLayout slidingUpPanelLayout;
    Fragment[] fragments = new Fragment[2];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UiHandler = new Handler(Looper.getMainLooper());


        videoFragment = new VideoFragment();
        searchFragment = new SearchFragment();
        viewPager = (ViewPager)findViewById(R.id.viewpager);

        viewPager.setAdapter(fragmentPagerAdapter);
        fragments[0] = searchFragment;
        fragments[1] = videoFragment;

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position == 0) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else if(position == 1) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                }


            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    private FragmentPagerAdapter fragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return 2;
        }
    };



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder musicBinder = (MediaPlayerService.MusicBinder)service;
            mediaService = musicBinder.getService();
            servicebound = true;
            connect();
            if(mediaService.currentData != null) {
                videoFragment.resume();
            } else {

            }
            Log.d("activity", "service connected");

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicebound = false;
            mediaService.updateSeekBar = false;

        }
    };

    public void connect() {
        mediaService.videoFragment = videoFragment;
        mediaService.connected = true;
        videoFragment.mediaService = mediaService;
        videoFragment.mediaPlayer = mediaService.mediaPlayer;
        videoFragment.connected = true;
    }

    @Override
    protected void onStart() {

        super.onStart();
        connectmgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (playIntent == null) {
            playIntent = new Intent(this, MediaPlayerService.class);
            startService(playIntent);
        }

        bindService(playIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d("activity", "onStart");
    }

    @Override
    protected void onResume() {

        super.onResume();
        NetworkInfo info = connectmgr.getActiveNetworkInfo();
        if (info != null && info.isAvailable() && info.isConnected()) {
            netConncted = true;
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.needNetwork), Toast.LENGTH_SHORT);
            toast.show();
            netConncted = false;
        }
        Log.d("activity", "onResume");

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(servicebound) {
            unbindService(serviceConnection);
        }

        Log.d("activity", "onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("activity", "onPause");
    }

    @Override
    protected void onDestroy() {
        Log.d("activity", "onDestroy");
        stopService(playIntent);
        mediaService = null;
        super.onDestroy();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


    @Override
    public void onreturnVideo(DataHolder dataHolder) {
        viewPager.setCurrentItem(1, true);
        videoFragment.start(dataHolder);
    }

    @Override
    public void onBackPressed() {
        System.out.println("activity back");
        moveTaskToBack(true);
    }
}