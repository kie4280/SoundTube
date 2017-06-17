package kie.com.soundtube;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;


public class VideoFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    public float Displayratio = 16f / 9f;
    public float Videoratio = 16f / 9f;
    public boolean prepared = false;
    boolean connected = false;
    boolean controlshow = true;
    boolean seekbarUpdating = false;
    private Button playbutton;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private RelativeLayout vrelativeLayout;
    private RelativeLayout drelativeLayout;
    private View videoFragmentView;
    private SeekBar seekBar;
    private Handler ui;
    private ProgressBar progressBar;
    private TextView textView;
    private ListView listView;
    private DisplayMetrics displayMetrics;
    private Context context;
    private RelativeLayout.LayoutParams portraitlayout;
    private RelativeLayout.LayoutParams landscapelayout;
    private Activity activity;
    private Handler seekHandler;
    private HandlerThread thread;

    MediaPlayerService mediaService;
    MediaPlayer mediaPlayer;
    MainActivity mainActivity;

    public VideoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ui = new Handler(Looper.getMainLooper());
        context = getContext();
        activity = getActivity();
        displayMetrics = context.getResources().getDisplayMetrics();
        thread = new HandlerThread("seek");
        thread.start();
        seekHandler = new Handler(thread.getLooper());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                changeToLandscape();
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                changeToPortrait();
                break;
            default:
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        videoFragmentView = inflater.inflate(R.layout.fragment_video, container, false);
        surfaceView = (SurfaceView) videoFragmentView.findViewById(R.id.surfaceView);
        seekBar = (SeekBar) videoFragmentView.findViewById(R.id.seekBar);
        //textView = (TextView) videoFragmentView.findViewById(R.id.textView);
        listView = (ListView) videoFragmentView.findViewById(R.id.recoList);
        playbutton = (Button) videoFragmentView.findViewById(R.id.playbutton);
        progressBar = (ProgressBar) videoFragmentView.findViewById(R.id.progressBar1);
        progressBar.setMax(100);
        progressBar.setIndeterminate(false);
        vrelativeLayout = (RelativeLayout) videoFragmentView.findViewById(R.id.videoRelativeLayout);
        drelativeLayout = (RelativeLayout) videoFragmentView.findViewById(R.id.descriptionRelativeLayout);
        landscapelayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int w = (int) (displayMetrics.heightPixels / Displayratio);
            portraitlayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, w);
            changeToLandscape();

        } else {
            int w = (int) (displayMetrics.widthPixels / Displayratio);
            portraitlayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, w);
            changeToPortrait();
        }

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                prepared = true;
                if (mediaService != null) {
                    mediaService.setDisplay(holder);
                }

                Log.d("video", "surfaceCreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                System.out.println("width " + width + " height " + height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                prepared = false;
                Log.d("video", "surfaceDestroyed");
                mediaService.setDisplay(null);

            }
        });
        playbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    mediaService.pause();
                    playbutton.setBackgroundResource(R.drawable.play);

                } else {
                    mediaService.play();
                    playbutton.setBackgroundResource(R.drawable.pause);
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if (mediaService.prepared && fromUser) {
                    mediaService.seekTo(progress);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            float prevX = 0;
            float prevY = 0;
            float threshold = 15f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    mainActivity.viewPager.setSwipingEnabled(false);
                    prevX = event.getX();
                    prevY = event.getY();

                    Log.d("surface", "touchdown");

                } else if (action == MotionEvent.ACTION_UP) {
                    mainActivity.viewPager.setSwipingEnabled(true);
                    if (Math.abs(event.getX() - prevX) <= threshold && Math.abs(event.getY() - prevY) <= threshold) {
                        if (controlshow) {
                            showcontrols(false);
                        } else {
                            showcontrols(true);
                        }
                    }

                    Log.d("surface", "touchup");
                }

                return true;
            }
        });


        Log.d("video", "createView");

        return videoFragmentView;
    }


    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        System.out.println("fragment back");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
//        if (mediaService.updateSeekBar) {
//            updateSeekBar();
//        }
    }

    @Override
    public void onStop() {
        super.onStop();
        connected = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.quit();
    }

    public void setActivity(MainActivity activity) {
        this.mainActivity = activity;
    }

    public void start(final DataHolder dataHolder) {

        ConnectivityManager connectmgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectmgr.getActiveNetworkInfo();
        if (info.isAvailable() && info.isConnected()) {
            for (int a = 0; a<VideoRetriver.mPreferredVideoQualities.size(); a++) {
                int quality = VideoRetriver.mPreferredVideoQualities.get(a);
                if (dataHolder.videoUris.containsKey(quality)) {
                    if (mediaService != null) {
                        mediaService.reset();
                        mediaService.prepare(dataHolder, quality);
                        mediaService.setDisplay(surfaceHolder);
                        mediaService.play();
                        playbutton.setBackgroundResource(R.drawable.pause);
                        textView.setText(dataHolder.title);

                    } else {
                        Toast toast = Toast.makeText(context, "Error!!!", Toast.LENGTH_LONG);
                        toast.show();
                    }

                    break;
                } else if(a == VideoRetriver.mPreferredVideoQualities.size() - 1){
                    Toast toast = Toast.makeText(context, "No video resolution", Toast.LENGTH_LONG);
                    toast.show();
                }

            }
        } else {
            Toast toast = Toast.makeText(context, getString(R.string.needNetwork), Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    public void resume() {


    }

    public void buffering(final boolean buff) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (buff) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }

            }
        });

    }

    public void setSeekBarMax(final int max) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                seekBar.setMax(max);
            }
        });
    }

    public void updateSeekBar() {
        if (activity != null && !seekbarUpdating) {
            seekbarUpdating = true;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mediaService.updateSeekBar) {
                        seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        seekHandler.postDelayed(this, 1000);
                    } else {
                        seekbarUpdating = false;
                    }
                }
            });
        }
    }

    public void onComplete() {
        seekHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playbutton.setBackgroundResource(R.drawable.play);
                        showcontrols(true);
                        seekBar.setProgress(0);

                    }
                });
            }
        }, 200);

    }

    public void changeToPortrait() {
        vrelativeLayout.setLayoutParams(portraitlayout);
        drelativeLayout.setVisibility(View.VISIBLE);
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        drelativeLayout.setVisibility(View.VISIBLE);
    }

    public void changeToLandscape() {
        vrelativeLayout.setLayoutParams(landscapelayout);
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        drelativeLayout.setVisibility(View.GONE);
    }

    public void showcontrols(final boolean show) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    seekBar.setVisibility(View.VISIBLE);
                    playbutton.setVisibility(View.VISIBLE);
                    controlshow = true;
                } else {
                    seekBar.setVisibility(View.GONE);
                    playbutton.setVisibility(View.GONE);
                    controlshow = false;
                }
            }
        });
    }

    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }

}
