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

public class SearchFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;
    private static final String APIKey = "AIzaSyANfhXgNlxpmkWKl7JNWdyRQZx4uS2vYuo";
    private static YouTube youtube;
    private View fragmentView = null;
    private HandlerThread WorkerThread = null;
    private static Handler WorkHandler = null;
    private Context context;
    public VideoRetriver videoRetriver;
    ListView listView;

    MainActivity mainActivity;
    Search searcher;


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
        videoRetriver = new VideoRetriver(WorkerThread);
        searcher = new Search();
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

        void onreturnVideo(DataHolder dataHolder);
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

                final DataHolder dataHolder = data.get(position);
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
                        Log.d("search", "errorextracting");

                    }
                });

            }
        });

    }

    public void setActivity(MainActivity activity) {
        this.mainActivity = activity;
    }

    public class Search {

        //private int tokenIndex = 0;
        YouTube.Search.List search;
        String nextPageToken = null;
        String prevPageToken = null;

        public void newSearch(final String queryTerm) {

            Runnable run1 = new Runnable() {
                @Override
                public void run() {
                    try {
                        // This object is used to make YouTube Data API requests. The last
                        // argument is required, but since we don't need anything
                        // initialized when the HttpRequest is initialized, we override
                        // the interface and provide a no-op function.

                        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                            public void initialize(HttpRequest request) throws IOException {
                            }
                        }).setApplicationName("power saver").build();
                        search = youtube.search().list("id,snippet");
                        // Define the API request for retrieving search results.
                        search.setKey(APIKey);
                        // Set your developer key from the {{ Google Cloud Console }} for
                        // non-authenticated requests. See:
                        // {{ https://cloud.google.com/console }}
                        search.setType("video");
                        // To increase efficiency, only retrieve the fields that the
                        // application uses.
                        search.setFields("items(id/kind,id/videoId),nextPageToken,prevPageToken,pageInfo/totalResults");
                        search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
                        search.setQ(queryTerm);
                        // Call the API and print results.
                        SearchListResponse searchResponse = search.execute();
                        nextPageToken = searchResponse.getNextPageToken();
                        prevPageToken = searchResponse.getPrevPageToken();
                        List<SearchResult> searchResultList = searchResponse.getItems();
                        onFound(toClass(searchResultList));
                    } catch (GoogleJsonResponseException e) {
                        System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                                + e.getDetails().getMessage());
                    } catch (IOException e) {
                        System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            };

            WorkHandler.post(run1);
        }

        public void nextPage() {
            WorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (nextPageToken != null) {
                        search.setPageToken(nextPageToken);
                        try {
                            SearchListResponse searchResponse = search.execute();
                            nextPageToken = searchResponse.getNextPageToken();
                            prevPageToken = searchResponse.getPrevPageToken();
                            List<SearchResult> searchResultList = searchResponse.getItems();
                            onFound(toClass(searchResultList));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast toast = Toast.makeText(context, "No next page", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                }
            });

        }

        public void prevPage() {
            WorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (prevPageToken != null) {
                        search.setPageToken(prevPageToken);
                        try {
                            SearchListResponse searchResponse = search.execute();
                            nextPageToken = searchResponse.getNextPageToken();
                            prevPageToken = searchResponse.getPrevPageToken();
                            List<SearchResult> searchResultList = searchResponse.getItems();
                            onFound(toClass(searchResultList));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast toast = Toast.makeText(context, "No previous page", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            });

        }

        public List<DataHolder> toClass(List<SearchResult> searchResultList) {

            List<String> videoList = new ArrayList<>((int) NUMBER_OF_VIDEOS_RETURNED);
            List<DataHolder> classes = new LinkedList<>();

            if (searchResultList != null) {
                if (searchResultList.size() == 0) {

                    Toast toast = Toast.makeText(context, "No matching result", Toast.LENGTH_LONG);
                    toast.show();


                } else {
                    for (SearchResult searchResult : searchResultList) {
                        videoList.add(searchResult.getId().getVideoId());
                    }
                    Joiner joiner = Joiner.on(',');
                    String videoID = joiner.join(videoList);
                    try {
                        YouTube.Videos.List videoRequest = youtube.videos().list("snippet,contentDetails,id").setId(videoID);
                        videoRequest.setKey(APIKey);
                        videoRequest.setFields("items(id,snippet/publishedAt,snippet/title," +
                                "snippet/thumbnails/default/url,contentDetails/duration)");

                        VideoListResponse videoResponse = videoRequest.execute();
                        List<Video> listResponse = videoResponse.getItems();
                        ListIterator<Video> iterator = listResponse.listIterator();

                        while (iterator.hasNext()) {
                            Video s = iterator.next();

                            InputStream in = new URL(URI.create(s.getSnippet().getThumbnails().getDefault().getUrl())
                                    .toURL().toString()).openStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(in);
                            in.close();

                            DataHolder holder = new DataHolder();
                            holder.thumbnail = bitmap;
                            holder.title = s.getSnippet().getTitle();
                            holder.publishdate = s.getSnippet().getPublishedAt().toString();
                            String d = s.getContentDetails().getDuration().replace('H', ':');
                            d = d.replace('M', ':');
                            d = d.replace("S", "");
                            d = d.replace("PT", "");
                            holder.videolength = d;
                            holder.videoID = s.getId();
                            classes.add(holder);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            return classes;
        }

        private void onFound(final List<DataHolder> data) {

            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    createListView(data);
                }
            });

        }
    }
}