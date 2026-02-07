package com.xyoye.common_component.storage.screencast.receiver

import com.xyoye.data_component.data.screeencast.ScreencastData

interface ScreencastReceiveHandler {
    fun onReceiveVideo(screencastData: ScreencastData)
}
