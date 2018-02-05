package kie.com.soundtube;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaCodec;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Queue;

public class MediaPlayerService extends Service {
    public int mposition = 0;
    public DataHolder currentData = null;
    public static boolean autoplay = true;

    public static boolean serviceStarted = false;
    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_REMOVED = "NOTIFICATION_REMOVED";
    private static final String NOTIFICATION_PLAY = "NOTIFICATION_PLAY";
    private static final String NOTIFICATION_NEXT = "NOTIFICATION_NEXT";
    private static final String NOTIFICATION_PREV = "NOTIFICATION_PREV";
    public SimpleExoPlayer exoPlayer;
    private MusicBinder musicBinder;
    Handler playHandler;
    HandlerThread thread;
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

    public void onPrepared() {

        playHandler.post(new Runnable() {
            @Override
            public void run() {

                prepared = true;
                Runnable task = preparetasks.poll();
                while (task != null) {
                    playHandler.post(task);
                    task = preparetasks.poll();
                }

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
        exoPlayer.seekTo(0);
        exoPlayer.setPlayWhenReady(false);
        pause();
        if (videoFragment != null) {
            videoFragment.onComplete();
        }

        if (!playList.isEmpty() && autoplay) {
            prepare(playList.pollFirst());

        } else {
            Log.d("service", "complete");
            stopForeground(false);
            notificationManager.notify(NOTIFICATION_ID, not);

        }

    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(NOTIFICATION_PLAY)) {
                if (isPlaying()) {
                    pause();

                } else {
                    play();

                }
            } else if (action.equals(NOTIFICATION_NEXT)) {

            } else if (action.equals(NOTIFICATION_PREV)) {

            } else if (action.equals(NOTIFICATION_REMOVED)) {

                stopSelf();
                Log.d("service", "notification remove");
            }
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
        startForeground(NOTIFICATION_ID, not);
        return musicBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d("service", "onRebind");
        if (videoFragment != null) {
            videoFragment.currentdata = currentData;
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
                videoFragment.setHeaderPlayButton(false);
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
        thread = new HandlerThread("playerhandler");
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        playHandler = new Handler(thread.getLooper());
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
        notContentView.setOnClickPendingIntent(R.id.next, nextIntent);
        notContentView.setOnClickPendingIntent(R.id.prev, prevIntent);
        notBuilder.setContentIntent(notAddIntent)
                .setSmallIcon(R.drawable.icon)
                .setOngoing(false)
                .setContentTitle("SoundTube")
                .setDeleteIntent(notRemoveIntent)
                .setContent(notContentView);
        not = notBuilder.build();
//        not.flags |= Notification.FLAG_NO_CLEAR;
        Log.d("service", "created");
        serviceStarted = true;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        exoPlayer.release();
        wifiLock.release();
        wakeLock.release();
        thread.quit();
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

    public void play() {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isPlaying()) {
                    exoPlayer.setPlayWhenReady(true);
                    updateSeekBar = true;
                    wifiLock.acquire();
                    wakeLock.acquire();
                    notContentView.setImageViewResource(R.id.ppButton, R.drawable.pause);
//                notBuilder.setContent(notContentView);
                    startForeground(NOTIFICATION_ID, notBuilder.build());
                    if (videoFragment != null) {
                        videoFragment.currentdata = currentData;
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
                    notContentView.setImageViewResource(R.id.ppButton, R.drawable.play);
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

    boolean suspend = false;

    public void phonecall(boolean calling) {
        if (calling) {
            pause();
            suspend = true;
        } else {
            if (suspend) {
                play();
            }
            suspend = false;
        }
    }

    public void prepare(final DataHolder dataHolder) {
        currentData = dataHolder;
        playHandler.post(new Runnable() {
            @Override
            public void run() {

                for (int a = 0; a < VideoRetriver.mPreferredVideoQualities.size(); a++) {
                    int quality = VideoRetriver.mPreferredVideoQualities.get(a);
                    if (dataHolder.videoUris.containsKey(quality)) {
                        notContentView.setTextViewText(R.id.notPlayingTitle, currentData.title);
                        prepared = false;
                        // Measures bandwidth during playback. Can be null if not required.
                        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
// Produces DataSource instances through which media data is loaded.
                        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(),
                                Util.getUserAgent(getApplicationContext(), "Soundtube"), bandwidthMeter);
// This is the MediaSource representing the media to be played.
                        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(Uri.parse(dataHolder.videoUris.get(quality)));
// Prepare the player with the source.
                        exoPlayer.stop();
                        exoPlayer.prepare(videoSource);

                        break;
                    } else if (a == VideoRetriver.mPreferredVideoQualities.size() - 1) {
                        Toast toast = Toast.makeText(getApplicationContext(), "No video resolution", Toast.LENGTH_LONG);
                        toast.show();
                    }
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

                }

            }
        });


    }

    public void seekTo(final int millis) {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isPlaying()) {
                    exoPlayer.seekTo(millis);
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

    public class MusicBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

}
