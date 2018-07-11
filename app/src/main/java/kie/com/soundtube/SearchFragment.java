package kie.com.soundtube;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.database.MatrixCursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class SearchFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private static final int REQUEST_WRITE_PERMISSION = 1007;
    private View searchFragmentView = null;
    private Handler workHandler = null;
    private Context context;
    private RecyclerView recyclerView;
    public VideoRetriever videoRetriever;
    SearchView searchView = null;
    ConstraintLayout mainWindow = null;
    SearchRecyclerAdapter adapter = null;
    OptionDialog downloadOptionDialog;
    ImageView blankspace, nextPageTab, prevPageTab;
    TextView searchTextView;
    ProgressBar progressBar;
    YoutubeClient youtubeClient;

    ArrayList<String> suggests;
    String queryUrl = "http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&client=firefox&q=";
    boolean waiting = false;
    boolean m_hasNext = false;
    boolean m_hasPrev = false;

    public SearchFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        workHandler = MainActivity.workHandler;
        youtubeClient = new YoutubeClient(context, workHandler);
        videoRetriever = new VideoRetriever(context, workHandler);
        downloadOptionDialog = new OptionDialog();
        context.registerReceiver(videoRetriever.downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        if (searchFragmentView == null) {

            searchFragmentView = inflater.inflate(R.layout.fragment_search, container, false);
            Log.d("searchfragment", "createview");
            nextPageTab = (ImageView) searchFragmentView.findViewById(R.id.nextPageTab);
            prevPageTab = (ImageView) searchFragmentView.findViewById(R.id.prevPageTab);
            recyclerView = (RecyclerView) searchFragmentView.findViewById(R.id.searchRecyclerView);
            progressBar = (ProgressBar) searchFragmentView.findViewById(R.id.loadingProgress);
            searchTextView = (TextView) searchFragmentView.findViewById(R.id.searchTerm);
            nextPageTab.setOnTouchListener(nextPageTouchListener);
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
        context.unregisterReceiver(videoRetriever.downloadReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, new EasyPermissions.PermissionCallbacks() {
            @Override
            public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

            }

            @Override
            public void onPermissionsGranted(int requestCode, List<String> perms) {
                downloadOptionDialog.show();
            }

            @Override
            public void onPermissionsDenied(int requestCode, List<String> perms) {
                Log.v("SearchFragment", "Permission is revoked");
                Toast.makeText(context, "Permission is denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

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
        Log.d("SearchFragment", "onDetach");
    }

//    private OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {
//        int previndex = 0;
//        boolean user = false;
//
//        @Override
//        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//
//        }
//
//        @Override
//        public void onPageSelected(int position) {
//            previndex = position;
//        }
//
//        @Override
//        public void onPageScrollStateChanged(int state) {
//
//            if (state == ViewPager.SCROLL_STATE_SETTLING) {
//
//                final int index = viewPager.getCurrentItem();
//                Log.d("viewpager", Integer.toString(index));
//                if (index > previndex && user) {
//                    youtubeClient.nextVideoPage();
//                    page.loading();
//                    youtubeClient.getVideoSearchResults(new YoutubeClient.YoutubeVideoSearchResult() {
//                        @Override
//                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
//                            pagerAdapter.changeSate(hasnext, hasprev);
//                            page.updateListView(data);
//
//                            if (hasnext) {
//                                getActivity().runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        viewPager.setCurrentItem(1, false);
//                                        bar1.setVisibility(View.GONE);
//                                        bar2.setVisibility(View.GONE);
//                                    }
//                                });
//                            }
//                        }
//
//                        @Override
//                        public void onError(String error) {
//
//                        }
//
//
//                    });
//                } else if (index < previndex && user) {
//                    youtubeClient.prevVideoPage();
//                    page.loading();
//                    youtubeClient.getVideoSearchResults(new YoutubeClient.YoutubeVideoSearchResult() {
//                        @Override
//                        public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
//                            pagerAdapter.changeSate(hasnext, hasprev);
//                            page.updateListView(data);
//                            if (hasprev) {
//                                getActivity().runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        viewPager.setCurrentItem(1, false);
//                                        bar1.setVisibility(View.GONE);
//                                        bar2.setVisibility(View.GONE);
//                                    }
//                                });
//                            }
//                        }
//
//                        @Override
//                        public void onError(String error) {
//
//                        }
//
//
//                    });
//                } else if (index == previndex && user) {
//                    bar1.setVisibility(View.GONE);
//                    bar2.setVisibility(View.GONE);
//                }
//
//                user = false;
//
//            } else if (state == ViewPager.SCROLL_STATE_DRAGGING) {
//                bar1.setVisibility(View.VISIBLE);
//                bar2.setVisibility(View.VISIBLE);
//                user = true;
//
//            }
//        }
//    };

    private OnTouchListener nextPageTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            nextPageTab.setX(event.getX());
            return true;
        }
    };

    private OnTouchListener prevPageTouchListsner = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    };

    public void search(String term) {
        youtubeClient.newVideoSearch(term);
        loading();

        youtubeClient.getVideoSearchResults(new YoutubeClient.YoutubeVideoSearchResult() {
            @Override
            public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {

                changeSate(hasnext, hasprev);
                updateListView(data);
                Log.d("searchFragment", "found");
            }

            @Override
            public void onError(String error) {

            }

        });

    }

    public boolean previousSearch() {
        return true;
    }


    public void changeSate(boolean hasNext, boolean hasPrev) {
        if (hasNext != m_hasNext) {
            nextPageTab.setImageResource(hasNext ? R.drawable.page_tab_enabled : R.drawable.page_tab_disabled);
            m_hasNext = hasNext;
        }
        if (hasPrev != m_hasPrev) {
            prevPageTab.setImageResource(hasPrev ? R.drawable.page_tab_enabled : R.drawable.page_tab_disabled);
            m_hasPrev = hasPrev;
        }
    }

    public void createSearchView() {
        if (mainWindow == null) {
            mainWindow = ((MainActivity) context).findViewById(R.id.mainWindow);
            blankspace = mainWindow.findViewById(R.id.blankspace);
            searchView = mainWindow.findViewById(R.id.searchview);
            int autoCompleteTextViewID = getResources().getIdentifier("android:id/search_src_text", null, null);
            final AutoCompleteTextView searchAutoCompleteTextView = (AutoCompleteTextView) searchView.findViewById(autoCompleteTextViewID);
            searchAutoCompleteTextView.setThreshold(1);
            blankspace.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    animateSearchArea(false);
                }
            });
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    System.out.println("submit");
                    if (query != null) {
                        if (MainActivity.netConncted) {
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
                            if (MainActivity.netConncted && newText.length() != 0) {
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
                                    getActivity().runOnUiThread(new Runnable() {
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
                                getActivity().runOnUiThread(new Runnable() {
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
                    Log.d("SearchView", "Focus change");
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


            searchTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    animateSearchArea(true);
                }
            });
//            searchView.setIconifiedByDefault(true);
//            searchView.setMaxWidth(Integer.MAX_VALUE);
//            searchView.setMinimumHeight(Integer.MAX_VALUE);
//        searchView.setQueryHint("Search");

            int rightMarginFrame = 0;
            View frame = searchView.findViewById(getResources().getIdentifier("android:id/search_edit_frame", null, null));
            if (frame != null) {
                LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                rightMarginFrame = ((LinearLayout.LayoutParams) frame.getLayoutParams()).rightMargin;
                frameParams.setMargins(0, 0, 0, 0);
                frame.setLayoutParams(frameParams);
            }

            View plate = searchView.findViewById(getResources().getIdentifier("android:id/search_plate", null, null));
            if (plate != null) {
                plate.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                plate.setPadding(0, 0, rightMarginFrame, 0);
                plate.setBackgroundColor(Color.TRANSPARENT);
            }

            int autoCompleteId = getResources().getIdentifier("android:id/search_src_text", null, null);
            if (searchView.findViewById(autoCompleteId) != null) {
                EditText autoComplete = (EditText) searchView.findViewById(autoCompleteId);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Tools.convertDpToPixel(36, context));
                params.weight = 1;
                params.gravity = Gravity.CENTER_VERTICAL;
                params.leftMargin = rightMarginFrame;
                autoComplete.setLayoutParams(params);
                autoComplete.setTextSize(16f);
            }

//            int searchMagId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
//            if (searchView.findViewById(searchMagId) != null) {
//                ImageView v = (ImageView) searchView.findViewById(searchMagId);
//                v.setImageDrawable(null);
//                v.setPadding(0, 0, 0, 0);
//                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                params.setMargins(0, 0, 0, 0);
//                v.setLayoutParams(params);
//            }

        }
    }

    public void animateSearchArea(boolean show) {
//        playerToolbar.setBackgroundColor(Color.WHITE);
        if (show) {
            Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    searchTextView.setVisibility(View.GONE);
                    searchTextView.setTranslationY(0f);
                    blankspace.setVisibility(View.VISIBLE);
                    searchView.setVisibility(View.VISIBLE);
                    searchView.requestFocus();
                }
            };
            searchTextView.clearAnimation();
            searchTextView.animate().translationY(-100f).setDuration(100).setListener(listener).start();

        } else {
            Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    blankspace.setVisibility(View.GONE);
                    searchView.setVisibility(View.GONE);
                    searchView.setTranslationY(0f);
                    searchView.clearFocus();
                    searchTextView.setVisibility(View.VISIBLE);
                }
            };

            searchView.clearAnimation();
            searchView.animate().translationY(100f).setDuration(100).setListener(listener).start();


        }
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
                videoRetriever.getDataHolder(dataHolder, new VideoRetriever.YouTubeExtractorListener() {
                    @Override
                    public void onSuccess(DataHolder result) {
                        mListener.onReturnSearchVideo(result);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.d("search", "onError extracting");

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
                videoRetriever.getDataHolder(dataHolder, new VideoRetriever.YouTubeExtractorListener() {

                    @Override
                    public void onSuccess(DataHolder result) {
                        downloadOptionDialog.createDialog(result);
                        downloadOptionDialog.setShow();
                        downloadOptionDialog.show();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.d("download", "onError extracting");

                    }
                });
            }
        };
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

    private class OptionDialog {

        AlertDialog.Builder builder = null;
        AlertDialog dialog = null;
        boolean show = false;

        public OptionDialog() {


        }

        public Dialog createDialog(final DataHolder dataHolder) {
            final ArrayList<String> items = VideoRetriever.getAvailableFormatString(dataHolder);
            final ArrayList<ArrayList<Integer>> res = VideoRetriever.getAvailableFormatType(dataHolder);
            final ArrayList<ArrayList<Integer>> downloadType = new ArrayList<>();
            final ArrayList<Integer> downloadIndex = new ArrayList<>();

            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Download options")
                    .setMultiChoiceItems(items.toArray(new String[items.size()]), null,
                            new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                    if (b) {
                                        downloadType.add(res.get(i));
                                        downloadIndex.add(i);
                                    } else {
                                        downloadType.remove(res.get(i));
                                        downloadIndex.remove(i);
                                    }

                                }
                            })
                    .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            for (int a = 0; a < downloadIndex.size(); a++) {
                                String res = items.get(downloadIndex.get(a));
                                if (show) {
                                    try {
                                        videoRetriever.downloadVideo(dataHolder, downloadType.get(a), res);
                                    } catch (VideoRetriever.DownloadException e) {
                                        Toast toast = Toast.makeText(context, "Download onError!!", Toast.LENGTH_SHORT);
                                        toast.show();
                                        e.printStackTrace();
                                    }

                                }
                            }

                        }
                    });
            dialog = builder.create();
            return dialog;

        }

        public void show() {
            if (dialog != null && show && isStoragePermissionGranted()) {
                dialog.show();

            }
        }

        public void setShow() {
            show = true;
        }

        public boolean isStoragePermissionGranted() {
            if (Build.VERSION.SDK_INT >= 23) {
                if (EasyPermissions.hasPermissions(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.v("SearchFragment", "Permission is granted");
                    return true;
                } else {
                    EasyPermissions.requestPermissions(getActivity(), "Need write permission",
                            REQUEST_WRITE_PERMISSION, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    return false;
                }
            } else { //permission is automatically granted on sdk<23 upon installation
                Log.v("SearchFragment", "Permission is granted");
                return true;
            }
        }

    }

    public interface OnFragmentInteractionListener {

        void onReturnSearchVideo(DataHolder dataHolder);
    }

}