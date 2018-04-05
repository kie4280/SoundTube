package kie.com.soundtube;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.mp4parser.Container;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.mp4parser.muxer.tracks.h264.H264TrackImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private View searchFragmentView = null;
    private Handler workHandler = null;
    private Context context;
    public VideoRetriever videoRetriever;
    private ViewPager viewPager;
    private ProgressBar bar1, bar2;
    public Button searchButton;
    public Toolbar playerToolbar;
    public AppBarLayout appBarLayout = null;
    public SearchView searchView = null;
    public LinearLayout searchAreaView = null;
    ImageView blankspace;
    PlayerActivity playerActivity;
    YoutubeClient youtubeClient;
    CustomPagerAdapter pagerAdapter;
    ArrayList<View> pageviews = new ArrayList<>(3);
    ArrayList<String> suggests;
    Page page;
    String queryUrl = "http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&client=firefox&q=";

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity().getApplicationContext();
        setHasOptionsMenu(true);
        workHandler = PlayerActivity.workHandler;
        youtubeClient = new YoutubeClient(context, workHandler);
        videoRetriever = new VideoRetriever(workHandler);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        if (searchFragmentView == null) {

            searchFragmentView = inflater.inflate(R.layout.fragment_search, container, false);
            playerToolbar = playerActivity.playerToolbar;
            Log.d("searchfragment", "createview");
            View r1 = inflater.inflate(R.layout.blank_loading, null);
            View r2 = inflater.inflate(R.layout.blank_loading, null);
            bar1 = (ProgressBar) r1.findViewById(R.id.pageLoadingBar);
            bar2 = (ProgressBar) r2.findViewById(R.id.pageLoadingBar);
            pageviews.add(r1);
            pageviews.add(inflater.inflate(R.layout.related_video_layout, null));
            pageviews.add(r2);
            page = new Page(pageviews.get(1));
            viewPager = (ViewPager) searchFragmentView.findViewById(R.id.searchViewPager);
            pagerAdapter = new CustomPagerAdapter(pageviews);
            viewPager.setAdapter(pagerAdapter);
            viewPager.addOnPageChangeListener(onPageChangeListener);
            createSearchView();
        }

        return searchFragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }


    private OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {
        int previndex = 0;
        boolean user = false;

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
                if (index > previndex && user) {
                    youtubeClient.nextPage();
                    page.loading();
                    youtubeClient.getResults(new YoutubeClient.YoutubeSearchResult() {
                        @Override
                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                            pagerAdapter.changeSate(hasnext, hasprev);
                            page.updateListView(data);

                            if (hasnext) {
                                playerActivity.runOnUiThread(new Runnable() {
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
                } else if (index < previndex && user) {
                    youtubeClient.prevPage();
                    page.loading();
                    youtubeClient.getResults(new YoutubeClient.YoutubeSearchResult() {
                        @Override
                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                            pagerAdapter.changeSate(hasnext, hasprev);
                            page.updateListView(data);
                            if (hasprev) {
                                playerActivity.runOnUiThread(new Runnable() {
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
                } else if (index == previndex && user) {
                    bar1.setVisibility(View.GONE);
                    bar2.setVisibility(View.GONE);
                }

                user = false;

            } else if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                bar1.setVisibility(View.VISIBLE);
                bar2.setVisibility(View.VISIBLE);
                user = true;

            }
        }
    };

    public void search(String term) {
        youtubeClient.newSearch(term);
        page.loading();

        youtubeClient.getResults(new YoutubeClient.YoutubeSearchResult() {
            @Override
            public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {

                pagerAdapter.changeSate(hasnext, hasprev);
                page.updateListView(data);
                Log.d("searchFragment", "found");
            }

            @Override
            public void noData() {

            }
        });

    }

    public boolean previousSearch() {
        return true;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
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
        Log.d("SearchFragment", "onDetach");
    }

    public interface OnFragmentInteractionListener {

        void onReturnSearchVideo(DataHolder dataHolder);
    }

    public void setActivity(PlayerActivity activity) {
        this.playerActivity = activity;
    }

    public void createSearchView() {
        if (searchAreaView == null) {
            searchAreaView = playerActivity.searchArea;
            appBarLayout = playerActivity.appBarLayout;
            blankspace = searchAreaView.findViewById(R.id.blankspace);
            searchView = searchAreaView.findViewById(R.id.searchview);
            int autoCompleteTextViewID = getResources().getIdentifier("android:id/search_src_text", null, null);
            final AutoCompleteTextView searchAutoCompleteTextView = (AutoCompleteTextView) searchView.findViewById(autoCompleteTextViewID);
            searchAutoCompleteTextView.setThreshold(1);

            blankspace.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        animateSearchArea(false);
                    }
                    return true;
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    System.out.println("submit");
                    if (query != null) {
                        if (PlayerActivity.netConncted) {
                            search(query);
                            Log.d("search", query);

                        } else {
                            Toast toast = Toast.makeText(context, getString(R.string.needNetwork), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        animateSearchArea(false);

                    }
                    return false;
                }

                @Override
                public boolean onQueryTextChange(final String newText) {

                    workHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("searchFragment", "text changed");
                            if (PlayerActivity.netConncted && newText.length() != 0) {
                                StringBuilder response = new StringBuilder();
                                try {
                                    URL url = new URL(queryUrl + URLEncoder.encode(newText, "UTF-8"));
                                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                                    httpURLConnection.setRequestMethod("GET");
                                    BufferedReader in = new BufferedReader(
                                            new InputStreamReader(httpURLConnection.getInputStream()));
                                    String inputLine;

                                    while ((inputLine = in.readLine()) != null) {
                                        response.append(inputLine);
                                    }
                                    in.close();
                                    httpURLConnection.disconnect();
                                    JsonArray jsonArray = new JsonParser().parse(response.toString()).getAsJsonArray();
                                    jsonArray = jsonArray.get(1).getAsJsonArray();
                                    JsonElement element;
                                    suggests = new ArrayList<>();
                                    MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_ID", "SUGGEST_COLUMN_TEXT_1"});
                                    for (int a = 0; a < jsonArray.size(); a++) {
                                        element = jsonArray.get(a);
                                        suggests.add(element.getAsString());
                                        matrixCursor.addRow(new Object[]{a, element.getAsString()});
//                                    Log.d("searchview", element.getAsString());
                                    }
                                    String[] from = new String[]{"SUGGEST_COLUMN_TEXT_1"};
                                    int[] to = new int[]{android.R.id.text1};

                                    final SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(context
                                            , R.layout.suggestion_item, matrixCursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                                    playerActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            searchView.setSuggestionsAdapter(simpleCursorAdapter);
                                            if (newText.length() == 1)
                                                searchAutoCompleteTextView.showDropDown();
                                        }
                                    });


                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else if (newText.length() == 0) {
                                playerActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        searchAutoCompleteTextView.dismissDropDown();
                                    }
                                });
                            }

                        }
                    });


                    return true;
                }
            });
            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
//                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                    if (getActivity().getCurrentFocus() != null) {
                        Log.d("focus", getActivity().getCurrentFocus().toString());
                    }

                    if (!hasFocus) {
                        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    } else {

                    }
                }
            });
            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {

                    searchView.clearFocus();
                    animateSearchArea(false);
                    Log.d("searchView", "close");
//                playerview.requestFocus();
                    return true;
                }
            });
            searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int i) {
                    return false;
                }

                @Override
                public boolean onSuggestionClick(int i) {
                    searchView.setQuery(suggests.get(i), true);
                    return true;
                }
            });


            searchButton = playerToolbar.findViewById(R.id.searchButton);

            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    searchView.requestFocus();
                    searchView.setIconified(false);
                    animateSearchArea(true);
                }
            });
//            searchView.setIconifiedByDefault(true);
//            searchView.setMaxWidth(Integer.MAX_VALUE);
//            searchView.setMinimumHeight(Integer.MAX_VALUE);
//        searchView.setQueryHint("Search");

//            int rightMarginFrame = 0;
//            View frame = searchView.findViewById(getResources().getIdentifier("android:id/search_edit_frame", null, null));
//            if (frame != null) {
//                LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//                rightMarginFrame = ((LinearLayout.LayoutParams) frame.getLayoutParams()).rightMargin;
//                frameParams.setMargins(0, 0, 0, 0);
//                frame.setLayoutParams(frameParams);
//            }
//
//            View plate = searchView.findViewById(getResources().getIdentifier("android:id/search_plate", null, null));
//            if (plate != null) {
//                plate.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//                plate.setPadding(0, 0, rightMarginFrame, 0);
//                plate.setBackgroundColor(Color.TRANSPARENT);
//            }
//
//            int autoCompleteId = getResources().getIdentifier("android:id/search_src_text", null, null);
//            if (searchView.findViewById(autoCompleteId) != null) {
//                EditText autoComplete = (EditText) searchView.findViewById(autoCompleteId);
//                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Tools.convertDpToPixel(36, context));
//                params.weight = 1;
//                params.gravity = Gravity.CENTER_VERTICAL;
//                params.leftMargin = rightMarginFrame;
//                autoComplete.setLayoutParams(params);
//                autoComplete.setTextSize(16f);
//            }
//
//            int searchMagId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
//            if (searchView.findViewById(searchMagId) != null) {
//                ImageView v = (ImageView) searchView.findViewById(searchMagId);
//                v.setImageDrawable(null);
//                v.setPadding(0, 0, 0, 0);
//                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                params.setMargins(0, 0, 0, 0);
//                v.setLayoutParams(params);
//            }
//            playerToolbar.setContentInsetsAbsolute(0, 0);
        }
    }

    public void animateSearchArea(boolean show) {
//        playerToolbar.setBackgroundColor(Color.WHITE);
        if (show) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int width = searchView.getWidth();
                Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(searchView,
                        (int) searchButton.getX() + Tools.convertDpToPixel(12.5f, context), searchView.getHeight() / 2, 0.0f, (float) width);
                createCircularReveal.setDuration(220);
                createCircularReveal.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        appBarLayout.setVisibility(View.INVISIBLE);
                        blankspace.setVisibility(View.VISIBLE);

                    }
                });
                searchAreaView.setVisibility(View.VISIBLE);
                searchView.setVisibility(View.VISIBLE);

                createCircularReveal.start();
            } else {
                TranslateAnimation translateAnimation = new TranslateAnimation(0.0f, 0.0f, (float) (-appBarLayout.getHeight()), 0.0f);
                translateAnimation.setDuration(220);
                translateAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        searchAreaView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                appBarLayout.clearAnimation();
                appBarLayout.setVisibility(View.GONE);

                appBarLayout.startAnimation(translateAnimation);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int width = searchView.getWidth();

                Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(searchView,
                        (int) searchButton.getX() + Tools.convertDpToPixel(12.5f, context), searchView.getHeight() / 2, (float) width, 0.0f);

                createCircularReveal.setDuration(220);
                createCircularReveal.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        searchView.setVisibility(View.INVISIBLE);
                        blankspace.setVisibility(View.INVISIBLE);
                    }
                });

                appBarLayout.setVisibility(View.VISIBLE);
                createCircularReveal.start();
            } else {
                AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
                Animation translateAnimation = new TranslateAnimation(0.0f, 0.0f, 0.0f, (float) (-appBarLayout.getHeight()));
                AnimationSet animationSet = new AnimationSet(true);
                animationSet.addAnimation(alphaAnimation);
                animationSet.addAnimation(translateAnimation);
                animationSet.setDuration(220);
                animationSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
//                        appBarLayout.setBackgroundColor();
                        appBarLayout.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                searchAreaView.setVisibility(View.GONE);
                appBarLayout.setVisibility(View.VISIBLE);
                appBarLayout.startAnimation(animationSet);
            }

        }
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
            playerActivity.runOnUiThread(new Runnable() {
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
            playerActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        createListView(recyclerView, data);
                    } else {
                        adapter.dataHolders = data;
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
            adapter = new SearchRecyclerAdapter(data);
            DividerItemDecoration decoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            recyclerView.addItemDecoration(decoration);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(adapter);
            RecyclerTouchListener listener = new RecyclerTouchListener(context, recyclerView, new OnItemClicked() {
                @Override
                public void onClick(View view, int position) {
                    System.out.println("clicked" + position);
                    Toast toast = Toast.makeText(context, "Decrypting...", Toast.LENGTH_SHORT);
                    toast.show();
                    final DataHolder dataHolder = adapter.dataHolders.get(position);
                    videoRetriever.startExtracting(dataHolder.videoID, new VideoRetriever.YouTubeExtractorListener() {
                        @Override
                        public void onSuccess(HashMap<Integer, String> result) {
                            dataHolder.videoUris = result;
                            mListener.onReturnSearchVideo(dataHolder);

                        }

                        @Override
                        public void onFailure(Error error) {
                            Log.d("search", "error extracting");

                        }
                    });
                }

                @Override
                public void onLongClick(View view, int position) {
                    Log.d("searchFragment", "Long click");
                }
            });
            recyclerView.addOnItemTouchListener(listener);
            adapter.menuActionListener = new SearchRecyclerAdapter.MenuActionListener() {

                @Override
                public void onDownload(int pos) {
                    final DataHolder dataHolder = adapter.dataHolders.get(pos);
                    videoRetriever.startExtracting(dataHolder.videoID, new VideoRetriever.YouTubeExtractorListener() {
                        @Override
                        public void onSuccess(HashMap<Integer, String> result) {
                            dataHolder.videoUris = result;

                            OptionDialog optionDialog = new OptionDialog();
                            optionDialog.createDialog(dataHolder);
                            optionDialog.show();

                        }

                        @Override
                        public void onFailure(Error error) {
                            Log.d("download", "error extracting");

                        }
                    });
                }
            };
        }
    }

    private class OptionDialog {

        AlertDialog.Builder builder = null;
        AlertDialog dialog = null;
        final String Download_Directory = Environment.DIRECTORY_MUSIC;

        public OptionDialog() {
            playerActivity.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        }

        public Dialog createDialog(final DataHolder dataHolder) {
            final ArrayList<String> items = VideoRetriever.getAvailableFormatString(dataHolder);
            final ArrayList<ArrayList<Integer>> res = VideoRetriever.getAvailableFormatType(dataHolder);
            final ArrayList<ArrayList<Integer>> download = new ArrayList<>();

            builder = new AlertDialog.Builder(playerActivity);
            builder.setTitle("Download options")
                    .setMultiChoiceItems(items.toArray(new String[items.size()]), null,
                            new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                    if (b) {
                                        download.add(res.get(i));
                                    } else {
                                        download.remove(res.get(i));
                                    }

                                }
                            })
                    .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            for (int a = 0; a < download.size(); a++) {
                                File file = new File(Environment.getExternalStoragePublicDirectory(Download_Directory),
                                        "SoundTube/" + dataHolder.videoID + "/" + items.get(a));
                                boolean permission = isStoragePermissionGranted();
                                if (!file.exists() && permission) {
                                    VideoRetriever.downloadVideo(dataHolder, download.get(a), context,
                                            Download_Directory, "SoundTube/" + dataHolder.videoID + "/" + items.get(a));
                                }
                            }

                        }
                    });
            dialog = builder.create();
            return dialog;

        }

        public void show() {
            if (dialog != null) {
                dialog.show();
            }
        }

        public boolean isStoragePermissionGranted() {
            if (Build.VERSION.SDK_INT >= 23) {
                if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.v("SearchFragment", "Permission is granted");
                    return true;
                } else {

                    Log.v("SearchFragment", "Permission is revoked");
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    return false;
                }
            } else { //permission is automatically granted on sdk<23 upon installation
                Log.v("SearchFragment", "Permission is granted");
                return true;
            }
        }

        BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Service.DOWNLOAD_SERVICE);

            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
//                        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                        if (!VideoRetriever.downloadList.isEmpty()) {
                            DownloadManager.Query query = new DownloadManager.Query();
                            Long[] pair = VideoRetriever.downloadList.get(0);
                            if (pair.length > 1) {
                                query.setFilterById(pair[0], pair[1]);
                                ArrayList<String> url = new ArrayList<>();
                                Cursor c = downloadManager.query(query);
                                int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                                int urlIndex = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                                String videoID = null;
                                boolean merge = true;
                                while (c.moveToNext()) {
                                    if (videoID == null) {
                                        videoID = c.getString(c.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
                                    }
                                    if (c.getInt(statusIndex) != DownloadManager.STATUS_SUCCESSFUL) {
                                        merge = false;
                                    } else {
                                        url.add(0, Uri.parse(c.getString(urlIndex)).getPath());
                                    }
                                }
                                if (merge) {
                                    try {
                                        H264TrackImpl video = new H264TrackImpl(new FileDataSourceImpl(url.get(0)));
                                        AACTrackImpl audio = new AACTrackImpl(new FileDataSourceImpl(url.get(1)));
                                        Movie movie = new Movie();
                                        movie.addTrack(video);
                                        movie.addTrack(audio);
                                        Container mp4file = new DefaultMp4Builder().build(movie);
                                        FileChannel channel = new FileOutputStream(
                                                Download_Directory + "/SoundTube/" + videoID + ".mp4").getChannel();
                                        mp4file.writeContainer(channel);
                                        channel.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                //nothing
                            }

                        }

                        break;
                    default:
                        break;
                }

            }
        };
    }

}