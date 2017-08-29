package kie.com.soundtube;

import android.content.res.Configuration;
import android.os.Debug;
import android.os.HandlerThread;
import android.os.Process;
import android.support.v4.app.FragmentTransaction;
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
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.SearchView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.*;

import kie.com.soundtube.MediaPlayerService.MusicBinder;

public class MainActivity extends AppCompatActivity implements SearchFragment.OnFragmentInteractionListener,
        VideoFragment.OnFragmentInteractionListener {

    public static Handler UiHandler = null;
    private Handler workHandler;
    private HandlerThread workThread;
    public static boolean servicebound = false;
    public static boolean activityRunning = false;
    private Intent serviceIntent;
    public SearchView searchView;
    public Toolbar toolbar;
    public DrawerLayout drawerLayout;
    public SlidingUpPanelLayout slidePanel;
    public static boolean netConncted = false;

    //    public CustomViewPager viewPager;
    VideoFragment videoFragment;
    SearchFragment searchFragment;
    MediaPlayerService mediaService;
    Context context;
    ConnectivityManager connectmgr;
    TelephonyManager telephonyManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceIntent = new Intent(this, MediaPlayerService.class);
        connectmgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        context = getApplicationContext();
        UiHandler = new Handler(Looper.getMainLooper());
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
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        slidePanel = (SlidingUpPanelLayout) findViewById(R.id.slidePanel);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setTitle("");

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction
                .add(R.id.videoPanel, videoFragment)
                .add(R.id.searchPanel, searchFragment)
                .commit();
        slidePanel.addPanelSlideListener(panelSlideListener);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
//        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
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
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(true);
//        "http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&client=firefox&q=Query";
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
                if (!hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                }
            }
        });

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        activityRunning = true;

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

    @Override
    protected void onStart() {
        super.onStart();
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
        if (servicebound) {
            unbindService(serviceConnection);
        }
        Log.d("activity", "onStop");
    }

    @Override
    protected void onDestroy() {
        Log.d("activity", "onDestroy");
//        mediaService = null;
        activityRunning = false;
        workThread.quit();
        super.onDestroy();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onreturnVideo(DataHolder dataHolder) {
//        viewPager.setCurrentItem(1, true);
        slidePanel.setPanelState(PanelState.EXPANDED);
        videoFragment.start(dataHolder);
    }

    @Override
    public void onBackPressed() {
        System.out.println("activity back");
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
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