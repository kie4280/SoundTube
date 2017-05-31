package com.kie.soundtube;

public class MediaPlayerService2 {
  private AudioManager.OnAudioFocusChangeListener afChangeListener;
private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
private MediaStyleNotification myPlayerNotification;
private MediaSessionCompat mediaSession;
private MediaBrowserService service;
private SomeKindOfPlayer player;

MediaSessionCompat.Callback callback = new
MediaSessionCompat.Callback() {
  @Override
  public void onPlay() {
    AudioManager am = mContext.getSystemService(Context.AUDIO_SERVICE);
    // Request audio focus for playback, this registers the afChangeListener
    int result = am.requestAudioFocus(afChangeListener,
                                 // Use the music stream.
                                 AudioManager.STREAM_MUSIC,
                                 // Request permanent focus.
                                 AudioManager.AUDIOFOCUS_GAIN);

    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      // Start the service
      service.start();
      // Set the session active  (and update metadata and state)
      mediaSession.setActive(true);
      // start the player (custom call)
      player.start();
      // Register BECOME_NOISY BroadcastReceiver
      registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
      // Put the service in the foreground, post notification
      service.startForeground(myPlayerNotification);
    }
  }

  @Override
  public void onStop() {
    AudioManager am = mContext.getSystemService(Context.AUDIO_SERVICE);
    // Abandon audio focus
    am.abandonAudioFocus(afChangeListener);
    unregisterReceiver(myNoisyAudioStreamReceiver);
    // Start the service
    service.stop(self);
    // Set the session inactive  (and update metadata and state)
    mediaSession.setActive(false);
    // stop the player (custom call)
    player.stop();
    // Take the service out of the foreground, remove notification
    service.stopForeground(true);
  }

  @Override
  public void onPause() {
    AudioManager am = mContext.getSystemService(Context.AUDIO_SERVICE);
    // Update metadata and state
    // pause the player (custom call)
    player.pause();
    // unregister BECOME_NOISY BroadcastReceiver
    unregisterReceiver(myNoisyAudioStreamReceiver, intentFilter);
    // Take the service out of the foreground, retain the notification
    service.stopForeground(false);
  }
}
