package com.wang17.myphone.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.SeekBar;

import com.wang17.myphone.fragment.MusicPlayerFragment;
import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.database.PlayList;
import com.wang17.myphone.model.ProgressEvent;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.database.Song;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MusicPlayerService extends Service {

    private MediaPlayer mediaPlayer;       //媒体播放器对象
    private Timer timer;

    private DataContext dataContext;
    private PlayList currentPlayList;
    private Song currentSong;
    private List<Song> songList;


    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer = new MediaPlayer();
        dataContext = new DataContext(getApplicationContext());

        EventBus.getDefault().register(this);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private static PowerManager.WakeLock wakeLock;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (mediaPlayer.isPlaying()) {
//            stop();
//        }

        wakeLock=_Utils.acquireWakeLock(getApplicationContext(), PARTIAL_WAKE_LOCK);
        currentPlayList = dataContext.getPlayList(UUID.fromString(dataContext.getSetting(Setting.KEYS.music_current_list_id, _Session.UUID_NULL.toString()).getString()));
        currentSong = dataContext.getSong(currentPlayList.getCurrentSongId());
        songList = dataContext.getSongs(currentPlayList.getId());

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.e("wangsc", "*******  completion  ******* position : " + currentPlayList.getCurrentSongPosition() + " ******* duration : " + currentSong.getDuration());
                if (currentPlayList.getCurrentSongPosition() > 2000)
                    playNext();
            }
        });

        play();
        startTimer();

//        _Utils.addRunLog2File(getApplicationContext(),"音乐播放器onStartCommand()","");

        return super.onStartCommand(intent, flags, startId);
    }


    private void startTimer() {
        if (timer != null) {
            return;
        }
        try {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        currentPlayList.setCurrentSongPosition(mediaPlayer.getCurrentPosition());
                        dataContext.updatePlayList(currentPlayList);
                        EventBus.getDefault().post(new ProgressEvent(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration()));
                    } catch (Exception e) {
                        _Utils.printException(getApplicationContext(), e);
                    }
                }
            }, 0, 1000);
//            _Utils.addRunLog2File(this, "运行记录", "timer start ...");
        } catch (Exception e) {
            _Utils.printException(getApplicationContext(), e);
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();

            timer = null;
//            _Utils.addRunLog2File(this, "运行记录", "timer stop ...");
        }
    }

    /**
     * 播放音乐
     */
    private void play() {
        try {
            mediaPlayer.reset();//把各项参数恢复到初始状态
            mediaPlayer.setDataSource(currentSong.getFileUrl());
            mediaPlayer.prepare();  //进行缓冲
            Log.e("wangsc", "song position : " + currentPlayList.getCurrentSongPosition());
            mediaPlayer.start();
            mediaPlayer.seekTo(currentPlayList.getCurrentSongPosition());


            dataContext.addSetting(Setting.KEYS.music_isplaying,"");
//            _Utils.addRunLog2File(getApplicationContext(),"音乐播放器play()","");
        } catch (Exception e) {
            _Utils.printException(this,e);
        }
    }


    private void playNext() {
        Log.e("wangsc", "******************************************play next ....");
        Song song = null;
        for (Song s : songList) {
            if (s.getId().equals(currentSong.getId())) {
                song = s;
                break;
            }
        }
        if (song != null) {
            int index = songList.indexOf(song) + 1;
            if (index >= songList.size()) {
                index = 0;
            }

            currentSong = songList.get(index);
            currentPlayList.setCurrentSongId(currentSong.getId());
            currentPlayList.setCurrentSongPosition(0);
            dataContext.updatePlayList(currentPlayList);

            play();
            EventBus.getDefault().post(currentSong);
        }
    }

    private void playPrev() {
        Log.e("wangsc", "******************************************play prev ....");
        Song song = null;
        for (Song s : songList) {
            if (s.getId().equals(currentSong.getId())) {
                song = s;
                break;
            }
        }

        if (song != null) {
            int index = songList.indexOf(song) - 1;
            if (index < 0) {
                index = songList.size() - 1;
            }

            currentSong = songList.get(index);
            currentPlayList.setCurrentSongId(currentSong.getId());
            currentPlayList.setCurrentSongPosition(0);
            dataContext.updatePlayList(currentPlayList);

            play();
            EventBus.getDefault().post(currentSong);
        }
    }


    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            currentPlayList.setCurrentSongId(currentSong.getId());
            currentPlayList.setCurrentSongPosition(mediaPlayer.getCurrentPosition());
            dataContext.updatePlayList(currentPlayList);
            stopTimer();
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        EventBus.getDefault().unregister(this);
        _Utils.releaseWakeLock(getApplicationContext(),wakeLock);
        dataContext.deleteSetting(Setting.KEYS.music_isplaying);
//        _Utils.addRunLog2File(getApplicationContext(),"音乐播放器onDestory()","");
    }

    @Subscribe
    public void onEventMainThread(MusicPlayerFragment.SourceButton sourceButton) {
        switch (sourceButton) {
            case BUTTON_PREV:
                playPrev();
                break;
            case BUTTON_NEXT:
                playNext();
                break;
            case BUTTON_BACK:
                int index = mediaPlayer.getCurrentPosition() - 30000;
                if (index < 0)
                    index = 0;
                mediaPlayer.seekTo(index);
                EventBus.getDefault().post(new ProgressEvent(index,currentSong.getDuration()));
                break;
            case BUTTON_FORWARD:
                int index1 = mediaPlayer.getCurrentPosition() + 30000;
                if (index1 > currentSong.getDuration()){
                    playNext();
                }else {
                    mediaPlayer.seekTo(index1);
                    EventBus.getDefault().post(new ProgressEvent(index1,currentSong.getDuration()));
                }
                break;
            case SONG_CHANGED:

                break;
        }
    }

    @Subscribe
    public void onEventMainThread(SeekBar seekBar) {
        Log.e("wangsc", "progress : " + seekBar.getProgress());
        mediaPlayer.seekTo(seekBar.getProgress());
    }
}
