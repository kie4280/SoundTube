package kie.com.soundtube;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.*;
import android.util.Log;
import android.view.SurfaceHolder;
//import android.view.SurfaceHolder;
//import com.google.android.exoplayer2.ExoPlayerFactory;
//import com.google.android.exoplayer2.SimpleExoPlayer;
//import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
//import com.google.android.exoplayer2.extractor.ExtractorsFactory;
//import com.google.android.exoplayer2.source.ExtractorMediaSource;
//import com.google.android.exoplayer2.source.MediaSource;
//import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
//import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
//import com.google.android.exoplayer2.trackselection.TrackSelection;
//import com.google.android.exoplayer2.trackselection.TrackSelector;
//import com.google.android.exoplayer2.upstream.BandwidthMeter;
//import com.google.android.exoplayer2.upstream.DataSource;
//import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
//import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
//import com.google.android.exoplayer2.util.Util;

import java.io.IOException;

public class MediaPlayerService2 extends Service {
    public int mposition = 0;
    public MediaPlayer mediaPlayer;
    private MusicBinder musicBinder;
    Handler playHandler;
    HandlerThread thread;
    boolean prepared = false;
    boolean connected = false;
    boolean updateSeekBar = true;
    Runnable task;
    DataHolder currentData = null;
    VideoFragment1 videoFragment;
    PowerManager.WakeLock wakeLock;
    WifiManager.WifiLock wifiLock;
    WifiManager wifiManager;
    Context context;
//    SimpleExoPlayer player;

    MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(final MediaPlayer mp) {

            playHandler.post(new Runnable() {
                @Override
                public void run() {
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    videoFragment.Videoratio = (float) mp.getVideoWidth() / (float) mp.getVideoHeight();
                    if (task != null) {
                        playHandler.post(task);
                        task = null;
                    }
                    prepared = true;
                    if (connected) {
                        videoFragment.buffering(false);
                        videoFragment.showcontrols(false);
                        videoFragment.setSeekBarMax(mediaPlayer.getDuration());
                        videoFragment.updateSeekBar();
                    }
                }
            });

        }
    };
    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            updateSeekBar = false;
            Log.d("service", "complete");
            videoFragment.onComplete();
        }
    };
    MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
    };
    MediaPlayer.OnInfoListener infoListener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                videoFragment.buffering(true);
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                videoFragment.buffering(false);
            }
            return true;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        musicBinder = new MusicBinder();
        Log.d("service", "onBind");
        return musicBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d("service", "onRebind");
        connected = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        connected = false;
        Log.d("service", "onUnbind");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        // 1. Create a default TrackSelector
        Handler mainHandler = new Handler();
//        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
//        TrackSelection.Factory videoTrackSelectionFactory =
//                new AdaptiveTrackSelection.Factory(bandwidthMeter);
//        TrackSelector trackSelector =
//                new DefaultTrackSelector(videoTrackSelectionFactory);
//
//// 2. Create the player
//        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);




        thread = new HandlerThread("playerhandler");
        thread.start();
        playHandler = new Handler(thread.getLooper());
        PowerManager powerManager = (PowerManager) getSystemService(Service.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "serviceWakeLock");
        wakeLock.acquire();
        wifiManager = (WifiManager) getSystemService(Service.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "ServiceWifilock");
        wifiLock.acquire();

//        Intent app = new Intent(MediaPlayerService2.this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(MediaPlayerService2.this,
//                0, app, PendingIntent.FLAG_NO_CREATE);
//        Notification.Builder builder = new Notification.Builder(MediaPlayerService2.this);
//        builder.setContentIntent(pendingIntent)
//                .setSmallIcon(R.drawable.icon)
//                .setOngoing(true);
//
//        Notification not = builder.build();
//        startForeground(1, not);
//        Log.d("service", "created");
    }

    @Override
    public void onDestroy() {
//        stopForeground(true);
        mediaPlayer.stop();
        mediaPlayer.release();
        wakeLock.release();
        wifiLock.release();
        thread.quit();
        updateSeekBar = false;
        Log.d("service", "onDestroy");
        super.onDestroy();
    }


    public void play() {
        if (prepared) {
            playHandler.post(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.start();
                    updateSeekBar = true;
                    videoFragment.updateSeekBar();

                }
            });

        } else {
            task = new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.start();

                }
            };
        }
    }

    public void pause() {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    updateSeekBar = false;
                }

            }
        });
    }

    public void prepare(final DataHolder dataHolder, final int a) {
        currentData = dataHolder;

        playHandler.post(new Runnable() {
            @Override
            public void run() {
//                DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
//// Produces DataSource instances through which media data is loaded.
//                DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
//                        Util.getUserAgent(context, "yourApplicationName"), bandwidthMeter);
//// Produces Extractor instances for parsing the media data.
//                ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
//// This is the MediaSource representing the media to be played.
//
//                MediaSource videoSource = new ExtractorMediaSource(Uri.parse(dataHolder.videoUris
//                        .get(a)), dataSourceFactory, extractorsFactory,
//                        null, null);
//// Prepare the player with the source.
//                player.prepare(videoSource);


            }
        });
    }

    public void setDisplay(final SurfaceHolder surfaceHolder) {
//        playHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                player.setVideoSurfaceHolder(surfaceHolder);
//            }
//        });
    }

    public void seekTo(final int millis) {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(millis);
                    videoFragment.buffering(true);
                }
            }
        });
    }

    public class MusicBinder extends Binder {
        MediaPlayerService2 getService() {
            return MediaPlayerService2.this;
        }
    }

}
