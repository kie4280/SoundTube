package kie.com.soundtube;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.liquidplayer.webkit.javascriptcore.JSContext;
import org.liquidplayer.webkit.javascriptcore.JSObject;
import org.liquidplayer.webkit.javascriptcore.JSValue;

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
import static java.util.Arrays.asList;

/**
 * Created by kieChang on 2017/5/21.
 */
public class VideoRetriver {

    private static final int YOUTUBE_VIDEO_QUALITY_SMALL_240 = 36;
    private static final int YOUTUBE_VIDEO_QUALITY_MEDIUM_360 = 18;
    private static final int YOUTUBE_VIDEO_QUALITY_HD_720 = 22;
    private static final int YOUTUBE_VIDEO_QUALITY_HD_1080 = 37;


    private List<Integer> mPreferredVideoQualities;
    JsonObject jsonObj = null;

    public static void main(String[] args) {
        VideoRetriver test = new VideoRetriver();
        test.getVideo("https://www.youtube.com/watch?v=UtF6Jej8yb4");

    }

    public VideoRetriver() {

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

    public HashMap<String, String> getVideo(String url) {

        String videoHTML = downloadWeb(url);
        String videoID = url.substring(url.indexOf("=") + 1);
        String language = "en";
        String link = String.format("https://www.youtube.com/get_video_info?video_id=%s&el=info&ps=default&eurl=&gl=US&hl=%s", videoID, language); //correct
        //String getvideoinfo = downloadWeb(link);
        HashMap<String, String> links = new HashMap<>();
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
                    stringBuilder.append("&signature=" + decipher(basejsurl, fake));

                } else {
                    stringBuilder.append("&signature=" + splitmap.get("signature"));
                }

                for (String par : params) {
                    stringBuilder.append("&" + par + "=" + splitmap.get(par));
                }

                links.put(splitmap.get("itag"), stringBuilder.toString());
                System.out.println(stringBuilder.toString());
            }


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

    private String decipher(String basejsurl, String in) {
        String out = null;
        StringBuilder dumby = new StringBuilder();
        for (int a = 0; a < in.indexOf("."); a++) {
            dumby.append(in.charAt(a));

        }
        dumby.append(".");
        for (int a = in.indexOf(".") + 1; a < in.length(); a++) {
            dumby.append(in.codePointAt(a));
        }
        String basejs = downloadWeb(basejsurl);
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


            JSContext context = new JSContext();
            JSObject jsObject = new JSObject(context);
            jsObject.
            out = context.evaluateScript(stringBuilder.toString() + res, ).toString();

            ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
            try {
                scriptEngine.eval(stringBuilder.toString() + res);
                Invocable func = (Invocable) scriptEngine;
                out = (String) func.invokeFunction(funcname, in);
            } catch (ScriptException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return out;
    }

}
