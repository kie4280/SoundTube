package kie.com.soundtube;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.*;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

public class MediaPlayerService extends Service {
    public int mposition = 0;
    public String nowplaying = null;
    public boolean autoplay = true;
    public static boolean serviceStarted = false;
    private static final String NOTIFICATION_REMOVED = "NOTIFICATION_REMOVED";
    private static final String NOTIFICATION_PLAY = "NOTIFICATION_PLAY";
    private static final String NOTIFICATION_NEXT = "NOTIFICATION_NEXT";
    private static final String NOTIFICATION_PREV = "NOTIFICATION_PREV";
    public MediaPlayer mediaPlayer;
    private MusicBinder musicBinder;
    Handler playHandler;
    HandlerThread thread;
    boolean prepared = false;
    boolean updateSeekBar = true;
    Queue<Runnable> preparetasks = new LinkedList<>();

    DataHolder currentData = null;
    VideoFragment videoFragment;
    PowerManager.WakeLock wakeLock;
    WifiManager.WifiLock wifiLock;
    WifiManager wifiManager;
    NotificationManager notificationManager;
    ArrayList<DataHolder> playList = new ArrayList<>();
    ListIterator<DataHolder> playiterator = playList.listIterator();
    Notification.Builder notbuilder;
    Notification not;

    MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(final MediaPlayer mp) {

            playHandler.post(new Runnable() {
                @Override
                public void run() {

                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    prepared = true;
                    Runnable task = preparetasks.poll();
                    while (task != null) {
                        playHandler.post(task);
                        task = preparetasks.poll();
                    }

                    if (videoFragment != null) {
                        videoFragment.Videoratio = (float) mp.getVideoWidth() / (float) mp.getVideoHeight();
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
            if (!playList.isEmpty() && autoplay) {
                prepared = false;
                if (playiterator.hasNext()) {
                    prepare(playiterator.next());
                } else {
                    playiterator = playList.listIterator(0);
                    prepare(playiterator.next());
                }

            } else if (videoFragment != null) {

                Log.d("service", "complete");
                stopForeground(false);
                notificationManager.notify(1, not);
                videoFragment.onComplete();
            }

        }
    };
    MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            wifiLock.release();
            wakeLock.release();
            return false;
        }
    };
    MediaPlayer.OnInfoListener infoListener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                buffering(true);
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                buffering(false);
            }
            return true;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        musicBinder = new MusicBinder();
        Log.d("service", "onBind");
        if (videoFragment != null && videoFragment.prepared) {
            setDisplay(videoFragment.surfaceHolder);
        }
        return musicBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d("service", "onRebind");
        if (videoFragment != null && videoFragment.prepared) {
            setDisplay(videoFragment.surfaceHolder);
        }

        if (videoFragment != null && mediaPlayer.isPlaying()) {
            updateSeekBar = true;
            videoFragment.setSeekBarMax(mediaPlayer.getDuration());
            videoFragment.updateSeekBar();
            videoFragment.setButtonPlay(false);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {

        updateSeekBar = false;
        MainActivity.servicebound = false;
        videoFragment.serviceDisconnected();
        setDisplay(null);
        videoFragment = null;
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
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Service.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "ServiceWifilock");
        wifiLock.setReferenceCounted(false);
        notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        Intent app = new Intent(getApplicationContext(), MainActivity.class);
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
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        contentView.setOnClickPendingIntent(R.id.ppButton, playIntent);
        contentView.setOnClickPendingIntent(R.id.next, nextIntent);
        contentView.setOnClickPendingIntent(R.id.prev, prevIntent);
        notbuilder = new Notification.Builder(MediaPlayerService.this);
        notbuilder.setContentIntent(notAddIntent)
                .setSmallIcon(R.drawable.icon)
                .setOngoing(false)
                .setContentTitle("SoundTube")
                .setDeleteIntent(notRemoveIntent)
                .setContent(contentView);
        not = notbuilder.build();
//        not.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(1, not);

        Log.d("service", "created");
        serviceStarted = true;

    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(NOTIFICATION_PLAY)) {
                if (mediaPlayer.isPlaying()) {
                    pause();
                    if (videoFragment != null) {
                        videoFragment.setButtonPlay(true);
                    }
                } else {
                    play();
                    if (videoFragment != null) {
                        videoFragment.setButtonPlay(false);
                    }
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
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        wifiLock.release();
        wakeLock.release();
        thread.quit();
        updateSeekBar = false;
        Log.d("service", "onDestroy");
        notificationManager.cancel(1);
        unregisterReceiver(broadcastReceiver);
        serviceStarted = false;
        notificationManager = null;
//        stopForeground(true);

    }

    public void play() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                mediaPlayer.start();
                updateSeekBar = true;
                if (videoFragment != null) {
                    videoFragment.updateSeekBar();
                }
                wifiLock.acquire();
                wakeLock.acquire();
//                notificationManager.notify(1, notbuilder.build());
                startForeground(1, notbuilder.build());

            }
        };
        if (prepared) {
            playHandler.post(r);

        } else {
            preparetasks.offer(r);
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
                    if (!MainActivity.activityRunning) {
                        stopForeground(false);
                        notificationManager.notify(1, not);
                    }
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
                nowplaying = dataHolder.videoID;
                for (int a = 0; a < VideoRetriver.mPreferredVideoQualities.size(); a++) {
                    int quality = VideoRetriver.mPreferredVideoQualities.get(a);
                    if (dataHolder.videoUris.containsKey(quality)) {
                        try {
                            mediaPlayer.setDataSource(getApplicationContext(),
                                    Uri.parse(dataHolder.videoUris.get(quality)));
                            mediaPlayer.prepareAsync();
//                    mediaPlayer.setOnBufferingUpdateListener(onBufferingUpdateListener);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
