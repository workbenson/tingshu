package com.github.eprendre.tingshu.utils

import android.content.Context
import android.content.SharedPreferences
import com.github.eprendre.tingshu.App

object Prefs {
    private const val PREFS_FILENAME = "tingshu.prefs"
    private lateinit var prefs: SharedPreferences

    fun init() {
        prefs = App.appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    var speed: Float
        get() = prefs.getFloat("speed", 1f)
        set(value) = prefs.edit().putFloat("speed", value).apply()

    var currentBookUrl: String?
        get() = prefs.getString("current_book", "")
        set(value) = prefs.edit().putString("current_book", value).apply()

    var currentEpisodeUrl: String?
        get() = prefs.getString("current_episode", "")
        set(value) = prefs.edit().putString("current_episode", value).apply()

    var currentEpisodePosition: Long
        get() = prefs.getLong("position", 0L)
        set(value) = prefs.edit().putLong("position", value).apply()

    var artist: String?
        get() = prefs.getString("artist", "")
        set(value) = prefs.edit().putString("artist", value).apply()

    var currentBookName: String?
        get() = prefs.getString("current_book_name", "")
        set(value) = prefs.edit().putString("current_book_name", value).apply()

//    var currentName: String?
//        get() = prefs.getString("current_name", "")
//        set(value) = prefs.edit().putString("current_name", value).apply()

//    var currentCover: String?
//        get() = prefs.getString("current_cover", "")
//        set(value) = prefs.edit().putString("current_cover", value).apply()
}