package com.github.eprendre.tingshu

import org.junit.Test
import java.net.URI

class MyTest {
    @Test
    fun test() {
        val url = "http://mp3-2e.ting89.com:9090/2017/32/全职法师/001.mp3"
        println(URI(url).toASCIIString())
    }
}
