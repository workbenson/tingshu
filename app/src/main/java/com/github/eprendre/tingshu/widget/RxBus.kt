package com.github.eprendre.tingshu.widget

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
  class ParsingPlayUrlEvent
}