package kie.com.soundtube;

import android.app.Activity;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class VideoFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    public float Displayratio = 16f / 9f;
    public float Videoratio = 16f / 9f;
    private float scaleFactor = 1f;
    private int HeaderDP = 70;
    private float headersize = 0;
    public boolean prepared = false;
    boolean connected = false;
    boolean controlshow = true;
    boolean seekbarUpdating = false;
    private Button playbutton;
    private SurfaceView surfaceView;
    private RelativeLayout vrelativeLayout;
    private RelativeLayout drelativeLayout;
    private RelativeLayout header;
    private View videoFragmentView;
    private SeekBar seekBar;
    private ProgressBar progressBar;
    private TextView currentTime;
    private TextView totalTime;

    private DisplayMetrics displayMetrics;
    private Context context;
    private RelativeLayout.LayoutParams portraitlayout;
    private RelativeLayout.LayoutParams landscapelayout;
    private Activity activity;
    private Handler seekHandler, workHandler;
    private HandlerThread thread;
    private RecyclerView recyclerView;
    private ViewPager viewPager;
    public VideoRetriver videoRetriver;
    public SurfaceHolder surfaceHolder;
    private CustomPagerAdapter pagerAdapter;
    private ProgressBar bar1;
    private ProgressBar bar2;

    MediaPlayerService mediaService;
    MainActivity mainActivity;
    Page page;
    Searcher searcher = null;
    ScaleGestureDetector scaleGestureDetector;
    ArrayList<View> pageviews = new ArrayList<>(3);

    public VideoFragment() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        activity = getActivity();
        displayMetrics = context.getResources().getDisplayMetrics();
        thread = new HandlerThread("seek");
        thread.start();
        seekHandler = new Handler(thread.getLooper());
        videoRetriver = new VideoRetriver(thread);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        videoFragmentView = inflater.inflate(R.layout.fragment_video, container, false);
        surfaceView = (SurfaceView) videoFragmentView.findViewById(R.id.surfaceView);
        seekBar = (SeekBar) videoFragmentView.findViewById(R.id.seekBar);

        currentTime = (TextView) videoFragmentView.findViewById(R.id.currentTime);
        totalTime = (TextView) videoFragmentView.findViewById(R.id.totalTime);
        header = (RelativeLayout) videoFragmentView.findViewById(R.id.headerView);

        viewPager = (ViewPager) videoFragmentView.findViewById(R.id.videoViewPager);
        headersize = Tools.convertDpToPixel(HeaderDP, context);
        playbutton = (Button) videoFragmentView.findViewById(R.id.playbutton);
        progressBar = (ProgressBar) videoFragmentView.findViewById(R.id.bufferProgressBar);
        View r1 = inflater.inflate(R.layout.blank_loading, null);
        View r2 = inflater.inflate(R.layout.blank_loading, null);
        bar1 = (ProgressBar) r1.findViewById(R.id.pageLoadingBar);
        bar2 = (ProgressBar) r2.findViewById(R.id.pageLoadingBar);
        View pageview = inflater.inflate(R.layout.searchpage, null);
        recyclerView = (RecyclerView) pageview.findViewById(R.id.searchrecyclerView);
        mainActivity.slidePanel.setScrollableView(recyclerView);
        pageviews.add(r1);
        pageviews.add(pageview);
        pageviews.add(r2);
        page = new Page(pageviews.get(1));
        pagerAdapter = new CustomPagerAdapter(pageviews);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(onPageChangeListener);

        vrelativeLayout = (RelativeLayout) videoFragmentView.findViewById(R.id.videoRelativeLayout);
        drelativeLayout = (RelativeLayout) videoFragmentView.findViewById(R.id.descriptionRelativeLayout);
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

        playbutton.setOnTouchListener(new View.OnTouchListener() {
            float prevX = 0;
            float prevY = 0;
            float thresholdX = 15f;
            float thresholdY = 15f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                if (action == MotionEvent.ACTION_DOWN) {
//                    mainActivity.viewPager.setSwipingEnabled(false);
                    Log.d("playbutton", "down");
                    prevX = event.getX();
                    prevY = event.getY();
                } else if (action == MotionEvent.ACTION_UP) {
                    if (Math.abs(event.getX() - prevX) <= thresholdX &&
                            Math.abs(event.getY() - prevY) <= thresholdY && mediaService != null) {

                        if (mediaService.mediaPlayer.isPlaying()) {
                            mediaService.pause();
                            setButtonPlay(true);

                        } else {
                            mediaService.play();
                            setButtonPlay(false);
                        }
//                    mainActivity.viewPager.setSwipingEnabled(true);
                    }

                }
                return false;
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

        scaleGestureDetector = new ScaleGestureDetector(vrelativeLayout.getContext(),
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
        vrelativeLayout.setOnTouchListener(touchListener);


        Log.d("video", "createView");

        return videoFragmentView;
    }

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        float prevX = 0;
        float prevY = 0;
        float thresholdX = 15f;
        float thresholdY = 15f;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = MotionEventCompat.getActionMasked(event);
            int pointerIndex = event.getActionIndex();
            int pointerID = event.getPointerId(pointerIndex);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    prevX = event.getX();
                    prevY = event.getY();
                case MotionEvent.ACTION_POINTER_DOWN:
                    Log.d("surface", "touchdown");
                case MotionEvent.ACTION_MOVE:
//                    mainActivity.viewPager.setSwipingEnabled(false);

//                    Log.d("surface", "move");
                    scaleGestureDetector.onTouchEvent(event);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                    if (Math.abs(event.getX() - prevX) <= thresholdX &&
                            Math.abs(event.getY() - prevY) <= thresholdY) {
                        if (controlshow) {
                            showcontrols(false);
                        } else {
                            showcontrols(true);
                        }
                    }

//                    mainActivity.viewPager.setSwipingEnabled(true);
                    Log.d("surface", "touchup");
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
                    searcher.nextPage();
                    page.loading();
                    searcher.getResults(new Searcher.YoutubeSearchResult() {
                        @Override
                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                            pagerAdapter.changeSate(hasnext, hasprev);
                            page.updateListView(data);

                            if (hasnext) {
                                mainActivity.runOnUiThread(new Runnable() {
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
                        public void noData() {

                        }
                    });
                } else if (index < previndex) {
                    searcher.prevPage();
                    page.loading();
                    searcher.getResults(new Searcher.YoutubeSearchResult() {
                        @Override
                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                            pagerAdapter.changeSate(hasnext, hasprev);
                            page.updateListView(data);
                            if (hasprev) {
                                mainActivity.runOnUiThread(new Runnable() {
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
                        public void noData() {

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

            if (mediaService != null) {
                mediaService.reset();
                mediaService.prepare(dataHolder);
                mediaService.setDisplay(surfaceHolder);
                mediaService.play();
                playbutton.setBackgroundResource(R.drawable.pause);
                page.setTitle(dataHolder.title);

                if (searcher != null) {
                    searcher.loadRelatedVideos(dataHolder.videoID);
                    page.loading();
                    searcher.getResults(new Searcher.YoutubeSearchResult() {
                        @Override
                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                            pagerAdapter.changeSate(hasnext, hasprev);
                            page.updateListView(data);
                        }

                        @Override
                        public void noData() {

                        }
                    });

                }


            } else {
                Toast toast = Toast.makeText(context, "No service error!!!", Toast.LENGTH_LONG);
                toast.show();
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
//                totalTime.setText(max);
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
                        int pos = mediaService.mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(pos);
//                        currentTime.setText(pos);
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

    public void setSearchWorker(Handler handler) {
        this.workHandler = handler;
        searcher = new Searcher(context, workHandler);
    }

    public void changeToPortrait() {
        if (vrelativeLayout != null && drelativeLayout != null && activity != null) {
            vrelativeLayout.setLayoutParams(portraitlayout);
            drelativeLayout.setVisibility(View.VISIBLE);
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    public void changeToLandscape() {
        if (vrelativeLayout != null && drelativeLayout != null && activity != null) {
            vrelativeLayout.setLayoutParams(landscapelayout);
            drelativeLayout.setVisibility(View.GONE);
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

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

    public void setHeaderPos(float alpha) {
        header.setAlpha(1 - alpha);
        float offset = headersize * (1 - alpha);
//        header.setTranslationY(offset);
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

    public void setButtonPlay(final boolean play) {
        mainActivity.runOnUiThread(new Runnable() {
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


    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
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
        String text = "error";


        public Page(View page) {
            recyclerView = (RecyclerView) page.findViewById(R.id.searchrecyclerView);
            progressBar = (ProgressBar) page.findViewById(R.id.pageprogressBar);
            relativeLayout = (RelativeLayout) page.findViewById(R.id.pageRL);

//            relativeLayout.setBackground(new TextDrawable());
        }

        public void loading() {
            mainActivity.runOnUiThread(new Runnable() {
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
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        createListView(recyclerView, data);
                    } else {
                        adapter.dataHolders = data;
                        adapter.notifyDataSetChanged();
                    }
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
            adapter.text = text;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(adapter);
            RecyclerTouchListener listener = new RecyclerTouchListener(context, recyclerView, new OnItemClicked() {
                @Override
                public void onClick(View view, int position) {
                    System.out.println("clicked" + (position - 1));
                    Toast toast = Toast.makeText(context, "Decrypting...", Toast.LENGTH_SHORT);
                    toast.show();
                    final DataHolder dataHolder = adapter.dataHolders.get(position - 1);
                    videoRetriver.startExtracting("https://www.youtube" +
                            ".com/watch?v=" + dataHolder.videoID, new VideoRetriver.YouTubeExtractorListener() {
                        @Override
                        public void onSuccess(HashMap<Integer, String> result) {
                            dataHolder.videoUris = result;
                            start(dataHolder);
                            //Log.d("search", ))
                        }

                        @Override
                        public void onFailure(Error error) {
                            Log.d("search", "error extracting");

                        }
                    });
                }

                @Override
                public void onLongClick(View view, int position) {

                }
            });
            recyclerView.addOnItemTouchListener(listener);


        }

        public void setTitle(String t) {
            text = t;
        }
    }

}


