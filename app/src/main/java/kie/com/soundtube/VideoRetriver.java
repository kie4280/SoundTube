package kie.com.soundtube;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.liquidplayer.webkit.javascriptcore.*;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static java.util.Arrays.asList;

/**
 * Created by kieChang on 2017/5/21.
 */
public class VideoRetriver {

    public static final int YOUTUBE_VIDEO_QUALITY_SMALL_240 = 36;
    public static final int YOUTUBE_VIDEO_QUALITY_MEDIUM_360 = 18;
    public static final int YOUTUBE_VIDEO_QUALITY_HD_720 = 22;
    public static final int YOUTUBE_VIDEO_QUALITY_HD_1080 = 37;
    public static final int YOUTUBE_VIDEO_QUALITY_4K = 38;
    public static final int YOUTUBE_VIDEO_QUALITY_AUTO = 0;
    HandlerThread youtubeExtractorThread;
    Handler youtubeExtractorHandler, listenerHandler;
    public static List<Integer> mPreferredVideoQualities =  asList(YOUTUBE_VIDEO_QUALITY_4K, YOUTUBE_VIDEO_QUALITY_HD_1080,
                                                                    YOUTUBE_VIDEO_QUALITY_HD_720, YOUTUBE_VIDEO_QUALITY_MEDIUM_360, YOUTUBE_VIDEO_QUALITY_SMALL_240);;
    JsonObject jsonObj = null;

    public static void main(String[] args) {
        VideoRetriver test = new VideoRetriver();
        test.getVideo("https://www.youtube.com/watch?v=UtF6Jej8yb4");

    }

    public VideoRetriver() {
        youtubeExtractorThread = new HandlerThread("YouTubeExtractorThread",
                THREAD_PRIORITY_BACKGROUND);
        youtubeExtractorThread.start();
        youtubeExtractorHandler= new Handler(youtubeExtractorThread.getLooper());
        listenerHandler  = new Handler(Looper.getMainLooper());
    }

    public String downloadWeb(String url) {
        String t = null;
        try {
            t = downloadWeb(new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return t;
    }

    public String downloadWeb(URL url) {
        StringBuilder response = new StringBuilder();
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty( "User-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64");
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

    public HashMap<Integer, String> getVideo(String url) {

        String videoHTML = downloadWeb(url);
        String videoID = url.substring(url.indexOf("=") + 1);
        String language = "en";
        String link = String.format("https://www.youtube.com/get_video_info?video_id=%s&el=info&ps=default&eurl=&gl=US&hl=%s", videoID, language); //correct



        HashMap<Integer, String> links = new HashMap<>();
        Document jsoup = Jsoup.parse(videoHTML);
        Elements q = jsoup.body().getElementsByTag("script");
        Elements j = q.eq(1);
        String part = j.toString();
        part = part.replaceAll("</?script>", "");
        int begin = part.indexOf("ytplayer.config = ");
        int l = 0;
        int r = 0;
        String ytplayer = null;
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

        jsonObj = new JsonParser().parse(ytplayer).getAsJsonObject();
        String basejsurl = "https://www.youtube.com" + jsonObj.getAsJsonObject("assets")
                .get("js").getAsString().replaceAll("\"", "");
        JsonObject videojson = jsonObj.getAsJsonObject("args");

        if (videojson.has("url_encoded_fmt_stream_map")) {

            String encoded_s = videojson.get("url_encoded_fmt_stream_map").getAsString();
            String adaptiveurl = videojson.get("adaptive_fmts").getAsString();
            List<String> videos = new LinkedList<>(asList(encoded_s.split(",")));
            videos.addAll(asList(adaptiveurl.split(",")));
            String basejs = downloadWeb(basejsurl);

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
                    stringBuilder.append("&signature=" + decipher(basejs, fake));

                } else {
                    stringBuilder.append("&signature=" + splitmap.get("signature"));
                }

                for (String par : params) {
                    stringBuilder.append("&" + par + "=" + splitmap.get(par));
                }

                links.put(Integer.parseInt(splitmap.get("itag")), stringBuilder.toString());
            }
        }

        String getvideoinfo = downloadWeb(link);
        String[] infos = getvideoinfo.split("&");
        HashMap<String, String> videoinfomap = new HashMap<>();
        for(String info : infos) {
            String[] pair = info.split("=");
            if (pair.length == 2) {
                String key = pair[0];
                String value = decode(pair[1]);
                videoinfomap.put(key, value);
            }
        }
        if(videoinfomap.containsKey("url_encoded_fmt_stream_map")) {
            String url_encoded_fmt = videoinfomap.get(("url_encoded_fmt_stream_map"));
            String adaptive_fmt = videoinfomap.get(("adaptive_fmts"));
            List<String> videos= new LinkedList<>(asList(url_encoded_fmt.split(",")));
            videos.addAll(asList(adaptive_fmt.split(",")));
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

    private String decipher(String basejs, String in) {
        String out = null;
        StringBuilder dumby = new StringBuilder();
        for (int a = 0; a < in.indexOf("."); a++) {
            dumby.append(in.charAt(a));

        }
        dumby.append(".");
        for (int a = in.indexOf(".") + 1; a < in.length(); a++) {
            dumby.append(in.codePointAt(a));
        }

        String[] regexes = {"([\"\'])signature\\1\\s*,\\s*([a-zA-Z0-9$]+)\\("};
        String funcname = null;
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

            String input = stringBuilder.toString() + res +
                    String.format("var output = %s(\"%s\");", funcname, in);

            JSContext context = new JSContext();

            context.setExceptionHandler(new JSContext.IJSExceptionHandler() {
                @Override
                public void handle(JSException exception) {
                    exception.printStackTrace();
                    System.out.println("error");
                }
            });
            context.evaluateScript(input);
            out = context.property("output").toString();
        }
        return out;
    }

    public void startExtracting(final String url, final YouTubeExtractorListener listener) {
        youtubeExtractorHandler.post(new Runnable() {
            @Override
            public void run() {
                final HashMap<Integer, String> result = getVideo(url);
                listenerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.onSuccess(result);
                        }
                    }
                });
            }
        });

    }

    public final class YouTubeExtractorException extends Exception {
        public YouTubeExtractorException(String detailMessage) {
            super(detailMessage);
        }
    }

    public interface YouTubeExtractorListener {
        void onSuccess(HashMap<Integer, String> result);

        void onFailure(Error error);
    }

}


//    _formats = {
//        '5': {'ext': 'flv', 'width': 400, 'height': 240, 'acodec': 'mp3', 'abr': 64, 'vcodec': 'h263'},
//        '6': {'ext': 'flv', 'width': 450, 'height': 270, 'acodec': 'mp3', 'abr': 64, 'vcodec': 'h263'},
//        '13': {'ext': '3gp', 'acodec': 'aac', 'vcodec': 'mp4v'},
//        '17': {'ext': '3gp', 'width': 176, 'height': 144, 'acodec': 'aac', 'abr': 24, 'vcodec': 'mp4v'},
//        '18': {'ext': 'mp4', 'width': 640, 'height': 360, 'acodec': 'aac', 'abr': 96, 'vcodec': 'h264'},
//        '22': {'ext': 'mp4', 'width': 1280, 'height': 720, 'acodec': 'aac', 'abr': 192, 'vcodec': 'h264'},
//        '34': {'ext': 'flv', 'width': 640, 'height': 360, 'acodec': 'aac', 'abr': 128, 'vcodec': 'h264'},
//        '35': {'ext': 'flv', 'width': 854, 'height': 480, 'acodec': 'aac', 'abr': 128, 'vcodec': 'h264'},
//        # itag 36 videos are either 320x180 (BaW_jenozKc) or 320x240 (__2ABJjxzNo), abr varies as well
//        '36': {'ext': '3gp', 'width': 320, 'acodec': 'aac', 'vcodec': 'mp4v'},
//        '37': {'ext': 'mp4', 'width': 1920, 'height': 1080, 'acodec': 'aac', 'abr': 192, 'vcodec': 'h264'},
//        '38': {'ext': 'mp4', 'width': 4096, 'height': 3072, 'acodec': 'aac', 'abr': 192, 'vcodec': 'h264'},
//        '43': {'ext': 'webm', 'width': 640, 'height': 360, 'acodec': 'vorbis', 'abr': 128, 'vcodec': 'vp8'},
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
//        '133': {'ext': 'mp4', 'height': 240, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '134': {'ext': 'mp4', 'height': 360, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '135': {'ext': 'mp4', 'height': 480, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '136': {'ext': 'mp4', 'height': 720, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '137': {'ext': 'mp4', 'height': 1080, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '138': {'ext': 'mp4', 'format_note': 'DASH video', 'vcodec': 'h264'},  # Height can vary (https://github.com/rg3/youtube-dl/issues/4559)
//        '160': {'ext': 'mp4', 'height': 144, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '212': {'ext': 'mp4', 'height': 480, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '264': {'ext': 'mp4', 'height': 1440, 'format_note': 'DASH video', 'vcodec': 'h264'},
//        '298': {'ext': 'mp4', 'height': 720, 'format_note': 'DASH video', 'vcodec': 'h264', 'fps': 60},
//        '299': {'ext': 'mp4', 'height': 1080, 'format_note': 'DASH video', 'vcodec': 'h264', 'fps': 60},
//        '266': {'ext': 'mp4', 'height': 2160, 'format_note': 'DASH video', 'vcodec': 'h264'},
//
//        # Dash mp4 audio
//        '139': {'ext': 'm4a', 'format_note': 'DASH audio', 'acodec': 'aac', 'abr': 48, 'container': 'm4a_dash'},
//        '140': {'ext': 'm4a', 'format_note': 'DASH audio', 'acodec': 'aac', 'abr': 128, 'container': 'm4a_dash'},
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
//        '278': {'ext': 'webm', 'height': 144, 'format_note': 'DASH video', 'container': 'webm', 'vcodec': 'vp9'},
//        '242': {'ext': 'webm', 'height': 240, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '243': {'ext': 'webm', 'height': 360, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '244': {'ext': 'webm', 'height': 480, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '245': {'ext': 'webm', 'height': 480, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '246': {'ext': 'webm', 'height': 480, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '247': {'ext': 'webm', 'height': 720, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '248': {'ext': 'webm', 'height': 1080, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '271': {'ext': 'webm', 'height': 1440, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        # itag 272 videos are either 3840x2160 (e.g. RtoitU2A-3E) or 7680x4320 (sLprVF6d7Ug)
//        '272': {'ext': 'webm', 'height': 2160, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '302': {'ext': 'webm', 'height': 720, 'format_note': 'DASH video', 'vcodec': 'vp9', 'fps': 60},
//        '303': {'ext': 'webm', 'height': 1080, 'format_note': 'DASH video', 'vcodec': 'vp9', 'fps': 60},
//        '308': {'ext': 'webm', 'height': 1440, 'format_note': 'DASH video', 'vcodec': 'vp9', 'fps': 60},
//        '313': {'ext': 'webm', 'height': 2160, 'format_note': 'DASH video', 'vcodec': 'vp9'},
//        '315': {'ext': 'webm', 'height': 2160, 'format_note': 'DASH video', 'vcodec': 'vp9', 'fps': 60},
//
//        # Dash webm audio
//        '171': {'ext': 'webm', 'acodec': 'vorbis', 'format_note': 'DASH audio', 'abr': 128},
//        '172': {'ext': 'webm', 'acodec': 'vorbis', 'format_note': 'DASH audio', 'abr': 256},
//
//        # Dash webm audio with opus inside
//        '249': {'ext': 'webm', 'format_note': 'DASH audio', 'acodec': 'opus', 'abr': 50},
//        '250': {'ext': 'webm', 'format_note': 'DASH audio', 'acodec': 'opus', 'abr': 70},
//        '251': {'ext': 'webm', 'format_note': 'DASH audio', 'acodec': 'opus', 'abr': 160},
//
//        # RTMP (unnamed)
//        '_rtmp': {'protocol': 'rtmp'},
//    }