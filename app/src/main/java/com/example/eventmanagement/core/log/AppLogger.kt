package com.example.eventmanagement.core.log

import android.util.Log
import com.example.eventmanagement.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

interface AppLogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

@Singleton
class AndroidAppLogger @Inject constructor() : AppLogger {
    override fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) Log.e(tag, message, throwable)
            else Log.e(tag, message)
        }
    }
}
