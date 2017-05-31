package kie.com.soundtube;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.view.SurfaceHolder;

import java.io.IOException;

public class MediaPlayerService2 extends Service {
    public int mposition = 0;
    public MediaPlayer mediaPlayer;
    private MusicBinder musicBinder;
    Handler playHandler;
    HandlerThread thread;
    MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {

        }
    };
    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {

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
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        mediaPlayer.setOnPreparedListener(preparedListener);
        mediaPlayer.setOnErrorListener(errorListener);
        mediaPlayer.setOnCompletionListener(completionListener);
        thread = new HandlerThread("playerThread");
        thread.start();
        playHandler = new Handler(thread.getLooper());
    }

    public class MusicBinder extends Binder {
        MediaPlayerService2 getService() {
            return MediaPlayerService2.this;
        }
    }

    public void play() {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.start();
            }
        });
    }

    public void pause() {
        playHandler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.pause();
            }
        });
    }

    public void setDataSource(final Uri uri) {
        playHandler.post(new Runnable() {
            @Override
            public void run() {

                try {
                    mediaPlayer.setDataSource(getApplicationContext(), uri);
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
}
