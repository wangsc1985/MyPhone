package com.wang17.myphone.fragment

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wang17.myphone.R
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.service.BuddhaPlayerService
import com.wang17.myphone.service.StockService
import com.wang17.myphone.util.DataContext
import com.wang17.myphone.util._AnimationUtils
import com.wang17.myphone.util._Utils
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.android.synthetic.main.fragment_player.abtn_stock

class BuddhaPlayerFragment : Fragment() {

    lateinit var abtn_stockAnimator: AnimatorSuofangView
    lateinit var btn_kAnimator: AnimatorSuofangView
    lateinit var btn_mAnimator: AnimatorSuofangView
    lateinit var btn_zAnimator: AnimatorSuofangView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val dataContext = DataContext(context)
        abtn_stockAnimator = AnimatorSuofangView(abtn_stock)
        btn_kAnimator = AnimatorSuofangView(btn_k)
        btn_mAnimator = AnimatorSuofangView(btn_m)
        btn_zAnimator = AnimatorSuofangView(btn_z)
        if (dataContext.getSetting(Setting.KEYS.is_stocks_listener, false).boolean == true) {
            animatorSuofang(abtn_stockAnimator)
        }
        if (dataContext.getSetting(Setting.KEYS.is_mnf, false).boolean == true) {
            animatorSuofang(btn_mAnimator)
        }
        if (dataContext.getSetting(Setting.KEYS.is_knf, false).boolean == true) {
            animatorSuofang(btn_kAnimator)
        }
        if (dataContext.getSetting(Setting.KEYS.is_znf, false).boolean == true) {
            animatorSuofang(btn_zAnimator)
        }

        abtn_stock.setOnClickListener {
            //region 在StockReportService里面执行
            if (dataContext.getSetting(Setting.KEYS.is_stocks_listener, false).boolean == false) {
                context!!.startService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stockAnimator)
            }
            //endregion
        }
        abtn_stock.setOnLongClickListener { //region 在StockReportService里面执行
            if (dataContext.getSetting(Setting.KEYS.is_stocks_listener, false).boolean == true) {
                context!!.stopService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, false)
                stopAnimatorSuofang(abtn_stockAnimator)
            } else {
                context!!.startService(Intent(context, StockService::class.java))
                dataContext.editSetting(Setting.KEYS.is_stocks_listener, true)
                animatorSuofang(abtn_stockAnimator)
                //
                _Utils.clickHomeButton(context)
            }
            //endregion
            true
        }

        btn_m.setOnClickListener {
            stopAnimatorSuofang(btn_kAnimator)
            stopAnimatorSuofang(btn_mAnimator)
            stopAnimatorSuofang(btn_zAnimator)

            val intent = Intent(context, BuddhaPlayerService::class.java)
            if (dataContext.getSetting(Setting.KEYS.is_mnf, false).boolean == false) {
                intent.putExtra("velocity", 1)
                context?.startService(intent)
                animatorSuofang(btn_mAnimator)
                dataContext.editSetting(Setting.KEYS.is_mnf, true)
            } else {
                context?.stopService(intent)
                dataContext.editSetting(Setting.KEYS.is_mnf, false)
                stopAnimatorSuofang(btn_mAnimator)
            }
        }
        btn_k.setOnClickListener {
            stopAnimatorSuofang(btn_kAnimator)
            stopAnimatorSuofang(btn_mAnimator)
            stopAnimatorSuofang(btn_zAnimator)
            animatorSuofang(btn_kAnimator)

            val intent = Intent(context, BuddhaPlayerService::class.java)
            if (dataContext.getSetting(Setting.KEYS.is_knf, false).boolean == false) {
                intent.putExtra("velocity", 3)
                context?.startService(intent)
                animatorSuofang(btn_kAnimator)
                dataContext.editSetting(Setting.KEYS.is_knf, true)
            } else {
                context?.stopService(intent)
                dataContext.editSetting(Setting.KEYS.is_knf, false)
                stopAnimatorSuofang(btn_kAnimator)
            }
        }
        btn_z.setOnClickListener {
            stopAnimatorSuofang(btn_kAnimator)
            stopAnimatorSuofang(btn_mAnimator)
            stopAnimatorSuofang(btn_zAnimator)
            animatorSuofang(btn_zAnimator)

            val intent = Intent(context, BuddhaPlayerService::class.java)
            if (dataContext.getSetting(Setting.KEYS.is_znf, false).boolean == false) {
                intent.putExtra("velocity", 2)
                context?.startService(intent)
                animatorSuofang(btn_zAnimator)
                dataContext.editSetting(Setting.KEYS.is_znf, true)
            } else {
                context?.stopService(intent)
                dataContext.editSetting(Setting.KEYS.is_znf, false)
                stopAnimatorSuofang(btn_zAnimator)
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    //region 动画
    fun animatorSuofang(view: AnimatorSuofangView) {
        if (view.scaleX == null)
            view.scaleX = ObjectAnimator.ofFloat(view.targetView, "scaleX", 1f, 0.7f, 1f)
        view.scaleX!!.repeatCount = -1
        view.scaleX!!.duration = 600
        view.scaleX!!.start()
        if (view.scaleY == null)
            view.scaleY = ObjectAnimator.ofFloat(view.targetView, "scaleY", 1f, 0.7f, 1f)
        view.scaleY!!.repeatCount = -1
        view.scaleY!!.duration = 600
        view.scaleY!!.start()
    }

    fun stopAnimatorSuofang(view: AnimatorSuofangView) {
        if (view.targetView != null) {
            view.scaleY?.repeatCount = 0
            view.scaleX?.repeatCount = 0
        }
    }

    class AnimatorSuofangView {
        var scaleY: ObjectAnimator? = null
        var scaleX: ObjectAnimator? = null
        var targetView: View

        constructor(view: View) {
            this.targetView = view
        }
    }
    //endregion
}