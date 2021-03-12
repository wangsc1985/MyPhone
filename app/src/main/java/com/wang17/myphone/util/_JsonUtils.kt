package com.wang17.myphone.util

import com.wang17.myphone.model.Lottery
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object _JsonUtils {
    @Throws(JSONException::class)
    fun getJSONObjectByKey(jsonArray: JSONArray, key: Any, value: Any): JSONObject? {
        var res: JSONObject? = null
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString(key.toString()) == value) {
                res = obj
            }
        }
        return res
    }

    @Throws(JSONException::class)
    fun getValueByKey(json: Any, key: Any): String {
        val res = JSONObject(json.toString())
        return res[key.toString()].toString()
    }

    @Throws(JSONException::class)
    fun isContainsKey(json: Any, key: Any): Boolean {
        val res = JSONObject(json.toString())





        return !res.isNull(key.toString())
    }
}