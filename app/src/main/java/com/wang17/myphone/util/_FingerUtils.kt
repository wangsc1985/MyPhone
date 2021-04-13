package com.wang17.myphone.util

import android.annotation.TargetApi
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import com.wang17.myphone.callback.MyCallback
import com.wang17.myphone.fragment.FingerprintDialogFragment
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class _FingerUtils {
    companion object {
        private const val DEFAULT_KEY_NAME = "default_key"
        private fun isSupportFingerprint(context:Context): Boolean {
            if (Build.VERSION.SDK_INT < 23) {
                Toast.makeText(context, "您的系统版本过低，不支持指纹功能", Toast.LENGTH_SHORT).show()
                return false
            } else {
                val keyguardManager = context?.getSystemService(KeyguardManager::class.java)
                val fingerprintManager = context?.getSystemService(FingerprintManager::class.java)
                if (!fingerprintManager?.isHardwareDetected!!) {
                    Toast.makeText(context, "您的手机不支持指纹功能", Toast.LENGTH_SHORT).show()
                    return false
                } else if (!keyguardManager?.isKeyguardSecure!!) {
                    Toast.makeText(context, "您还未设置锁屏，请先设置锁屏并添加一个指纹", Toast.LENGTH_SHORT).show()
                    return false
                } else if (!fingerprintManager?.hasEnrolledFingerprints()) {
                    Toast.makeText(context, "您至少需要在系统设置中添加一个指纹", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            return true
        }
        @TargetApi(23)
        fun showFingerPrintDialog(activity: FragmentActivity, callback:MyCallback?) {
            if(isSupportFingerprint(activity.applicationContext)){
                // 获取key
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore?.load(null)
                // 使用指纹秘钥加密解密字符串
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val builder = KeyGenParameterSpec.Builder(DEFAULT_KEY_NAME, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC).setUserAuthenticationRequired(true)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
                // 获取cipher
                val key = keyStore!!.getKey(DEFAULT_KEY_NAME, null) as SecretKey
                val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
                cipher?.init(Cipher.ENCRYPT_MODE, key)
                //
                val fragment = FingerprintDialogFragment()
                fragment.setCipher(cipher, callback)
                fragment.show(activity.supportFragmentManager, "fingerprint")
            }
        }
    }
}