package com.github.eprendre.tingshu.sources

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.utils.Prefs
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator

class WLQueueNavigator(private val mediaSessionCompat: MediaSessionCompat) :
    TimelineQueueNavigator(mediaSessionCompat) {
    private val window = Timeline.Window()

    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
        return player.currentTimeline.getWindow(windowIndex, window, true).tag as MediaDescriptionCompat
    }

    /**
     * 返回能否跳转上一首、下一首的action
     */
    override fun getSupportedQueueNavigatorActions(player: Player?): Long {
        if (player == null) {
            return 0
        }
        var actions = 0L
        if (App.currentEpisodeIndex() == -1) {
            return actions
        }
        if (App.currentEpisodeIndex() < App.playList.size - 1) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        }
        actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        return actions
    }

    /**
     * 处理跳转上一首的逻辑
     */
    override fun onSkipToPrevious(player: Player) {
        Prefs.currentEpisodePosition = 0
        if (App.currentEpisodeIndex() < 1) {
            player.seekTo(0)
        } else {
            mediaSessionCompat.controller.transportControls.playFromUri(
                Uri.parse(App.playList[App.currentEpisodeIndex() - 1].url),
                null
            )
        }
    }

    /**
     * 处理跳转下一首的逻辑, 不必担心越界 getSupportedQueueNavigatorActions 里面已经检查了
     */
    override fun onSkipToNext(player: Player) {
        Prefs.currentEpisodePosition = 0
        mediaSessionCompat.controller.transportControls.playFromUri(Uri.parse(App.playList[App.currentEpisodeIndex() + 1].url), null)
    }
}