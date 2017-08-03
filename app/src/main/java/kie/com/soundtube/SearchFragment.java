package kie.com.soundtube;

import android.app.ActionBar;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v4.view.ViewPager.*;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.HashMap;
import java.util.List;


public class SearchFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private View fragmentView = null;
    private HandlerThread WorkerThread = null;
    private static Handler WorkHandler = null;
    private Context context;
    public VideoRetriver videoRetriver;

    private RecyclerView recyclerView;
    private ViewPager viewPager;

    MainActivity mainActivity;
    Searcher searcher;
    RecyclerAdapter adapter;


    public SearchFragment() {
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
//        recyclerView = (RecyclerView) fragmentView.findViewById(R.id.recyclerView);
        viewPager = (ViewPager) fragmentView.findViewById(R.id.searchViewPager);
        TextView t1 = (TextView) fragmentView.findViewById(R.id.textView3);
        recyclerView = (RecyclerView) fragmentView.findViewById(R.id.recyclerView);
        TextView t2 = (TextView) fragmentView.findViewById(R.id.textView4);
        CustomPagerAdapter pagerAdapter = new CustomPagerAdapter();
        pagerAdapter.addView(t1);
        pagerAdapter.addView(recyclerView);
        pagerAdapter.addView(t2);
        pagerAdapter.count = 3;
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(1);
        viewPager.addOnPageChangeListener(onPageChangeListener);

        return fragmentView;
    }

    private OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            if (position == 2) {
                viewPager.setCurrentItem(1);
                searcher.nextPage();
                Log.d("pager", "nextpage");
            } else if (position == 0) {
                viewPager.setCurrentItem(1);
                searcher.prevPage();
                Log.d("pager", "prevpage");
            }

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        WorkerThread.quit();
    }

    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);

        void onreturnVideo(DataHolder dataHolder, Handler handler);
    }

    public void updateListView(final List<DataHolder> data) {
        if (adapter == null) {
            createListView(data);
        }
//        recyclerView.setTranslationY(mainActivity.toolbar.getHeight());
        adapter.dataHolders = data;
        adapter.notifyDataSetChanged();

    }

    public void createListView(final List<DataHolder> data) {

//        Log.d("createlist", "create");
        adapter = new RecyclerAdapter(data);
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

//        ListAdapter listAdapter = new ListAdapter() {
//
//            @Override
//            public boolean areAllItemsEnabled() {
//                return true;
//            }
//
//            @Override
//            public boolean isEnabled(int position) {
//                return true;
//            }
//
//            @Override
//            public void registerDataSetObserver(DataSetObserver observer) {
//
//            }
//
//            @Override
//            public void unregisterDataSetObserver(DataSetObserver observer) {
//
//            }
//
//            @Override
//            public int getCount() {
//                return data.size();
//            }
//
//            @Override
//            public DataHolder getItem(int position) {
//                return data.get(position);
//            }
//
//            @Override
//            public long getItemId(int position) {
//                return 0;
//            }
//
//            @Override
//            public boolean hasStableIds() {
//                return false;
//            }
//
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                View view;
//                if (convertView == null) {
//                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                    view = inflater.inflate(R.layout.video_layout, parent, false);
//                    DataHolder dataHolder = data.get(position);
//                    ViewHolder viewHolder = new ViewHolder();
//                    viewHolder.imageView = (ImageView) view.findViewById(R.id.imageView);
//                    viewHolder.titleview = (TextView) view.findViewById(R.id.titleview);
//                    viewHolder.durationview = (TextView) view.findViewById(R.id.durationview);
//                    viewHolder.imageView.setImageBitmap(dataHolder.thumbnail);
//                    viewHolder.titleview.setText(dataHolder.title);
//                    viewHolder.durationview.setText(dataHolder.videolength);
//                    view.setTag(viewHolder);
//
//
//                } else {
//                    view = convertView;
//                    DataHolder dataHolder = data.get(position);
//                    ViewHolder viewHolder = (ViewHolder) view.getTag();
//                    viewHolder.imageView.setImageBitmap(dataHolder.thumbnail);
//                    viewHolder.titleview.setText(dataHolder.title);
//                    viewHolder.durationview.setText(dataHolder.videolength);
//
//                }
//
//                return view;
//            }
//
//            @Override
//            public int getItemViewType(int position) {
//                return 0;
//            }
//
//            @Override
//            public int getViewTypeCount() {
//                return 1;
//            }
//
//            @Override
//            public boolean isEmpty() {
//                return false;
//            }
//        };
//        listView.setAdapter(listAdapter);
//
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                System.out.println("clicked" + position);
//                Toast toast = Toast.makeText(context, "Decrypting...", Toast.LENGTH_SHORT);
//                toast.show();
//                final DataHolder dataHolder = data.get(position);
//                videoRetriver.startExtracting("https://www.youtube" +
//                        ".com/watch?v=" + dataHolder.videoID, new VideoRetriver.YouTubeExtractorListener() {
//                    @Override
//                    public void onSuccess(HashMap<Integer, String> result) {
//                        dataHolder.videoUris = result;
//                        mListener.onreturnVideo(dataHolder, WorkHandler);
//                        //Log.d("search", ))
//                    }
//
//                    @Override
//                    public void onFailure(Error error) {
//                        Log.d("search", "error extracting");
//
//                    }
//                });
//
//            }
//        });

    }

    public void search(String term) {
        searcher.newSearch(term, new Searcher.YoutubeSearchResult() {
            @Override
            public void onFound(final List<DataHolder> data) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateListView(data);
                    }
                });
            }
        });
    }

    public void setActivity(MainActivity activity) {
        this.mainActivity = activity;
    }


}