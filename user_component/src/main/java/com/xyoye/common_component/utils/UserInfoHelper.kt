package com.xyoye.common_component.utils

import androidx.lifecycle.LiveData
import com.xyoye.common_component.session.UserSessionManager
import com.xyoye.data_component.data.LoginData

/**
 * Created by xyoye on 2021/1/11.
 */

object UserInfoHelper {
    val loginLiveData: LiveData<LoginData?> = UserSessionManager.loginState()

    var mLoginData: LoginData?
        get() = UserSessionManager.currentLoginData()
        set(value) {
            UserSessionManager.updateLoginData(value)
        }

    fun login(loginData: LoginData): Boolean {
        return UserSessionManager.login(loginData)
    }

    fun exitLogin() {
        UserSessionManager.logout()
    }

    fun updateLoginInfo() {
        UserSessionManager.updateLoginData(UserSessionManager.currentLoginData())
    }
}
