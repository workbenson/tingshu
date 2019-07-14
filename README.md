# 我的听书

一款自用的安卓听书app

咱平时没事干的时候喜欢听书打发时间，但是`懒人听书`和`喜马拉雅`都开始收费了。某天发现听书界也有不少类似小说界`笔趣阁`的网站。不过听书不像看小说，在网页端用浏览器打开音频播放体验很差，于是就有了这一款 app。

目前核心的播放功能已完成，UI方面不是强项，有兴趣的朋友欢迎帮忙 PR 改进。

## 下载

* [Release](https://github.com/eprendre/tingshu/releases)
* [Play Store](https://play.google.com/store/apps/details?id=com.github.eprendre.tingshu)

## 已支持的站点

* [http://m.ting89.com](http://m.ting89.com)
* [http://m.520tingshu.com](http://m.520tingshu.com)
* [http://m.ting56.com](http://m.ting56.com)
* [http://m.audio699.com](http://m.audio699.com)
* [https://www.tingshubao.com](https://www.tingshubao.com)
* [http://www.tingshuge.com](http://www.tingshuge.com)(现在挂了，不知道啥时候能好。)

## 注意事项

* 这个 app 只是一个爬虫, 不对书籍是否能够播放的可靠性负责。如果原网站已经失效了, 那些书在 app 里面听不了也很正常。如果所有书都听不了, 那应该是机型的问题。碰到这种的希望大家可以提 issue 告之一下机型以及安卓系统版本。
* 在某些手机里锁屏自动切换下一首时会卡住, 原因是由于每次播放时都要先去抓取实际的播放地址。需要加到后台白名单才能解决。

## 截图

主页 | 播放界面
---------|---------
![home](art/home.jpg) | ![play](art/play.jpg)
