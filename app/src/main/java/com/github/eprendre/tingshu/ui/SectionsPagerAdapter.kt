package com.github.eprendre.tingshu.ui

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.github.eprendre.tingshu.utils.SectionTab

class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    var sections = emptyList<SectionTab>()

    override fun getItem(position: Int): Fragment {
        return SectionFragment.newInstance(sections[position].url)
    }

    override fun getItemPosition(item: Any): Int {
        val index = sections.indexOfFirst { it.url == (item as SectionFragment).sectionUrl }
        return if (index > 0) {
            index
        } else {
            PagerAdapter.POSITION_NONE
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return sections[position].title
    }

    override fun getCount(): Int {
        return sections.size
    }
}