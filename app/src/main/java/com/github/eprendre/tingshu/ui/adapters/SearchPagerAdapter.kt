package com.github.eprendre.tingshu.ui.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.github.eprendre.tingshu.ui.SearchFragment

class SearchPagerAdapter(private val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    var sources = emptyList<String>()
    var keywords: String = ""

    override fun getItem(position: Int): Fragment {
        return SearchFragment.newInstance(keywords, position)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return sources[position]
    }

    override fun getCount(): Int {
        return sources.size
    }
}