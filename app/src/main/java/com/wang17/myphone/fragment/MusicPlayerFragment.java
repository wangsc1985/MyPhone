package com.wang17.myphone.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.service.MusicPlayerService;
import com.wang17.myphone.util._AudioUtils;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.PlayList;
import com.wang17.myphone.model.ProgressEvent;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.model.database.Song;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;
import java.util.UUID;

/**
 * 未播放音乐，按播放键：
 * 启动服务，并播放。
 * 播放中，按暂停键：
 * 停止播放，停止记录器，停止服务。
 * 播放中，按停止键：
 * 停止播放，停止记录器，停止服务。
 * 暂停后，按播放键：
 * 启动服务，并播放。
 * <p>
 * 也就是暂停键和停止键功能上是一样的，所以只设计暂停键，不设置停止键。界面的播放按钮，也只有播放和暂停两种操作状态。
 * <p>
 * 再，暂停后播放，和首次播放，流程也是一样的，唯一不一样的就是服务中，要判断文件是否和数据库中存储的path是否一致，如果不一致，position要清零。
 */
@Deprecated
public class MusicPlayerFragment extends Fragment{

    private ImageView imageViewPrev;
    private ImageView imageViewPlay;
    private ImageView imageViewNext;
    private ImageView imageViewPlayList;
    private ImageView imageViewBack;
    private ListView listViewAllList;
    private TextView textViewTimer1, textViewTimer2, textViewInfo;
    private SeekBar seekBarMusic;


    private DataContext dataContext;
    private boolean isPlaying;
    private List<Song> songList;
    private PlayList currentPlayList;
    private Song currentSong;
    

    private SongListdAdapter adapter;
    private int currentItemPostion;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        initData();
    }

    private void initData() {
        try {
            dataContext = new DataContext(getContext());

            UUID id = UUID.fromString(dataContext.getSetting(Setting.KEYS.music_current_list_id, _Session.UUID_NULL.toString()).getString());
            List<PlayList> pls = dataContext.getPlayLists();
            currentPlayList = dataContext.getPlayList(id);

            List<Song> systemAllSongs = _AudioUtils.getAllSongs(getContext(),currentPlayList.getId());
            songList = dataContext.getSongs(currentPlayList.getId());
            if (songList.size() == 0) {
                songList = _AudioUtils.getAllSongs(getContext(),currentPlayList.getId());
                reSaveSongList();
            }else{
                for (Song s : songList){
                    Boolean isValid = false;
                    for(Song ss: systemAllSongs){
                        if(ss.getFileUrl().equals(s.getFileUrl())){
                            isValid=true;
                        }
                    }
                    s.setValid(isValid);
                }
            }

            currentSong = dataContext.getSong(currentPlayList.getCurrentSongId());
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    /**
     * 将歌曲列表存入数据库
     */
    private void reSaveSongList() {
        dataContext.deleteSongs(currentPlayList.getId());
        dataContext.addSongs(songList);
    }

//    public static MusicPlayerFragment musicFragment;
//    public static MusicPlayerFragment newInstant(){
//        if(musicFragment==null){
//            musicFragment = new MusicPlayerFragment();
//        }
//        return musicFragment;
//    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music, container, false);

        //
        textViewInfo = (TextView) view.findViewById(R.id.textView_info);
        reflushInfo();

        //
        textViewTimer1 = (TextView) view.findViewById(R.id.textView_time1);
        textViewTimer2 = (TextView) view.findViewById(R.id.textView_time2);
        seekBarMusic = (SeekBar) view.findViewById(R.id.seekBar_music);
        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewTimer1.setText(DateTime.toSpanString(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                EventBus.getDefault().post(seekBar);

                textViewTimer1.setText(DateTime.toSpanString(seekBar.getProgress()));
                currentPlayList.setCurrentSongPosition(seekBar.getProgress());
                dataContext.updatePlayList(currentPlayList);
            }
        });
        if (currentSong != null) {
            textViewTimer1.setText(DateTime.toSpanString(currentPlayList.getCurrentSongPosition()));
            textViewTimer2.setText(DateTime.toSpanString(currentSong.getDuration()));
            seekBarMusic.setMax(currentSong.getDuration());
            seekBarMusic.setProgress(currentPlayList.getCurrentSongPosition());
        }

        //
        imageViewPlay = (ImageView) view.findViewById(R.id.imageView_play);
        if (dataContext.getSetting(Setting.KEYS.music_isplaying) != null) {
            setButtonPlaying();
        }
        imageViewPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (isPlaying) {
                        getContext().stopService(new Intent(getContext(), MusicPlayerService.class));
                        setButtonStoped();
                    } else {
                        if (currentSong == null) {
                            Snackbar.make(imageViewPlay, "请先选择曲目", Snackbar.LENGTH_LONG).show();
                        } else {
                            startMusicService();
                        }
                    }
                } catch (Exception e) {
                    _Utils.printException(getContext(), e);
                }
            }
        });

        //
        imageViewPrev = (ImageView) view.findViewById(R.id.imageView_prev);
        imageViewPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(SourceButton.BUTTON_PREV);
            }
        });

        //
        imageViewNext = (ImageView) view.findViewById(R.id.imageView_next);
        imageViewNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(SourceButton.BUTTON_NEXT);
            }
        });

        //
        imageViewPlayList = (ImageView) view.findViewById(R.id.imageView_playList);
        imageViewPlayList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reSaveSongList();
            }
        });
        imageViewPlayList.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                songList = _AudioUtils.getAllSongs(getContext(),currentPlayList.getId());
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        //
        imageViewBack = (ImageView) view.findViewById(R.id.imageView_backOff);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(SourceButton.BUTTON_BACK);
            }
        });
        imageViewBack.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                EventBus.getDefault().post(SourceButton.BUTTON_FORWARD);
//                }
                return true;
            }
        });

        //
        listViewAllList = (ListView) view.findViewById(R.id.listView_allList);
        adapter = new SongListdAdapter();
        listViewAllList.setAdapter(adapter);
        moveToCurrentSongItem();

        listViewAllList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {

                    if(!songList.get(position).isValid()){
                        return;
                    }
                    currentSong = songList.get(position);

                    for (int i = 0; i < parent.getChildCount(); i++) {
                        View vv = parent.getChildAt(i);
                        TextView v1 = (TextView) vv.findViewById(R.id.textView_title);
                        v1.setTextColor(getResources().getColor(android.R.color.background_dark, null));
                        TextView v2 = (TextView) vv.findViewById(R.id.textView_singer);
                        v2.setTextColor(getResources().getColor(R.color.common_google_signin_btn_text_light_focused, null));
                    }
                    TextView v1 = (TextView) view.findViewById(R.id.textView_title);
                    v1.setTextColor(getResources().getColor(R.color.a, null));
                    TextView v2 = (TextView) view.findViewById(R.id.textView_singer);
                    v2.setTextColor(getResources().getColor(R.color.a, null));

                    currentPlayList = dataContext.getPlayList(currentSong.getPlayListId());
                    currentPlayList.setCurrentSongPosition(0);
                    currentPlayList.setCurrentSongId(currentSong.getId());
                    dataContext.updatePlayList(currentPlayList);
                    dataContext.editSetting(Setting.KEYS.music_current_list_id, currentPlayList.getId());
                    startMusicService();

                    reflushInfo();
                } catch (Exception e) {
                    Log.e("wangsc", e.getMessage());
                }
            }
        });
        listViewAllList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                try {
//                    listViewAllList.getCheckedItemPositions();
//                    Song song = songList.get(position);
//                    String singer = song.getSinger();
//                    songList = AudioUtils.getSongsBySinger(getContext(), singer,currentPlayList.getId());
//                    currentSong = null;
//                    currentPlayList.setCurrentSongId(_Session.UUID_NULL);
//                    currentPlayList.setCurrentSongPosition(0);
//                    reSaveSongList();
//                    reflushInfo();
//                    adapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Log.e("wangsc", e.getMessage());
                }
                return true;
            }
        });

        return view;
    }

    private void moveToCurrentSongItem() {
        if (currentSong != null && songList.size() > 0) {
            for (int i = 0; i < songList.size(); i++) {
                if (songList.get(i).getId().equals(currentSong.getId())) {
                    currentItemPostion = i;
                }
            }
            listViewAllList.setSelection(currentItemPostion);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dataContext.getSetting(Setting.KEYS.music_isplaying) != null) {
            setButtonPlaying();
        }
    }

    private void reflushInfo() {
        if (currentSong == null) {
            textViewInfo.setText("");
        } else {
            textViewInfo.setText("正在播放：" + currentSong.getFileName().substring(0, currentSong.getFileName().lastIndexOf('.'))
                    + "\n曲目时长：" + DateTime.toSpanString(currentSong.getDuration()));
        }
    }

    private void startMusicService() {
        Intent intent = new Intent(getContext(), MusicPlayerService.class);
        getContext().startService(intent);
        setButtonPlaying();
    }

    private void setButtonPlaying() {
        imageViewPlay.setImageResource(android.R.drawable.ic_media_pause);
        isPlaying = true;
        dataContext.addSetting(Setting.KEYS.music_isplaying, "");
    }

    private void setButtonStoped() {
        imageViewPlay.setImageResource(android.R.drawable.ic_media_play);
        isPlaying = false;
        dataContext.deleteSetting(Setting.KEYS.music_isplaying);
    }


    public enum SourceButton {
        BUTTON_PREV, BUTTON_NEXT, BUTTON_BACK, BUTTON_FORWARD, SONG_CHANGED
    }

    //region EventBus事件处理
    @Subscribe
    public void onEventMainThread(Song song) {
        currentSong = song;
        currentPlayList.setCurrentSongId(song.getId());
        adapter.notifyDataSetChanged();
        reflushInfo();
        reflushProgressbar(new ProgressEvent(0, song.getDuration()));
        Log.e("wangsc", "   --    onEventMainThread");
    }

    @Subscribe
    public void onEventMainThread(ProgressEvent event) {
        reflushProgressbar(event);
    }
    //endregion

    private void reflushProgressbar(ProgressEvent event) {
        seekBarMusic.setMax(event.getMax());
        seekBarMusic.setProgress(event.getProgress());
        textViewTimer1.setText(event.getTime1());
        textViewTimer2.setText(event.getTime2());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    class SongListdAdapter extends BaseAdapter implements Checkable {
        @Override
        public int getCount() {
            return songList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int index = position;
            try {
                convertView = View.inflate(getContext(), R.layout.inflate_list_item_song, null);
                final Song song = songList.get(position);
                TextView textViewTitle = (TextView) convertView.findViewById(R.id.textView_title);
                TextView textViewSinger = (TextView) convertView.findViewById(R.id.textView_singer);
                textViewTitle.setText(song.getFileName().substring(0, song.getFileName().lastIndexOf('.')));
                textViewSinger.setText(song.getSinger());
                if (song.getId().equals(currentSong.getId())) {
                    textViewTitle.setTextColor(getResources().getColor(R.color.a, null));
                    textViewSinger.setTextColor(getResources().getColor(R.color.a, null));
                }
                if(!song.isValid()){
                    textViewTitle.setTextColor(getResources().getColor(R.color.invalid));
                    textViewSinger.setTextColor(getResources().getColor(R.color.invalid));
                    convertView.findViewById(R.id.textView_invalid).setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return convertView;
        }

        //region 可选状态设置
        @Override
        public void setChecked(boolean checked) {

        }

        @Override
        public boolean isChecked() {
            return false;
        }

        @Override
        public void toggle() {

        }
        //endregion
    }

}
