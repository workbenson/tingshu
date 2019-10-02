package com.github.eprendre.tingshu.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.sources.TingShuSourceHandler
import com.github.eprendre.tingshu.ui.adapters.CategoryPagerAdapter
import com.github.eprendre.tingshu.utils.CategoryTab
import com.github.eprendre.tingshu.utils.Prefs
import kotlinx.android.synthetic.main.fragment_menu.tabs
import kotlinx.android.synthetic.main.fragment_menu.view_pager
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.startActivity

/**
 * 小说分类的容器
 */
class MenuFragment : Fragment(), AnkoLogger {
    private lateinit var categoryPagerAdapter: CategoryPagerAdapter
    private lateinit var categoryTabs: ArrayList<CategoryTab>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        categoryTabs = arguments?.getParcelableArrayList(ARG_TABS)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> {
                context?.startActivity<SearchActivity>()
            }
            R.id.switch_source -> {
                if (context == null) return false
                val values = resources.getStringArray(R.array.source_values)
                val checkedItem = values.indexOfFirst { it == Prefs.source }
                AlertDialog.Builder(context!!)
                    .setSingleChoiceItems(R.array.source_entries, checkedItem) { dialog, which ->
                        Prefs.source = values[which]
                        TingShuSourceHandler.setupConfig()
                        (activity as MainActivity).refreshMenus()
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
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