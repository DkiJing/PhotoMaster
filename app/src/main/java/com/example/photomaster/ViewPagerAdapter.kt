package com.example.photomaster

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter


class ViewPagerAdapter(
        fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        var fragment: Fragment? = null
        if (position == 0)
            fragment = filterFragment()
        else if (position == 1)
            fragment = toolsFragment()
        else if (position == 2)
            fragment = exportFragment()
        return fragment!!
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence? {
        var title: String? = null
        if (position == 0)
            title = "Filter"
        else if (position == 1)
            title = "Tools"
        else if (position == 2)
            title = "Export"
        return title
    }
}