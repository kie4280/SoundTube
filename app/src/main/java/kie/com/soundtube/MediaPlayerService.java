package kie.com.soundtube;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaCodec;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

public class MediaPlayerService extends Service {
    public int mposition = 0;
    public DataHolder currentData = null, nextData = null;
    public static boolean autoplay = true;
    public static boolean serviceStarted = false;
    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_REMOVED = "NOTIFICATION_REMOVED";
    private static final String NOTIFICATION_PLAY = "NOTIFICATION_PLAY";
    private static final String NOTIFICATION_NEXT = "NOTIFICATION_NEXT";
    private static final String NOTIFICATION_PREV = "NOTIFICATION_PREV";
    public static final int PLAY_FROM_PLAYLIST = 1;
    public static final int PLAY_FROM_RELATED_VIDEO = 2;
    public static int PLAYING_MODE = 2;
    public static int VIDEO_QUALITY = -1;
    public SimpleExoPlayer exoPlayer;
    private MusicBinder musicBinder;
    Handler playHandler, networkHandler;
    HandlerThread playThread, networkThread;
    boolean prepared = false;
    boolean updateSeekBar = true;
    Queue<Runnable> preparetasks = new LinkedList<>();
    VideoFragment videoFragment;
    PowerManager.WakeLock wakeLock;
    WifiManager.WifiLock wifiLock;
    WifiManager wifiManager;
    NotificationManager notificationManager;
    LinkedList<DataHolder> playList = new LinkedList<>();
    ListIterator<DataHolder> playiterator = playList.listIterator();
    Notification.Builder notBuilder;
    Notification not;
    RemoteViews notContentView;
    YoutubeClient youtubeClient = null;
    VideoRetriever videoRetriever = null;
    LinkedList<DataHolder> watchedQueue = new LinkedList<>();

    public void onPrepared() {

        prepared = true;
        Runnable task = preparetasks.poll();
        while (task != null) {
            playHandler.post(task);
            task = preparetasks.poll();
        }
        playHandler.post(new Runnable() {
            @Override
            public void run() {

                if (videoFragment != null) {
                    videoFragment.Videoratio = exoPlayer.getVideoFormat().pixelWidthHeightRatio;
                    buffering(false);
                    videoFragment.showcontrols(false);
                    videoFragment.setSeekBarMax(getDuration());

                }
                play();
            }
        });

    }

    public void onCompletion() {
        updateSeekBar = false;
//        exoPlayer.seekTo(0);
//        pause();

        //do the same as pause() but without pausing the player
        wifiLock.release();
        wakeLock.release();
        stopForeground(false);
        notContentView.setImageViewResource(R.id.ppButton, R.drawable.ic_play_arrow_black_36dp);
        notBuilder.setContent(notContentView);
        notificationManager.notify(NOTIFICATION_ID, notBuilder.build());
        updateSeekBar = false;

        if (videoFragment != null) {
            videoFragment.setButtonPlay(true);
            videoFragment.setHeaderPlayButton(true);
        }
        if (videoFragment != null) {
            videoFragment.onComplete();
        }
        if (nextData != null && autoplay) {
            prepare(nextData);
        }


        Log.d("service", "complete");
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case NOTIFICATION_PLAY:
                    if (isPlaying()) {
                        pause();
                    } else {
                        play();
                    }
                    break;
                case NOTIFICATION_NEXT:
                    prepare(nextData);
                    Log.d("service", "notification next");
                    break;
                case NOTIFICATION_PREV:
                    if (videoFragment != null) {
                        videoFragment.previousVideo();
                    } else {
                        previousVideo();
                        DataHolder dataHolder = watchedQueue.pollLast();
                        prepare(dataHolder);
                    }
                    Log.d("service", "notification previous");
                    break;
                case NOTIFICATION_REMOVED:
                    stopSelf();
                    Log.d("service", "notification remove");
                    break;
                default:
                    break;
            }

        }
    };

    boolean suspend = false;

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    pause();
                    suspend = true;

                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (suspend) {
                        play();
                    }
                    suspend = false;

                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;

            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        musicBinder = new MusicBinder();
        Log.d("service", "onBind");
        if (videoFragment != null && videoFragment.prepared) {
            setDisplay(videoFragment.surfaceHolder);
        }
//        startForeground(NOTIFICATION_ID, not);
        return musicBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d("service", "onRebind");
        if (videoFragment != null) {
            videoFragment.currentData = currentData;
            if (videoFragment.prepared) {
                setDisplay(videoFragment.surfaceHolder);
            }
            if (isPlaying()) {
                updateSeekBar = true;
                videoFragment.setSeekBarMax(getDuration());
                videoFragment.updateSeekBar();
                videoFragment.setButtonPlay(false);
                videoFragment.setHeaderPlayButton(false);
            } else {
                updateSeekBar = false;
                videoFragment.setButtonPlay(true);
                videoFragment.setHeaderPlayButton(true);
            }
        }


        startForeground(NOTIFICATION_ID, not);
    }

    @Override
    public boolean onUnbind(Intent intent) {

        updateSeekBar = false;
        PlayerActivity.servicebound = false;
        videoFragment.serviceDisconnected();
        videoFragment = null;
        Log.d("service", "onUnbind");
        if (!isPlaying()) {
            stopForeground(false);
        }

//        notificationManager.notify(NOTIFICATION_ID, not);

        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(Service.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "serviceWakeLock");
        wakeLock.setReferenceCounted(false);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Service.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "ServiceWifilock");
        wifiLock.setReferenceCounted(false);
        playThread = new HandlerThread("playerThread");
        playThread.setPriority(Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE);
        playThread.start();
        playHandler = new Handler(playThread.getLooper());
        networkThread = new HandlerThread("networkThread");
        networkThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        networkThread.start();
        networkHandler = new Handler(networkThread.getLooper());
        newPlayer();

        notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        Intent app = new Intent(getApplicationContext(), PlayerActivity.class);
        app.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent destroy = new Intent(NOTIFICATION_REMOVED);
        Intent play = new Intent(NOTIFICATION_PLAY);
        Intent next = new Intent(NOTIFICATION_NEXT);
        Intent prev = new Intent(NOTIFICATION_PREV);
        PendingIntent notAddIntent = PendingIntent.getActivity(MediaPlayerService.this, 0, app,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent notRemoveIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, destroy,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent playIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, play,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent nextIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, next,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent prevIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, prev,
                PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(broadcastReceiver, new IntentFilter(NOTIFICATION_REMOVED));
        registerReceiver(broadcastReceiver, new IntentFilter(NOTIFICATION_NEXT));
        registerReceiver(broadcastReceiver, new IntentFilter(NOTIFICATION_PREV));
        registerReceiver(broadcastReceiver, new IntentFilter(NOTIFICATION_PLAY));
        notBuilder = new Notification.Builder(MediaPlayerService.this);
        notContentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        notContentView.setOnClickPendingIntent(R.id.ppButton, playIntent);
        notContentView.setOnClickPendingIntent(R.id.nextButton, nextIntent);
        notContentView.setOnClickPendingIntent(R.id.prevButton, prevIntent);
        notContentView.setImageViewResource(R.id.ppButton, R.drawable.ic_play_arrow_black_36dp);
        notContentView.setImageViewResource(R.id.nextButton, R.drawable.ic_skip_next_black_36dp);
        notContentView.setImageViewResource(R.id.prevButton, R.drawable.ic_skip_previous_black_36dp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = notificationManager.getNotificationChannel("MyChannel");
            if (mChannel == null) {
                mChannel = new NotificationChannel("Not_1", "SoundTube", NotificationManager.IMPORTANCE_HIGH);
                mChannel.setDescription("SoundTube_service");
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                notificationManager.createNotificationChannel(mChannel);

            }

        }
        notBuilder.setContentIntent(notAddIntent)
                .setSmallIcon(R.drawable.icon)
                .setOngoing(false)
                .setContentTitle("SoundTube")
                .setDeleteIntent(notRemoveIntent)
                .setContent(notContentView);
        not = notBuilder.build();
        Log.d("service", "created");
        serviceStarted = true;
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        exoPlayer.release();
        wifiLock.release();
        wakeLock.release();
        playThread.quit();
        updateSeekBar = false;
        Log.d("service", "onDestroy");
        stopForeground(true);
        unregisterReceiver(broadcastReceiver);
        serviceStarted = false;
        notificationManager = null;
        prepared = false;

    }

    public void newPlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector);
        exoPlayer.addListener(new Player.DefaultEventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                super.onPlayerStateChanged(playWhenReady, playbackState);
                switch (playbackState) {
                    case Player.STATE_IDLE:
                        prepared = false;
                        exoPlayer.setPlayWhenReady(false);
                        Log.d("exoPlayer", "stateIdle");
                        Log.d("exoPlayer", "Play " + Boolean.toString(playWhenReady));
                        break;
                    case Player.STATE_BUFFERING:
                        buffering(true);
                        Log.d("exoPlayer", "stateBuffering");
                        break;
                    case Player.STATE_READY:
                        buffering(false);
                        if (!playWhenReady && !prepared) {
                            onPrepared();
                            Log.d("exoPlayer", "stateReady");
                        }
                        break;
                    case Player.STATE_ENDED:
                        onCompletion();
                        Log.d("exoPlayer", "stateEnded");
                        break;
                    default:
                        break;

                }
            }
        });

        exoPlayer.addListener(new Player.DefaultEventListener() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                super.onPlayerError(error);
                wakeLock.release();
                wifiLock.release();
            }
        });

    }

    public void start() {
        exoPlayer.seekTo(0);
        play();
    }

    public void play() {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isPlaying()) {
                    exoPlayer.setPlayWhenReady(true);
                    updateSeekBar = true;
                    wifiLock.acquire();
                    wakeLock.acquire();
                    notContentView.setImageViewResource(R.id.ppButton, R.drawable.ic_pause_black_36dp);
//                notBuilder.setContent(notContentView);
                    startForeground(NOTIFICATION_ID, notBuilder.build());
                    if (videoFragment != null) {
                        videoFragment.currentData = currentData;
                        videoFragment.updateSeekBar();
                        videoFragment.setButtonPlay(false);
                        videoFragment.setHeaderPlayButton(false);

                    }
                }

            }
        });

    }

    public void pause() {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isPlaying()) {
                    exoPlayer.setPlayWhenReady(false);
                    wifiLock.release();
                    wakeLock.release();
                    stopForeground(false);
                    notContentView.setImageViewResource(R.id.ppButton, R.drawable.ic_play_arrow_black_36dp);
                    notBuilder.setContent(notContentView);
                    notificationManager.notify(NOTIFICATION_ID, notBuilder.build());
                    updateSeekBar = false;
                    if (videoFragment != null) {
                        videoFragment.setButtonPlay(true);
                        videoFragment.setHeaderPlayButton(true);
                    }
                }


            }
        });
    }

    public void stop() {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                exoPlayer.stop();
                wifiLock.release();
                wakeLock.release();
                updateSeekBar = false;
                if (videoFragment == null) {
                    stopForeground(false);
                    notificationManager.notify(NOTIFICATION_ID, not);
                }
            }
        });
    }

    public int getCurrentPos() {
        return (int) exoPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return (int) exoPlayer.getDuration();
    }

    public boolean isPlaying() {
        return exoPlayer.getPlayWhenReady();
    }

    public void prepare(final DataHolder dataHolder) {
        prepare(dataHolder, VideoRetriever.YOUTUBE_VIDEO_QUALITY_AUTO);
    }

    public void prepare(final DataHolder dataHolder, final ArrayList<Integer> quality) {

        playHandler.post(new Runnable() {
            @Override
            public void run() {

                MediaSource videoSource = VideoRetriever.toMediaSource(dataHolder, quality, getApplicationContext());

                if (videoSource != null) {
                    notContentView.setTextViewText(R.id.notPlayingTitle, dataHolder.title);
                    prepared = false;

// Prepare the player with the source.
                    exoPlayer.stop();
                    exoPlayer.prepare(videoSource);
                    if (currentData != null) {
                        watchedQueue.offer(currentData);
                        Log.d("watchedQueue", "add");
                    }
                    currentData = dataHolder;
                    nextVideo();
                    if (videoFragment != null) {
                        videoFragment.loadRelatedVideos(dataHolder);
                    }

                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "No video resolution", Toast.LENGTH_LONG);
                    toast.show();
                }

            }
        });


    }

    public void setDisplay(final SurfaceHolder surfaceHolder) {

        playHandler.post(new Runnable() {
            @Override
            public void run() {
                if (surfaceHolder != null) {
                    exoPlayer.setVideoSurfaceHolder(surfaceHolder);
                    exoPlayer.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    surfaceHolder.setKeepScreenOn(true);
                }

            }
        });


    }

    public void seekTo(final int millis) {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                if (exoPlayer.isCurrentWindowSeekable()
                        ) {
                    exoPlayer.seekTo(millis);
                } else if (!prepared) {
                    preparetasks.offer(new Runnable() {
                        @Override
                        public void run() {
                            exoPlayer.seekTo(millis);
                        }
                    });
                }
            }
        });
    }

    public void addToPlayList(DataHolder dataHolder) {
        playList.add(dataHolder);
    }


    public void buffering(boolean buff) {
        if (videoFragment != null) {
            videoFragment.buffering(buff);
        }
    }

    public void loadPlaylist() {

    }

    public void nextVideo() {
        if (videoRetriever == null || youtubeClient == null) {
            videoRetriever = new VideoRetriever(getApplicationContext(), networkHandler);
            youtubeClient = new YoutubeClient(getApplicationContext(), networkHandler);
        }
        if (!playList.isEmpty() && PLAYING_MODE == PLAY_FROM_PLAYLIST) {
            nextData = playList.pollFirst();

        } else if (PLAYING_MODE == PLAY_FROM_RELATED_VIDEO && currentData != null) {

            youtubeClient.loadRelatedVideos(currentData.videoID);
            youtubeClient.getResults(new YoutubeClient.YoutubeSearchResult() {
                @Override
                public void onFound(List<DataHolder> data, boolean hasnext, boolean hasprev) {
                    final DataHolder target = data.get(0);
                    videoRetriever.getDataHolder(target, new VideoRetriever.YouTubeExtractorListener() {

                        @Override
                        public void onSuccess(DataHolder result) {

                            nextData = result;
                            Log.d("mediaService", "nextPlay");
                        }

                        @Override
                        public void onFailure(Error error) {
                            Log.d("search", "error extracting");

                        }
                    });
                }

                @Override
                public void noData() {
                    Log.d("search", "noData");
                }
            });


        }
    }

    public boolean previousVideo() {
        currentData = null;
        return !watchedQueue.isEmpty();
    }

    public void setVideoQuality(int quality) {
        VIDEO_QUALITY = quality;
        if (currentData.videoUris.containsKey(VIDEO_QUALITY)) {
            int pos = getCurrentPos();
            prepare(currentData);
            seekTo(pos);
        }
    }

    public void setPlayMode(int mode) {
        PLAYING_MODE = mode;
    }

    public class MusicBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

}
