package com.github.eprendre.tingshu

import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URLEncoder

class TingChinaUnitTest {

    @Test
    fun audioUrl() {
        val str = """
            <script language="javascript">
//记录播放模式
var the_datea = new Date("December 31, 2020");var expiresDatea = the_datea.toGMTString();
document.cookie = "t_play_mode=" + escape("media1") + "; expires=" + expiresDatea + "; path=/";
var tmpurl=window.parent.playurl_flash+"&mode=flash";
//初始化参数
var tmpid=26164;
var tmpnum=0;
var url_status=0;
var i=1;
var url= new Array();
url[1]= "http://t44.tingchina.com";url[2]= "http://t33.tingchina.com";url[3]= "/yousheng/玄幻奇幻/九星天辰诀/01.mp3?key=e5725bcd4f01f47f5389ed523514e362_617410001";
var xianlu= new Array();
xianlu[1]= "网通A <a href=\"javascript:\" onclick=\"chang_xl(2);\">切换电信<\/a>";
xianlu[2]= "电信A <a href=\"javascript:\" onclick=\"chang_xl(1);\">切换网通<\/a>";

//启动方式选择
var tingchina_xianlu=getCookie("tingchina_xianlu");
if( tingchina_xianlu !="1" && tingchina_xianlu != "2") {run();}
else{chang_xl(tingchina_xianlu);}

//设置层高度
window.parent.document.getElementById('playmedia').style.height ="100px";

//函数库
function run(){for(i=1;i<3;i++) document.write("<img src=" + url[i]+ " width=1 heigth=1 onerror=auto("+i+")>");}
function auto(auto_i){if (url_status==0){url_status=1;czplayer.URL=url[auto_i]+url[3];document.getElementById("xdname").innerHTML=xianlu[auto_i];}}
function chang_xl(ti){document.getElementById("xdstr").innerHTML="当前服务器：";document.getElementById("xdname").innerHTML=xianlu[ti];czplayer.URL=url[ti]+url[3];setTimeout("czplayer.controls.play()", 10);SetCookie("tingchina_xianlu",ti);}
function SetCookie(name,value){var Days = 300;var exp  = new Date();exp.setTime(exp.getTime() + Days*24*60*60*1000);document.cookie = name + "="+ escape (value) + ";expires=" + exp.toGMTString();}
function getCookie(name){var arr = document.cookie.match(new RegExp("(^| )"+name+"=([^;]*)(;|${'$'})"));if(arr != null) return unescape(arr[2]); return null;}
//下一级处理函数
//nextpl='/yousheng/26164/play_26164_1.htm';
//if (nextpl!='') setTimeout('czplayer_go()',nextpl);
//function czplayer_go(){czgo=setInterval('czplayer_end()',700);}

</script>
        """.trimIndent()

        val url2 = Regex("url\\[2]= \"(.*?)\";").find(str)?.groupValues?.get(1)
        val url3 = Regex("url\\[3]= \"(.*?)\";").find(str)?.groupValues?.get(1)
        println("$url2$url3")
    }

    @Test
    fun search() {
        val keywords = "仙"
        val encodedKeywords = URLEncoder.encode(keywords, "gb2312")
        val url = "http://www.tingchina.com/search1.asp?mainlei=0&lei=0&keyword=$encodedKeywords"
        val doc = Jsoup.connect(url).get()

        val totalPage = 1
        val list = ArrayList<Book>()
        val elementList = doc.select(".singerlist1 dd ul li a")
        elementList.forEach { element ->
            val bookUrl = element.absUrl("href")
            val title = element.text()
            val book = Book("", bookUrl, title, "", "")
            list.add(book)
        }
        list.forEach { println(it) }
    }

    @Test
    fun fetchSearchInfo() {
        val doc = Jsoup.connect("http://www.tingchina.com/yousheng/disp_30156.htm").get()
        val book = doc.getElementsByClass("book01").first()
        val coverUrl = book.selectFirst("img").absUrl("src")
        val lis = book.select("ul li")
        val author = lis.get(5).text()
        val artist = lis.get(4).text()
        println(author)
        println(artist)

        val bookInfo = doc.selectFirst(".book02").ownText()
        println(bookInfo)
    }

    @Test
    fun bookDetail() {
        val doc = Jsoup.connect("http://www.tingchina.com/yousheng/30140/play_30140_0.htm").get()
//        println(doc)
        val play = Jsoup.connect(doc.getElementById("playmedia").absUrl("src")).get()
        println(play)
//        val doc = Jsoup.connect("http://www.tingchina.com/yousheng/disp_30156.htm").get()

//        val episodes = doc.select(".main03 .list .b2 a").map {
//            Episode(it.text(), it.attr("abs:href"))
//        }
//
//        episodes.take(10).forEach { println(it) }
//        assertThat(episodes.size).isGreaterThan(0)
    }

    @Test
    fun category() {
        val url = "http://www.tingchina.com/pingshu/leip_136_1.htm"
        val doc = Jsoup.connect(url).get()
        val pages = doc.selectFirst(".yema span").children()
        val currentPage = Regex(".+leip_\\d+_(\\d+)\\.htm").find(url)!!.groupValues[1].toInt()
        var totalPage = currentPage
        if (pages.last().absUrl("href") != url) {
            totalPage = currentPage + 1
        }
        var nextUrl = ""
        if (currentPage != totalPage) {
            val index = pages.indexOfFirst { it.text() == currentPage.toString() }
            nextUrl = pages[index + 1].absUrl("href")
        }
        println("$currentPage/$totalPage")
        println("nextUrl: $nextUrl")

        val list = ArrayList<Book>()
        val elementList = doc.select(".showlist dl")
        elementList.forEach { element ->
            val coverUrl = element.selectFirst("dt a img").absUrl("src")
            val titleElement = element.selectFirst("dd .title a")
            val bookUrl =  titleElement.absUrl("href")
            val (title, author, artist) = titleElement.text().split(" ").let {
                val i = it[0].replace("《", "").replace("》", "")
                val j = if (it.size > 2) it[1] else ""
                val k = if (it.size > 2) it[2].split("　")[0] else ""
                Triple(i, j, k)
            }
            val intro = element.selectFirst("dd .info").ownText()
            list.add(Book(coverUrl, bookUrl, title, author, artist).apply { this.intro = intro })
        }

        list.take(5).forEach { println(it) }
        assertThat(list.size).isGreaterThan(0)
    }
}