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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import kie.com.soundtube.MediaPlayerService2.MusicBinder;

import java.util.HashMap;


public class MainActivity1 extends AppCompatActivity implements SearchFragment.OnFragmentInteractionListener, VideoFragment1.OnFragmentInteractionListener {

    public static Handler UiHandler = null;
    public MediaPlayerService2 mediaService;
    private boolean servicebound = false;
    private Intent playIntent;

    VideoFragment1 videoFragment;
    SearchFragment searchFragment;
    SlidingUpPanelLayout slidingUpPanelLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UiHandler = new Handler(Looper.getMainLooper());

        videoFragment = new VideoFragment1();
        searchFragment = new SearchFragment();
//        slidingUpPanelLayout = (SlidingUpPanelLayout)findViewById(R.id.sliding_layout);
//        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.frame, videoFragment, "video");
//        fragmentTransaction.add(searchFragment, "search");
        fragmentTransaction.commitNow();

//        slidingUpPanelLayout.setDragView(videoFragment.getView());
//        slidingUpPanelLayout.addView(searchFragment.getView());

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder musicBinder = (MusicBinder)service;
            mediaService = musicBinder.getService();
            servicebound = true;
            connect();
            if(mediaService.currentData != null) {
                videoFragment.resume();
            } else {
                Log.d("activity", "videoRetrive");
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
        if (playIntent == null) {
            playIntent = new Intent(this, MediaPlayerService2.class);
            startService(playIntent);
        }
        bindService(playIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d("activity", "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        //viewPager.setCurrentItem(1, true);
    }

    @Override
    public void onBackPressed() {
        System.out.println("activity back");
        moveTaskToBack(true);
    }
}