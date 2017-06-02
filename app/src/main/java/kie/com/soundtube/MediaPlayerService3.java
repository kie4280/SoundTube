package kie.com.soundtube;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.view.SurfaceHolder;

import java.io.IOException;

public class MediaPlayerService3 extends Service {
    public int mposition = 0;
    public MediaPlayer mediaPlayer;
    private MusicBinder musicBinder;
    Handler playHandler;
    HandlerThread thread;
    boolean prepared = false;
    boolean connected = false;
    Runnable task;
    DataHolder currentData;
    VideoFragment2 videoFragment;

    MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(final MediaPlayer mp) {

            playHandler.post(new Runnable() {
                @Override
                public void run() {
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    videoFragment.Videoratio = (float) mp.getVideoWidth() / (float) mp.getVideoHeight();
                    if(task!=null) {
                        playHandler.post(task);
                        task = null;
                    }
                    prepared = true;
                    if(connected) {
                        videoFragment.buffering(false);
                    }
                }
            });

        }
    };
    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mp.reset();

        }
    };
    MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        musicBinder = new MusicBinder();
        return musicBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mediaPlayer.stop();
        mediaPlayer.release();
        connected = false;
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnPreparedListener(preparedListener);
        mediaPlayer.setOnErrorListener(errorListener);
        mediaPlayer.setOnCompletionListener(completionListener);
        thread = new HandlerThread("playerhandler", Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();
        playHandler = new Handler(thread.getLooper());

    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }


    public void play() {
        if (prepared) {
            playHandler.post(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.start();
                    //                Intent app = new Intent(MediaPlayerService2.this, MainActivity.class);
//                PendingIntent pendingIntent = PendingIntent.getActivity(MediaPlayerService2.this,
//                        0, app, PendingIntent.FLAG_UPDATE_CURRENT);
//                Notification.Builder builder = new Notification.Builder(MediaPlayerService2.this);
//                builder.setContentIntent(pendingIntent)
//                        .setSmallIcon(R.drawable.play)
//                        .setOngoing(true);
//
//
//                Notification not = builder.build();
//
//              startForeground(1, not);
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
                if(mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }

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
                mediaPlayer.setDisplay(surfaceHolder);
            }
        });
    }

    public void seekTo(final int millis) {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.seekTo(millis);
            }
        });
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public void connect(VideoFragment2 vid) {
        videoFragment = vid;
        connected = true;
    }


    public class MusicBinder extends Binder {
        MediaPlayerService3 getService() {
            return MediaPlayerService3.this;
        }
    }

}
