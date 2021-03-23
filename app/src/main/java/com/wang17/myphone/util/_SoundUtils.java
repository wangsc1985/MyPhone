package com.wang17.myphone.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;

public class _SoundUtils {
    public static void play(Context context, int soundRawId) {
        SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        soundPool.load(context, soundRawId, 1);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                soundPool.play(1, 1, 1, 0, 0, 1);
            }
        });
    }

    public static void mediaPlay(Context context, int soundRawId) {
        MediaPlayer mPlayer = MediaPlayer.create(context, soundRawId);
        mPlayer.setVolume(1f, 1f);
        mPlayer.setLooping(false);
        mPlayer.start();
    }
}
