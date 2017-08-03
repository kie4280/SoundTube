package kie.com.soundtube;

import android.content.res.Configuration;
import android.support.v4.app.FragmentTransaction;
import android.app.Service;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.*;
import kie.com.soundtube.MediaPlayerService.MusicBinder;

public class MainActivity extends AppCompatActivity implements SearchFragment.OnFragmentInteractionListener, VideoFragment.OnFragmentInteractionListener {

    public static Handler UiHandler = null;

    private boolean servicebound = false;
    private Intent playIntent;
    private SearchView searchView;
    public Toolbar toolbar;
    private Button prevButton;
    private Button nextButton;
    public SlidingUpPanelLayout slidePanel;
    public static boolean netConncted = false;
    //    public CustomViewPager viewPager;
    VideoFragment videoFragment;
    SearchFragment searchFragment;
    MediaPlayerService mediaService;
    Context context;
    ConnectivityManager connectmgr;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectmgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        context = getApplicationContext();
        UiHandler = new Handler(Looper.getMainLooper());
        videoFragment = new VideoFragment();
        videoFragment.setActivity(this);
        searchFragment = new SearchFragment();
        searchFragment.setActivity(this);
//        viewPager = (CustomViewPager) findViewById(R.id.viewpager);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        nextButton = (Button) findViewById(R.id.nextButton);
        prevButton = (Button) findViewById(R.id.prevButton);
        slidePanel = (SlidingUpPanelLayout) findViewById(R.id.slidePanel);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
//        viewPager.setAdapter(fragmentPagerAdapter);

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction
                .add(R.id.videoPanel, videoFragment)
                .add(R.id.searchPanel, searchFragment)
                .commit();
        slidePanel.addPanelSlideListener(panelSlideListener);
        searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {

                System.out.println("submit");
                if (query != null) {
                    if (MainActivity.netConncted) {
                        searchFragment.search(query);
                    } else {
                        Toast toast = Toast.makeText(context, getString(R.string.needNetwork), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    searchView.clearFocus();
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
//                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                if(!hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchFragment.searcher.nextPage();
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchFragment.searcher.prevPage();
            }
        });

    }

    private PanelSlideListener panelSlideListener = new PanelSlideListener() {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
//            Log.d("panel", Float.toString(slideOffset));
            videoFragment.setHeaderPos(slideOffset);
        }

        @Override
        public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {

            if (newState == PanelState.EXPANDED) {
                videoFragment.setHeaderVisible(false);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
//                if (getSupportActionBar() != null) {
//                    getSupportActionBar().hide();
//                }

                Log.d("Panel", "expanded");
            } else if (newState == PanelState.COLLAPSED) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                if (getSupportActionBar() != null) {
//                    getSupportActionBar().show();
//                }
                Log.d("Panel", "collapsed");
            } else if (newState == PanelState.DRAGGING) {
                videoFragment.setHeaderVisible(true);
                Log.d("Panel", "dragging");
            }


        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder musicBinder = (MusicBinder) service;
            mediaService = musicBinder.getService();
            servicebound = true;
            connect();
            if(mediaService.mediaPlayer.isPlaying()) {
//                viewPager.setCurrentItem(1, true);
            }
            if(mediaService.currentData != null) {
                videoFragment.resume();
            } else {

            }
            Log.d("activity", "service connected");

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicebound = false;

            Log.d("activity", "service unbind");

        }
    };

    public void connect() {
        mediaService.videoFragment = videoFragment;
        videoFragment.mediaService = mediaService;
        videoFragment.mediaPlayer = mediaService.mediaPlayer;
    }

    @Override
    protected void onStart() {

        super.onStart();

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
    public void onreturnVideo(DataHolder dataHolder, Handler handler) {
//        viewPager.setCurrentItem(1, true);
        videoFragment.setSearchWorker(handler);
        videoFragment.start(dataHolder);
    }

    @Override
    public void onBackPressed() {
        System.out.println("activity back");
        moveTaskToBack(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                videoFragment.changeToLandscape();
                slidePanel.setTouchEnabled(false);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                videoFragment.changeToPortrait();
                slidePanel.setTouchEnabled(true);
                break;
            default:
                break;
        }
    }

    public void setToolbar(int dy) {

        int toolbaroffset = (int) (dy - toolbar.getTranslationY());
        if (dy > 0) {
            if (toolbaroffset < toolbar.getHeight()) {
                toolbar.setTranslationY(-toolbaroffset);
            } else {
                toolbar.setTranslationY(-toolbar.getHeight());
            }

        } else {
            if (toolbaroffset < 0) {
                toolbar.setTranslationY(0);
            } else {
                toolbar.setTranslationY(-toolbaroffset);
            }

        }

    }
}