package com.wang17.myphone.util;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.wang17.myphone.database.Song;

import java.util.ArrayList;
import java.util.UUID;

/**
 *
 * @ClassName: com.example.mediastore.AudioUtils
 * @Description: 音频文件帮助类
 * @author zhaokaiqiang
 * @date 2014-12-4 上午11:39:45
 *
 */
public class _AudioUtils {

    /**
     * 获取sd卡所有的音乐文件
     *
     * @return
     * @throws Exception
     */
    public static ArrayList<Song> getAllSongs(Context context,UUID playListId) {

        ArrayList<Song> songs = null;

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.YEAR,
                        MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DATA },
                "("+ MediaStore.Audio.Media.MIME_TYPE + "=? or "
                        + MediaStore.Audio.Media.MIME_TYPE + "=? or "
                        + MediaStore.Audio.Media.MIME_TYPE + "=?) and "
                        + MediaStore.Audio.Media.DURATION + ">?",
                new String[] { "audio/mpeg", "audio/x-ms-wma", "audio/mp3","471085"}, MediaStore.Audio.Media.DISPLAY_NAME+" ASC");

        songs = new ArrayList<Song>();

        if (cursor.moveToFirst()) {

            Song song = null;

            do {
                song = new Song(UUID.randomUUID());
                // 文件名
                song.setFileName(cursor.getString(1));
                // 歌曲名
                song.setTitle(cursor.getString(2));
                // 时长
                song.setDuration(cursor.getInt(3));
                // 歌手名
                song.setSinger(cursor.getString(4));
                // 专辑名
                song.setAlbum(cursor.getString(5));
                // 年代
                if (cursor.getString(6) != null) {
                    song.setYear(cursor.getString(6));
                } else {
                    song.setYear("未知");
                }
                // 歌曲格式
                if ("audio/mpeg".equals(cursor.getString(7).trim())) {
                    song.setType("mp3");
                } else if ("audio/x-ms-wma".equals(cursor.getString(7).trim())) {
                    song.setType("wma");
                }
                // 文件大小
                if (cursor.getString(8) != null) {
                    float size = cursor.getInt(8) / 1024f / 1024f;
                    song.setSize((size + "").substring(0, 4) + "M");
                } else {
                    song.setSize("未知");
                }
                // 文件路径
                if (cursor.getString(9) != null) {
                    song.setFileUrl(cursor.getString(9));
                }
                song.setPlayListId(playListId);
                songs.add(song);
            } while (cursor.moveToNext());

            cursor.close();

        }
        return songs;
    }

    /**
     * 获取sd卡所有的音乐文件
     *
     * @return
     * @throws Exception
     */
    public static ArrayList<Song> getSongsBySinger(Context context,String singer,UUID playListId) {

        ArrayList<Song> songs = null;

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.YEAR,
                        MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DATA },
                "("+ MediaStore.Audio.Media.MIME_TYPE + "=? or "
                        + MediaStore.Audio.Media.MIME_TYPE + "=? or "
                        + MediaStore.Audio.Media.MIME_TYPE + "=?) and "
                        + MediaStore.Audio.Media.DURATION + ">? and "
                        + MediaStore.Audio.Media.ARTIST + "=?",
                new String[] { "audio/mpeg", "audio/x-ms-wma", "audio/mp3","471085" ,singer}, MediaStore.Audio.Media.DISPLAY_NAME+" ASC");

        songs = new ArrayList<Song>();

        if (cursor.moveToFirst()) {

            Song song = null;

            do {
                song = new Song(UUID.randomUUID());
                // 文件名
                song.setFileName(cursor.getString(1));
                // 歌曲名
                song.setTitle(cursor.getString(2));
                // 时长
                song.setDuration(cursor.getInt(3));
                // 歌手名
                song.setSinger(cursor.getString(4));
                // 专辑名
                song.setAlbum(cursor.getString(5));
                // 年代
                if (cursor.getString(6) != null) {
                    song.setYear(cursor.getString(6));
                } else {
                    song.setYear("未知");
                }
                // 歌曲格式
                if ("audio/mpeg".equals(cursor.getString(7).trim())) {
                    song.setType("mp3");
                } else if ("audio/x-ms-wma".equals(cursor.getString(7).trim())) {
                    song.setType("wma");
                }
                // 文件大小
                if (cursor.getString(8) != null) {
                    float size = cursor.getInt(8) / 1024f / 1024f;
                    song.setSize((size + "").substring(0, 4) + "M");
                } else {
                    song.setSize("未知");
                }
                // 文件路径
                if (cursor.getString(9) != null) {
                    song.setFileUrl(cursor.getString(9));
                }
                song.setPlayListId(playListId);
                songs.add(song);
            } while (cursor.moveToNext());

            cursor.close();

        }
        return songs;
    }

}