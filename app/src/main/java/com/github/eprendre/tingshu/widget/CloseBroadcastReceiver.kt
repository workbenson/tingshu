package com.github.eprendre.tingshu.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.github.eprendre.tingshu.TingShuService
import java.lang.Exception

class CloseBroadcastReceiver (val service: TingShuService): BroadcastReceiver() {
    private val closeIntentFilter = IntentFilter(CLOSE_ACTION)
    private var registered = false

    fun register() {
        if (!registered) {
            service.registerReceiver(this, closeIntentFilter)
            registered = true
        }
    }

    fun unregister() {
        if (registered) {
            try {
                service.unregisterReceiver(this)
            } catch (e: Exception) {
            }
            registered = false
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == CLOSE_ACTION) {
            service.exit()
        }
    }

    companion object {
        const val CLOSE_ACTION = "com.github.eprendre.action_close"
    }
}