package com.xyoye.common_component.log.http.model

enum class HttpDegradeMode {
    NORMAL,
    THROTTLED,
    DROP_LOW_PRIORITY,
    LOGCAT_PAUSED,
    PERSISTENCE_PAUSED,
}

