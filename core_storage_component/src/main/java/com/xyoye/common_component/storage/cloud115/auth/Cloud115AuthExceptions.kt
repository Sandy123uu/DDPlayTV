package com.xyoye.common_component.storage.cloud115.auth

import com.xyoye.common_component.network.request.PassThroughException

class Cloud115ReAuthRequiredException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause),
    PassThroughException

class Cloud115NotConfiguredException(
    message: String
) : RuntimeException(message),
    PassThroughException

