package com.xyoye.common_component.bridge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Created by xyoye on 2021/8/8.
 */

object PlayTaskBridge {
    private val _taskRemoveLiveData = MutableLiveData<Long>()
    val taskRemoveLiveData: LiveData<Long> = _taskRemoveLiveData
    var taskInfoQuery: ((id: Long) -> String)? = null

    fun sendTaskRemoveMsg(taskId: Long) {
        if (taskId == -1L) {
            return
        }
        _taskRemoveLiveData.postValue(taskId)
    }

    fun getTaskLog(id: Long): String = taskInfoQuery?.invoke(id) ?: ""
}
