package com.wang17.myphone.util

import com.wang17.myphone.model.DateTime
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.jvm.JvmOverloads

object _LogUtils {

    @JvmStatic
    fun log2file(item: String) {
        try {
            val logFile = File(_Session.ROOT_DIR, "err.log")
            val writer = BufferedWriter(FileWriter(logFile, true))
            writer.write(DateTime().toLongDateTimeString())
            writer.newLine()
            writer.write(item)
            writer.newLine()
            writer.write(item)
            writer.newLine()
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun log2file(item: String, message: String="") {
        try {
            val logFile = File(_Session.ROOT_DIR, "run.log")
            val writer = BufferedWriter(FileWriter(logFile, true))
            writer.write(DateTime().toLongDateTimeString())
            writer.newLine()
            writer.write(item)
            writer.newLine()
            writer.write(message)
            writer.newLine()
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}