package com.github.eprendre.tingshu.sources

import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Category
import com.github.eprendre.tingshu.utils.CategoryMenu
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import io.reactivex.Completable
import io.reactivex.Single

interface TingShu {
    /**
     * 返回第二个参数为最大页数
     */
    fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>>

    fun playFromBookUrl(bookUrl: String): Completable

    fun getAudioUrlExtractor(): AudioUrlExtractor

    fun getCategoryMenus(): List<CategoryMenu>

    fun getCategoryDetail(url: String): Single<Category>
}

interface AudioUrlExtractor {
    /**
     * 根据传入的章节地址提取实际的播放地址, 并请求播放
     */
    fun extract(url: String, autoPlay: Boolean)
}