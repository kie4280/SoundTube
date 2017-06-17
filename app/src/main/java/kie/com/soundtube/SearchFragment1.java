package kie.com.soundtube;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Joiner;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class SearchFragment1 extends Fragment {

    private OnFragmentInteractionListener mListener;
    private View fragmentView = null;
    private HandlerThread WorkerThread = null;
    private static Handler WorkHandler = null;
    private Context context;
    public VideoRetriver videoRetriver;
    ListView listView;

    MainActivity1 mainActivity;
    Searcher searcher;


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
        listView = (ListView) fragmentView.findViewById(R.id.result_list);
        return fragmentView;

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        WorkerThread.quit();
    }

    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);

        void onreturnVideo(DataHolder dataHolder, Handler handler);
    }

    private class ViewHolder {
        ImageView imageView;
        TextView titleview;
        TextView durationview;
    }

    public void createListView(final List<DataHolder> data) {

        ListAdapter listAdapter = new ListAdapter() {

            @Override
            public boolean areAllItemsEnabled() {
                return true;
            }

            @Override
            public boolean isEnabled(int position) {
                return true;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public int getCount() {
                return data.size();
            }

            @Override
            public DataHolder getItem(int position) {
                return data.get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view;
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.video_layout, parent, false);
                    DataHolder dataHolder = data.get(position);
                    ViewHolder viewHolder = new ViewHolder();
                    viewHolder.imageView = (ImageView) view.findViewById(R.id.imageView);
                    viewHolder.titleview = (TextView) view.findViewById(R.id.titleview);
                    viewHolder.durationview = (TextView) view.findViewById(R.id.durationview);
                    viewHolder.imageView.setImageBitmap(dataHolder.thumbnail);
                    viewHolder.titleview.setText(dataHolder.title);
                    viewHolder.durationview.setText(dataHolder.videolength);
                    view.setTag(viewHolder);


                } else {
                    view = convertView;
                    DataHolder dataHolder = data.get(position);
                    ViewHolder viewHolder = (ViewHolder) view.getTag();
                    viewHolder.imageView.setImageBitmap(dataHolder.thumbnail);
                    viewHolder.titleview.setText(dataHolder.title);
                    viewHolder.durationview.setText(dataHolder.videolength);

                }

                return view;
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("clicked" + position);
                Toast toast = Toast.makeText(context, "Decrypting...", Toast.LENGTH_SHORT);
                toast.show();
                final DataHolder dataHolder = data.get(position);
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
                        Log.d("search", "errorextracting");

                    }
                });

            }
        });

    }

    public void search(String term) {
        searcher.newSearch(term, new Searcher.YoutubeSearchResult() {
            @Override
            public void onFound(final List<DataHolder> data) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        createListView(data);
                    }
                });
            }
        });
    }

    public void setActivity(MainActivity1 activity) {
        this.mainActivity = activity;
    }


}