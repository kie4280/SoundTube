package kie.com.soundtube;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.*;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

public class MediaPlayerService1 extends Service {
    public int mposition = 0;
    public MediaPlayer mediaPlayer;
    private MusicBinder musicBinder;
    Handler playHandler;
    HandlerThread thread;
    boolean prepared = false;
    boolean updateSeekBar = true;
    Runnable task;
    DataHolder currentData = null;
    VideoFragment1 videoFragment;
    PowerManager.WakeLock wakeLock;
    WifiManager.WifiLock wifiLock;
    WifiManager wifiManager;

    MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(final MediaPlayer mp) {

            playHandler.post(new Runnable() {
                @Override
                public void run() {
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    videoFragment.Videoratio = (float) mp.getVideoWidth() / (float) mp.getVideoHeight();
                    prepared = true;
                    if (task != null) {
                        playHandler.post(task);
                        task = null;
                    }

                    if (videoFragment != null) {
                        videoFragment.buffering(false);
                        videoFragment.showcontrols(false);
                        videoFragment.setSeekBarMax(mediaPlayer.getDuration());

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

        if(mediaPlayer.isPlaying()) {
            updateSeekBar = true;
            videoFragment.updateSeekBar();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {

        updateSeekBar = false;
        Log.d("service", "onUnbind");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(preparedListener);
        mediaPlayer.setOnErrorListener(errorListener);
        mediaPlayer.setOnCompletionListener(completionListener);
        mediaPlayer.setOnInfoListener(infoListener);
        thread = new HandlerThread("playerhandler");
        thread.start();
        playHandler = new Handler(thread.getLooper());
        PowerManager powerManager = (PowerManager) getSystemService(Service.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "serviceWakeLock");
        wakeLock.setReferenceCounted(false);
        wifiManager = (WifiManager) getSystemService(Service.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "ServiceWifilock");
        wifiLock.setReferenceCounted(false);

        Intent app = new Intent(getApplicationContext(), MainActivity1.class);
        app.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(MediaPlayerService1.this,
                0, app, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(MediaPlayerService1.this);
        builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true)
                .setContentTitle("SoundTube");



        Notification not = builder.build();
        startForeground(1, not);
        Log.d("service", "created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        wifiLock.release();
        wakeLock.release();
        thread.quit();
        updateSeekBar = false;
        Log.d("service", "onDestroy");
        stopForeground(true);
    }

    public void play() {
        if (prepared) {
            playHandler.post(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.start();
                    updateSeekBar = true;
                    videoFragment.updateSeekBar();
                    wifiLock.acquire();
                    wakeLock.acquire();

                }
            });

        } else {
            task = new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.start();
                    updateSeekBar = true;
                    videoFragment.updateSeekBar();
                    wifiLock.acquire();
                    wakeLock.acquire();

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
                    wifiLock.release();
                    wakeLock.release();
                    updateSeekBar = false;
                }

            }
        });
    }

    public void stop() {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.stop();
                updateSeekBar = false;
            }
        });
    }

    public void reset() {
        prepared = false;
        playHandler.post(new Runnable() {
            @Override
            public void run() {

                mediaPlayer.reset();
                updateSeekBar = false;
            }
        });
    }

    public void prepare(final DataHolder dataHolder, final int a) {
        currentData = dataHolder;
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mediaPlayer.setDataSource(getApplicationContext(),
                            Uri.parse(dataHolder.videoUris.get(a)));
                    mediaPlayer.prepareAsync();
//                    mediaPlayer.setOnBufferingUpdateListener(onBufferingUpdateListener);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setDisplay(final SurfaceHolder surfaceHolder) {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                if(videoFragment.prepared) {
                    mediaPlayer.setDisplay(surfaceHolder);
                    mediaPlayer.setScreenOnWhilePlaying(true);
                } else {
                    mediaPlayer.setDisplay(null);
                }

            }
        });
    }

    public void seekTo(final int millis) {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(millis);
                }
            }
        });
    }

    public class MusicBinder extends Binder {
        MediaPlayerService1 getService() {
            return MediaPlayerService1.this;
        }
    }

}
