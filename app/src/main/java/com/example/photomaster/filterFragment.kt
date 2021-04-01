package com.example.photomaster

import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.photomaster.filters.FilterListFragmentListener
import com.example.photomaster.filters.FilterViewAdapter
import com.example.photomaster.util.BitmapUtils
import com.example.photomaster.util.SpacesItemDecoration
import com.zomato.photofilters.FilterPack
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.utils.ThumbnailItem
import com.zomato.photofilters.utils.ThumbnailsManager

/**
 * A simple [filterFragment] subclass.
 * Use the [filterFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class filterFragment : Fragment(), FilterListFragmentListener {

    private var listener: FilterListFragmentListener? = null
    private lateinit var adapter: FilterViewAdapter
    private lateinit var filterItemList: MutableList<ThumbnailItem>
    private lateinit var filterListView: RecyclerView

    fun setListener(listener: FilterListFragmentListener) {
        this.listener = listener
    }

    override fun onFilterSelected(filter: Filter) {
        if(listener != null) {
            listener!!.onFilterSelected(filter)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_filter, container, false)
        // 初始化filterItemList, adapter, filterListView
        filterItemList = ArrayList()
        adapter = FilterViewAdapter(activity!!, filterItemList, this)
        filterListView = view.findViewById(R.id.filterListView)
        // 设置filterListView为横向的
        filterListView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        filterListView.itemAnimator = DefaultItemAnimator()
        val space = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        filterListView.addItemDecoration(SpacesItemDecoration(space))
        filterListView.adapter = adapter
        // 展示图片，默认为assests中的图片
        displayImage(null)
        return view
    }

    fun displayImage(bitmap: Bitmap?) {
        val runnable = Runnable {
            val image: Bitmap?
            if (bitmap == null) {
                // 加载 assests文件夹中的imgsample.jpg
                image = BitmapUtils.getBitmapFromAssets(activity!!, "imgsample.jpg", 100, 100)
            } else {
                image = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
            }
            if(image == null) {
                return@Runnable
            }
            ThumbnailsManager.clearThumbs()
            filterItemList.clear()

            // add normal bitmap first
            val imageFilterItem = ThumbnailItem()
            imageFilterItem.image = image
            imageFilterItem.filterName = "Normal"
            ThumbnailsManager.addThumb(imageFilterItem)

            // add filter pack
            val filters = FilterPack.getFilterPack(activity!!)
            for (filter in filters) {
                val item = ThumbnailItem()
                item.image = image
                item.filter = filter
                item.filterName = filter.name
                ThumbnailsManager.addThumb(item)
            }

            // UI drawing
            filterItemList.addAll(ThumbnailsManager.processThumbs(activity))
            activity!!.runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        }
        Thread(runnable).start()
    }
}