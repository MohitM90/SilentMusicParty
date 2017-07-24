package de.tudarmstadt.informatik.tk.silentmusicparty.services;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;

/**
 * Service class that controls the media player.
 *
 * Created by chrisbe on 07.02.2017.
 */

public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener {
    private static final String ACTION_PLAY = "com.example.action.PLAY";

    public static long startPos = 0;
    public static long setTime = -1;

    private MediaPlayer mMediaPlayer = null;
    private IBinder mBinder = new LocalBinder();
    boolean mute = false;

    /**
     * Needed for synchronization
     */
    public long startTime;
    public long ping = 0;
    public long prepareTime;


    public int onStartCommand(Intent intent, int flags, int startId) {
        // initialize player and set parameters
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        return startId;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Enables binding to service.
     */
    public class LocalBinder extends Binder {
        public MediaPlayerService getServerInstance() {
            return MediaPlayerService.this;
        }
    }

    /**
     * Triggers the media player to prepare for playing a song.
     * @param song
     * @param ping
     */
    public void playSong(Song song, long ping) {
        try {
            startTime = song.getStartTime();
            this.ping = ping;

            // reset player and set data source
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(song.getPath().replace(" ", "%20"));

            // prepare asynchronous
            mMediaPlayer.setOnPreparedListener(this);
            prepareTime = System.currentTimeMillis();
            mMediaPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Turns the sound of the player on or off.
     *
     * @param fab FloatingActionButton
     */
    public boolean mute(FloatingActionButton fab){
        // make it mute
        if(!mute){
            mMediaPlayer.setVolume(0,0);
            mute = true;
            fab.setImageResource(R.drawable.ic_no_sound);
        }
        // unmute it
        else{
            mMediaPlayer.setVolume(1,1);
            mute = false;
            fab.setImageResource(R.drawable.ic_sound);
        }
        return mute;
    }

    /**
     * Determines whether player is playing or not.
     * @return
     */
    public boolean isPlaying() {
        if (mMediaPlayer == null) return false;
        try {
            return mMediaPlayer.isPlaying();
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    /**
     * Called when MediaPlayer is ready.
     */
    public void onPrepared(MediaPlayer player) {
        player.start();
        // in case the song must be started somewhere in the middle
        if (startPos > 0) {
            player.seekTo((int)(startPos + (System.currentTimeMillis()-setTime) + ping/2 + (System.currentTimeMillis()-prepareTime)/3)/*(int)(System.currentTimeMillis()-startTime)*/);
            startPos = 0;
        // in case the song can be started regularly from the beginning
        } else {
            player.seekTo((int)(ping/2 + (System.currentTimeMillis()-prepareTime))/*(int)(System.currentTimeMillis()-startTime)*/);
        }
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        // The MediaPlayer has moved to the Error state, must be reset!
        Log.d("SERVICE", "something went wrong with MediaPlayer");
        return false;
    }


    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

}
