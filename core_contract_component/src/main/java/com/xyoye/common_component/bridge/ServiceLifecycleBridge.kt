package com.xyoye.common_component.bridge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Created by xyoye on 2022/9/18.
 */

object ServiceLifecycleBridge {
    private val screencastReceiveLiveData = MutableLiveData<Boolean>()

    fun getScreencastReceiveObserver(): LiveData<Boolean> = screencastReceiveLiveData

    fun onScreencastReceiveLifeChange(alive: Boolean) {
        screencastReceiveLiveData.postValue(alive)
    }
}
