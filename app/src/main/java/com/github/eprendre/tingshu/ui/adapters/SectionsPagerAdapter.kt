package com.github.eprendre.tingshu.ui.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.github.eprendre.tingshu.ui.SectionFragment
import com.github.eprendre.tingshu.utils.SectionTab

class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    var sections = emptyList<SectionTab>()

    override fun getItem(position: Int): Fragment {
        return SectionFragment.newInstance(sections[position].url)
    }

    override fun getItemPosition(item: Any): Int {
        //SectionFragment的 sectionUrl 就是 SectionTab 传进来的链接，以此可以判断当前Fragment是位于第几页。
        //如果不存在代表已经切换源了，返回POSITION_NONE可以让它自动刷新ViewPager
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