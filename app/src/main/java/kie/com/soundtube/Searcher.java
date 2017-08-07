package kie.com.soundtube;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


public class Searcher {

    //private int tokenIndex = 0;
    YouTube.Search.List search;
    String nextPageToken = null;
    String prevPageToken = null;
    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;
    private static final String APIKey = "AIzaSyANfhXgNlxpmkWKl7JNWdyRQZx4uS2vYuo";
    private static final String Order = "relevance";
    private YouTube youtube;
    private Handler WorkHandler = null;
    private Context context;
    public LinkedList<String> tokens = new LinkedList<>();
    public ListIterator<String> iterator = tokens.listIterator();

    public Searcher(Context context, Handler handler) {
        this.context = context;
        this.WorkHandler = handler;
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("power saver").build();

    }

    public void newSearch(final String queryTerm, final YoutubeSearchResult result) {

        Runnable run1 = new Runnable() {
            @Override
            public void run() {
                try {
                    // This object is used to make YouTube Data API requests. The last
                    // argument is required, but since we don't need anything
                    // initialized when the HttpRequest is initialized, we override
                    // the interface and provide a no-op function.
                    Toast toast = Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT);
                    toast.show();
                    search = youtube.search().list("snippet");
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
                    search.setOrder(Order);
                    // Call the API and print results.
                    SearchListResponse searchResponse = search.execute();
                    nextPageToken = searchResponse.getNextPageToken();
                    prevPageToken = searchResponse.getPrevPageToken();
                    List<SearchResult> searchResultList = searchResponse.getItems();
                    if (searchResultList.isEmpty()) {
                        result.noData();
                    } else {
                        result.onFound(toClass(searchResultList), nextPageToken != null, prevPageToken != null);
                    }

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

    public void nextPage(final YoutubeSearchResult result) {
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
                        result.onFound(toClass(searchResultList), nextPageToken != null, prevPageToken != null);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    result.noData();
                }

            }
        });

    }

    public void prevPage(final YoutubeSearchResult result) {
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
                        result.onFound(toClass(searchResultList), nextPageToken != null, prevPageToken != null);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    result.noData();
                }
            }
        });

    }

    public void loadRelatedVideos(final String id, final YoutubeSearchResult result) {
        WorkHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    YouTube.Search.List search = youtube.search().list("snippet");
                    search.setRelatedToVideoId(id);
                    search.setType("video");
                    search.setFields("items(id/kind,id/videoId),nextPageToken,prevPageToken,pageInfo/totalResults");
                    search.setKey(APIKey);
                    search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
                    SearchListResponse response = search.execute();
                    nextPageToken = response.getNextPageToken();
                    prevPageToken = response.getPrevPageToken();
                    result.onFound(toClass(response.getItems()), nextPageToken != null, prevPageToken != null);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });
    }

    public List<DataHolder> toClass(List<SearchResult> searchResultList) {

        List<String> videoList = new ArrayList<>((int) NUMBER_OF_VIDEOS_RETURNED);
        List<DataHolder> classes = new LinkedList<>();

//        if (searchResultList != null) {
//            if (searchResultList.size() == 0) {
//                Toast toast = Toast.makeText(context, "No matching result", Toast.LENGTH_SHORT);
//                toast.show();
//            } else {


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

            for (Video s : listResponse) {
                InputStream in = new URL(URI.create(s.getSnippet().getThumbnails().getDefault().getUrl())
                        .toURL().toString()).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                in.close();

                DataHolder holder = new DataHolder();
                holder.thumbnail = bitmap;
                holder.title = s.getSnippet().getTitle();
                holder.publishdate = s.getSnippet().getPublishedAt().toString();
                String d = s.getContentDetails().getDuration();
                StringBuilder stringBuilder = new StringBuilder();
                if (d.contains("D")) {
                    stringBuilder.append(d.substring(d.indexOf("P") + 1, d.indexOf("D")));
                    stringBuilder.append(":");
                }
                if (d.contains("H")) {
                    int index = d.indexOf("H");
                    if (Character.isDigit(d.charAt(index - 2))) {
                        stringBuilder.append(d.charAt(index - 2));
                    } else {
                        stringBuilder.append(0);
                    }
                    stringBuilder.append(d.charAt(index - 1));
                    stringBuilder.append(":");
                }
                if (d.contains("M")) {
                    int index = d.indexOf("M");
                    if (Character.isDigit(d.charAt(index - 2))) {
                        stringBuilder.append(d.charAt(index - 2));
                    } else {
                        stringBuilder.append(0);
                    }
                    stringBuilder.append(d.charAt(index - 1));
                    stringBuilder.append(":");
                }
                if (d.contains("S")) {
                    int index = d.indexOf("S");
                    if (Character.isDigit(d.charAt(index - 2))) {
                        stringBuilder.append(d.charAt(index - 2));
                    } else {
                        stringBuilder.append(0);
                    }
                    stringBuilder.append(d.charAt(index - 1));
                }


                holder.videolength = stringBuilder.toString();
                holder.videoID = s.getId();
                classes.add(holder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//            }
//        }


        return classes;
    }

    public interface YoutubeSearchResult {
        void onFound(final List<DataHolder> data, final boolean hasnext, final boolean hasprev);

        void noData();
    }

}