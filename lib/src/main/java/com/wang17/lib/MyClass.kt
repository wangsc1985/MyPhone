package com.wang17.lib

import java.lang.Exception
import java.util.*
import java.util.regex.Pattern

class MyClass {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                Thread {
                    throw Exception("aaaaa")
                }.start()
            } catch (e: Exception) {
                println(e.message)
            }

        }
    }
}