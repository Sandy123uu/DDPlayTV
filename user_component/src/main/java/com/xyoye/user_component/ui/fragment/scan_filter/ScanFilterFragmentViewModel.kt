package com.xyoye.user_component.ui.fragment.scan_filter

import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.database.repository.ScanSettingsRepository
import kotlinx.coroutines.launch

class ScanFilterFragmentViewModel : BaseViewModel() {
    val folderLiveData = ScanSettingsRepository.getAllFolderFilters()

    fun updateFolder(
        folderPath: String,
        filter: Boolean
    ) {
        viewModelScope.launch {
            ScanSettingsRepository.updateFolderFilter(folderPath, filter)
        }
    }
}
