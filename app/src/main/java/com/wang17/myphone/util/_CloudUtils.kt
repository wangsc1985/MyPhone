package com.wang17.myphone.util

import android.content.Context
import com.wang17.myphone.callback.CloudCallback
import com.wang17.myphone.callback.HttpCallback
import com.wang17.myphone.e
import com.wang17.myphone.model.DateTime
import com.wang17.myphone.model.PostArgument
import com.wang17.myphone.database.BuddhaRecord
import com.wang17.myphone.database.DataContext
import com.wang17.myphone.database.Position
import com.wang17.myphone.database.Setting
import com.wang17.myphone.util._OkHttpUtil.getRequest
import com.wang17.myphone.util._OkHttpUtil.postRequestByJson
import com.wang17.myphone.util._OkHttpUtil.postRequestByJsonStr
import org.json.JSONArray
import java.util.*
import java.util.concurrent.CountDownLatch

object _CloudUtils {
    private var newMsgCount = 0

    private val env = "yipinshangdu-4wk7z"
    private val appid = "wxbdf065bdeba96196"
    private val secret = "d2834f10c0d81728e73a4fe4012c0a5d"
    private val phone = "18509513143"

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
//                e("本地token已过期，微软网站获取新的token。")
                return loadNewTokenFromHttp((context))
            } else {
                /**
                 * token仍有效
                 */
//                e(dc.getSetting("token").string)
//                e("有效期：${DateTime(exprires).toLongDateTimeString()}")
                return dc.getSetting("token").string
            }
        } else {
//            e("本地不存在token信息，微软网站获取新的token。")
            return loadNewTokenFromHttp(context)
        }
    }

    fun loadNewTokenFromHttp(context: Context): String {
        var token = ""
        // https://sahacloudmanager.azurewebsites.net/home/token/wxbdf065bdeba96196/d2834f10c0d81728e73a4fe4012c0a5d
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
//                    e("从微软获取到token：$token, 有效期：${DateTime(exprires).toLongDateTimeString()} 用时：${b - a}")
                }
            } catch (e: Exception) {
                e(e.message!!)
            } finally {
                latch.countDown()
            }
        })
        latch.await()
        return token
    }

    /**
     * string phone, long startTime ,long duration , int count, string summary, int type
     */
    @JvmStatic
    fun addBuddha(context: Context, buddha:BuddhaRecord, callback: CloudCallback?) {
        Thread{
            try {
                val accessToken = getToken(context)
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=addBuddha"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
                args.add(PostArgument("startTime", buddha.startTime.timeInMillis))
                args.add(PostArgument("duration", buddha.duration))
                args.add(PostArgument("count", buddha.count))
                args.add(PostArgument("summary", buddha.summary))
                args.add(PostArgument("type", buddha.type))
                postRequestByJson(url, args, HttpCallback { html ->
                    try {
                        e(html)
                        val errcode = _JsonUtils.getValueByKey(html, "errcode")
                        val errmsg = _JsonUtils.getValueByKey(html, "errmsg")
                        if(errcode=="0"){
                            val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                            val code = _JsonUtils.getValueByKey(resp_data.toString(), "code").toInt()
                            val msg = _JsonUtils.getValueByKey(resp_data.toString(), "msg")
                            when (code) {
                                0 -> callback?.excute(code, msg)
                                else-> callback?.excute(code, msg)
                            }
                        }else{
                            callback?.excute(-2, errmsg)
                        }
                    } catch (e: Exception) {
                        callback?.excute(-200, e.message)
                    }
                })
            } catch (e: Exception) {
                callback?.excute(-100, e.message)
            }

        }.start()
    }

    @JvmStatic
    fun editBuddha(context: Context, buddha:BuddhaRecord, callback: CloudCallback?) {
        Thread{
            try {
                val accessToken = getToken(context)
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=editBuddha"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
                args.add(PostArgument("startTime", buddha.startTime.timeInMillis))
                args.add(PostArgument("duration", buddha.duration))
                args.add(PostArgument("count", buddha.count))
                args.add(PostArgument("summary", buddha.summary))
                args.add(PostArgument("type", buddha.type))
                postRequestByJson(url, args, HttpCallback { html ->
                    try {
                        e(html)
                        val errcode = _JsonUtils.getValueByKey(html, "errcode")
                        val errmsg = _JsonUtils.getValueByKey(html, "errmsg")
                        if(errcode=="0"){
                            val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                            val code = _JsonUtils.getValueByKey(resp_data.toString(), "code").toInt()
                            val msg = _JsonUtils.getValueByKey(resp_data.toString(), "msg")
                            when (code) {
                                0 -> callback?.excute(code, msg)
                                else-> callback?.excute(code, msg)
                            }
                        }else{
                            callback?.excute(-2, errmsg)
                        }
                    } catch (e: Exception) {
                        callback?.excute(-200, e.message)
                    }
                })
            } catch (e: Exception) {
                callback?.excute(-100, e.message)
            }

        }.start()
    }

    fun delBuddha(context: Context, buddha:BuddhaRecord, callback: CloudCallback?) {
        Thread{
            try {
                val accessToken = getToken(context)
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=delBuddha"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
                args.add(PostArgument("startTime", buddha.startTime.timeInMillis))
                postRequestByJson(url, args, HttpCallback { html ->
                    try {
                        e(html)

                        val errcode = _JsonUtils.getValueByKey(html, "errcode")
                        val errmsg = _JsonUtils.getValueByKey(html, "errmsg")
                        if(errcode=="0"){
                            val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                            val code = _JsonUtils.getValueByKey(resp_data.toString(), "code").toInt()
                            val msg = _JsonUtils.getValueByKey(resp_data.toString(), "msg")
                            when (code) {
                                0 -> callback?.excute(0, msg)
                                else-> callback?.excute(-1, msg)
                            }
                        }else{
                            callback?.excute(-2, errmsg)
                        }
                    } catch (e: Exception) {
                        callback?.excute(-1, e.message)
                    }
                })
            } catch (e: Exception) {
                callback?.excute(-1, e.message)
            }

        }.start()
    }

    @JvmStatic
    fun addBuddhaList(context: Context, buddhaList:MutableList<BuddhaRecord>, callback: CloudCallback?) {
        Thread{
            try {
                val accessToken = getToken(context)

                var json = StringBuilder()
                json.append("{\"phone\":\"18509513143\",\"data\":")
                json.append("[")
                for (i in buddhaList.indices)
                {
                    json.append("{")
                    json.append("\"startTime\":\"${ buddhaList[i].startTime.timeInMillis}\"")
                    json.append(",\"duration\":\"${buddhaList[i].duration}\"")
                    json.append(",\"count\":\"${buddhaList[i].count}\"")
                    json.append(",\"summary\":\"${buddhaList[i].summary}\"")
                    json.append(",\"type\":\"${buddhaList[i].type}\"")
                    json.append("}")
                    if (i < buddhaList.size - 1)
                        json.append(",")
                }
                json.append("]")
                json.append("}")

                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=addBuddhaRange"
                postRequestByJsonStr(url, json.toString(), HttpCallback { html ->
                    try {
                        e("add buddha list html: "+html)
                        val errcode = _JsonUtils.getValueByKey(html, "errcode")
                        val errmsg = _JsonUtils.getValueByKey(html, "errmsg")
                        if(errcode=="0"){
                            val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                            val code = _JsonUtils.getValueByKey(resp_data.toString(), "code").toInt()
                            val msg = _JsonUtils.getValueByKey(resp_data.toString(), "msg")
                            when (code) {
                                0 -> callback?.excute(0, msg)
                                else-> callback?.excute(-1, msg)
                            }
                        }else{
                            callback?.excute(-2, errmsg)
                        }
                    } catch (e: Exception) {
                        callback?.excute(-3, e.message)
                    }
                })
            } catch (e: Exception) {
                callback?.excute(-4, e.message)
            }

        }.start()
    }

    @JvmStatic
    fun addIntegrateBuddhas(context: Context, lastBuddha:BuddhaRecord?, buddhaList:MutableList<BuddhaRecord>, callback: CloudCallback?) {
        Thread{
            try {
                val accessToken = getToken(context)

                var json = StringBuilder()
                json.append("{\"phone\":\"18509513143\",")
                if(lastBuddha!=null){
                    json.append("\"lastdata\":{\"startTime\":\"${lastBuddha.startTime.timeInMillis}\",\"duration\":\"${lastBuddha.duration}\",\"count\":\"${lastBuddha.count}\",\"summary\":\"${lastBuddha.summary}\",\"type\":\"${lastBuddha.type}\"},")
                }
                json.append("\"newdata\":[");
                for (i in buddhaList.indices)
                {
                    json.append("{");
                    json.append("\"startTime\":\"${ buddhaList[i].startTime.timeInMillis}\"");
                    json.append(",\"duration\":\"${buddhaList[i].duration}\"");
                    json.append(",\"count\":\"${buddhaList[i].count}\"");
                    json.append(",\"summary\":\"${buddhaList[i].summary}\"");
                    json.append(",\"type\":\"${buddhaList[i].type}\"");
                    json.append("}");
                    if (i < buddhaList.size - 1)
                        json.append(",");
                }
                json.append("]");
                json.append("}");

                e("addIntegrateBuddhas json : "+json.toString())
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=addIntegrateBuddhaRange"
                postRequestByJsonStr(url, json.toString(), HttpCallback { html ->
                    try {
                        e("addIntegrateBuddhas html: "+html)
                        val errcode = _JsonUtils.getValueByKey(html, "errcode")
                        val errmsg = _JsonUtils.getValueByKey(html, "errmsg")
                        if(errcode=="0"){
                            val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                            val code = _JsonUtils.getValueByKey(resp_data.toString(), "code").toInt()
                            val msg = _JsonUtils.getValueByKey(resp_data.toString(), "msg")
                            when (code) {
                                0 -> callback?.excute(0, msg)
                                else-> callback?.excute(-1, msg)
                            }
                        }else{
                            callback?.excute(-1, errmsg)
                        }
                    } catch (e: Exception) {
                        callback?.excute(-2, e.message)
                    }
                })
            } catch (e: Exception) {
                callback?.excute(-1, e.message)
            }

        }.start()
    }

    /**
     * string phone, long startTime ,long duration , int count, string summary, int type
     */
    @JvmStatic
    fun loadBuddha(context: Context, startTime: DateTime, callback: CloudCallback?) {
        Thread {
            try {
                val accessToken = getToken(context)
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=loadBuddha"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
                args.add(PostArgument("startTime", startTime.timeInMillis))
                postRequestByJson(url, args, HttpCallback { html ->
                    try {
                        val resp_data = _JsonUtils.getValueByKey(html, "resp_data")
                        callback?.excute(0, resp_data)
                    } catch (e: Exception) {
                        callback?.excute(-1, e.message)
                    }
                })
            } catch (e: Exception) {
                callback?.excute(-1, e.message)
            }
        }.start()
    }

    @JvmStatic
    fun saveSetting(context: Context, pwd: String?, name: String?, value: Any, callback: CloudCallback?) {
        Thread {
            try {
                val accessToken = getToken(context)
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=saveSetting"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
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
        }.start()
    }

    @JvmStatic
    fun getSetting(context: Context, pwd: String, name: String, callback: CloudCallback) {
        Thread {
            // 获取accessToken
            try {
                val accessToken = getToken(context)
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=getSetting"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
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
        }.start()
    }

    @JvmStatic
    fun getSettingList(context: Context, pwd: String, callback: CloudCallback) {
        Thread {
            // 获取accessToken
            try {
                val accessToken = getToken(context)
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=getSettingList"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
                postRequestByJson(url, args, object : HttpCallback {
                    override fun excute(html: String) {
                        try {
                            val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                            val data = _JsonUtils.getValueByKey(resp_data.toString(), "data")
                            val jsonArray = JSONArray(data)
                            var result = StringBuffer()
                            for(i in 0..jsonArray.length()-1){
                                val jsonObject = jsonArray.getString(i)
                                val name = _JsonUtils.getValueByKey(jsonObject, "name")
                                val value = _JsonUtils.getValueByKey(jsonObject, "value")
                                result.append("name : ${name} , value : ${value}\n")
                            }
                            callback.excute(0,result.toString())
                        } catch (e: Exception) {
                            callback.excute(-2, e.message!!)
                        }
                    }
                })
            } catch (e: Exception) {
                callback.excute(-1, e.message!!)
            }
        }.start()
    }

    @JvmStatic
    fun getNewMsg(context: Context, callback: CloudCallback) {
        Thread {
            // 获取accessToken
            try {
                val dataContext = DataContext(context)
                val accessToken = getToken(context)
//            e(accessToken)
//            "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=yipinshangdu-4wk7z&name=getNewMsg"
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=getNewMsg"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
                postRequestByJson(url, args, HttpCallback { html ->
                    try {
                        e(html)
                        val errcode = _JsonUtils.getValueByKey(html, "errcode")
                        val errmsg = _JsonUtils.getValueByKey(html, "errmsg")

                        when (errcode) {
                            "0" -> {
                                val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")

                                if (_JsonUtils.isContainsKey(resp_data.toString(), "lt")) {
                                    val count = _JsonUtils.getValueByKey(resp_data.toString(), "ct").toString().toInt()
                                    if (count > 0) {
                                        val date = _JsonUtils.getValueByKey(resp_data.toString(), "lt").toString().toLong()
                                        callback.excute(1, DateTime(date).toOffset2() + "  +" + count)
                                    } else {
                                        callback.excute(0, "")
                                    }
                                }
                            }
                            else -> {
                                callback.excute(-2, errmsg.toString())
                            }
                        }

                    } catch (e: Exception) {
                        callback.excute(-2, e.message)
                    }
                })
            } catch (e: Exception) {
                callback.excute(-1, e.message)
            }
        }.start()
    }

    @JvmStatic
    fun getUser(context: Context, pwd: String?, callback: CloudCallback?) {
        Thread {
            // 获取accessToken
            try {
                val accessToken = getToken(context)
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=getUser"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
                postRequestByJson(url, args, HttpCallback { html ->
                    try {
                        val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                        val data = _JsonUtils.getValueByKey(resp_data.toString(), "data")
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
        }.start()
    }

    @JvmStatic
    fun addLocation(context: Context, pwd: String,speed:Float, latitude: Double, longitude: Double, address: String?, callback: CloudCallback?) {
        Thread {
            // 获取accessToken
            try {
                val accessToken = getToken(context)
                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=addLocation"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
                args.add(PostArgument("date", System.currentTimeMillis()))
                args.add(PostArgument("latitude", latitude))
                args.add(PostArgument("longitude", longitude))
                args.add(PostArgument("speed", speed))
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
        }.start()
    }

    fun getLocations(context: Context, callback: CloudCallback?) {
        Thread {
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
        }.start()
    }

    fun updatePositions(context: Context, pwd: String?, positoinsJson: String?, callback: CloudCallback?) {
        Thread {
            // 获取accessToken
            try {
                val accessToken = getToken(context)

                // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
                val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=updatePositions"
                val args: MutableList<PostArgument> = ArrayList()
                args.add(PostArgument("phone", phone))
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
        }.start()
    }

    fun getPositions(context: Context, pwd: String, callback: CloudCallback) {
        Thread {
            val result: MutableList<Position> = ArrayList()
            // 获取accessToken
            val accessToken = getToken(context)
            // 通过accessToken，env，云函数名，args 在微信小程序云端获取数据
            val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=yipinshangdu-4wk7z&name=getPositions"
            val args: MutableList<PostArgument> = ArrayList()
            args.add(PostArgument("phone", phone))
            postRequestByJson(url, args, object : HttpCallback {
                override fun excute(html: String) {
                    try {
                        val resp_data: Any? = _JsonUtils.getValueByKey(html, "resp_data")
                        e("get positions : $resp_data")
                        val data = _JsonUtils.getValueByKey(resp_data.toString(), "data")
                        val jsonArray = JSONArray(data)
                        for (i in 0 until jsonArray.length()) {

                            val jsonObject = jsonArray.getString(i)

                            val position = Position(_JsonUtils.getValueByKey(jsonObject, "code"),
                                _JsonUtils.getValueByKey(jsonObject, "name"),
                                _JsonUtils.getValueByKey(jsonObject, "cost").toBigDecimal(),
                            0,
                                _JsonUtils.getValueByKey(jsonObject, "amount").toInt(),
                                _JsonUtils.getValueByKey(jsonObject, "exchange"),0.toBigDecimal())
                            if (position.amount > 0) result.add(position)
                        }
                        callback.excute(0, result)
                    } catch (e: Exception) {
                        callback.excute(-2, e.message ?: "")
                    }
                }
            })
        }.start()
    }

}