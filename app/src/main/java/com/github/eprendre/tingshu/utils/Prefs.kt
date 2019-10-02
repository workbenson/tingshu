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
     * 当前简介
     */
    var currentIntro: String?
        get() = prefs.getString("current_intro", "")
        set(value) = prefs.edit().putString("current_intro", value).apply()

    var currentBook: Book?
        get() {
            val json = prefs.getString("current_audio_book", "")
            return if (json.isNullOrBlank()) {
                null
            } else {
                Gson().fromJson(json, Book::class.java)
            }
        }
        set(value) {
            prefs.edit().putString("current_audio_book", Gson().toJson(value)).apply()
        }

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

    var historyList: ArrayList<Book>
        get() {
            val json = prefs.getString("history_list", "[]")
            return Gson().fromJson(json, object : TypeToken<ArrayList<Book>>() {}.type)
        }
        set(value) {
            prefs.edit().putString("history_list", Gson().toJson(value.distinctBy { it.bookUrl }.take(20))).apply()
        }

    fun addToHistory(book: Book) {
        val list = historyList
        list.add(0, book)
        historyList = ArrayList(list.distinctBy { it.bookUrl }.take(20))
    }

    fun storeHistoryPosition(currentBook: Book) {
        val list = historyList
        val book = list.firstOrNull { it.bookUrl == currentBook.bookUrl }
        if (book != null) {
            book.currentEpisodePosition = currentBook.currentEpisodePosition
            book.currentEpisodeName = currentBook.currentEpisodeName
            book.currentEpisodeUrl = currentBook.currentEpisodeUrl
            historyList = list
        }
    }

    fun storeSkipPosition(currentBook: Book) {
        val list = historyList
        val book = list.firstOrNull { it.bookUrl == currentBook.bookUrl }
        if (book != null) {
            book.skipBeginning = currentBook.skipBeginning
            book.skipEnd = currentBook.skipEnd
            historyList = list
        }
    }

    var sortType: Int
        get() = prefs.getInt("sort_type", 1)
        set(value) = prefs.edit().putInt("sort_type", value).apply()

    var ignoreFocus: Boolean
        get() = prefs.getBoolean("ignore_focus", false)
        set(value) = prefs.edit().putBoolean("ignore_focus", value).apply()

    var audioOnError: Boolean
        get() = prefs.getBoolean("audio_on_error", false)
        set(value) = prefs.edit().putBoolean("audio_on_error", value).apply()
}