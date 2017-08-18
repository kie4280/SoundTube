package kie.com.soundtube;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchFragment1 extends Fragment {

    private OnFragmentInteractionListener mListener;
    private View fragmentView = null;
    private HandlerThread WorkerThread = null;
    private static Handler WorkHandler = null;
    private Context context;
    public VideoRetriver videoRetriver;
    private ViewPager viewPager;

    MainActivity mainActivity;
    Searcher searcher;
    CustomPagerAdapter pagerAdapter;
    ArrayList<View> pageviews = new ArrayList<>(3);
    ArrayList<Page> pages = new ArrayList<>(3);

    public SearchFragment1() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        WorkerThread = new HandlerThread("WorkThread");
        WorkerThread.start();
        WorkHandler = new Handler(WorkerThread.getLooper());
        searcher = new Searcher(context, WorkHandler);
        videoRetriver = new VideoRetriver(WorkerThread);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_search, container, false);
        pageviews.add(inflater.inflate(R.layout.searchpage, null));
        pageviews.add(inflater.inflate(R.layout.searchpage, null));
        pageviews.add(inflater.inflate(R.layout.searchpage, null));
        pages.add(new Page(pageviews.get(0)));
        pages.add(new Page(pageviews.get(1)));
        pages.add(new Page(pageviews.get(2)));

        viewPager = (ViewPager) fragmentView.findViewById(R.id.searchViewPager);
        pagerAdapter = new CustomPagerAdapter(pageviews);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(onPageChangeListener);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.toolbar.setTranslationY(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        WorkerThread.quit();
    }

    private OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {
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
                    searcher.getResults(new Searcher.YoutubeSearchResult() {
                        @Override
                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                            pagerAdapter.changeSate(hasnext, hasprev);
                            pages.get(0).updateListView(pages.get(1).adapter.dataHolders);
                            pages.get(1).updateListView(data);
                            if (hasnext) {
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewPager.setCurrentItem(1, false);
                                    }
                                });
                                searcher.nextPage();
                                searcher.getResults(new Searcher.YoutubeSearchResult() {
                                    @Override
                                    public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                                        pages.get(2).updateListView(data);
                                    }

                                    @Override
                                    public void noData() {

                                    }
                                });
                                searcher.prevPage();
                            }
                        }

                        @Override
                        public void noData() {

                        }
                    });
                } else if (index < previndex && index < 1) {
                    searcher.prevPage();
                    searcher.getResults(new Searcher.YoutubeSearchResult() {
                        @Override
                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                            pagerAdapter.changeSate(hasnext, hasprev);
                            pages.get(2).updateListView(pages.get(1).adapter.dataHolders);
                            pages.get(1).updateListView(data);
                            if (hasprev) {
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewPager.setCurrentItem(1, false);
                                    }
                                });
                                searcher.prevPage();
                                searcher.getResults(new Searcher.YoutubeSearchResult() {
                                    @Override
                                    public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                                        pages.get(0).updateListView(data);
                                    }

                                    @Override
                                    public void noData() {

                                    }
                                });
                                searcher.nextPage();
                            }

                        }

                        @Override
                        public void noData() {

                        }
                    });
                }


            } else if (state == ViewPager.SCROLL_STATE_DRAGGING) {

            }
        }
    };

    public void search(String term) {
        searcher.newSearch(term);
        pages.get(1).loading();

        searcher.getResults(new Searcher.YoutubeSearchResult() {
            @Override
            public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {

                pagerAdapter.changeSate(hasnext, hasprev);
                pages.get(1).updateListView(data);

                Log.d("searchFragment", "found");

//                if (hasnext) {
//                    pages.get(2).loading();
//                    searcher.nextPage();
//                    searcher.getResults(new Searcher.YoutubeSearchResult() {
//                        @Override
//                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
//                            pages.get(2).updateListView(data);
//                        }
//
//                        @Override
//                        public void noData() {
//
//                        }
//                    });
//                }

            }

            @Override
            public void noData() {

            }
        });

    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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

    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);

        void onreturnVideo(DataHolder dataHolder, Handler handler);
    }

    public void setActivity(MainActivity activity) {
        this.mainActivity = activity;
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

        public SearchRecyclerAdapter adapter = null;
        public RecyclerView recyclerView;
        public ProgressBar progressBar;
        public RelativeLayout relativeLayout;
        boolean waiting = false;


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
            adapter = new SearchRecyclerAdapter(data);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(adapter);
            RecyclerTouchListener listener = new RecyclerTouchListener(context, recyclerView, new OnItemClicked() {
                @Override
                public void onClick(View view, int position) {
                    System.out.println("clicked" + (position - 1));
                    Toast toast = Toast.makeText(context, "Decrypting...", Toast.LENGTH_SHORT);
                    toast.show();
                    final DataHolder dataHolder = data.get(position - 1);
                    videoRetriver.startExtracting("https://www.youtube" +
                            ".com/watch?v=" + dataHolder.videoID, new VideoRetriver.YouTubeExtractorListener() {
                        @Override
                        public void onSuccess(HashMap<Integer, String> result) {
                            dataHolder.videoUris = result;
                            mListener.onreturnVideo(dataHolder, WorkHandler);
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
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {


                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                Log.d("recyclerView dx", Integer.toString(dx));
                    mainActivity.setToolbar(dy);

                }
            });

        }
    }

}