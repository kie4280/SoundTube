package kie.com.soundtube;

import android.content.Context;
import android.database.MatrixCursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private View fragmentView = null;
    private Handler workHandler = null;
    private Context context;
    public VideoRetriver videoRetriver;
    private ViewPager viewPager;
    private ProgressBar bar1, bar2;
    public Toolbar playerToolbar;
    public SearchView searchView;
    MainActivity mainActivity;
    DrawerLayout drawerLayout;
    Searcher searcher;
    HttpURLConnection httpURLConnection;
    CustomPagerAdapter pagerAdapter;
    ArrayList<View> pageviews = new ArrayList<>(3);
    Page page;
    String httpurl = "http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&client=firefox&q=";

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();

        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        drawerLayout = mainActivity.drawerLayout;
        fragmentView = inflater.inflate(R.layout.fragment_search, container, false);
        playerToolbar = mainActivity.playerToolbar;

        View r1 = inflater.inflate(R.layout.blank_loading, null);
        View r2 = inflater.inflate(R.layout.blank_loading, null);
        bar1 = (ProgressBar) r1.findViewById(R.id.pageLoadingBar);
        bar2 = (ProgressBar) r2.findViewById(R.id.pageLoadingBar);
        pageviews.add(r1);
        pageviews.add(inflater.inflate(R.layout.searchpage, null));
        pageviews.add(r2);
        page = new Page(pageviews.get(1));
        viewPager = (ViewPager) fragmentView.findViewById(R.id.searchViewPager);
        pagerAdapter = new CustomPagerAdapter(pageviews);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(onPageChangeListener);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                getActivity()
                , drawerLayout, playerToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        };
        playerToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        createSearchView();



        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_options, menu);
//        MenuItem item = menu.findItem(R.id.action_search);
//        MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
//            @Override
//            public boolean onMenuItemActionExpand(MenuItem menuItem) {
//                setHasOptionsMenu(false);
//                return false;
//            }
//
//            @Override
//            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
//                setHasOptionsMenu(true);
//                return false;
//            }
//        });
//        final SearchView searchView = (SearchView) item.getActionView();
////        TextView textView = (TextView) searchView.findViewById(R.id.search_src_text);
////        textView.setPadding(2, 0, 0, 0);
//
////        "http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&client=firefox&q=Query";
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//
//                System.out.println("submit");
//                if (query != null) {
//                    if (MainActivity.netConncted) {
//                        search(query);
//                    } else {
//                        Toast toast = Toast.makeText(context, getString(R.string.needNetwork), Toast.LENGTH_SHORT);
//                        toast.show();
//                    }
//                    searchView.clearFocus();
//                }
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(final String newText) {
//                workHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (MainActivity.netConncted && newText.length() != 0) {
//                            StringBuilder response = new StringBuilder();
//                            try {
//                                URL url = new URL(httpurl + newText);
//                                httpURLConnection = (HttpURLConnection) url.openConnection();
//                                httpURLConnection.setRequestMethod("GET");
//                                BufferedReader in = new BufferedReader(
//                                        new InputStreamReader(httpURLConnection.getInputStream()));
//                                String inputLine;
//
//                                while ((inputLine = in.readLine()) != null) {
//                                    response.append(inputLine);
//                                }
//                                in.close();
//                                httpURLConnection.disconnect();
//                                JsonArray jsonArray = new JsonParser().parse(response.toString()).getAsJsonArray();
//                                jsonArray = jsonArray.get(1).getAsJsonArray();
//                                JsonElement element;
//                                ArrayList<String> suggests = new ArrayList<>();
//                                MatrixCursor matrixCursor = new MatrixCursor(new String[] {"results"});
//                                for (int a = 0; a < jsonArray.size(); a++) {
//                                    element = jsonArray.get(a);
//                                    suggests.add(element.getAsString());
//                                    matrixCursor.addRow(new Object[]{element.getAsString()});
//                                    Log.d("searchview", element.getAsString());
//                                }
//
////                                SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(context, );
////                                searchView.setSuggestionsAdapter(simpleCursorAdapter);
//
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                    }
//                });
////                SearchView.SearchAutoComplete searchAutoComplete = new SearchView.SearchAutoComplete(context);
//
//                return true;
//            }
//        });
//        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
////                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
////                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
//                if (!hasFocus) {
//                    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//                }
//            }
//        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
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
                } else if (index < previndex && user) {
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
        searcher.newSearch(term);
        page.loading();

        searcher.getResults(new Searcher.YoutubeSearchResult() {
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

    public void setSearchWorker(Handler handler) {
        workHandler = handler;
        searcher = new Searcher(context, workHandler);
        videoRetriver = new VideoRetriver(workHandler);
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

        void onreturnVideo(DataHolder dataHolder);
    }

    public void setActivity(MainActivity activity) {
        this.mainActivity = activity;
    }

    public void createSearchView() {
        searchView = new SearchView(context);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                System.out.println("submit");
                if (query != null) {
                    if (MainActivity.netConncted) {
                        search(query);
                    } else {
                        Toast toast = Toast.makeText(context, getString(R.string.needNetwork), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    searchView.clearFocus();


                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                workHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (MainActivity.netConncted && newText.length() != 0) {
                            StringBuilder response = new StringBuilder();
                            try {
                                URL url = new URL(httpurl + newText);
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
                                ArrayList<String> suggests = new ArrayList<>();
                                MatrixCursor matrixCursor = new MatrixCursor(new String[]{"results"});
                                for (int a = 0; a < jsonArray.size(); a++) {
                                    element = jsonArray.get(a);
                                    suggests.add(element.getAsString());
                                    matrixCursor.addRow(new Object[]{element.getAsString()});
//                                    Log.d("searchview", element.getAsString());
                                }

//                                SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(context, );
//                                searchView.setSuggestionsAdapter(simpleCursorAdapter);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
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
                    setHasOptionsMenu(true);

                } else {
                    setHasOptionsMenu(false);

                }
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {

                searchView.clearFocus();
//                playerview.requestFocus();
                return true;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerToolbar.setBackgroundColor(Color.WHITE);

            }
        });


        searchView.setIconifiedByDefault(true);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setMinimumHeight(Integer.MAX_VALUE);
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
        if (searchView.findViewById(searchMagId) != null) {
            ImageView v = (ImageView) searchView.findViewById(searchMagId);
            v.setImageDrawable(null);
            v.setPadding(0, 0, 0, 0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 0);
            v.setLayoutParams(params);
        }

        playerToolbar.setTitle(null);
        playerToolbar.setContentInsetsAbsolute(0, 0);
        playerToolbar.addView(searchView);

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
                    final DataHolder dataHolder = adapter.dataHolders.get(position - 1);
                    videoRetriver.startExtracting("https://www.youtube" +
                            ".com/watch?v=" + dataHolder.videoID, new VideoRetriver.YouTubeExtractorListener() {
                        @Override
                        public void onSuccess(HashMap<Integer, String> result) {
                            dataHolder.videoUris = result;
                            mListener.onreturnVideo(dataHolder);
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
//                    mainActivity.setPlayerToolbar(dy);

                }
            });

        }
    }

}