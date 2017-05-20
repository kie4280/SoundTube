package kie.com.soundtube;

import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.io.IOException;

public class VideoFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    public float Displayratio = 16f / 9f;
    public float Videoratio = 16f / 9f;
    public View videoFragmentView;
    MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    Handler playHandler;
    HandlerThread thread;
    DisplayMetrics displayMetrics;
    Context context;
    RelativeLayout.LayoutParams portraitlayout;
    RelativeLayout.LayoutParams landscapelayout;

    public VideoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thread = new HandlerThread("playerThread");
        thread.start();
        playHandler = new Handler(thread.getLooper());
        context = getContext();
        displayMetrics = context.getResources().getDisplayMetrics();
        mediaPlayer = new MediaPlayer();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                surfaceView.setLayoutParams(landscapelayout);
                getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                surfaceView.setLayoutParams(portraitlayout);
                getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
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
        landscapelayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int w = (int) (displayMetrics.heightPixels / Displayratio);
            portraitlayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, w);
            surfaceView.setLayoutParams(landscapelayout);
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            int w = (int) (displayMetrics.widthPixels / Displayratio);
            portraitlayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, w);
            surfaceView.setLayoutParams(portraitlayout);
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mediaPlayer.setDisplay(surfaceHolder);
                System.out.println("surfacecreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                System.out.println("width " + width + " height " + height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

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
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
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
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        super.onDestroy();
    }

    public void playVideo(final DataHolder dataHolder) {
        playHandler.post(new Runnable() {
            @Override
            public void run() {

                try {
                    if (dataHolder.videoUri != null) {
                        mediaPlayer.setDataSource(context, dataHolder.videoUri);
                        mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                        mediaPlayer.setOnPreparedListener(onPreparedListener);
                        mediaPlayer.prepareAsync();

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            Videoratio = (float)mp.getVideoWidth() / (float)mp.getVideoHeight();
            mp.start();
        }
    };

    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }

}
