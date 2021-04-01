package com.example.photomaster

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ViewPagerAdapter(manager: FragmentManager): FragmentPagerAdapter(manager) {
    private val fragmentList = ArrayList<Fragment>()
    private val fragmentTitleList = ArrayList<String>()

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getCount(): Int {
        return fragmentList.size
    }

    fun addFragment(fragment: Fragment, title: String) {
        fragmentList.add(fragment)
        fragmentTitleList.add(title)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return fragmentTitleList[position]
    }

}
//class ViewPagerAdapter(
//        fm: FragmentManager) : FragmentPagerAdapter(fm) {
//    override fun getItem(position: Int): Fragment {
//        var fragment: Fragment? = null
//        if (position == 0)
//            fragment = filterFragment()
//        else if (position == 1)
//            fragment = toolsFragment()
//        else if (position == 2)
//            fragment = exportFragment()
//        return fragment!!
//    }
//
//    override fun getCount(): Int {
//        return 3
//    }
//
//    override fun getPageTitle(position: Int): CharSequence? {
//        var title: String? = null
//        if (position == 0)
//            title = "Filter"
//        else if (position == 1)
//            title = "Tools"
//        else if (position == 2)
//            title = "Export"
//        return title
//    }
//}