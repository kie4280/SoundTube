package kie.com.soundtube;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.util.LinkedList;

import kie.com.soundtube.MediaPlayerService.MusicBinder;

public class PlayerActivity extends AppCompatActivity implements SearchFragment.OnFragmentInteractionListener,
        VideoFragment.OnFragmentInteractionListener {


    private Handler workHandler;
    private HandlerThread workThread;
    public static boolean servicebound = false;
    private Intent serviceIntent;
    public Toolbar playerToolbar;
    public LinearLayout searchArea;
    public DrawerLayout drawerLayout;
    public RelativeLayout mainRelativeLayout;
    public AppBarLayout appBarLayout;
    public CustomSlideUpPanel slidePanel;
    public static boolean netConncted = false;

    //    public CustomViewPager viewPager;
    VideoFragment videoFragment;
    SearchFragment searchFragment;
    //    SettingFragment settingFragment;
//    PlaylistFragment playlistFragment;
    FragmentManager fragmentManager;
    MediaPlayerService mediaService;
    Context context;
    ConnectivityManager connectmgr;
    TelephonyManager telephonyManager;
    LinkedList<DataHolder> watchedQueue = new LinkedList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            Thread.UncaughtExceptionHandler defualtexception = Thread.getDefaultUncaughtExceptionHandler();

            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
//                throwable.printStackTrace();
                Intent intent = new Intent(context, LogActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                String stack = Log.getStackTraceString(throwable);
                intent.putExtra("error message", stack);
                Log.d("Exception", "caught");
                startActivity(intent);
//                continue as normal
                defualtexception.uncaughtException(thread, throwable);
//                System.exit(1);
//                finish();

            }
        });

        mainRelativeLayout = (RelativeLayout) findViewById(R.id.mainRelativeLayout);
        View view = LayoutInflater.from(context).inflate(R.layout.slide_layout, mainRelativeLayout);
        playerToolbar = (Toolbar) findViewById(R.id.playerToolbar);
        appBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
        searchArea = (LinearLayout) findViewById(R.id.searchArea);
        serviceIntent = new Intent(this, MediaPlayerService.class);
        connectmgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        workThread = new HandlerThread("WorkThread");
        workThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        workThread.start();
        workHandler = new Handler(workThread.getLooper());
        videoFragment = new VideoFragment();
        videoFragment.setActivity(this);
        videoFragment.setSearchWorker(workHandler);
        searchFragment = new SearchFragment();
        searchFragment.setActivity(this);
        searchFragment.setSearchWorker(workHandler);
//        playlistFragment = new PlaylistFragment();
//        playlistFragment.setActivity(this);
//        settingFragment = new SettingFragment();
//        settingFragment.setActivity(this);
        slidePanel = (CustomSlideUpPanel) findViewById(R.id.slidePanel);
//        slidePanel.setTouchEnabled(false);
//        actionBar.setDisplayShowCustomEnabled(true);
//        actionBar.setDisplayHomeAsUpEnabled(true);
//        actionBar.setDisplayShowCustomEnabled(true);
//        actionBar.setDisplayShowTitleEnabled(false);

        fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction
                .add(R.id.videoPanel, videoFragment, "videoFragment")
                .add(R.id.searchPanel, searchFragment, "searchFragment")
                .commit();
        slidePanel.addPanelSlideListener(panelSlideListener);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
//        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this
                , drawerLayout, playerToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        };
        playerToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navView = (NavigationView) findViewById(R.id.navigationView);
        navView.setNavigationItemSelectedListener(navigationItemSelectedListener);


    }

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (mediaService != null) {
                        mediaService.phonecall(true);
                    }

                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mediaService != null) {
                        mediaService.phonecall(false);
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;

            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

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
                videoFragment.setHeaderPos(1);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
//                if (getSupportActionBar() != null) {
//                    getSupportActionBar().hide();
//                }

                Log.d("Panel", "expanded");
            } else if (newState == PanelState.COLLAPSED) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                videoFragment.setHeaderPos(0);
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

    private NavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =
            new NavigationView.OnNavigationItemSelectedListener() {


                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int i = item.getItemId();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    switch (i) {

                        case R.id.playlists:

//                                mainRelativeLayout.removeViewAt(0);
//                                fragmentManager.beginTransaction()
//                                        .remove(searchFragment)
//                                        .remove(videoFragment)
//                                        .remove(settingFragment)
//                                        .add(R.id.mainRelativeLayout, playlistFragment, "playlistFragment")
//                                        .commit();
                            Intent playlistintent = new Intent(context, PlaylistActivity.class);
                            playlistintent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                            startActivity(playlistintent);


                            break;
                        case R.id.settings:

//                                mainRelativeLayout.removeViewAt(0);
//                                fragmentManager.beginTransaction()
//                                        .remove(searchFragment)
//                                        .remove(videoFragment)
//                                        .remove(playlistFragment)
//                                        .add(R.id.mainRelativeLayout, settingFragment, "settingFragment")
//                                        .commit();
                            Intent settingintent = new Intent(context, SettingActivity.class);
                            settingintent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(settingintent);

                            break;
                        default:
                            break;
                    }


                    return true;
                }
            };


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder musicBinder = (MusicBinder) service;
            mediaService = musicBinder.getService();
            servicebound = true;
            mediaService.videoFragment = videoFragment;
            videoFragment.mediaService = mediaService;
            videoFragment.serviceConnected();
            Log.d("activity", "service bind");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicebound = false;
            mediaService.videoFragment = null;
            videoFragment.mediaService = null;
            mediaService = null;
            Log.w("activity", "service unbind due to error");
        }
    };

    public void connect() {
        startService(serviceIntent);
        if (mediaService == null || !servicebound) {
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

    }

    public void disconnect() {
        if (servicebound) {
            unbindService(serviceConnection);
            servicebound = false;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("activity", "onStartVideo");

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
        disconnect();
        Log.d("activity", "onStop");
    }

    @Override
    protected void onDestroy() {
        Log.d("activity", "onDestroy");
        mediaService = null;
        workThread.quit();
        super.onDestroy();
    }

    @Override
    public void onReturnSearchVideo(DataHolder dataHolder) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                slidePanel.setPanelState(PanelState.EXPANDED);
            }
        });
        videoFragment.start(dataHolder);
    }

    @Override
    public void onBackPressed() {
        Log.d("PlayerActivity", "activity back");

//        moveTaskToBack(true);

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

    @Override
    public void onStartVideo(DataHolder dataHolder) {
        watchedQueue.offer(dataHolder);
    }


//    public void setPlayerToolbar(int dy) {
//
//        int toolbaroffset = (int) (dy - playerToolbar.getTranslationY());
//        if (dy > 0) {
//            if (toolbaroffset < playerToolbar.getHeight()) {
//                playerToolbar.setTranslationY(-toolbaroffset);
//            } else {
//                playerToolbar.setTranslationY(-playerToolbar.getHeight());
//            }
//
//        } else {
//            if (toolbaroffset < 0) {
//                playerToolbar.setTranslationY(0);
//            } else {
//                playerToolbar.setTranslationY(-toolbaroffset);
//            }
//
//        }
//
//    }


}