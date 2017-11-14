package kie.com.soundtube;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;


/**
 * Created by kieChang on 2017/11/5.
 */

public class Github {
    public final static String token = "c2a9e249db01fda7dfc17885d46c975d107c0b19";
    public String versionName = null;

    public void report(String msg) {

        try {

            HashMap<String, String> params = new HashMap<String, String>(1, 1);
            params.put("body", msg);
            Gson gson = new Gson();
            byte[] data = gson.toJson(params).getBytes("UTF-8");
            HttpURLConnection urlConnection = (HttpURLConnection) new URL("https://api.github.com/repos/kie4280/SoundTube/issues/1/comments").openConnection();
            urlConnection.setDoOutput(true);

            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Authorization", "token " + token);
            urlConnection.setRequestProperty("User-Agent", "GitHubJava/2.1.0");
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            DataOutputStream dataOutputStream = new DataOutputStream(urlConnection.getOutputStream());


            dataOutputStream.write(data);
//            dataOutputStream.writeBytes("{ \"body\": \"gfhsdgfhjhjhehergherjghjerkghgh egrer\"}");
            dataOutputStream.flush();
            dataOutputStream.close();
            System.out.println(urlConnection.getResponseMessage());
//            GitHubClient client = new GitHubClient();
//            client.setOAuth2Token(token);
//            IssueService service = new IssueService(client);
//            service.createComment("kie4280", "SoundTube", 1, msg);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public String getupdate() {

        String url = null;
        try {

            HttpURLConnection urlConnection = (HttpURLConnection) new URL
                    ("https://api.github.com/repos/kie4280/SoundTube/releases/latest")
                    .openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Authorization", "token " + token);
            urlConnection.setRequestProperty("User-Agent", "GitHubJava/2.1.0");
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();
            String content = stringBuilder.toString();

            JsonObject object = new JsonParser().parse(content).getAsJsonObject();
            versionName = object.get("tag_name").getAsString();
            url = object.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("browser_download_url").getAsString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (versionName != null && BuildConfig.VERSION_NAME.contentEquals(versionName)) {
            return "latest";
        } else {
            return url;
        }

    }
}
