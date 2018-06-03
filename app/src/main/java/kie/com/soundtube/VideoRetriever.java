package kie.com.soundtube;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;

import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.liquidplayer.webkit.javascriptcore.JSContext;
import org.liquidplayer.webkit.javascriptcore.JSException;
import org.mp4parser.Container;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import static java.util.Arrays.asList;

/**
 * Created by kieChang on 2017/5/21.
 */
public class VideoRetriever {

    static final ArrayList<Integer> YOUTUBE_AUTO = new ArrayList<>(asList(-1));
    static final ArrayList<Integer> YOUTUBE_144 = new ArrayList<>(asList(160, 278, 17));
    static final ArrayList<Integer> YOUTUBE_240 = new ArrayList<>(asList(133, 242, 5));
    static final ArrayList<Integer> YOUTUBE_360 = new ArrayList<>(asList(134, 243, 18, 43));
    static final ArrayList<Integer> YOUTUBE_480 = new ArrayList<>(asList(135, 244, 44));
    static final ArrayList<Integer> YOUTUBE_720 = new ArrayList<>(asList(136, 247, 298, 302, 22, 45));
    static final ArrayList<Integer> YOUTUBE_1080 = new ArrayList<>(asList(137, 248, 299, 303, 37, 46));
    static final ArrayList<Integer> YOUTUBE_1440 = new ArrayList<>(asList(264, 271, 308));
    static final ArrayList<Integer> YOUTUBE_4K = new ArrayList<>(asList(266, 313, 315));
    static final ArrayList<Integer> YOUTUBE_60FPS = new ArrayList<>(asList(302, 303, 308, 315, 298, 299));
    static final ArrayList<Integer> DASH_VIDEO_MP4 = new ArrayList<>(asList(160, 133, 134, 135, 136, 137, 264, 266, 298, 299));
    static final ArrayList<Integer> DASH_VIDEO_WEBM = new ArrayList<>(asList(278, 242, 243, 244, 247, 248, 271, 313, 302, 303, 308, 315));
    static final ArrayList<Integer> DASH_AUDIO_MP4 = new ArrayList<>(asList(139, 140, 141, 256, 258, 325, 328));
    static final ArrayList<Integer> DASH_AUDIO_WEBM = new ArrayList<>(asList(171, 172, 249, 250, 251));
    static Integer defaultAudioQualityIndex = 0;
    Context context;
    BroadcastReceiver downloadReceiver;

    File currentMusicDir;
    File downloadCacheFileDir;
    File defaultMusicDir;

    private static final ArrayList<ArrayList<Integer>> VideoFormats = new ArrayList<>(asList(YOUTUBE_144, YOUTUBE_240, YOUTUBE_360
            , YOUTUBE_480, YOUTUBE_720, YOUTUBE_1080, YOUTUBE_1440, YOUTUBE_4K));
//    public static final int[] DASH_WEBM_OPUS_AUDIO = {249, 250, 251};

    Handler youtubeExtractorHandler;
    //    public static List<int[]> mPreferredVideoQualities = asList(YOUTUBE_1080, YOUTUBE_720, YOUTUBE_480, YOUTUBE_360
//            , YOUTUBE_240, YOUTUBE_144);
//    public static ArrayList<String> VideoQualityStrings = new ArrayList<>(asList("4K", "1440p", "1080p"
//    ,"720p", "480p", "360p", "240p", "144p", "audio only"));
    public static ArrayList<String> VideoQualityStrings = new ArrayList<>(asList("144p", "240p", "360p"
            , "480p", "720p", "1080p", "1440p", "4k"));
    JsonObject jsonObj = null;
    String decipherfunc = null;
    String basejsurl = null;
    static ArrayList<Long[]> downloadList = new ArrayList<>();

    public VideoRetriever(final Context context, Handler handler) {
        this.context = context;
        youtubeExtractorHandler = handler;
        downloadCacheFileDir = new File(context.getExternalCacheDir(), "downloaded");
        defaultMusicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SoundTube");
        downloadReceiver = new BroadcastReceiver() {

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
                                String resolution = null;
                                boolean merge = true;
                                while (c.moveToNext()) {
                                    if (videoID == null) {
                                        String id = c.getString(c.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
                                        videoID = id.substring(0, id.indexOf("_"));
                                        resolution = id.substring(id.indexOf("_") + 1);
                                    }

                                    if (c.getInt(statusIndex) != DownloadManager.STATUS_SUCCESSFUL) {
                                        merge = false;
                                    } else {
                                        url.add(0, Uri.parse(c.getString(urlIndex)).getPath());
                                    }
                                }
                                if (merge) {
                                    generateMergedVideo(videoID, resolution, url);
                                }
                            } else {
                                //nothing. keep waiting
                            }

                        }

                        break;
                    default:
                        break;
                }

            }
        };

    }

    private MediaSource getOfflineMediaSource(DataHolder dataHolder, ArrayList<Integer> preferredRes) {

        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "SoundTube"), bandwidthMeter);
        if (preferredRes != YOUTUBE_AUTO) {
            if (dataHolder.localUris.containsKey(preferredRes)) {
                MediaSource source = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(
                        Uri.parse(dataHolder.localUris.get(preferredRes)));
                return source;
            }
        } else {
            for (ArrayList<Integer> formats : VideoFormats) {
                if (dataHolder.localUris.containsKey(formats)) {
                    MediaSource source = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(
                            Uri.parse(dataHolder.localUris.get(preferredRes)));
                    return source;
                }
            }

        }
        return null;

    }


    private MediaSource getOnlineMediaSource(DataHolder dataHolder, ArrayList<Integer> preferredRes) {

        HashMap<Integer, String> onlineUris = new HashMap<>(dataHolder.onlineUris);
        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
// Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "SoundTube"), bandwidthMeter);
// This is the MediaSource representing the media to be played.

        if (preferredRes != YOUTUBE_AUTO) {
            for (Integer mpreferredRes : preferredRes) {
                if (onlineUris.containsKey(mpreferredRes)) {
                    ArrayList<Integer> dashes = filter(AND, filter(OR, DASH_VIDEO_MP4, DASH_VIDEO_WEBM), preferredRes);
                    ArrayList<Integer> sound = new ArrayList<>(onlineUris.keySet());
                    ArrayList<Integer> audios = filter(AND, filter(OR, DASH_AUDIO_MP4, DASH_AUDIO_WEBM), sound);
                    if (!dashes.isEmpty() && !audios.isEmpty()) {
                        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(Uri.parse(onlineUris.get(mpreferredRes)));
                        MediaSource audioSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(Uri.parse(onlineUris.get(audios.get(defaultAudioQualityIndex))));
                        MediaSource mediaSource = new MergingMediaSource(videoSource, audioSource);
                        return mediaSource;
                    } else {
                        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(Uri.parse(onlineUris.get(mpreferredRes)));
                        return mediaSource;
                    }
                }
            }
        } else {
            ArrayList<ArrayList<Integer>> available = getAvailableFormatType(dataHolder);
            for (int a = available.size() - 1; a >= 0; a--) {
                ArrayList<Integer> sameQuality = available.get(a);
                for (Integer aSameQuality : sameQuality) {
                    if (onlineUris.containsKey(aSameQuality)) {
                        ArrayList<Integer> dashes = filter(AND, filter(OR, DASH_VIDEO_MP4, DASH_VIDEO_WEBM), sameQuality);
                        ArrayList<Integer> sound = new ArrayList<>(onlineUris.keySet());
                        ArrayList<Integer> audios = filter(AND, filter(OR, DASH_AUDIO_MP4, DASH_AUDIO_WEBM), sound);
                        if (!dashes.isEmpty() && !audios.isEmpty()) {
                            MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                    .createMediaSource(Uri.parse(onlineUris.get(aSameQuality)));
                            MediaSource audioSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                    .createMediaSource(Uri.parse(onlineUris.get(audios.get(defaultAudioQualityIndex))));
                            MediaSource mediaSource = new MergingMediaSource(videoSource, audioSource);
                            return mediaSource;

                        } else {
                            MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                    .createMediaSource(Uri.parse(onlineUris.get(aSameQuality)));
                            return mediaSource;
                        }
                    }
                }
            }
        }

        return null;
    }

    public MediaSource getMediaSource(DataHolder dataHolder, ArrayList<Integer> quality, boolean online) {
        return online ? getOnlineMediaSource(dataHolder, quality) : getOfflineMediaSource(dataHolder, quality);
    }


    public static ArrayList<Boolean> getAvailableFormatIndex(DataHolder dataHolder) {
        ArrayList<Boolean> available = new ArrayList<>(VideoFormats.size());
        Collections.fill(available, false);
        HashMap<Integer, String> urls = dataHolder.onlineUris;
        for (int a = 0; a < VideoFormats.size(); a++) {
            ArrayList<Integer> b = VideoFormats.get(a);
            for (Integer format : b) {
                if (urls.containsKey(format)) {
                    available.set(a, true);
                    break;
                }
            }
        }
        return available;
    }

    public static ArrayList<String> getAvailableFormatString(DataHolder dataHolder) {
        ArrayList<String> available = new ArrayList<>();
        HashMap<Integer, String> urls = dataHolder.onlineUris;
        for (int a = 0; a < VideoQualityStrings.size(); a++) {
            for (int q : VideoFormats.get(a)) {
                if (urls.containsKey(q)) {
                    available.add(VideoQualityStrings.get(a));
                    break;
                }
            }
        }
        return available;
    }

    public static ArrayList<ArrayList<Integer>> getAvailableFormatType(DataHolder dataHolder) {
        ArrayList<ArrayList<Integer>> available = new ArrayList<>();
        HashMap<Integer, String> urls = dataHolder.onlineUris;
        for (int a = 0; a < VideoFormats.size(); a++) {
            ArrayList<Integer> b = VideoFormats.get(a);
            for (Integer format : b) {
                if (urls.containsKey(format)) {
                    available.add(b);
                    break;
                }
            }
        }
        return available;
    }

    public static String getAsString(ArrayList<Integer> in) {
        return VideoQualityStrings.get(VideoFormats.indexOf(in));
    }

    public static ArrayList<Integer> getAsFormat(String in) {
        return VideoFormats.get(VideoQualityStrings.indexOf(in));
    }

    static final String NOT = "not";
    static final String AND = "and";
    static final String OR = "or";

    public static ArrayList<Integer> filter(String options, ArrayList<Integer>... flags) {
        ArrayList<Integer> res = new ArrayList<>();
        if (options.contentEquals(AND)) {

            int min = Integer.MAX_VALUE;
            int MinIndex = 0;
            for (int a = 0; a < flags.length; a++) {
                ArrayList<Integer> c = flags[a];
                if (c.size() < min) {
                    MinIndex = a;
                    min = c.size();
                }
            }

            ArrayList<Integer> keep = new ArrayList<>(flags[MinIndex]);
            for (Integer com : keep) {
                boolean has = true;
                for (ArrayList<Integer> a : flags) {
                    if (!a.contains(com)) {
                        has = false;
                    }
                }
                if (has) {
                    res.add(com);
                }
            }

            return res;
        } else if (options.contentEquals(OR)) {

            for (ArrayList<Integer> a : flags) {
                for (Integer b : a) {
                    if (!res.contains(a)) {
                        res.add(b);
                    }
                }

            }

            return res;
        } else if (options.contentEquals(NOT)) {
            return res;
        }

        return res;
    }

    private String downloadWeb(String url) {
        String t = null;
        try {
            t = downloadWeb(new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return t;
    }

    private String downloadWeb(URL url) {
        StringBuilder response = new StringBuilder();
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64");
            int responseCode = connection.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    private HashMap<Integer, String> getVideoUris(String id) {

        String url = "https://www.youtube.com/watch?v=" + id;
        String videoHTML = downloadWeb(url);
        String videoID = url.substring(url.indexOf("=") + 1);
        String language = "en";
        String link = String.format(
                "https://www.youtube.com/get_video_info?video_id=%s&el=info&ps=default&eurl=&gl=US&hl=%s"
                , videoID, language); //correct


        HashMap<Integer, String> links = null;
        Document jsoup = Jsoup.parse(videoHTML);
        Elements q = jsoup.body().getElementsByTag("script");
        Elements j = q.eq(1);
        String part = j.toString();
        part = part.replaceAll("</?script>", "");
        int begin = part.indexOf("ytplayer.config = ");
        int l = 0;
        int r = 0;
        String ytplayer = null;
        if (begin >= 0) {
            for (int a = begin; a < part.length(); a++) {
                char i = (char) part.codePointAt(a);
                if (i == '{') {
                    l++;
                } else if (i == '}') {
                    r++;
                }
                if (r == l && r != 0) {
                    ytplayer = part.substring(begin + 18, a + 1);
                    break;
                }
            }
        }
        if (ytplayer != null) {
            links = new HashMap<>();
            jsonObj = new JsonParser().parse(ytplayer).getAsJsonObject();
            basejsurl = "https://www.youtube.com" + jsonObj.getAsJsonObject("assets")
                    .get("js").getAsString().replaceAll("\"", "");
            JsonObject videojson = jsonObj.getAsJsonObject("args");
            JsonElement encoded_s = videojson.get("url_encoded_fmt_stream_map");
            JsonElement adaptiveurl = videojson.get("adaptive_fmts");
            List<String> videos = new LinkedList<>();
            if (encoded_s != null && !encoded_s.getAsString().isEmpty()) {
                videos.addAll(asList(encoded_s.getAsString().split(",")));
            }
            if (adaptiveurl != null && !adaptiveurl.getAsString().isEmpty()) {
                videos.addAll(asList(adaptiveurl.getAsString().split(",")));
            }
            for (String e : videos) {
                e = decode(e);
                String[] fields = e.split("[&\\?;]");
                HashMap<String, String> splitmap = new HashMap<>();
                for (String i : fields) {
                    String[] pair = i.split("=");
                    if (pair.length == 2) {
                        splitmap.put(pair[0], pair[1]);
                    }
                }

                String[] params = splitmap.get("sparams").split(",");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(splitmap.get("url") + "?" + "sparams=" + splitmap.get
                        ("sparams") + "&key=" + splitmap.get("key"));
                if (splitmap.containsKey("s")) {
                    String fake = splitmap.get("s");
                    stringBuilder.append("&signature=" + decipher(fake));

                } else {
                    stringBuilder.append("&signature=" + splitmap.get("signature"));
                }

                for (String par : params) {
                    stringBuilder.append("&" + par + "=" + splitmap.get(par));
                }

                links.put(Integer.parseInt(splitmap.get("itag")), stringBuilder.toString());
            }
//        String getvideoinfo = downloadWeb(link);
//        String[] infos = getvideoinfo.split("&");
//        HashMap<String, String> videoinfomap = new HashMap<>();
//        for (String info : infos) {
//            String[] pair = info.split("=");
//            if (pair.length == 2) {
//                String key = pair[0];
//                String value = decode(pair[1]);
//                videoinfomap.put(key, value);
//            }
//        }
//        if (videoinfomap.containsKey("url_encoded_fmt_stream_map")) {
//            String url_encoded_fmt = videoinfomap.get(("url_encoded_fmt_stream_map"));
//            String adaptive_fmt = videoinfomap.get(("adaptive_fmts"));
//            List<String> videos = new LinkedList<>(asList(url_encoded_fmt.split(",")));
////            videos.addAll(asList(adaptive_fmt.split(",")));
//        }
        }

        return links;
    }

    private String decode(String encoded_s) {
        String decode = null;
        try {
            decode = URLDecoder.decode(URLDecoder.decode(encoded_s, "UTF-8"),
                    "UTF-8");
            //decode = decode.replaceAll("\\\\u0026", "&");
            decode = decode.replaceAll(" ", "");

        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        return decode;
    }

    private String decipher(String in) {
        String out = null;
        String funcname = null;

        if (decipherfunc == null) {
            String basejs = downloadWeb(basejsurl);
            StringBuilder dumby = new StringBuilder();
            for (int a = 0; a < in.indexOf("."); a++) {
                dumby.append(in.charAt(a));

            }
            dumby.append(".");
            for (int a = in.indexOf(".") + 1; a < in.length(); a++) {
                dumby.append(in.codePointAt(a));
            }

            String[] regexes = {"([\"\'])signature\\1\\s*,\\s*([a-zA-Z0-9$]+)\\("};

            for (int a = 0; a < regexes.length; a++) {
                Matcher matcher = Pattern.compile(regexes[a]).matcher(basejs);
                if (matcher.find()) {
                    funcname = matcher.group();
                    funcname = funcname.substring(funcname.indexOf(",") + 1, funcname.indexOf("("));
                }
            }
            String search1 = String.format("(?x)(?:function\\s+%s|[{;,]\\s*%s\\s*=\\s*function|var" +
                    "\\s+%s\\s*=\\s*function)\\s*\\(([^)]*)\\)\\s*" +
                    "\\{([^}]+)\\}", funcname, funcname, funcname);
            Matcher matcher1 = Pattern.compile(search1).matcher(basejs);

            if (matcher1.find()) {
                String res = matcher1.group();
                res = res + ";";
                res = res.replaceFirst(";", "");
                String temp = res.substring(res.indexOf("{") + 1, res.indexOf("}"));
                List<String> stmts = asList(temp.split(";"));
                String varfunc = null;
                for (String stmt : stmts) {
                    if (!stmt.contains("=")) {
                        String mems[] = stmt.split("\\.");
                        varfunc = mems[0];
                        break;
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                int l = 0;
                int r = 0;
                for (int p = basejs.indexOf("var " + varfunc + "="); p < basejs.length(); p++) {
                    char a = (char) basejs.codePointAt(p);
                    stringBuilder.append(a);
                    if (a == '{') {
                        l++;
                    } else if (a == '}') {
                        r++;
                    }
                    if (r == l && r != 0 && a == ';') {
                        break;
                    }
                }

                decipherfunc = stringBuilder.toString() + res + String.format("var output = %s", funcname);
                String input = decipherfunc + String.format("(\"%s\");", in);
                JSContext context = new JSContext();
                context.setExceptionHandler(new JSContext.IJSExceptionHandler() {
                    @Override
                    public void handle(JSException exception) {
                        exception.printStackTrace();
                        System.out.println("onError");
                    }
                });
                context.evaluateScript(input);
                out = context.property("output").toString();
            }
        } else {

            String input = decipherfunc + String.format("(\"%s\");", in);
            JSContext context = new JSContext();
            context.setExceptionHandler(new JSContext.IJSExceptionHandler() {
                @Override
                public void handle(JSException exception) {
                    exception.printStackTrace();
                    System.out.println("onError");
                }
            });
            context.evaluateScript(input);
            out = context.property("output").toString();
        }

        return out;
    }

    public void getDataHolder(final DataHolder dataHolder, final YouTubeExtractorListener listener) {
        youtubeExtractorHandler.post(new Runnable() {
            @Override
            public void run() {
                File musicFolder = new File(currentMusicDir, "SoundTube/" + dataHolder.videoID);

                if (musicFolder.exists()) {
                    List<File> locals = asList(musicFolder.listFiles());
                    HashMap<ArrayList<Integer>, String> local = new HashMap<>();
                    for (File f : locals) {
                        String name = f.getName();
                        Integer index = VideoQualityStrings.indexOf(name);
                        local.put(VideoFormats.get(index), f.toString());
                    }
                    dataHolder.localUris = local;
                }
                if (PlayerActivity.netConncted) {
                    dataHolder.onlineUris = getVideoUris(dataHolder.videoID);
                }

                if (!(dataHolder.onlineUris == null && dataHolder.localUris == null)) {
                    listener.onSuccess(dataHolder);

                } else {
                    String error = "No data";
                    listener.onFailure(error);
                }
            }
        });

    }

    public void downloadVideo(DataHolder dataHolder, ArrayList<Integer> preferredRes,
                              String resolution) throws DownloadException {
        downloadVideo(dataHolder, preferredRes, resolution, defaultMusicDir);

    }

    public void downloadVideo(DataHolder dataHolder, ArrayList<Integer> preferredRes,
                              String resolution, File destinationDir) throws DownloadException {
        HashMap<Integer, String> urls = dataHolder.onlineUris;
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        currentMusicDir = destinationDir;

        if (Environment.getExternalStorageState().contentEquals(Environment.MEDIA_MOUNTED)) {

            if (preferredRes != YOUTUBE_AUTO && !hasVideo(dataHolder.videoID, resolution)) {
                for (Integer mpreferredRes : preferredRes) {
                    if (urls.containsKey(mpreferredRes)) {
                        ArrayList<Integer> dashes = filter(AND, filter(OR, DASH_VIDEO_MP4, DASH_VIDEO_WEBM), preferredRes);
                        ArrayList<Integer> sound = new ArrayList<>(urls.keySet());
                        ArrayList<Integer> audios = filter(AND, filter(OR, DASH_AUDIO_MP4, DASH_AUDIO_WEBM), sound);
                        if (!dashes.isEmpty() && !audios.isEmpty()) {
                            DownloadManager.Request requestVideo = new DownloadManager.Request(Uri.parse(urls.get(mpreferredRes)));
                            File video = new File(downloadCacheFileDir, dataHolder.videoID + "/" + resolution + "Video");
                            requestVideo.setDestinationUri(Uri.fromFile(video));
                            requestVideo.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                            requestVideo.allowScanningByMediaScanner();
                            requestVideo.setDescription(dataHolder.videoID + "_" + resolution);
                            long v = manager.enqueue(requestVideo);

                            DownloadManager.Request requestAudio = new DownloadManager.Request(Uri.parse(urls.get(
                                    audios.get(defaultAudioQualityIndex))));
                            File audio = new File(downloadCacheFileDir, dataHolder.videoID + "/" + resolution + "Audio");
                            requestAudio.setDestinationUri(Uri.fromFile(audio));
                            requestAudio.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                            requestAudio.allowScanningByMediaScanner();
                            requestAudio.setDescription(dataHolder.videoID + "_" + resolution);
                            long a = manager.enqueue(requestAudio);

                            downloadList.add(new Long[]{v, a});

                        } else {
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urls.get(mpreferredRes)));
                            File des = new File(destinationDir, dataHolder.videoID + "/" + resolution + "Full");
                            request.setDestinationUri(Uri.fromFile(des));
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                            request.allowScanningByMediaScanner();
                            long all = manager.enqueue(request);
                        }

                    }
                    break;
                }
            }
        } else {
            throw new DownloadException(Environment.getExternalStorageState());
        }
    }

    public boolean hasVideo(String videoID, String resolution) {
//        if (!currentMusicDir.exists())
//            return false;
//        else {
        File file = new File(currentMusicDir, videoID + "/" + resolution + ".mp4");
        return file.exists();
//        }
    }

    public void generateMergedVideo(final String videoID, final String resolution, final ArrayList<String> url) {
        youtubeExtractorHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Track video = MovieCreator.build(url.get(0)).getTracks().get(0);
                    Track audio = MovieCreator.build(url.get(1)).getTracks().get(0);
                    Movie movie = new Movie();
                    movie.addTrack(video);
                    movie.addTrack(audio);
                    Container mp4file = new DefaultMp4Builder().build(movie);
                    if (Environment.getExternalStorageState().contentEquals(Environment.MEDIA_MOUNTED)) {
                        File file = new File(currentMusicDir,
                                videoID + "/" + resolution + ".mp4");
                        file.getParentFile().mkdirs();
                        FileChannel channel = new FileOutputStream(file).getChannel();
                        mp4file.writeContainer(channel);
                        channel.close();
                        File cache = new File(downloadCacheFileDir, videoID);
                        cache.delete();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    public final class YouTubeExtractorException extends Exception {
        public YouTubeExtractorException(String detailMessage) {
            super(detailMessage);
        }
    }

    public final class DownloadException extends Exception {
        public DownloadException(String detailMessage) {
            super(detailMessage);
        }
    }

    public interface YouTubeExtractorListener {
        void onSuccess(DataHolder result);

        void onFailure(String error);
    }


}

//    _formats = {
//        '5': {'ext': 'flv', 'width': 400, 'height': 240, 'acodec': 'mp3', 'abr': 64, 'vcodec': 'h263'},
//        '6': {'ext': 'flv', 'width': 450, 'height': 270, 'acodec': 'mp3', 'abr': 64, 'vcodec': 'h263'},
//        '13': {'ext': '3gp', 'acodec': 'aac', 'vcodec': 'mp4v'},
//   X     '17': {'ext': '3gp', 'width': 176, 'height': 144, 'acodec': 'aac', 'abr': 24, 'vcodec': 'mp4v'},
//   X     '18': {'ext': 'mp4', 'width': 640, 'height': 360, 'acodec': 'aac', 'abr': 96, 'vcodec': 'h264'},
//   X     '22': {'ext': 'mp4', 'width': 1280, 'height': 720, 'acodec': 'aac', 'abr': 192, 'vcodec': 'h264'},
//        '34': {'ext': 'flv', 'width': 640, 'height': 360, 'acodec': 'aac', 'abr': 128, 'vcodec': 'h264'},
//        '35': {'ext': 'flv', 'width': 854, 'height': 480, 'acodec': 'aac', 'abr': 128, 'vcodec': 'h264'},
//        # itag 36 videos are either 320x180 (BaW_jenozKc) or 320x240 (__2ABJjxzNo), abr varies as well
//   X     '36': {'ext': '3gp', 'width': 320, 'acodec': 'aac', 'vcodec': 'mp4v'},
//        '37': {'ext': 'mp4', 'width': 1920, 'height': 1080, 'acodec': 'aac', 'abr': 192, 'vcodec': 'h264'},
//        '38': {'ext': 'mp4', 'width': 4096, 'height': 3072, 'acodec': 'aac', 'abr': 192, 'vcodec': 'h264'},
//   X     '43': {'ext': 'webm', 'width': 640, 'height': 360, 'acodec': 'vorbis', 'abr': 128, 'vcodec': 'vp8'},
//        '44': {'ext': 'webm', 'width': 854, 'height': 480, 'acodec': 'vorbis', 'abr': 128, 'vcodec': 'vp8'},
//        '45': {'ext': 'webm', 'width': 1280, 'height': 720, 'acodec': 'vorbis', 'abr': 192, 'vcodec': 'vp8'},
//        '46': {'ext': 'webm', 'width': 1920, 'height': 1080, 'acodec': 'vorbis', 'abr': 192, 'vcodec': 'vp8'},
//        '59': {'ext': 'mp4', 'width': 854, 'height': 480, 'acodec': 'aac', 'abr': 128, 'vcodec': 'h264'},
//        '78': {'ext': 'mp4', 'width': 854, 'height': 480, 'acodec': 'aac', 'abr': 128, 'vcodec': 'h264'},
//
//
//        # 3D videos
//        '82': {'ext': 'mp4', 'height': 360, 'format_note': '3D', 'acodec': 'aac', 'abr': 128, 'vcodec': 'h264', 'preference': -20},
//        '83': {'ext': 'mp4', 'height': 480, 'format_note': '3D', 'acodec': 'aac', 'abr': 128, 'vcodec': 'h264', 'preference': -20},
//        '84': {'ext': 'mp4', 'height': 720, 'format_note': '3D', 'acodec': 'aac', 'abr': 192, 'vcodec': 'h264', 'preference': -20},
//        '85': {'ext': 'mp4', 'height': 1080, 'format_note': '3D', 'acodec': 'aac', 'abr': 192, 'vcodec': 'h264', 'preference': -20},
//        '100': {'ext': 'webm', 'height': 360, 'format_note': '3D', 'acodec': 'vorbis', 'abr': 128, 'vcodec': 'vp8', 'preference': -20},
//        '101': {'ext': 'webm', 'height': 480, 'format_note': '3D', 'acodec': 'vorbis', 'abr': 192, 'vcodec': 'vp8', 'preference': -20},
//        '102': {'ext': 'webm', 'height': 720, 'format_note': '3D', 'acodec': 'vorbis', 'abr': 192, 'vcodec': 'vp8', 'preference': -20},
//
//        # Apple HTTP Live Streaming
//        '91': {'ext': 'mp4', 'height': 144, 'format_note': 'HLS', 'acodec': 'aac', 'abr': 48, 'vcodec': 'h264', 'preference': -10},
//        '92': {'ext': 'mp4', 'height': 240, 'format_note': 'HLS', 'acodec': 'aac', 'abr': 48, 'vcodec': 'h264', 'preference': -10},
//        '93': {'ext': 'mp4', 'height': 360, 'format_note': 'HLS', 'acodec': 'aac', 'abr': 128, 'vcodec': 'h264', 'preference': -10},
//        '94': {'ext': 'mp4', 'height': 480, 'format_note': 'HLS', 'acodec': 'aac', 'abr': 128, 'vcodec': 'h264', 'preference': -10},
//        '95': {'ext': 'mp4', 'height': 720, 'format_note': 'HLS', 'acodec': 'aac', 'abr': 256, 'vcodec': 'h264', 'preference': -10},
//        '96': {'ext': 'mp4', 'height': 1080, 'format_note': 'HLS', 'acodec': 'aac', 'abr': 256, 'vcodec': 'h264', 'preference': -10},
//        '132': {'ext': 'mp4', 'height': 240, 'format_note': 'HLS', 'acodec': 'aac', 'abr': 48, 'vcodec': 'h264', 'preference': -10},
//        '151': {'ext': 'mp4', 'height': 72, 'format_note': 'HLS', 'acodec': 'aac', 'abr': 24, 'vcodec': 'h264', 'preference': -10},
//
//        # DASH mp4 video
//   X     '133': {'ext': 'mp4', 'height': 240, 'format_note': 'DASH video', 'vcodec': 'h264'},
//   X     '134': {'ext': 'mp4', 'height': 360, 'format_note': 'DASH video', 'vcodec': 'h264'},
//   X     '135': {'ext': 'mp4', 'height': 480, 'format_note': 'DASH video', 'vcodec': 'h264'},
//   X     '136': {'ext': 'mp4', 'height': 720, 'format_note': 'DASH video', 'vcodec': 'h264'},
//   X     '137': {'ext': 'mp4', 'height': 1080, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '138': {'ext': 'mp4', 'format_note': 'DASH video', 'vcodec': 'h264'},  # Height can vary (https://github.com/rg3/youtube-dl/issues/4559)
//   X     '160': {'ext': 'mp4', 'height': 144, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '212': {'ext': 'mp4', 'height': 480, 'format_note': 'DASH video', 'vcodec': 'h264'},
//   X     '264': {'ext': 'mp4', 'height': 1440, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '298': {'ext': 'mp4', 'height': 720, 'format_note': 'DASH video', 'vcodec': 'h264', 'fps': 60},
//        '299': {'ext': 'mp4', 'height': 1080, 'format_note': 'DASH video', 'vcodec': 'h264', 'fps': 60},
//        '266': {'ext': 'mp4', 'height': 2160, 'format_note': 'DASH video', 'vcodec': 'h264'},
//
//        # Dash mp4 audio
//        '139': {'ext': 'm4a', 'format_note': 'DASH audio', 'acodec': 'aac', 'abr': 48, 'container': 'm4a_dash'},
//   X     '140': {'ext': 'm4a', 'format_note': 'DASH audio', 'acodec': 'aac', 'abr': 128, 'container': 'm4a_dash'},
//        '141': {'ext': 'm4a', 'format_note': 'DASH audio', 'acodec': 'aac', 'abr': 256, 'container': 'm4a_dash'},
//        '256': {'ext': 'm4a', 'format_note': 'DASH audio', 'acodec': 'aac', 'container': 'm4a_dash'},
//        '258': {'ext': 'm4a', 'format_note': 'DASH audio', 'acodec': 'aac', 'container': 'm4a_dash'},
//        '325': {'ext': 'm4a', 'format_note': 'DASH audio', 'acodec': 'dtse', 'container': 'm4a_dash'},
//        '328': {'ext': 'm4a', 'format_note': 'DASH audio', 'acodec': 'ec-3', 'container': 'm4a_dash'},
//
//        # Dash webm
//        '167': {'ext': 'webm', 'height': 360, 'width': 640, 'format_note': 'DASH video', 'container': 'webm', 'vcodec': 'vp8'},
//        '168': {'ext': 'webm', 'height': 480, 'width': 854, 'format_note': 'DASH video', 'container': 'webm', 'vcodec': 'vp8'},
//        '169': {'ext': 'webm', 'height': 720, 'width': 1280, 'format_note': 'DASH video', 'container': 'webm', 'vcodec': 'vp8'},
//        '170': {'ext': 'webm', 'height': 1080, 'width': 1920, 'format_note': 'DASH video', 'container': 'webm', 'vcodec': 'vp8'},
//        '218': {'ext': 'webm', 'height': 480, 'width': 854, 'format_note': 'DASH video', 'container': 'webm', 'vcodec': 'vp8'},
//        '219': {'ext': 'webm', 'height': 480, 'width': 854, 'format_note': 'DASH video', 'container': 'webm', 'vcodec': 'vp8'},
//   X     '278': {'ext': 'webm', 'height': 144, 'format_note': 'DASH video', 'container': 'webm', 'vcodec': 'vp9'},
//   X     '242': {'ext': 'webm', 'height': 240, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//   X     '243': {'ext': 'webm', 'height': 360, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//   X     '244': {'ext': 'webm', 'height': 480, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '245': {'ext': 'webm', 'height': 480, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '246': {'ext': 'webm', 'height': 480, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//   X     '247': {'ext': 'webm', 'height': 720, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//   X     '248': {'ext': 'webm', 'height': 1080, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//   X     '271': {'ext': 'webm', 'height': 1440, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        # itag 272 videos are either 3840x2160 (e.g. RtoitU2A-3E) or 7680x4320 (sLprVF6d7Ug)
//        '272': {'ext': 'webm', 'height': 2160, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '302': {'ext': 'webm', 'height': 720, 'format_note': 'DASH video', 'vcodec': 'vp9', 'fps': 60},
//        '303': {'ext': 'webm', 'height': 1080, 'format_note': 'DASH video', 'vcodec': 'vp9', 'fps': 60},
//        '308': {'ext': 'webm', 'height': 1440, 'format_note': 'DASH video', 'vcodec': 'vp9', 'fps': 60},
//    X    '313': {'ext': 'webm', 'height': 2160, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '315': {'ext': 'webm', 'height': 2160, 'format_note': 'DASH video', 'vcodec': 'vp9', 'fps': 60},
//
//        # Dash webm audio
//   X     '171': {'ext': 'webm', 'acodec': 'vorbis', 'format_note': 'DASH audio', 'abr': 128},
//        '172': {'ext': 'webm', 'acodec': 'vorbis', 'format_note': 'DASH audio', 'abr': 256},
//
//        # Dash webm audio with opus inside
//   X     '249': {'ext': 'webm', 'format_note': 'DASH audio', 'acodec': 'opus', 'abr': 50},
//   X     '250': {'ext': 'webm', 'format_note': 'DASH audio', 'acodec': 'opus', 'abr': 70},
//   X     '251': {'ext': 'webm', 'format_note': 'DASH audio', 'acodec': 'opus', 'abr': 160},
//
//        # RTMP (unnamed)
//        '_rtmp': {'protocol': 'rtmp'},
//    }


