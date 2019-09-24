package com.github.eprendre.tingshu.utils

import android.content.Context
import android.content.SharedPreferences
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

    /**
     * 当前简介
     */
    var currentIntro: String?
        get() = prefs.getString("current_intro", "")
        set(value) = prefs.edit().putString("current_intro", value).apply()

    var source: String
        get() = prefs.getString("current_source", TingShuSourceHandler.SOURCE_URL_HUANTINGWANG)!!
        set(value) = prefs.edit().putString("current_source", value).apply()

    /**
     * 记录上一次检查更新的时候，避免频繁调用
     */
    var lastUpdate: Long
        get() = prefs.getLong("last_update", 0)
        set(value) = prefs.edit().putLong("last_update", value).apply()

    var showAlbumInLockScreen: Boolean
        get() = prefs.getBoolean("show_album_art", false)
        set(value) = prefs.edit().putBoolean("show_album_art", value).apply()

    var isFirst: Boolean
        get() = prefs.getBoolean("is_first", true)
        set(value) = prefs.edit().putBoolean("is_first", value).apply()

    var playList: List<Episode>
        get() {
            val json = prefs.getString("play_list", "[]")
            return Gson().fromJson(json, object : TypeToken<List<Episode>>() {}.type)
        }
        set(value) {
            prefs.edit().putString("play_list", Gson().toJson(value)).apply()
        }

    var sortType: Int
        get() = prefs.getInt("sort_type", 1)
        set(value) = prefs.edit().putInt("sort_type", value).apply()
}