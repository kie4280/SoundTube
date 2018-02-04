package kie.com.soundtube;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static java.util.Arrays.asList;

/**
 * Created by Pietro Caselani
 * On 06/03/14
 * YoutubeExtractor
 */
public final class YoutubeExtractor {
    //region Fields
    public static final int YOUTUBE_VIDEO_QUALITY_SMALL_240 = 36;
    public static final int YOUTUBE_VIDEO_QUALITY_MEDIUM_360 = 18;
    public static final int YOUTUBE_VIDEO_QUALITY_HD_720 = 22;
    public static final int YOUTUBE_VIDEO_QUALITY_HD_1080 = 37;
    public static final int YOUTUBE_VIDEO_QUALITY_4K = 38;
    public static final int YOUTUBE_VIDEO_QUALITY_AUTO = 0;
    private final String mVideoIdentifier;
    private final List<String> mElFields;
    private HttpsURLConnection mConnection;
    private List<Integer> mPreferredVideoQualities;
    private boolean mCancelled;
    //endregion

    //region Constructors
    public YoutubeExtractor(String videoIdentifier) {
        mVideoIdentifier = videoIdentifier;
        mElFields = new ArrayList<String>(asList("embedded", "detailpage", "vevo", ""));

        mPreferredVideoQualities = asList(YOUTUBE_VIDEO_QUALITY_4K, YOUTUBE_VIDEO_QUALITY_HD_1080,
                YOUTUBE_VIDEO_QUALITY_HD_720, YOUTUBE_VIDEO_QUALITY_MEDIUM_360, YOUTUBE_VIDEO_QUALITY_SMALL_240);
    }
    //endregion

    //region Getters and Setters
    public List<Integer> getPreferredVideoQualities() {
        return mPreferredVideoQualities;
    }

    public void setPreferredVideoQualities(List<Integer> preferredVideoQualities) {
        mPreferredVideoQualities = preferredVideoQualities;
    }
    //endregion

    //region Public
    public void startExtracting(final YouTubeExtractorListener listener, final int preferredquality) {
        String elField = mElFields.get(1);
        mElFields.remove(0);
        if (elField.length() > 0) elField = "&el=" + elField;

        final String language = Locale.getDefault().getLanguage();

//        final String link = String.format("https://www.youtube.com/get_video_info?video_id=%s%s&ps=default&eurl=&gl=US&hl=%s",
//                mVideoIdentifier, elField, language);
        final String link = "https://www.youtube.com/get_video_info?video_id=" + mVideoIdentifier;

        final HandlerThread youtubeExtractorThread = new HandlerThread("YouTubeExtractorThread",
                THREAD_PRIORITY_BACKGROUND);
        youtubeExtractorThread.start();

        final Handler youtubeExtractorHandler = new Handler(youtubeExtractorThread.getLooper());

        final Handler listenerHandler = new Handler(Looper.getMainLooper());

        youtubeExtractorHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mConnection = (HttpsURLConnection) new URL(link).openConnection();
                    mConnection.setRequestProperty("Accept-Language", language);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(mConnection.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null && !mCancelled) builder.append(line);

                    reader.close();

                    if (!mCancelled) {
                        final YouTubeExtractorResult result = getYouTubeResult(builder.toString(), preferredquality);

                        listenerHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!mCancelled && listener != null) {
                                    listener.onSuccess(result);
                                }
                            }
                        });
                    }
                } catch (final Exception e) {
                    listenerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!mCancelled && listener != null) {
                                listener.onFailure(new Error(e));
                            }
                        }
                    });
                } finally {
                    if (mConnection != null) {
                        mConnection.disconnect();
                    }

                    youtubeExtractorThread.quit();
                }
            }
        });
    }

    public void cancelExtracting() {
        mCancelled = true;
    }
    //endregion

    //region Private
    private static HashMap<String, String> getQueryMap(String queryString, String charsetName) throws UnsupportedEncodingException {
        HashMap<String, String> map = new HashMap<String, String>();

        String[] fields = queryString.split("&");

        for (String field : fields) {
            String[] pair = field.split("=");
            if (pair.length == 2) {
                String key = pair[0];
                String value = URLDecoder.decode(pair[1], charsetName).replace('+', ' ');
                map.put(key, value);
            }
        }

        return map;
    }

    private YouTubeExtractorResult getYouTubeResult(String html, int quality) throws UnsupportedEncodingException, YouTubeExtractorException {
        HashMap<String, String> video = getQueryMap(html, "UTF-8");

        Uri videoUri = null;

        if (video.containsKey("url_encoded_fmt_stream_map")) {
            List<String> streamQueries = new ArrayList<String>(asList(video.get("url_encoded_fmt_stream_map").split(",")));

            String adaptiveFmts = video.get("adaptive_fmts");
            String[] split = adaptiveFmts.split(",");

            streamQueries.addAll(asList(split));

            SparseArray<String> streamLinks = new SparseArray<String>();
            for (String streamQuery : streamQueries) {
                HashMap<String, String> stream = getQueryMap(streamQuery, "UTF-8");
                String type = stream.get("type").split(";")[0];
                String urlString = stream.get("url");

                if (urlString != null && MimeTypeMap.getSingleton().hasMimeType(type)) {
                    String signature = stream.get("sig");

                    if (signature != null) {
                        urlString = urlString + "&signature=" + signature;
                    }

                    if (getQueryMap(urlString, "UTF-8").containsKey("signature")) {
                        streamLinks.put(Integer.parseInt(stream.get("itag")), urlString);
                    }
                }
            }
            final Uri mediumThumbUri = video.containsKey("iurlmq") ? Uri.parse(video.get("iurlmq")) : null;
            final Uri highThumbUri = video.containsKey("iurlhq") ? Uri.parse(video.get("iurlhq")) : null;
            final Uri defaultThumbUri = video.containsKey("iurl") ? Uri.parse(video.get("iurl")) : null;
            final Uri standardThumbUri = video.containsKey("iurlsd") ? Uri.parse(video.get("iurlsd")) : null;
            if(quality == YOUTUBE_VIDEO_QUALITY_AUTO) {

                for (Integer videoQuality : mPreferredVideoQualities) {
                    if (streamLinks.get(videoQuality, null) != null) {
                        String streamLink = streamLinks.get(videoQuality);
                        videoUri = Uri.parse(streamLink);
                        break;
                    }
                }
            } else {

                if (streamLinks.get(quality, null) != null) {
                    String streamLink = streamLinks.get(quality);
                    videoUri = Uri.parse(streamLink);
                } else {
                    for (Integer videoQuality : mPreferredVideoQualities) {
                        if (streamLinks.get(videoQuality, null) != null) {
                            String streamLink = streamLinks.get(videoQuality);
                            videoUri = Uri.parse(streamLink);
                            break;
                        }
                    }
                }
            }

            return new YouTubeExtractorResult(videoUri, mediumThumbUri, highThumbUri, defaultThumbUri, standardThumbUri);
        } else {
            throw new YouTubeExtractorException("Status: " + video.get("status") + "\nReason: " + video.get("reason")
                    + "\nError code: " + video.get("errorcode"));
        }
    }
    //endregion

    public static final class YouTubeExtractorResult {
        private final Uri mVideoUri, mMediumThumbUri, mHighThumbUri;
        private final Uri mDefaultThumbUri, mStandardThumbUri;

        private YouTubeExtractorResult(Uri videoUri, Uri mediumThumbUri, Uri highThumbUri, Uri defaultThumbUri, Uri standardThumbUri) {
            mVideoUri = videoUri;
            mMediumThumbUri = mediumThumbUri;
            mHighThumbUri = highThumbUri;
            mDefaultThumbUri = defaultThumbUri;
            mStandardThumbUri = standardThumbUri;
        }

        public Uri getVideoUri() {
            return mVideoUri;
        }

        public Uri getMediumThumbUri() {
            return mMediumThumbUri;
        }

        public Uri getHighThumbUri() {
            return mHighThumbUri;
        }

        public Uri getDefaultThumbUri() {
            return mDefaultThumbUri;
        }

        public Uri getStandardThumbUri() {
            return mStandardThumbUri;
        }
    }

    public final class YouTubeExtractorException extends Exception {
        public YouTubeExtractorException(String detailMessage) {
            super(detailMessage);
        }
    }

    public interface YouTubeExtractorListener {
        void onSuccess(YouTubeExtractorResult result);

        void onFailure(Error error);
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
}