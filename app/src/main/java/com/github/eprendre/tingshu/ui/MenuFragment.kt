package com.github.eprendre.tingshu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.ui.adapters.CategoryPagerAdapter
import com.github.eprendre.tingshu.utils.CategoryTab
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_menu.*
import kotlinx.android.synthetic.main.fragment_menu.tabs
import kotlinx.android.synthetic.main.fragment_menu.view_pager
import org.jetbrains.anko.AnkoLogger

class MenuFragment : Fragment(), AnkoLogger {
    private lateinit var categoryPagerAdapter: CategoryPagerAdapter
    private lateinit var categoryTabs: ArrayList<CategoryTab>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryTabs = arguments?.getParcelableArrayList(ARG_TABS)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        categoryPagerAdapter = CategoryPagerAdapter(fragmentManager!!)
        categoryPagerAdapter.categories = categoryTabs
        view_pager.adapter = categoryPagerAdapter
        tabs.setupWithViewPager(view_pager)
    }

    companion object {
        private const val ARG_TABS = "category_tabs"

        @JvmStatic
        fun newInstance(categoryTabs: List<CategoryTab>): MenuFragment {
            return MenuFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_TABS, ArrayList(categoryTabs))
                }
            }
        }
    }
}