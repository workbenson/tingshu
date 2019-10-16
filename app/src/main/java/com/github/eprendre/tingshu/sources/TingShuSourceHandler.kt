package com.github.eprendre.tingshu.sources

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.impl.*
import com.github.eprendre.tingshu.utils.*
import com.github.eprendre.tingshu.widget.GlideApp
import io.reactivex.Completable
import io.reactivex.Single

/**
 * 里面有两种方法
 * 一种直接根据保存好的配置请求相应的站点
 * 另一种需要动态的根据传入url去判断用对应的站点解析
 */
object TingShuSourceHandler {
    const val SOURCE_URL_56 = "http://m.ting56.com"
    const val SOURCE_URL_520 = "http://m.520tingshu.com"
    const val SOURCE_URL_TINGSHUGE = "http://www.tingshuge.com"
    const val SOURCE_URL_HUANTINGWANG = "http://m.ting89.com"
    const val SOURCE_URL_JINGTINGWANG = "http://m.audio699.com"
    const val SOURCE_URL_TINGSHUBAO = "https://www.tingshubao.com"
    const val SOURCE_URL_TINGCHINA = "http://www.tingchina.com"
    const val SOURCE_URL_TIANTIANPINGSHU = "https://www.pingshu365.com"
    const val SOURCE_URL_22TINGSHU = "https://m.ting22.com"
    const val SOURCE_URL_QIANKUN = "http://m.qktsw.com"
    const val SOURCE_URL_LIANTING = "https://ting55.com"
    const val SOURCE_URL_WOTINGPINGSHU = "https://m.5tps.com"
    const val SOURCE_URL_SHENGBO = "http://fm.shengbo.org"

    private lateinit var tingShu: TingShu
    val sourceList by lazy {
        val keyArray = App.appContext.resources.getStringArray(R.array.source_values)
        val valueArray = listOf(//这里面的顺序要和 R.array.source_values 里面的对应上
            M56TingShu,
            M520TingShu,
//            TingShuGe,
            HuanTingWang,
            JingTingWang,
            TingShuBao,
            TingChina,
            TianTianPingShu,
            M22TingShu,
            QianKun,
            LianTingWang,
            WoTingPingShu,
            ShengBoFM
        )
        keyArray.zip(valueArray)
    }

    init {
        setupConfig()
    }

    fun setupConfig() {
        tingShu = findSource(Prefs.source)
    }

    //以下直接从已设置好的站点去获取数据
    fun search(keywords: String, page: Int): Single<Pair<List<Book>, Int>> {
        return tingShu.search(keywords, page)
    }

    fun getCategoryMenus(): List<CategoryMenu> {
        return tingShu.getCategoryMenus()
    }

    //以下的方法需要根据传入的url判断用哪个站点解析
    fun getCategoryDetail(url: String): Single<Category> {
        return findSource(url).getCategoryDetail(url)
    }

    fun getAudioUrlExtractor(url: String): AudioUrlExtractor {
        return findSource(url).getAudioUrlExtractor()
    }

    /**
     * 根据书本链接加载章节信息和简介
     */
    fun playFromBookUrl(bookUrl: String): Completable {
        return findSource(bookUrl).playFromBookUrl(bookUrl)
    }

    /**
     * 判断请求时的 url， 选择对应的解析
     */
    private fun findSource(url: String): TingShu {
        return sourceList
            .first { url.startsWith(it.first) }
            .second
    }

    /**
     * 播放之前先调用这个方法确保当前通知所需要的封面已缓存
     */
    fun downloadCoverForNotification() {
        //下载封面
        val glideOptions = RequestOptions()
            .error(R.drawable.default_art)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        try {
            App.coverBitmap = GlideApp.with(App.appContext)
                .applyDefaultRequestOptions(glideOptions)
                .asBitmap()
                .load(Prefs.currentBook!!.coverUrl)
                .submit(144, 144)
                .get()
        } catch (e: Exception) {
            e.printStackTrace()
            App.coverBitmap = decodeCover()
        }
    }

    private fun decodeCover(): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(App.appContext.resources, R.drawable.default_art, options)
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        val picSize = Math.max(height, width)
        var targetSize = 256
        if (picSize > targetSize) {
            val halfSize = picSize / 2
            while (halfSize / inSampleSize > targetSize) {
                inSampleSize *= 2
            }
        } else {
            targetSize = picSize
        }

        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeResource(App.appContext.resources, R.drawable.default_art, options)
        val matrix = Matrix()
        val ratio = Math.min(
            targetSize.toFloat() / bitmap.width,
            targetSize.toFloat() / bitmap.height
        )
        matrix.postScale(ratio, ratio)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}