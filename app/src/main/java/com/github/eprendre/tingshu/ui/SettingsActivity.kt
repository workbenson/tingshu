package com.github.eprendre.tingshu.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.utils.Prefs
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        when (Prefs.source) {
            Prefs.SOURCE_56TINGSHU -> m56tingshu.isChecked = true
            Prefs.SOURCE_520TINGSHU -> m520tingshu.isChecked = true
        }

        source_group.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.m56tingshu -> Prefs.source = Prefs.SOURCE_56TINGSHU
                R.id.m520tingshu -> Prefs.source = Prefs.SOURCE_520TINGSHU
            }
            TingShuSourceHandler.setupConfig()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
