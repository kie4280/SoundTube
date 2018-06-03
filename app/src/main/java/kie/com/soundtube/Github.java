package kie.com.soundtube;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;


/**
 * Created by kieChang on 2017/11/5.
 */

public class Github {

    public String versionName = null;
    public String url = null;
    Context context;

    public Github(Context c) {
        this.context = c;
    }

    public String getToken() {

        Properties properties = new Properties();
        try {
            InputStream in = context.getAssets().open("config.properties");
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String t = properties.getProperty("github_access_token");
        t = t.replaceAll("#813|#jkfAS", "");
        if (t != null) {
            return t;
        } else {
            return "Cannot read config file";
        }
    }

    public void report(String msg) {

        try {

            HashMap<String, String> params = new HashMap<String, String>(1, 1);
            params.put("body", msg);
            Gson gson = new Gson();
            byte[] data = gson.toJson(params).getBytes("UTF-8");
            HttpURLConnection urlConnection = (HttpURLConnection) new URL("https://api.github.com/repos/kie4280/SoundTube/issues/1/comments").openConnection();
            urlConnection.setDoOutput(true);

            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Authorization", "token " + getToken());
            urlConnection.setRequestProperty("User-Agent", "GitHubJava/2.1.0");
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            DataOutputStream dataOutputStream = new DataOutputStream(urlConnection.getOutputStream());
            dataOutputStream.write(data);
            dataOutputStream.flush();
            dataOutputStream.close();
            Log.d("Github", urlConnection.getResponseMessage());

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public String getupdate() {

        if (versionName == null) {
            try {

                HttpURLConnection urlConnection = (HttpURLConnection) new URL
                        ("https://api.github.com/repos/kie4280/SoundTube/releases/latest")
                        .openConnection();
                urlConnection.setRequestMethod("GET");
//                urlConnection.setRequestProperty("Authorization", "token " + token);
                urlConnection.setRequestProperty("User-Agent", "GitHubJava/2.1.0");
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();
                Log.d("Github", urlConnection.getResponseMessage());
                String content = stringBuilder.toString();

                JsonObject object = new JsonParser().parse(content).getAsJsonObject();
                versionName = object.get("tag_name").getAsString();
                url = object.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("browser_download_url").getAsString();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (versionName != null && BuildConfig.VERSION_NAME.contentEquals(versionName)) {
            return "latest";
        } else {
            return url;
        }

    }
}
