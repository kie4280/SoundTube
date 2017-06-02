package kie.com.soundtube;

import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;


public class VideoFragment1 extends Fragment {

    private OnFragmentInteractionListener mListener;
    public float Displayratio = 16f / 9f;
    public float Videoratio = 16f / 9f;
    public View videoFragmentView;
    public SeekBar seekBar;
    public ProgressBar progressBar;
    public MediaPlayer mediaPlayer;
    public boolean prepared = false;

    private Button playbutton;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private RelativeLayout relativeLayout;
    Handler ui;
    DisplayMetrics displayMetrics;
    Context context;
    FrameLayout.LayoutParams portraitlayout;
    FrameLayout.LayoutParams landscapelayout;
    MediaPlayerService2 mediaService;

    public VideoFragment1() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ui = new Handler(Looper.getMainLooper());
        context = getContext();
        displayMetrics = context.getResources().getDisplayMetrics();

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
        playbutton = (Button) videoFragmentView.findViewById(R.id.playbutton);
        progressBar = (ProgressBar) videoFragmentView.findViewById(R.id.progressBar1);
        progressBar.setMax(100);
        progressBar.setIndeterminate(false);
        relativeLayout = (RelativeLayout) videoFragmentView.findViewById(R.id.relativeLayout);
        landscapelayout = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int w = (int) (displayMetrics.heightPixels / Displayratio);
            portraitlayout = new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, w);
            changeToLandscape();

        } else {
            int w = (int) (displayMetrics.widthPixels / Displayratio);
            portraitlayout = new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, w);
            changeToPortrait();
        }

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
               prepared = true;
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                System.out.println("width " + width + " height " + height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                prepared = false;

            }
        });
//        playbutton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mediaPlayer.isPlaying()) {
//                    pause();
//                    playbutton.setBackgroundResource(R.drawable.play);
////                    ui.postDelayed(new Runnable() {
////                        @Override
////                        public void run() {
////                            playbutton.setVisibility(View.GONE);
////                        }
////                    }, 3000);
//
//
//                } else {
//                    play();
////                    ui.postDelayed(new Runnable() {
////                        @Override
////                        public void run() {
////                            playbutton.setVisibility(View.GONE);
////                        }
////                    }, 3000);
//                    playbutton.setBackgroundResource(R.drawable.pause);
//                }
//            }
//        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if(mediaService.prepared) {
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
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        //mediaPlayer.pause();
        System.out.println("pause");
        super.onPause();
    }

    @Override
    public void onStop() {
        System.out.println("stop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void start(final DataHolder dataHolder, final MediaPlayerService2 mediaService2) {

        mediaService = mediaService2;
        mediaPlayer = mediaService2.mediaPlayer;
        connect();

        ConnectivityManager connectmgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectmgr.getActiveNetworkInfo();
        if (info.isAvailable() && info.isConnected()) {
            for (int a : VideoRetriver.mPreferredVideoQualities) {
                if (dataHolder.videoUris.containsKey(a)) {
                    mediaService2.prepare(dataHolder, a);
                    mediaService2.setDisplay(surfaceHolder);
                    mediaService2.play();

                    break;
                }

            }
        } else {

        }

    }

    public void connect() {
        mediaService.connect(this);
    }



    private MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {

        }
    };

    public void buffering(boolean buff) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                playbutton.setVisibility(View.VISIBLE);
                seekBar.setMax(mediaPlayer.getDuration());
            }
        });

    }

    private MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            System.out.println("update");
            progressBar.setProgress(percent);
        }
    };


    public void changeToPortrait() {
        relativeLayout.setLayoutParams(portraitlayout);
        getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    public void changeToLandscape() {
        relativeLayout.setLayoutParams(landscapelayout);
        getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }

}
