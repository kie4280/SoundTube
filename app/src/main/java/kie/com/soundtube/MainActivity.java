package kie.com.soundtube;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import kie.com.soundtube.MediaPlayerService3.*;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements SearchFragment.OnFragmentInteractionListener, VideoFragment2.OnFragmentInteractionListener {

    public static Handler UiHandler = null;
    public Fragment[] fragments = new Fragment[2];
    public MediaPlayerService3 mediaService;
    private boolean servicebound = false;
    private Intent playIntent;
    NonSwipeViewPager viewPager;
    TabLayout tabLayout;
    VideoFragment2 videoFragment;
    SearchFragment searchFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UiHandler = new Handler(Looper.getMainLooper());
        FragmentManager fragmentManager = getSupportFragmentManager();
        videoFragment = new VideoFragment2();
        searchFragment = new SearchFragment();

        fragments[0] = searchFragment;
        fragments[1] = videoFragment;
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.frame, videoFragment, "video");
        fragmentTransaction.commit();



    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder musicBinder = (MediaPlayerService3.MusicBinder)service;
            mediaService = musicBinder.getService();
            servicebound = true;
            connect();
            if(mediaService.currentData != null) {
                videoFragment.resume();
            } else {
                VideoRetriver videoRetriver = new VideoRetriver();
                videoRetriver.startExtracting("https://www.youtube.com/watch?v=_sQSXwdtxlY", new VideoRetriver.YouTubeExtractorListener() {
                    @Override
                    public void onSuccess(HashMap<Integer, String> result) {
                        DataHolder dataHolder = new DataHolder();
                        dataHolder.videoUris = result;
                        videoFragment.start(dataHolder);
                    }

                    @Override
                    public void onFailure(Error error) {

                    }
                });

            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicebound = false;

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
        if (playIntent == null) {
            playIntent = new Intent(this, MediaPlayerService3.class);
            bindService(playIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
            servicebound = true;
        } else if (!servicebound){
            bindService(playIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("activity", "onResume");

    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        servicebound = false;
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
        super.onDestroy();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


    @Override
    public void onreturnVideo(DataHolder dataHolder) {
        //viewPager.setCurrentItem(1, true);
    }

    @Override
    public void onBackPressed() {
        System.out.println("activity back");
        super.onBackPressed();
    }
}