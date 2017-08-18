package kie.com.soundtube;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


public class Searcher {

    YouTube.Search.List search;
    YouTube.Search.List tokensearch;
    String nextPageToken = null;
    String prevPageToken = null;

    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;
    private static final String APIKey = "AIzaSyANfhXgNlxpmkWKl7JNWdyRQZx4uS2vYuo";
    private static final String Order = "relevance";
    private YouTube youtube;
    private Handler WorkHandler = null;
    private Context context;
    public ArrayList<String> tokens = new ArrayList<>(500);
    public HashMap<String, List<DataHolder>> pages = new HashMap<>(500);
    public int index = 0;
    private SearchListResponse searchResponse;
    private SearchListResponse tokenseachResponse;


    public Searcher(Context context, Handler handler) {
        this.context = context;
        this.WorkHandler = handler;
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("power saver").build();

    }

    public void newSearch(final String queryTerm) {

        WorkHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // This object is used to make YouTube Data API requests. The last
                    // argument is required, but since we don't need anything
                    // initialized when the HttpRequest is initialized, we override
                    // the interface and provide a no-op function.

                    search = youtube.search().list("snippet");
                    // Define the API request for retrieving search results.
                    search.setKey(APIKey);
                    // Set your developer key from the {{ Google Cloud Console }} for
                    // non-authenticated requests. See:
                    // {{ https://cloud.google.com/console }}
                    search.setType("video");
                    // To increase efficiency, only retrieve the fields that the
                    // application uses.
                    search.setFields("items(id/kind,id/videoId)");
                    search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
                    search.setQ(queryTerm);
                    search.setOrder(Order);

                    tokensearch = youtube.search().list("snippet");
                    tokensearch.setKey(APIKey);
                    tokensearch.setFields("nextPageToken,prevPageToken,pageInfo/totalResults");
                    tokensearch.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
                    tokensearch.setQ(queryTerm);
                    tokensearch.setOrder(Order);
                    tokenseachResponse = tokensearch.execute();
                    nextPageToken = tokenseachResponse.getNextPageToken();

                    prevPageToken = tokenseachResponse.getPrevPageToken();
                    tokens.clear();
                    index = 0;
                    if (nextPageToken != null) {
                        String head = tokensearch.setPageToken(nextPageToken).execute().getPrevPageToken();
                        tokens.add(head);
                    }


                } catch (IOException e) {
                    System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });

    }

    public void loadRelatedVideos(final String id) {
        WorkHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    search = youtube.search().list("snippet");
                    search.setRelatedToVideoId(id);
                    search.setType("video");
                    search.setFields("items(id/kind,id/videoId)");
                    search.setKey(APIKey);
                    search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
                    tokensearch = youtube.search().list("snippet");
                    tokensearch.setRelatedToVideoId(id);
                    tokensearch.setKey(APIKey);
                    tokensearch.setFields("nextPageToken,prevPageToken,pageInfo/totalResults");
                    tokensearch.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
//                    tokensearch.setOrder(Order);
                    tokensearch.setType("video");
                    tokenseachResponse = tokensearch.execute();
                    nextPageToken = tokenseachResponse.getNextPageToken();
                    prevPageToken = tokenseachResponse.getPrevPageToken();
                    tokens.clear();
                    index = 0;
                    if (nextPageToken != null) {
                        String head = tokensearch.setPageToken(nextPageToken).execute().getPrevPageToken();
                        tokens.add(head);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });
    }

    public void nextPage() {
        WorkHandler.post(new Runnable() {
            @Override
            public void run() {
                if (nextPageToken != null) {
                    Log.d("index", Integer.toString(index));

                    if (index < tokens.size() - 1) {
                        nextPageToken = tokens.get(index + 1);
                        prevPageToken = tokens.get(index);
                    } else {
                        try {
                            tokensearch.setPageToken(nextPageToken);
                            tokenseachResponse = tokensearch.execute();
                            nextPageToken = tokenseachResponse.getNextPageToken();
                            prevPageToken = tokenseachResponse.getPrevPageToken();
                            if (nextPageToken != null) {
                                tokens.add(nextPageToken);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    index++;
                }

            }
        });

    }

    public void prevPage() {
        WorkHandler.post(new Runnable() {
            @Override
            public void run() {
                if (prevPageToken != null) {
                    Log.d("index", Integer.toString(index));
                    index--;
                    if (index >= 1) {
                        prevPageToken = tokens.get(index - 1);
                        nextPageToken = tokens.get(index);
                    } else {
                        try {
                            tokensearch.setPageToken(prevPageToken);
                            tokenseachResponse = tokensearch.execute();
                            nextPageToken = tokenseachResponse.getNextPageToken();
                            prevPageToken = tokenseachResponse.getPrevPageToken();
                            index = 0;
                            if (prevPageToken != null) {
                                tokens.add(0, prevPageToken);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        });

    }


    public void getResults(final YoutubeSearchResult result) {
        WorkHandler.post(new Runnable() {
            @Override
            public void run() {
                if (index >= 0 && index < tokens.size()) {
                    String key = tokens.get(index);
                    if (pages.containsKey(key)) {
                        result.onFound(pages.get(key), nextPageToken != null, prevPageToken != null);
                    } else {
                        try {
                            search.setPageToken(key);
                            searchResponse = search.execute();
                            List<SearchResult> searchResultList = searchResponse.getItems();
                            if (searchResultList.isEmpty()) {
                                result.noData();
                            } else {
                                List<DataHolder> dataHolders = toClass(searchResultList);
                                pages.put(key, dataHolders);
                                result.onFound(dataHolders, nextPageToken != null, prevPageToken != null);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    result.noData();
                }
            }
        });
    }


    private List<DataHolder> toClass(List<SearchResult> searchResultList) {

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
                int highest = 0;
                if (d.contains("D")) {
                    stringBuilder.append(d.substring(d.indexOf("P") + 1, d.indexOf("D")));
                    stringBuilder.append(":");
                    highest = 3;
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
                    highest = 2;
                } else if (highest > 2) {
                    stringBuilder.append("00");
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
                    highest = 1;

                } else if (highest > 1) {
                    stringBuilder.append("00");
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
                } else if (highest > 0) {
                    stringBuilder.append("00");
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