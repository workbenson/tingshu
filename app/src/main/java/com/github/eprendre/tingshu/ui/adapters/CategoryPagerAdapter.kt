package com.github.eprendre.tingshu.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.github.eprendre.tingshu.ui.CategoryFragment
import com.github.eprendre.tingshu.utils.CategoryTab

class CategoryPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    var categories = emptyList<CategoryTab>()

    override fun getItem(position: Int): Fragment {
        return CategoryFragment.newInstance(categories[position].url)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return categories[position].title
    }

    override fun getCount(): Int {
        return categories.size
    }
}