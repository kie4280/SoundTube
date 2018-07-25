package kie.com.soundtube;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class VideoFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    public float Displayratio = 16f / 9f;
    public float Videoratio = 16f / 9f;
    private float scaleFactor = 1f;
    private int HeaderDP = 55;
    private float headersize = 0;
    public boolean prepared = false;
    boolean controlshow = false;
    boolean seekbarUpdating = false;
    private Button playbutton;
    private SurfaceView surfaceView;
    private ConstraintLayout header, relatedVideoLayout, videoPlayerLayout, nextPage, prevPage;
    private View videoFragmentView = null;
    private SeekBar seekBar;
    private ProgressBar videoBufferProgress, relatedBufferProgress;
    private TextView currentTime;
    private TextView totalTime;
    private RecyclerView recyclerView;
    public ImageView headerPlayButton, videoSettingButton, nextPageTab, prevPageTab, nextPageImg, prevPageImg;
    public TextView playingTextView;
    private ScaleGestureDetector scaleGestureDetector;
    private DisplayMetrics displayMetrics;
    private Context context;
    private ConstraintLayout.LayoutParams portraitlayout;
    private ConstraintLayout.LayoutParams landscapelayout;
    private Handler seekHandler, workHandler;
    private HandlerThread thread;
    public VideoRetriever videoRetriever;
    public SurfaceHolder surfaceHolder;
    public DataHolder currentData;
    private boolean started = false;
    boolean waiting = false;
    boolean m_hasNext = true;
    boolean m_hasPrev = true;
    int index;
    VideoRecyclerAdapter videoAdapter;
    MediaPlayerService mediaService;
    YoutubeClient youtubeClient = null;
    String text = "onError";


    public VideoFragment() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        workHandler = ((MainActivity) context).workHandler;
        youtubeClient = new YoutubeClient(context, workHandler);
        videoRetriever = new VideoRetriever(context, workHandler);
        displayMetrics = context.getResources().getDisplayMetrics();
        thread = new HandlerThread("seek");
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        seekHandler = new Handler(thread.getLooper());
        scaleGestureDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.OnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        Log.d("scale", Float.toString(scaleFactor));
                        scaleFactor *= detector.getScaleFactor();
                        scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));
                        return true;
                    }

                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        return true;
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector detector) {

                    }
                });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (videoFragmentView == null) {

            videoFragmentView = inflater.inflate(R.layout.fragment_video, container, false);
            surfaceView = (SurfaceView) videoFragmentView.findViewById(R.id.surfaceView);
            seekBar = (SeekBar) videoFragmentView.findViewById(R.id.seekBar);
            playingTextView = (TextView) videoFragmentView.findViewById(R.id.playingTextView);
            currentTime = (TextView) videoFragmentView.findViewById(R.id.currentTime);
            totalTime = (TextView) videoFragmentView.findViewById(R.id.totalTime);
            header = (ConstraintLayout) videoFragmentView.findViewById(R.id.headerView);
            headerPlayButton = (ImageView) videoFragmentView.findViewById(R.id.headerPlayButton);
            videoSettingButton = (ImageView) videoFragmentView.findViewById(R.id.videoSettingButton);
            recyclerView = (RecyclerView) videoFragmentView.findViewById(R.id.videoRecyclerView);
            headersize = Tools.convertDpToPixel(HeaderDP, context);
            playbutton = (Button) videoFragmentView.findViewById(R.id.playbutton);
            videoBufferProgress = (ProgressBar) videoFragmentView.findViewById(R.id.bufferProgressBar);
            relatedBufferProgress = (ProgressBar) videoFragmentView.findViewById(R.id.loadingProgress);
            nextPageImg = (ImageView) videoFragmentView.findViewById(R.id.nextPageImage);
            prevPageImg = (ImageView) videoFragmentView.findViewById(R.id.prevPageImage);
            nextPage = (ConstraintLayout) videoFragmentView.findViewById(R.id.nextPage);
            prevPage = (ConstraintLayout) videoFragmentView.findViewById(R.id.prevPage);
            nextPageTab = (ImageView) videoFragmentView.findViewById(R.id.nextPageTab);
            prevPageTab = (ImageView) videoFragmentView.findViewById(R.id.prevPageTab);

            nextPage.setX(displayMetrics.widthPixels);
            prevPage.setX(-displayMetrics.widthPixels);
            nextPageTab.setOnTouchListener(nextPageTouchListener);
            prevPageTab.setOnTouchListener(prevPageTouchListsner);
            nextPageImg.setImageDrawable(new TextDrawable("3"));


            videoPlayerLayout = (ConstraintLayout) videoFragmentView.findViewById(R.id.videoPlayerLayout);
            relatedVideoLayout = (ConstraintLayout) videoFragmentView.findViewById(R.id.RelatedVideoLayout);
            ConstraintLayout.LayoutParams orig = (ConstraintLayout.LayoutParams) videoPlayerLayout.getLayoutParams();
            orig.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
            landscapelayout = new ConstraintLayout.LayoutParams(orig);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                orig.height = (int) (displayMetrics.heightPixels / Displayratio);
                portraitlayout = new ConstraintLayout.LayoutParams(orig);
                changeToLandscape();

            } else {
                orig.height = (int) (displayMetrics.widthPixels / Displayratio);
                portraitlayout = new ConstraintLayout.LayoutParams(orig);
                changeToPortrait();
            }

            surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    prepared = true;
                    if (mediaService != null) {
                        mediaService.setDisplay(holder);
                        updateSeekBar();
                    }

                    Log.d("video", "surfaceCreated");
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                System.out.println("width " + width + " height " + height);
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    prepared = false;
                    Log.d("video", "surfaceDestroyed");
                    if (mediaService != null) {
                        mediaService.setDisplay(null);
                    }

                }
            });
            playbutton.setOnTouchListener(buttonTouchListener);
            videoPlayerLayout.setOnTouchListener(videoTouchListener);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                    if (mediaService != null && mediaService.prepared && fromUser) {
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
            headerPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mediaService != null && started) {
                        if (mediaService.isPlaying()) {
                            mediaService.pause();
                            headerPlayButton.setImageResource(R.drawable.play);
                        } else {
                            mediaService.play();
                            headerPlayButton.setImageResource(R.drawable.pause);
                        }
                    }
                }
            });

        }

        Log.d("video", "createView");

        return videoFragmentView;
    }

    private View.OnTouchListener buttonTouchListener = new View.OnTouchListener() {
        float prevX = 0;
        float prevY = 0;
        float thresholdX = 15f;
        float thresholdY = 15f;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
//                    playerActivity.viewPager.setSwipingEnabled(false);
                Log.d("playbutton", "down");
                prevX = event.getX();
                prevY = event.getY();
            } else if (action == MotionEvent.ACTION_UP) {

                if (Math.abs(event.getX() - prevX) <= thresholdX &&
                        Math.abs(event.getY() - prevY) <= thresholdY && mediaService != null) {

                    if (mediaService.isPlaying()) {
                        mediaService.pause();


                    } else {
                        mediaService.play();

                    }
//                    playerActivity.viewPager.setSwipingEnabled(true);
                }

            }

            return false;
        }


    };

    private View.OnTouchListener videoTouchListener = new View.OnTouchListener() {
        float prevX = 0;
        float prevY = 0;
        float thresholdX = 15f;
        float thresholdY = 15f;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            int pointerIndex = event.getActionIndex();
            int pointerID = event.getPointerId(pointerIndex);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    prevX = event.getX();
                    prevY = event.getY();
//                    playerActivity.slidePanel.setTouchEnabled(true);
                    Log.d("videotouch", "touchdown");
                case MotionEvent.ACTION_POINTER_DOWN:

                case MotionEvent.ACTION_MOVE:
//                    playerActivity.viewPager.setSwipingEnabled(false);

//                    Log.d("surface", "move");
                    scaleGestureDetector.onTouchEvent(event);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    Log.d("videotouch", "pointer touchup");
                case MotionEvent.ACTION_UP:
                    if (Math.abs(event.getX() - prevX) <= thresholdX &&
                            Math.abs(event.getY() - prevY) <= thresholdY) {
                        if (controlshow) {
                            showcontrols(false);
                        } else {
                            showcontrols(true);
                        }
                    }

//                    playerActivity.viewPager.setSwipingEnabled(true);
                    Log.d("videotouch", "touchup");
//                    playerActivity.slidePanel.setTouchEnabled(false);
                    break;
            }


            return true;
        }
    };

    private View.OnTouchListener nextPageTouchListener = new View.OnTouchListener() {
        float OrgX;
        float half;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    float delta = displayMetrics.widthPixels / 5 * 2;

                    if (nextPageTab.getX() <= delta) {
                        index++;
                        nextPageTab.setX(OrgX);
                        nextPage.animate().x(0).setDuration(200).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                nextPage.animate().alpha(0).setDuration(200)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                nextPage.setX(displayMetrics.widthPixels);
                                                nextPage.setAlpha(1);
                                                nextPageImg.setImageDrawable(new TextDrawable(Integer.toString(index + 1)));
                                                prevPageImg.setImageDrawable(new TextDrawable(Integer.toString(index - 1)));
                                            }
                                        });
                            }
                        });

                        youtubeClient.nextVideoPage();
                        loading();
                        getSearchResult();
                    } else {
                        nextPageTab.animate().x(OrgX).setDuration(100);
                        nextPage.animate().x(displayMetrics.widthPixels).setDuration(100);
                    }

                    break;

                case MotionEvent.ACTION_DOWN:
                    half = nextPageTab.getWidth() / 2;
                    OrgX = displayMetrics.widthPixels - half * 2;
                    nextPageTab.clearAnimation();
                    nextPage.clearAnimation();
                case MotionEvent.ACTION_MOVE:
                    float x = event.getRawX();
                    if ((x + half) < displayMetrics.widthPixels) {
                        nextPageTab.setX(x - half);
                    }
                    nextPage.setX(x + half);
                    break;

            }

            return true;
        }
    };

    private View.OnTouchListener prevPageTouchListsner = new View.OnTouchListener() {
        float OrgX;
        float half;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    float delta = displayMetrics.widthPixels / 5 * 3;
                    if (prevPageTab.getX() >= delta) {
                        index = index - 1 > 1 ? index - 1 : 1;
                        prevPageTab.setX(OrgX);
                        prevPage.animate().x(0).setDuration(200).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                prevPage.animate().alpha(0).setDuration(200)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                prevPage.setX(-displayMetrics.widthPixels);
                                                prevPage.setAlpha(1);
                                                nextPageImg.setImageDrawable(new TextDrawable(Integer.toString(index + 1)));
                                                prevPageImg.setImageDrawable(new TextDrawable(Integer.toString(index - 1)));

                                            }
                                        });
                            }
                        });


                        youtubeClient.prevVideoPage();
                        loading();
                        getSearchResult();
                    } else {
                        prevPageTab.animate().x(OrgX).setDuration(100);
                        prevPage.animate().x(-displayMetrics.widthPixels).setDuration(100);
                    }

                    break;

                case MotionEvent.ACTION_DOWN:

                    OrgX = prevPageTab.getX();
                    half = prevPageTab.getWidth() / 2;
                    prevPageTab.clearAnimation();
                    prevPage.clearAnimation();
                case MotionEvent.ACTION_MOVE:
                    float x = event.getRawX();
                    if ((x - half) > 0) {
                        prevPageTab.setX(x - half);
                    }
                    prevPage.setX(-displayMetrics.widthPixels + x - half);
                    break;

            }

            return true;
        }
    };



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
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
    public void onStart() {
        super.onStart();
//        playerActivity.connect();
        started = true;


    }

    @Override
    public void onStop() {
        super.onStop();
//        playerActivity.disconnect();
        started = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.quitSafely();
    }

    public void start(final DataHolder dataHolder) {

        currentData = dataHolder;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playingTextView.setText(currentData.title);
            }
        });

        seekHandler.post(new Runnable() {
            @Override
            public void run() {
                ConnectivityManager connectmgr = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = connectmgr.getActiveNetworkInfo();
                if (info.isAvailable() && info.isConnected()) {

                    if (mediaService != null) {
                        mediaService.prepare(dataHolder);
                        mediaService.setDisplay(surfaceHolder);
                        loadRelatedVideos(dataHolder);
                    }

                } else {
                    Toast toast = Toast.makeText(context, getString(R.string.needNetwork), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

    }

    public void loadRelatedVideos(DataHolder dataHolder) {
        setTitle(dataHolder.title);
        if (youtubeClient != null) {
            index = 1;
            nextPageImg.setImageDrawable(new TextDrawable(Integer.toString(index + 1)));
            youtubeClient.loadRelatedVideos(dataHolder.videoID);
            loading();
            youtubeClient.getVideoSearchResults(new YoutubeClient.YoutubeVideoSearchResult() {
                @Override
                public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                    changeSate(hasnext, hasprev);
                    updateListView(data);
                }

                @Override
                public void onError(String error) {

                }


            });

        }
    }

    public void getSearchResult() {

        youtubeClient.getVideoSearchResults(new YoutubeClient.YoutubeVideoSearchResult() {
            @Override
            public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                changeSate(hasnext, hasprev);
                updateListView(data);
            }

            @Override
            public void onError(String error) {
                Log.d("SearchFramgent", "error: " + error);

            }

        });
    }

    public void resume() {


    }

    public void buffering(final boolean buff) {
        if (started) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (buff) {
                        videoBufferProgress.setVisibility(View.VISIBLE);
                    } else {
                        videoBufferProgress.setVisibility(View.GONE);
                    }

                }
            });
        }

    }

    public void setSeekBarMax(final int max) {
        if (started) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    seekBar.setMax(max);
//                totalTime.setText(max);
                }
            });
        }
    }

    public void updateSeekBar() {
        Log.d("VideoFragment", "updateSeekBar");
        if (!seekbarUpdating && started) {
            seekHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mediaService != null && mediaService.updateSeekBar && started) {
                        int pos = mediaService.getCurrentPos();
                        seekBar.setProgress(pos);
//                        currentTime.setText(pos);
                        seekHandler.postDelayed(this, 1000);
                        seekbarUpdating = true;
                    } else {
                        seekbarUpdating = false;
                    }
                }
            });
        }
    }

    public void onComplete() {

        if (started) {
            seekHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setButtonPlay(true);
                            showcontrols(true);
                            seekBar.setProgress(0);

                        }
                    });
                }
            }, 200);
        }

    }

    public void changeToPortrait() {
        if (videoPlayerLayout != null && relatedVideoLayout != null && getActivity() != null) {
            videoPlayerLayout.setLayoutParams(portraitlayout);
            relatedVideoLayout.setVisibility(View.VISIBLE);
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    public void changeToLandscape() {
        if (videoPlayerLayout != null && relatedVideoLayout != null && getActivity() != null) {
            videoPlayerLayout.setLayoutParams(landscapelayout);
            relatedVideoLayout.setVisibility(View.GONE);
            getActivity().getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

    }

    public void showcontrols(final boolean show) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    seekBar.setVisibility(View.VISIBLE);
                    playbutton.setVisibility(View.VISIBLE);
                    videoSettingButton.setVisibility(View.VISIBLE);
                    controlshow = true;
                } else {
                    seekBar.setVisibility(View.GONE);
                    playbutton.setVisibility(View.GONE);
                    videoSettingButton.setVisibility(View.GONE);
                    controlshow = false;
                }
            }
        });
    }

    public void setHeaderPos(float alpha) {
//        header.animate().alpha(1 - alpha).withLayer();
        float offset = headersize * (1 - alpha);
        videoPlayerLayout.setTranslationY(offset);
        relatedVideoLayout.setTranslationY(offset);
    }

    public void setHeaderVisible(boolean visible) {
        if (header != null) {
            if (visible) {
                header.setVisibility(View.VISIBLE);
            } else {
                header.setVisibility(View.GONE);
            }

        }
    }

    public void loading() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!waiting) {
                    waiting = true;
                    recyclerView.setVisibility(View.GONE);
                    relatedBufferProgress.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    public void updateListView(final List<DataHolder> data) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (videoAdapter == null) {
                    createListView(recyclerView, data);
                } else {
                    videoAdapter.dataHolders = data;
                    videoAdapter.title = text;
                    videoAdapter.notifyDataSetChanged();

                }
                recyclerView.scrollToPosition(0);
                if (waiting) {
                    waiting = false;
                    recyclerView.setVisibility(View.VISIBLE);
                    relatedBufferProgress.setVisibility(View.GONE);
                }
            }
        });

    }

    private void createListView(final RecyclerView recyclerView, final List<DataHolder> data) {

//        Log.d("createlist", "create");
        videoAdapter = new VideoRecyclerAdapter(data);
        videoAdapter.title = text;
        DividerItemDecoration decoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(decoration);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(videoAdapter);
        RecyclerTouchListener listener = new RecyclerTouchListener(context, recyclerView, new OnItemClicked() {
            @Override
            public void onClick(View view, int position) {
                if (position > 0) {
                    System.out.println("clicked" + (position - 1));
                    Toast toast = Toast.makeText(context, "Decrypting...", Toast.LENGTH_SHORT);
                    toast.show();
                    final DataHolder dataHolder = videoAdapter.dataHolders.get(position - 1);
                    videoRetriever.getDataHolder(dataHolder, new VideoRetriever.YouTubeExtractorListener() {

                        @Override
                        public void onSuccess(DataHolder result) {
                            start(result);
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.d("search", "onError extracting");

                        }
                    });
                }

            }

            @Override
            public void onLongClick(View view, int position) {
                Log.d("videoFragment", "Long click");

            }
        });
        CustomSlideUpPanel slideUpPanel = ((MainActivity) context).slidePanel;
        listener.setSlidePanel(slideUpPanel);
        recyclerView.addOnItemTouchListener(listener);

    }

    public void setTitle(String t) {
        text = t;
    }

    public void nextPageEnabled(final boolean enable) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nextPageTab.setImageResource(enable ? R.drawable.page_tab_enabled : R.drawable.page_tab_disabled);
                nextPageTab.setEnabled(enable);
            }
        });

    }

    public void prevPageEnabled(final boolean enable) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                prevPageTab.setImageResource(enable ? R.drawable.page_tab_enabled : R.drawable.page_tab_disabled);
                prevPageTab.setEnabled(enable);
            }
        });

    }


    public void changeSate(boolean hasNext, boolean hasPrev) {
        if (hasNext != m_hasNext) {
            nextPageEnabled(hasNext);
            m_hasNext = hasNext;
        }
        if (hasPrev != m_hasPrev) {
            prevPageEnabled(hasPrev);
            m_hasPrev = hasPrev;
        }
    }

    public void serviceConnected() {
        if (mediaService != null) {
            if (mediaService.isPlaying()) {
                mediaService.updateSeekBar = true;
                setSeekBarMax(mediaService.getDuration());
                updateSeekBar();
                setButtonPlay(false);
                setHeaderPlayButton(false);
            } else {
                mediaService.updateSeekBar = false;
                setButtonPlay(true);
                setHeaderPlayButton(true);
            }
            if (mediaService.currentData != null) {
                currentData = mediaService.currentData;
            }
            if (currentData != null && mediaService.isPlaying()) {
//            playerActivity.slidePanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED); //optional
                loadRelatedVideos(currentData);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playingTextView.setText(currentData.title);
                    }
                });
            }
        }


    }

    public void serviceDisconnected() {
        mediaService = null;
    }

    public void setButtonPlay(final boolean play) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (play) {
                    playbutton.setBackgroundResource(R.drawable.play);
                } else {
                    playbutton.setBackgroundResource(R.drawable.pause);
                }
            }
        });
    }

    public void setHeaderPlayButton(final boolean play) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (play) {
                    headerPlayButton.setImageResource(R.drawable.play);
                } else {
                    headerPlayButton.setImageResource(R.drawable.pause);
                }
            }
        });
    }

    public boolean previousVideo() {

        final DataHolder dataHolder = mediaService.watchedQueue.pollLast();
        if (dataHolder != null) {
            mediaService.previousVideo();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    playingTextView.setText(dataHolder.title);
                }
            });

            seekHandler.post(new Runnable() {
                @Override
                public void run() {
                    ConnectivityManager connectmgr = (ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo info = connectmgr.getActiveNetworkInfo();
                    if (info.isAvailable() && info.isConnected()) {

                        if (mediaService != null) {
                            mediaService.prepare(dataHolder);
                            mediaService.setDisplay(surfaceHolder);
                            loadRelatedVideos(dataHolder);
                        }

                    } else {
                        Toast toast = Toast.makeText(context, getString(R.string.needNetwork), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            });
            return true;
        } else
            return false;

    }


    public interface OnFragmentInteractionListener {

        void onStartVideo(DataHolder dataHolder);
    }

    private class TextDrawable extends Drawable {

        private final String text;
        private final Paint paint;

        public TextDrawable(String text) {

            this.text = text;
            this.paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(220f);
            paint.setAntiAlias(true);
            paint.setFakeBoldText(true);
            paint.setShadowLayer(6f, 0, 0, Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawText(text, 170, 200, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            paint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

}


