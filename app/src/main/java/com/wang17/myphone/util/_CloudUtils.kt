package com.wang17.myphone.util

import android.content.Context
import android.util.Log
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.callback.HttpCallback
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.PostArgument
import com.wang17.myphone.model.database.Position
import com.wang17.myphone.model.database.Setting
import com.wang17.myphone.util._OkHttpUtil.getRequest
import com.wang17.myphone.util._OkHttpUtil.postRequestByJson
import org.json.JSONArray
import java.util.*
import java.util.concurrent.CountDownLatch

object _CloudUtils {
    private var newMsgCount = 0

    private val env = "yipinshangdu-4wk7z"
    private val appid = "wxbdf065bdeba96196"
    private val secret = "d2834f10c0d81728e73a4fe4012c0a5d"

    @JvmStatic
    fun getToken(context: Context): String {
        val dc = DataContext(context)
        val setting = dc.getSetting("token_exprires")
        if (setting != null) {
            val exprires = setting.long
            if (System.currentTimeMillis() > exprires) {
                /**
                 * token过期
                 */
                return loadNewTokenFromHttp((context))
            } else {
                /**
                 * token仍有效
                 */
                e("本地token有效期：${DateTime(exprires).toLongDateTimeString()}")
                return dc.getSetting("token").string
            }
        } else {
            return loadNewTokenFromHttp(context)
        }
    }

    private fun loadNewTokenFromHttp(context: Context): String {
        var token = ""

        val a = System.currentTimeMillis()
        val latch = CountDownLatch(1)
        getRequest("https://sahacloudmanager.azurewebsites.net/home/token/${appid}/${secret}", HttpCallback { html ->
            try {
//                e(html)
                val data = html.split(":")
                if (data.size == 2) {
                    token = data[0]
                    e(data[1].toDouble())
                    e(data[1].toDouble().toLong())
                    val exprires = data[1].toDouble().toLong()

                    // 将新获取的token及exprires存入本地数据库
                    val dc = DataContext(context)
                    dc.editSetting("token", token)
                    dc.editSetting("token_exprires", exprires)


                    val b = System.currentTimeMillis()
                    e("从微软获取token：$token , 有效期：${DateTime(exprires).toLongDateTimeString()} 用时：${b - a}")
                }
            } catch (e: java.lang.Exception) {
                e(e.message!!)
            } finally {
                latch.countDown()
            }
        })
        latch.await()
        return token
    }

    @JvmStatic
    fun saveSetting(context: Context, pwd: String?, name: String?, value: Any, callback: CloudCallback?) {
        newMsgCount = 0

        // 获取accessToken

        try {
            val accessToken = getToken(context)
            // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
            val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=saveSetting"
            val args: MutableList<PostArgument> = ArrayList()
            args.add(PostArgument("pwd", pwd))
            args.add(PostArgument("name", name))
            args.add(PostArgument("value", value.toString()))
            postRequestByJson(url, args, HttpCallback { html ->
                try {
                    e(html)
                    val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                    if (_JsonUtils.isContainsKey(resp_data, "success")) {
                        val code = _JsonUtils.getValueByKey(resp_data.toString(), "code").toInt()
                        when (code) {
                            0 -> callback?.excute(0, "修改完毕")
                            1 -> callback?.excute(1, "添加成功")
                        }
                    } else if (_JsonUtils.isContainsKey(resp_data, "msg")) {
                        callback?.excute(-1, "访问码错误")
                    }
                } catch (e: Exception) {
                    callback?.excute(-2, e.message)
                }
            })
        } catch (e: Exception) {
            callback?.excute(-1, e.message)
        }
    }

    @JvmStatic
    fun getSetting(context: Context, pwd: String, name: String, callback: CloudCallback) {
        newMsgCount = 0

        // 获取accessToken
        try {
            val accessToken = getToken(context)
            // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
            val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=getSetting"
            val args: MutableList<PostArgument> = ArrayList()
            args.add(PostArgument("pwd", pwd))
            args.add(PostArgument("name", name))
            postRequestByJson(url, args, object : HttpCallback {
                override fun excute(html: String) {
                    try {
                        e(html)
                        val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                        if (_JsonUtils.isContainsKey(resp_data, "value")) {
                            val value = _JsonUtils.getValueByKey(resp_data, "value")
                            callback.excute(0, value)
                        } else if (_JsonUtils.isContainsKey(resp_data, "msg")) {
                            val code = _JsonUtils.getValueByKey(resp_data, "code").toInt()
                            when (code) {
                                0 -> callback.excute(-3, "操作码错误")
                                1 -> callback.excute(-4, "不存在配置信息")
                            }
                        }
                    } catch (e: Exception) {
                        callback.excute(-2, e.message!!)
                    }
                }
            })
        } catch (e: Exception) {
            callback.excute(-1, e.message!!)
        }
    }

    @JvmStatic
    fun getNewMsg(context: Context, callback: CloudCallback) {
        newMsgCount = 0

        // 获取accessToken
        try {
            val dataContext = DataContext(context)
            val accessToken = getToken(context)
            // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
            val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=getNewMsg"
            val args: MutableList<PostArgument> = ArrayList()
            args.add(PostArgument("pwd", dataContext.getSetting(Setting.KEYS.wx_request_code, "0000").string))
//                args.add(PostArgument("date", dataContext.getSetting(Setting.KEYS.wx_db_mark_date, System.currentTimeMillis()).string))
            postRequestByJson(url, args, HttpCallback { html ->
                try {
                    e(html)
                    val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                    e(resp_data);
                    if (_JsonUtils.isContainsKey(resp_data.toString(), "lt")) {
                        val count = _JsonUtils.getValueByKey(resp_data.toString(), "ct").toString().toInt()
                        if (count > 0) {
                            val date = _JsonUtils.getValueByKey(resp_data.toString(), "lt").toString().toLong()
                            callback.excute(1, DateTime(date).toOffset2() + "  +" + count)
                        } else {
                            callback.excute(0, "")
                        }
                    }

                } catch (e: Exception) {
                    callback.excute(-2, e.message)
                }
            })
        } catch (e: Exception) {
            callback.excute(-1, e.message)
        }
    }

    @JvmStatic
    fun getUser(context: Context, pwd: String?, callback: CloudCallback?) {
        newMsgCount = 0

        // 获取accessToken
        try {
            val accessToken = getToken(context)
            // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
            val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=getUser"
            val args: MutableList<PostArgument> = ArrayList()
            args.add(PostArgument("pwd", pwd))
            postRequestByJson(url, args, HttpCallback { html ->
                try {
                    val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                    val data = _JsonUtils.getValueByKey(resp_data.toString(), "data").toString()
                    val jsonArray = JSONArray(data)
                    if (jsonArray.length() > 0) {
                        val jsonObject = jsonArray.getString(0)
                        val name = _JsonUtils.getValueByKey(jsonObject, "name").toString()
                        callback?.excute(0, name)
                    } else {
                        callback?.excute(1, "访问码有误")
                    }
                } catch (e: Exception) {
                    callback?.excute(-2, e.message)
                }
            })
        } catch (e: Exception) {
            callback?.excute(-1, e.message)
        }
    }

    @JvmStatic
    fun addLocation(context: Context, pwd: String?, latitude: Double, longitude: Double, address: String?, callback: CloudCallback?) {
        newMsgCount = 0

        // 获取accessToken
        try {
            val accessToken = getToken(context)
            // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
            val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=addLocation"
            val args: MutableList<PostArgument> = ArrayList()
            args.add(PostArgument("pwd", pwd))
            args.add(PostArgument("date", System.currentTimeMillis()))
            args.add(PostArgument("latitude", latitude))
            args.add(PostArgument("longitude", longitude))
            args.add(PostArgument("address", address))
            postRequestByJson(url, args, HttpCallback { html ->
                try {
                    e(html)
                    callback?.excute(0, html)
                } catch (e: Exception) {
                    callback?.excute(-2, e.message)
                }
            })
        } catch (e: Exception) {
            callback?.excute(-1, e.message)
        }
    }

    fun getLocations(context: Context, callback: CloudCallback?) {
        newMsgCount = 0

        // 获取accessToken
        try {
            val accessToken = getToken(context)

            // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
            val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=getLocations"
            val args: List<PostArgument> = ArrayList()
            postRequestByJson(url, args, HttpCallback { html ->
                try {
                    e(html)
                    val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                    val data = _JsonUtils.getValueByKey(resp_data.toString(), "data").toString()
                    val jsonArray = JSONArray(data)
                    for (i in jsonArray.length() - 1 downTo 0) {
                        val jsonObject = jsonArray.getString(i)
                        val address = _JsonUtils.getValueByKey(jsonObject, "address").toString()
                        val dateTime = _JsonUtils.getValueByKey(jsonObject, "dateTime").toString()
                        e(dateTime)
                    }
                } catch (e: Exception) {
                    callback?.excute(-2, e.message)
                }
            })
        } catch (e: Exception) {
            callback?.excute(-1, e.message)
        }
    }

    fun updatePositions(context: Context, pwd: String?, positoinsJson: String?, callback: CloudCallback?) {
        newMsgCount = 0

        // 获取accessToken
        try {
            val accessToken = getToken(context)

            // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
            val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=updatePositions"
            val args: MutableList<PostArgument> = ArrayList()
            args.add(PostArgument("pwd", pwd))
            args.add(PostArgument("positions", positoinsJson))
            postRequestByJson(url, args, HttpCallback { html ->
                try {
                    e(html)
                    callback?.excute(0, html)
                } catch (e: Exception) {
                    callback?.excute(-2, e.message)
                }
            })
        } catch (e: Exception) {
            callback?.excute(-1, e.message)
        }
    }

    fun getPositions(context: Context, pwd: String, callback: CloudCallback) {
        val result: MutableList<Position> = ArrayList()
        newMsgCount = 0

        // 获取accessToken
        val accessToken = getToken(context)
        // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
        val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=yipinshangdu-4wk7z&name=getPositions"
        val args: MutableList<PostArgument> = ArrayList()
        args.add(PostArgument("pwd", pwd))
        postRequestByJson(url, args, object : HttpCallback {
            override fun excute(html: String) {
                try {
                    val resp_data: Any? = _JsonUtils.getValueByKey(html, "resp_data")
                    e("get positions : $resp_data")
                    val data = _JsonUtils.getValueByKey(resp_data.toString(), "data")
                    val jsonArray = JSONArray(data)
                    for (i in 0 until jsonArray.length()) {

                        val jsonObject = jsonArray.getString(i)

                        val position = Position()
                        position.name = _JsonUtils.getValueByKey(jsonObject, "name")
                        position.code = _JsonUtils.getValueByKey(jsonObject, "code")
                        position.amount = _JsonUtils.getValueByKey(jsonObject, "amount").toInt()
                        position.cost = _JsonUtils.getValueByKey(jsonObject, "cost").toDouble()
                        position.exchange = if (position.code[0] == '6') "sh" else "sz"
                        if (position.amount > 0) result.add(position)
                    }
                    callback.excute(0, result)
                } catch (e: Exception) {
                    callback.excute(-2, e.message ?: "")
                }
            }
        })
    }


    private fun e(data: Any) {
        Log.e("wangsc", data.toString())
    }
}