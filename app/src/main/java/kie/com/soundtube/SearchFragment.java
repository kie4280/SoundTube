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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    ConstraintLayout mainWindow = null, nextPage, prevPage;
    SearchRecyclerAdapter adapter = null;
    OptionDialog downloadOptionDialog;
    ImageView blankspace, nextPageTab, prevPageTab, nextPageImg, prevPageImg;
    TextView searchTextView;
    ProgressBar progressBar;
    YoutubeClient youtubeClient;
    ArrayList<String> suggests;
    String queryUrl = "http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&client=firefox&q=";
    boolean waiting = false;
    boolean m_hasNext = true;
    boolean m_hasPrev = true;
    int widthPixels, index;

    public SearchFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        workHandler = ((MainActivity) context).workHandler;
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
            nextPageImg = (ImageView) searchFragmentView.findViewById(R.id.nextPageImage);
            prevPageImg = (ImageView) searchFragmentView.findViewById(R.id.prevPageImage);
            nextPage = (ConstraintLayout) searchFragmentView.findViewById(R.id.nextPage);
            prevPage = (ConstraintLayout) searchFragmentView.findViewById(R.id.prevPage);

            widthPixels = context.getResources().getDisplayMetrics().widthPixels;
            nextPage.setX(widthPixels);
            prevPage.setX(-widthPixels);
            nextPageTab.setOnTouchListener(nextPageTouchListener);
            prevPageTab.setOnTouchListener(prevPageTouchListsner);
            nextPageImg.setImageDrawable(new TextDrawable("3"));

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
                switch (requestCode) {
                    case REQUEST_WRITE_PERMISSION:
                        downloadOptionDialog.show();
                        break;
                }

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


    private OnTouchListener nextPageTouchListener = new OnTouchListener() {
        float OrgX;
        float half;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    float delta = widthPixels / 5 * 2;

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
                                                nextPage.setX(widthPixels);
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
                        nextPage.animate().x(widthPixels).setDuration(100);
                    }

                    break;

                case MotionEvent.ACTION_DOWN:
                    half = nextPageTab.getWidth() / 2;
                    OrgX = widthPixels - half * 2;
                    nextPageTab.clearAnimation();
                    nextPage.clearAnimation();
                case MotionEvent.ACTION_MOVE:
                    float x = event.getRawX();
                    if ((x + half) < widthPixels) {
                        nextPageTab.setX(x - half);
                    }
                    nextPage.setX(x + half);
                    break;

            }

            return true;
        }
    };

    private OnTouchListener prevPageTouchListsner = new OnTouchListener() {
        float OrgX;
        float half;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    float delta = widthPixels / 5 * 3;
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
                                                prevPage.setX(-widthPixels);
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
                        prevPage.animate().x(-widthPixels).setDuration(100);
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
                    prevPage.setX(-widthPixels + x - half);
                    break;

            }

            return true;
        }
    };

    public void getSearchResult() {

        youtubeClient.getVideoSearchResults(new YoutubeClient.YoutubeVideoSearchResult() {
            @Override
            public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                changeSate(hasnext, hasprev);
                updateListView(data);
                recyclerView.scrollToPosition(0);
            }

            @Override
            public void onError(String error) {
                Log.d("SearchFramgent", "error: " + error);

            }

        });
    }

    public void search(String term) {
        youtubeClient.newVideoSearch(term);
        loading();
        index = 1;
        nextPageImg.setImageDrawable(new TextDrawable(Integer.toString(index + 1)));
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
                    System.out.println("submit: " + query);
                    if (query != null) {
                        searchTextView.setText(query);
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
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

                    Log.d("SearchView", "Focus change");
                    if (getActivity().getCurrentFocus() != null) {
                        Log.d("focus", getActivity().getCurrentFocus().toString());
                    }

                    if (hasFocus) {
                        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.RESULT_UNCHANGED_SHOWN);
                    } else {
                        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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

            int searchMagId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
            ImageView v = (ImageView) searchView.findViewById(searchMagId);
            if (v != null) {
                v.setImageResource(R.drawable.baseline_arrow_back_black_48dp);
                v.setClickable(true);
                int pixels = Tools.convertDpToPixel(5, context);
                v.setPadding(pixels, pixels, pixels, pixels);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        animateSearchArea(false);
                    }
                });
            }

        }
    }

    public void animateSearchArea(boolean show) {
//        playerToolbar.setBackgroundColor(Color.WHITE);
        if (show) {
            Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    searchView.requestFocus();
                }
            };
            searchView.clearAnimation();
            searchView.setTranslationY(searchTextView.getY() + searchTextView.getHeight());
            blankspace.setVisibility(View.VISIBLE);
            searchView.setVisibility(View.VISIBLE);

            searchView.animate().translationY(0f).setDuration(100).setListener(listener).start();

        } else {
            Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    searchView.setVisibility(View.GONE);
                    searchView.setAlpha(1f);
                    searchView.clearFocus();
                }
            };

            searchView.clearAnimation();
            blankspace.setVisibility(View.GONE);
            searchView.animate().alpha(0f).setDuration(100).setListener(listener).start();


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
                        Log.d("search", "onError extracting" + error);

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

    private class OptionDialog {

        AlertDialog.Builder builder = null;
        AlertDialog dialog = null;
        boolean show = false;

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