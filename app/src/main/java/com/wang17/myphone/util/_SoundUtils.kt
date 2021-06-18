package com.wang17.myphone.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool

object _SoundUtils {
    enum class SoundType{
        SYSTEM,MUSIC
    }
    fun play(context: Context?, soundRawId: Int,soundType: SoundType) {
        val streamType = if(soundType==SoundType.MUSIC) AudioManager.STREAM_MUSIC else AudioManager.STREAM_SYSTEM
        val soundPool = SoundPool(10, streamType, 5)
        soundPool.load(context, soundRawId, 1)
        soundPool.setOnLoadCompleteListener { soundPool, sampleId, status -> soundPool.play(1, 1f, 1f, 0, 0, 1f) }
    }

    fun play(context: Context?, soundRawId: Int, volume: Float,soundType: SoundType) {
        val streamType = if(soundType==SoundType.MUSIC) AudioManager.STREAM_MUSIC else AudioManager.STREAM_SYSTEM
        val soundPool = SoundPool(10,streamType, 5)
        soundPool.load(context, soundRawId, 1)
        soundPool.setOnLoadCompleteListener { soundPool, sampleId, status -> soundPool.play(1, volume, volume, 0, 0, 1f) }
    }

    fun mediaPlay(context: Context?, soundRawId: Int) {
        val mPlayer = MediaPlayer.create(context, soundRawId)
        mPlayer.setVolume(1f, 1f)
        mPlayer.isLooping = false
        mPlayer.start()
    }
}