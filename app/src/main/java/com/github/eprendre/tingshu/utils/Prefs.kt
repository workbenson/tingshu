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

    /**
     * 记录当前播放书籍的地址
     */
    var currentBookUrl: String?
        get() = prefs.getString("current_book", "")
        set(value) = prefs.edit().putString("current_book", value).apply()

    /**
     * 记录当前播放集数
     */
    var currentEpisodeUrl: String?
        get() = prefs.getString("current_episode", "")
        set(value) = prefs.edit().putString("current_episode", value).apply()

    /**
     * 记录播放时间
     */
    var currentEpisodePosition: Long
        get() = prefs.getLong("position", 0L)
        set(value) = prefs.edit().putLong("position", value).apply()

    /**
     * 当前播放书籍的作者
     */
    var author: String?
        get() = prefs.getString("author", "")
        set(value) = prefs.edit().putString("author", value).apply()

    /**
     * 记录当前播放艺术家
     */
    var artist: String?
        get() = prefs.getString("artist", "")
        set(value) = prefs.edit().putString("artist", value).apply()

    /**
     * 记录当前播放书名
     */
    var currentBookName: String?
        get() = prefs.getString("current_book_name", "")
        set(value) = prefs.edit().putString("current_book_name", value).apply()

    /**
     * 当前章节名
     */
    var currentEpisodeName: String?
        get() = prefs.getString("current_episode_name", "")
        set(value) = prefs.edit().putString("current_episode_name", value).apply()

    /**
     * 当前播放封面
     */
    var currentCover: String?
        get() = prefs.getString("current_cover", "")
        set(value) = prefs.edit().putString("current_cover", value).apply()

    var source: String
        get() = prefs.getString("current_source", SOURCE_56TINGSHU)
        set(value) = prefs.edit().putString("current_source", value).apply()

    const val SOURCE_56TINGSHU = "56tingshu"
    const val SOURCE_520TINGSHU = "520tingshu"
}