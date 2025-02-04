package com.wang17.myphone.activity

import android.support.v7.app.AppCompatActivity
import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.wang17.myphone.R
import kotlinx.android.synthetic.main.activity_knocker.*

class KnockerActivity : AppCompatActivity() {
    private lateinit var daKnockSound:SoundPool
    private lateinit var xiaoKnockSound:SoundPool
    private var isDaKnock=true
    private var count = 0

    private val hideHandler = Handler()
    private var prvClickTime = 0L
    private var avg=0L


    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val showPart2Runnable = Runnable {
        supportActionBar?.show()
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_knocker)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true

        daKnockSound = SoundPool(100, AudioManager.STREAM_MUSIC,0)
        xiaoKnockSound = SoundPool(100,AudioManager.STREAM_MUSIC,0)
        daKnockSound.load(this,R.raw.gu,1)
        xiaoKnockSound.load(this,R.raw.muyu,1)

        fullscreen_content.setOnClickListener {
            val now =  System.currentTimeMillis()
            if(now-prvClickTime<3000){
                if(avg==0L){
                    avg = now-prvClickTime
                }else{
                    avg = (avg+(now - prvClickTime))/2
                }
                val cc = avg*1.080
                fullscreen_content.text = "${avg}毫秒/次  ${(cc/60).toInt()}分${(cc%60).toInt()}秒/圈"
            }
            prvClickTime = now

            if(isDaKnock){
//                daKnockSound.play(1,1.0f,1.0f,0,0,1.0f)
                isDaKnock=false
                count++
            }else{
                isDaKnock=true
            }
//            xiaoKnockSound.play(1,1.0f,1.0f,0,0,1.0f)
        }
        fullscreen_content.setOnLongClickListener {
            avg=0
            fullscreen_content.text = ""
            prvClickTime = 0
            true
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delayedHide(100)
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        supportActionBar?.hide()
        isFullscreen = false

        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        isFullscreen = true

        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        private const val AUTO_HIDE = true
        private const val AUTO_HIDE_DELAY_MILLIS = 3000
        private const val UI_ANIMATION_DELAY = 300
    }
}