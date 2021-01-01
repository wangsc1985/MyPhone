package com.wang17.myphone.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import com.wang17.myphone.R;

public class NumberSpeaker {

    MediaPlayer s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s100, s1000, s10000, sd, sf;
    MediaPlayer s_0, s_1, s_2, s_3, s_4, s_5, s_6, s_7, s_8, s_9, s_10, s_100, s_1000, s_10000, s_d, s_f;

    public NumberSpeaker(Context context) {
        s0 = MediaPlayer.create(context, R.raw.s0);
        s1 = MediaPlayer.create(context, R.raw.s1);
        s2 = MediaPlayer.create(context, R.raw.s2);
        s3 = MediaPlayer.create(context, R.raw.s3);
        s4 = MediaPlayer.create(context, R.raw.s4);
        s5 = MediaPlayer.create(context, R.raw.s5);
        s6 = MediaPlayer.create(context, R.raw.s6);
        s7 = MediaPlayer.create(context, R.raw.s7);
        s8 = MediaPlayer.create(context, R.raw.s8);
        s9 = MediaPlayer.create(context, R.raw.s9);
        s10 = MediaPlayer.create(context, R.raw.s10);
        s100 = MediaPlayer.create(context, R.raw.s100);
        s1000 = MediaPlayer.create(context, R.raw.s1000);
        s10000 = MediaPlayer.create(context, R.raw.s10000);
        sd = MediaPlayer.create(context, R.raw.sd);
        sf = MediaPlayer.create(context, R.raw.sf);


        s_0 = MediaPlayer.create(context, R.raw.s_0);
        s_1 = MediaPlayer.create(context, R.raw.s_1);
        s_2 = MediaPlayer.create(context, R.raw.s_2);
        s_3 = MediaPlayer.create(context, R.raw.s_3);
        s_4 = MediaPlayer.create(context, R.raw.s_4);
        s_5 = MediaPlayer.create(context, R.raw.s_5);
        s_6 = MediaPlayer.create(context, R.raw.s_6);
        s_7 = MediaPlayer.create(context, R.raw.s_7);
        s_8 = MediaPlayer.create(context, R.raw.s_8);
        s_9 = MediaPlayer.create(context, R.raw.s_9);
        s_10 = MediaPlayer.create(context, R.raw.s_10);
        s_100 = MediaPlayer.create(context, R.raw.s_100);
        s_1000 = MediaPlayer.create(context, R.raw.s_1000);
        s_10000 = MediaPlayer.create(context, R.raw.s_10000);
        s_d = MediaPlayer.create(context, R.raw.s_d);
        s_f = MediaPlayer.create(context, R.raw.s_f);
    }

    public void readNumber(Context context,float num) {
        try {
            boolean isMan = true;
            if (num < 0)
                isMan = false;

            NumToWord numToWord = new NumToWord();
            String number = numToWord.cvt(num + "", true);

            for (int i = 0; i < number.length(); i++) {
                switch (number.charAt(i)) {
                    case '十':
                        s0 = MediaPlayer.create(context, R.raw.s10);
                        s0.start();
                        break;
                    case '百':
                        s0 = MediaPlayer.create(context, R.raw.s100);
                        s0.start();
                        break;
                    case '千':
                        s0 = MediaPlayer.create(context, R.raw.s1000);
                        s0.start();
                        break;
                    case '万':
                        s0 = MediaPlayer.create(context, R.raw.s10000);
                        s0.start();
                        break;
                    case '零':
                        s0 = MediaPlayer.create(context, R.raw.s0);
                        s0.start();
                        break;
                    case '一':
                        s0 = MediaPlayer.create(context, R.raw.s1);
                        s0.start();
                        break;
                    case '二':
                        s0 = MediaPlayer.create(context, R.raw.s2);
                        s0.start();
                        break;
                    case '三':
                        s0 = MediaPlayer.create(context, R.raw.s3);
                        s0.start();
                        break;
                    case '四':
                        s0 = MediaPlayer.create(context, R.raw.s4);
                        s0.start();
                        break;
                    case '五':
                        s0 = MediaPlayer.create(context, R.raw.s5);
                        s0.start();
                        break;
                    case '六':
                        s0 = MediaPlayer.create(context, R.raw.s6);
                        s0.start();
                        break;
                    case '七':
                        s0 = MediaPlayer.create(context, R.raw.s7);
                        s0.start();
                        break;
                    case '八':
                        s0 = MediaPlayer.create(context, R.raw.s8);
                        s0.start();
                        break;
                    case '九':
                        s0 = MediaPlayer.create(context, R.raw.s9);
                        s0.start();
                        break;
                    case '点':
                        s0 = MediaPlayer.create(context, R.raw.sd);
                        s0.start();
                        break;
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
//            soundpool.release();
        }
    }


}
