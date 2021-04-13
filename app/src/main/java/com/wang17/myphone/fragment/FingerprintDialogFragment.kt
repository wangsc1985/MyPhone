package com.wang17.myphone.fragment

import android.R
import android.annotation.TargetApi
import android.app.FragmentManager
import android.content.Context
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.widget.TextView
import android.os.Bundle
import android.os.CancellationSignal
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.wang17.myphone.activity.SmsActivity
import com.wang17.myphone.callback.MyCallback
import javax.crypto.Cipher

@TargetApi(23)
class FingerprintDialogFragment : DialogFragment() {
    private var fingerprintManager: FingerprintManager? = null
    private var mCancellationSignal: CancellationSignal? = null
    private var mCipher: Cipher? = null
    private var mIntent :Intent?=null
    private var mCallback:MyCallback?=null
    private var errorMsg: TextView? = null

    /**
     * 标识是否是用户主动取消的认证。
     */
    private var isSelfCancelled = false
    fun setCipher(cipher: Cipher?,callback: MyCallback?) {
        mCipher = cipher
//        mIntent = intent
        mCallback = callback
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fingerprintManager = context!!.getSystemService(FingerprintManager::class.java)
        setStyle(STYLE_NORMAL, R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(com.wang17.myphone.R.layout.fragment_finger, container, false)
        errorMsg = v.findViewById(com.wang17.myphone.R.id.error_msg)
        return v
    }

    override fun onResume() {
        super.onResume()
        // 开始指纹认证监听
        startListening(mCipher)
    }

    override fun onPause() {
        super.onPause()
        // 停止指纹认证监听
        stopListening()
    }

    private fun startListening(cipher: Cipher?) {
        isSelfCancelled = false
        mCancellationSignal = CancellationSignal()
        fingerprintManager!!.authenticate(FingerprintManager.CryptoObject(cipher!!), mCancellationSignal, 0, object : FingerprintManager.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!isSelfCancelled) {
                    errorMsg!!.text = errString
                    if (errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                        dismiss()
                    }
                }
            }

            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
                errorMsg!!.text = helpString
            }

            override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
//                val intent = Intent(context, SmsActivity::class.java)
//                startActivity(mIntent)

                mCallback?.execute()

                dismiss()
                stopListening()
            }

            override fun onAuthenticationFailed() {
                errorMsg!!.text = "指纹认证失败，请再试一次"
            }
        }, null)
    }

    private fun stopListening() {
        if (mCancellationSignal != null) {
            mCancellationSignal!!.cancel()
            mCancellationSignal = null
            isSelfCancelled = true
        }
    }
}