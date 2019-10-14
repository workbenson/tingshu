package com.github.eprendre.tingshu.widget

import com.github.eprendre.tingshu.utils.Book
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor

object RxBus {
    private val bus = PublishProcessor.create<Any>().toSerialized()

    fun post(any: Any) {
        bus.onNext(any)
    }

    fun toFlowable(): Flowable<Any> = bus

    fun <T> toFlowable(tClass: Class<T>): Flowable<T> = bus.ofType(tClass)

    fun hasSubscribers(): Boolean = bus.hasSubscribers()
}

class RxEvent {
    data class TimerEvent(val msg: String)
    /**
     * status: 0: 开始 1: 结束 2: 失败 3: 播放
     */
    data class ParsingPlayUrlEvent(val status: Int)

    /**
     * status: 0: 开始 1: 成功 2: 失败 3: 进度
     */
    data class CacheEvent(val episodeUrl: String, val audioUrl: String, val status: Int) {
        var progress: Long = 0
        var msg: String = ""
    }
    data class StorePositionEvent(val book: Book)
}