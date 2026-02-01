package com.xyoye.user_component.ui.fragment.scan_filter

import com.xyoye.common_component.adapter.addItem
import com.xyoye.common_component.adapter.buildAdapter
import com.xyoye.common_component.base.BaseFragment
import com.xyoye.common_component.extension.setData
import com.xyoye.common_component.extension.setTextColorRes
import com.xyoye.common_component.extension.vertical
import com.xyoye.common_component.focus.RecyclerViewFocusDelegate
import com.xyoye.common_component.utils.getFolderName
import com.xyoye.data_component.bean.FolderBean
import com.xyoye.user_component.BR
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.FragmentScanFilterBinding
import com.xyoye.user_component.databinding.ItemFilterFolderBinding

class ScanFilterFragment : BaseFragment<ScanFilterFragmentViewModel, FragmentScanFilterBinding>() {
    companion object {
        fun newInstance() = ScanFilterFragment()
    }

    private val focusDelegate by lazy(LazyThreadSafetyMode.NONE) {
        RecyclerViewFocusDelegate(recyclerView = dataBinding.filterFolderRv)
    }

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            ScanFilterFragmentViewModel::class.java,
        )

    override fun getLayoutId() = R.layout.fragment_scan_filter

    override fun initView() {
        dataBinding.filterFolderRv.apply {
            itemAnimator = null

            layoutManager = vertical()

            adapter =
                buildAdapter {
                    addItem<FolderBean, ItemFilterFolderBinding>(R.layout.item_filter_folder) {
                        initView { data, _, _ ->
                            itemBinding.apply {
                                val fileCountText = "${data.fileCount}视频"

                                folderTv.text = getFolderName(data.folderPath)
                                fileCountTv.text = fileCountText

                                folderIv.setImageResource(if (data.isFilter) R.drawable.ic_folder_filter else R.drawable.ic_folder)
                                folderTv.setTextColorRes(if (data.isFilter) R.color.text_red else R.color.text_black)
                                fileCountTv.setTextColorRes(if (data.isFilter) R.color.text_red else R.color.text_gray)

                                filterFolderCb.setOnCheckedChangeListener(null)
                                filterFolderCb.isChecked = data.isFilter
                                filterFolderCb.setOnCheckedChangeListener { _, isChecked ->
                                    viewModel.updateFolder(data.folderPath, isChecked)
                                }

                                itemLayout.setOnClickListener {
                                    viewModel.updateFolder(data.folderPath, !data.isFilter)
                                }
                            }
                        }
                    }
                }
        }

        focusDelegate.installVerticalDpadKeyNavigation()

        viewModel.folderLiveData.observe(this) {
            dataBinding.filterFolderRv.setData(it)
        }
    }

    override fun onResume() {
        super.onResume()
        if (bindingOrNull == null) return
        focusDelegate.onResume()
    }

    override fun onPause() {
        if (bindingOrNull != null) {
            focusDelegate.onPause()
        }
        super.onPause()
    }
}
