package com.wang17.myphone.util;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.wang17.myphone.R;

/**
 * Created by Administrator on 2017/6/30.
 */

public class _AnimationUtils {

    public static void heartBeat(View myView) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(myView, "scaleX", 1f, 0.5f, 1f);
        scaleX.setDuration(800);
        scaleX.start();
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(myView, "scaleY", 1f, 0.5f, 1f);
        scaleY.setDuration(800);
        scaleY.start();
    }
    /**
     * 闪烁动画
     *
     * @param targetView 动画对象
     */
    public static void setFlickerAnimation(View targetView, long... duration) {
        Animation animation = new AlphaAnimation(1, 0);
        if (duration.length == 0)
            animation.setDuration(700);//闪烁时间间隔
        else if (duration.length == 1)
            animation.setDuration(duration[0]);//闪烁时间间隔
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        targetView.setAnimation(animation);
    }

    /**
     * 旋转动画
     * @param context
     * @param target
     * @param duration
     */
    public static void setRorateAnimation(Context context, View target ,long... duration) {
        Animation rorateAnimation = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.rotate_animation);
        if (duration.length == 1)
            rorateAnimation.setDuration(duration[0]);
        LinearInterpolator lin = new LinearInterpolator();
        rorateAnimation.setInterpolator(lin);
        target.startAnimation(rorateAnimation);
    }

    public static void setRorateAnimationOnce(Context context, View target ,long... duration) {
        Animation rorateAnimation = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.rotate_once_animation);
        if (duration.length == 1)
            rorateAnimation.setDuration(duration[0]);
        LinearInterpolator lin = new LinearInterpolator();
        rorateAnimation.setInterpolator(lin);
        target.startAnimation(rorateAnimation);
    }

    /**
     * 纯代码实现旋转动画
     * @param view
     * @param duration
     * @param repeatCount
     */
    public static void setRorateAnimationOnce(View view,int duration,int repeatCount){
        RotateAnimation animation = new RotateAnimation(0, 359);
        animation.setDuration(duration);
        animation.setRepeatCount(repeatCount);//动画的反复次数
        animation.setFillAfter(true);//设置为true，动画转化结束后被应用
        view.startAnimation(animation);//開始动画
    }
}
