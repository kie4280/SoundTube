package kie.com.soundtube;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.Joiner;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import pub.devrel.easypermissions.EasyPermissions;


public class YoutubeClient {

    YouTube.Search.List search;
    YouTube.Search.List tokensearch;
    String nextPageToken = null;
    String prevPageToken = null;

    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;
    private static final int MAX_LOAD_PAGES = 20;
    private static String APIKey = null;
    private static final String Order = "relevance";
    private static final String Type = "video";
    private YouTube youtube;
    private Handler WorkHandler = null;
    private Context context;
    public boolean signedIn = false;
    private ArrayList<String> tokens = new ArrayList<>(500);
    private HashMap<String, List<DataHolder>> pages = new HashMap<>(500);
    public HashMap<String, SearchContainer> searches = new HashMap<>(50);
    private int index = 0;
    private SearchListResponse searchResponse;
    private SearchListResponse tokenseachResponse;
    private static final String[] SCOPES = {YouTubeScopes.YOUTUBEPARTNER, YouTubeScopes.YOUTUBE_READONLY};
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String YOUTUBE_CLIENT_PREFERENCES = BuildConfig.APPLICATION_ID + ".youtubePreferences";
    static final String NO_DATA = "no data";
    static final String NOT_SIGN_IN = "not signed in";

    public YoutubeClient(Context context, Handler handler) {
        this.context = context;
        this.WorkHandler = handler;
        APIKey = getAPIKey();
        getYoutubeService();

    }

    public void getYoutubeService() {

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        if (!EasyPermissions.hasPermissions(context, Manifest.permission.GET_ACCOUNTS)) {
            youtube = new YouTube.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), null)
                    .setApplicationName("SoundTube").build();
        } else if (credential.getSelectedAccountName() == null) {
            String accountName = context.getSharedPreferences(YOUTUBE_CLIENT_PREFERENCES
                    , Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                credential.setSelectedAccountName(accountName);
                youtube = new YouTube.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), credential)
                        .setApplicationName("SoundTube").build();
            } else {
                youtube = new YouTube.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), null)
                        .setApplicationName("SoundTube").build();
            }
        } else {
            youtube = new YouTube.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), credential)
                    .setApplicationName("SoundTube").build();
        }
    }

    private String getAPIKey() {

        Properties properties = new Properties();
        try {
            InputStream in = context.getAssets().open("config.properties");
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String t = properties.getProperty("youtube_api_key");
        t = t.replaceAll("#813|#jkfAS", "");
        if (t != null) {
            return t;
        } else {
            return "Cannot read config file";
        }

    }

    private void addSearchResult(String id, SearchContainer container) {
        if (searches.size() >= 49) {
            Iterator<String> iterator = searches.keySet().iterator();
            if (iterator.hasNext()) {
                searches.remove(iterator.next());
            }
        }
        searches.put(id, container);

    }

    public void newSearch(final String queryTerm) {

        WorkHandler.post(new Runnable() {
            @Override
            public void run() {
                if (searches.containsKey(queryTerm)) {
                    SearchContainer container = searches.get(queryTerm);
                    pages = container.pages;
                    tokens = container.tokens;
                    search = container.search;
                    tokensearch = container.tokensearch;
                    nextPageToken = container.nextPageToken;
                    prevPageToken = container.prevPageToken;
                } else {

                    try {
                        pages = new HashMap<>(500);
                        tokens = new ArrayList<>(500);
                        search = youtube.search().list("snippet");
                        search.setKey(APIKey);
                        search.setType(Type);
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
                        tokensearch.setType(Type);
                        tokenseachResponse = tokensearch.execute();

                        nextPageToken = tokenseachResponse.getNextPageToken();
                        prevPageToken = tokenseachResponse.getPrevPageToken();

                        index = 0;
                        if (nextPageToken != null) {
                            String head = tokensearch.setPageToken(nextPageToken).execute().getPrevPageToken();
                            tokens.add(head);
                        }
                        SearchContainer container = new SearchContainer();
                        container.pages = pages;
                        container.tokens = tokens;
                        container.nextPageToken = nextPageToken;
                        container.prevPageToken = prevPageToken;
                        container.search = search;
                        container.tokensearch = tokensearch;
                        addSearchResult(queryTerm, container);

                    } catch (IOException e) {
                        Log.w("YoutubeClient", "There was an IO onError: " + e.getCause() + " : " + e.getMessage());
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

            }
        });
        Log.d("youtubeClient", "newsearch");

    }

    public void loadRelatedVideos(final String id) {
        WorkHandler.post(new Runnable() {
            @Override
            public void run() {
                if (searches.containsKey(id)) {
                    SearchContainer container = searches.get(id);
                    pages = container.pages;
                    tokens = container.tokens;
                    search = container.search;
                    tokensearch = container.tokensearch;
                    nextPageToken = container.nextPageToken;
                    prevPageToken = container.prevPageToken;
                } else {
                    try {
                        pages = new HashMap<>(500);
                        tokens = new ArrayList<>(500);
                        search = youtube.search().list("snippet");
                        search.setRelatedToVideoId(id);
                        search.setType(Type);
                        search.setFields("items(id/kind,id/videoId)");
                        search.setKey(APIKey);
                        search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

                        tokensearch = youtube.search().list("snippet");
                        tokensearch.setRelatedToVideoId(id);
                        tokensearch.setKey(APIKey);
                        tokensearch.setFields("nextPageToken,prevPageToken,pageInfo/totalResults");
                        tokensearch.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
//                    tokensearch.setOrder(Order);
                        tokensearch.setType(Type);
                        tokenseachResponse = tokensearch.execute();
                        nextPageToken = tokenseachResponse.getNextPageToken();
                        prevPageToken = tokenseachResponse.getPrevPageToken();
                        tokens.clear();
                        pages.clear();
                        index = 0;
                        if (nextPageToken != null) {
                            String head = tokensearch.setPageToken(nextPageToken).execute().getPrevPageToken();
                            tokens.add(head);
                        }
                        SearchContainer container = new SearchContainer();
                        container.pages = pages;
                        container.tokens = tokens;
                        container.nextPageToken = nextPageToken;
                        container.prevPageToken = prevPageToken;
                        container.search = search;
                        container.tokensearch = tokensearch;
                        addSearchResult(id, container);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                                if (pages.size() > MAX_LOAD_PAGES) {
                                    pages.remove(tokens.get(0));
                                }
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
                                if (pages.size() > MAX_LOAD_PAGES) {
                                    pages.remove(tokens.get(tokens.size() - 1));
                                }
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
                                result.onError(NO_DATA);
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
                    result.onError("Array index out of bounds");
                }
            }
        });
    }

    public void cancel() {
        WorkHandler.removeCallbacks(null);

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

        void onError(String error);
    }

    private class SearchContainer {
        ArrayList<String> tokens = new ArrayList<>(500);
        HashMap<String, List<DataHolder>> pages = new HashMap<>(500);
        YouTube.Search.List search;
        YouTube.Search.List tokensearch;
        String nextPageToken = null;
        String prevPageToken = null;
    }

}