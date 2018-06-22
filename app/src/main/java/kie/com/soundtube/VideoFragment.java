package kie.com.soundtube;

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
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
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
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class VideoFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    public float Displayratio = 16f / 9f;
    public float Videoratio = 16f / 9f;
    private float scaleFactor = 1f;
    private int HeaderDP = 70;
    private float headersize = 0;
    public boolean prepared = false;
    boolean controlshow = false;
    boolean seekbarUpdating = false;
    private Button playbutton;
    private SurfaceView surfaceView;
    private RelativeLayout vrelativeLayout;
    private RelativeLayout drelativeLayout;
    private RelativeLayout header;
    private View videoFragmentView = null;
    private SeekBar seekBar;
    private ProgressBar progressBar;
    private TextView currentTime;
    private TextView totalTime;
    public ImageView headerPlayButton, videoSettingButton;
    public TextView playingTextView;
    private ScaleGestureDetector scaleGestureDetector;
    private DisplayMetrics displayMetrics;
    private Context context;
    private RelativeLayout.LayoutParams portraitlayout;
    private RelativeLayout.LayoutParams landscapelayout;

    private Handler seekHandler;
    private HandlerThread thread;

    private CustomViewPager viewPager;
    public VideoRetriever videoRetriever;
    public SurfaceHolder surfaceHolder;
    public DataHolder currentData;
    private CustomPagerAdapter pagerAdapter;
    private ProgressBar bar1;
    private ProgressBar bar2;
    private boolean started = false;

    MediaPlayerService mediaService;

    Page page;
    YoutubeClient youtubeClient = null;
    ArrayList<View> pageviews = new ArrayList<>(3);


    public VideoFragment() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity().getApplicationContext();

        youtubeClient = new YoutubeClient(context, SearchActivity.workHandler);
        videoRetriever = new VideoRetriever(context, SearchActivity.workHandler);
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
            header = (RelativeLayout) videoFragmentView.findViewById(R.id.headerView);
            headerPlayButton = (ImageView) videoFragmentView.findViewById(R.id.headerPlayButton);
            videoSettingButton = (ImageView) videoFragmentView.findViewById(R.id.videoSettingButton);
            viewPager = (CustomViewPager) videoFragmentView.findViewById(R.id.videoViewPager);
            headersize = Tools.convertDpToPixel(HeaderDP, context);
            playbutton = (Button) videoFragmentView.findViewById(R.id.playbutton);
            progressBar = (ProgressBar) videoFragmentView.findViewById(R.id.bufferProgressBar);
            View r1 = inflater.inflate(R.layout.blank_loading, null);
            View r2 = inflater.inflate(R.layout.blank_loading, null);
            bar1 = (ProgressBar) r1.findViewById(R.id.pageLoadingBar);
            bar2 = (ProgressBar) r2.findViewById(R.id.pageLoadingBar);
            View pageview = inflater.inflate(R.layout.related_video_layout, null);
            pageviews.add(r1);
            pageviews.add(pageview);
            pageviews.add(r2);
            page = new Page(pageviews.get(1));
            pagerAdapter = new CustomPagerAdapter(pageviews);
            viewPager.setAdapter(pagerAdapter);
            viewPager.addOnPageChangeListener(onPageChangeListener);

            vrelativeLayout = (RelativeLayout) videoFragmentView.findViewById(R.id.videoRelativeLayout);
            drelativeLayout = (RelativeLayout) videoFragmentView.findViewById(R.id.RelatedVideoLayout);
            RelativeLayout.LayoutParams orig = (RelativeLayout.LayoutParams) vrelativeLayout.getLayoutParams();
            orig.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            landscapelayout = new RelativeLayout.LayoutParams(orig);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                int w = (int) (displayMetrics.heightPixels / Displayratio);
                orig.height = w;
                portraitlayout = new RelativeLayout.LayoutParams(orig);
                changeToLandscape();

            } else {
                int w = (int) (displayMetrics.widthPixels / Displayratio);
                orig.height = w;
                portraitlayout = new RelativeLayout.LayoutParams(orig);
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
            vrelativeLayout.setOnTouchListener(videoTouchListener);
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

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        int previndex = 0;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            previndex = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {

            if (state == ViewPager.SCROLL_STATE_SETTLING) {

                final int index = viewPager.getCurrentItem();
                Log.d("viewpager", Integer.toString(index));
                if (index > previndex) {
                    youtubeClient.nextVideoPage();
                    page.loading();
                    youtubeClient.getVideoSearchResults(new YoutubeClient.YoutubeVideoSearchResult() {
                        @Override
                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                            pagerAdapter.changeSate(hasnext, hasprev);
                            page.updateListView(data);

                            if (hasnext) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewPager.setCurrentItem(1, false);
                                        bar1.setVisibility(View.GONE);
                                        bar2.setVisibility(View.GONE);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(String error) {

                        }


                    });
                } else if (index < previndex) {
                    youtubeClient.prevVideoPage();
                    page.loading();
                    youtubeClient.getVideoSearchResults(new YoutubeClient.YoutubeVideoSearchResult() {
                        @Override
                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                            pagerAdapter.changeSate(hasnext, hasprev);
                            page.updateListView(data);
                            if (hasprev) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewPager.setCurrentItem(1, false);
                                        bar1.setVisibility(View.GONE);
                                        bar2.setVisibility(View.GONE);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(String error) {

                        }


                    });
                } else if (index == previndex) {
                    bar1.setVisibility(View.GONE);
                    bar2.setVisibility(View.GONE);
                }


            } else if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                bar1.setVisibility(View.VISIBLE);
                bar2.setVisibility(View.VISIBLE);

            }
        }
    };

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
        page.setTitle(dataHolder.title);
        if (youtubeClient != null) {
            youtubeClient.loadRelatedVideos(dataHolder.videoID);
            page.loading();
            youtubeClient.getVideoSearchResults(new YoutubeClient.YoutubeVideoSearchResult() {
                @Override
                public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                    pagerAdapter.changeSate(hasnext, hasprev);
                    page.updateListView(data);
                }

                @Override
                public void onError(String error) {

                }


            });

        }
    }

    public void resume() {


    }

    public void buffering(final boolean buff) {
        if (started) {
            getActivity().runOnUiThread(new Runnable() {
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
        if (vrelativeLayout != null && drelativeLayout != null && getActivity() != null) {
            vrelativeLayout.setLayoutParams(portraitlayout);
            drelativeLayout.setVisibility(View.VISIBLE);
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    public void changeToLandscape() {
        if (vrelativeLayout != null && drelativeLayout != null && getActivity() != null) {
            vrelativeLayout.setLayoutParams(landscapelayout);
            drelativeLayout.setVisibility(View.GONE);
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
        vrelativeLayout.setTranslationY(offset);
        drelativeLayout.setTranslationY(offset);
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
            paint.setTextSize(22f);
            paint.setAntiAlias(true);
            paint.setFakeBoldText(true);
            paint.setShadowLayer(6f, 0, 0, Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawText(text, 0, 0, paint);
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

    private class Page {

        public VideoRecyclerAdapter adapter = null;
        public RecyclerView recyclerView;
        public ProgressBar progressBar;
        public RelativeLayout relativeLayout;
        boolean waiting = false;
        String text = "onError";


        public Page(View page) {
            recyclerView = (RecyclerView) page.findViewById(R.id.searchrecyclerView);
            progressBar = (ProgressBar) page.findViewById(R.id.pageprogressBar);
            relativeLayout = (RelativeLayout) page.findViewById(R.id.pageRL);

//            relativeLayout.setBackground(new TextDrawable());
        }

        public void loading() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!waiting) {
                        waiting = true;
                        recyclerView.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
            });

        }

        public void updateListView(final List<DataHolder> data) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        createListView(recyclerView, data);
                    } else {
                        adapter.dataHolders = data;
                        adapter.title = text;
                        adapter.notifyDataSetChanged();

                    }
                    recyclerView.scrollToPosition(0);
                    if (waiting) {
                        waiting = false;
                        recyclerView.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                }
            });

        }

        private void createListView(final RecyclerView recyclerView, final List<DataHolder> data) {

//        Log.d("createlist", "create");
            adapter = new VideoRecyclerAdapter(data);
            adapter.title = text;
            DividerItemDecoration decoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            recyclerView.addItemDecoration(decoration);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(adapter);
            RecyclerTouchListener listener = new RecyclerTouchListener(context, recyclerView, new OnItemClicked() {
                @Override
                public void onClick(View view, int position) {
                    if (position > 0) {
                        System.out.println("clicked" + (position - 1));
                        Toast toast = Toast.makeText(context, "Decrypting...", Toast.LENGTH_SHORT);
                        toast.show();
                        final DataHolder dataHolder = adapter.dataHolders.get(position - 1);
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
            CustomSlideUpPanel slideUpPanel = ((SearchActivity) getActivity()).slidePanel;
            viewPager.setSlidePanel(slideUpPanel);
            listener.setSlidePanel(slideUpPanel);
            recyclerView.addOnItemTouchListener(listener);

        }

        public void setTitle(String t) {
            text = t;
        }
    }

}


