package com.github.eprendre.tingshu.ui.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.github.eprendre.tingshu.ui.CategoryFragment
import com.github.eprendre.tingshu.utils.CategoryTab

class CategoryPagerAdapter(private val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    var categories = emptyList<CategoryTab>()

    override fun getItem(position: Int): Fragment {
        return CategoryFragment.newInstance(categories[position].url)
    }

    override fun getItemPosition(item: Any): Int {
        //CategoryFragment的 categoryUrl 就是 CategoryTab 传进来的链接，以此可以判断当前Fragment是位于第几页。
        //如果不存在代表已经切换源了，返回POSITION_NONE可以让它自动刷新ViewPager
        val index = categories.indexOfFirst { it.url == (item as CategoryFragment).categoryUrl }
        return if (index > 0) {
            index
        } else {
            PagerAdapter.POSITION_NONE
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return categories[position].title
    }

    override fun getCount(): Int {
        return categories.size
    }
}