package com.example.photomaster.filters

import com.zomato.photofilters.imageprocessors.Filter

/**
 * 滤镜监听器接口
 */
interface FilterListFragmentListener {
    fun onFilterSelected(filter: Filter)
}