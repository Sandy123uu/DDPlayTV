package com.xyoye.common_component.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.xyoye.common_component.config.UserConfig
import com.xyoye.data_component.data.LoginData

/**
 * 统一用户会话入口。
 *
 * - 读：对外通过 core_contract 的只读会话契约暴露查询能力。
 * - 写：仅由登录/退出/登录态恢复链路调用本对象的更新方法。
 */
object UserSessionManager {
    private val loginStateLiveData = MutableLiveData<LoginData?>()

    @Volatile
    private var loginDataCache: LoginData? = null

    fun login(loginData: LoginData): Boolean {
        val userToken = loginData.token?.trim().orEmpty()
        if (userToken.isEmpty()) {
            logout()
            return false
        }

        loginDataCache = loginData.copy(token = userToken)
        UserConfig.putUserToken(userToken)
        UserConfig.putUserLoggedIn(true)
        loginStateLiveData.postValue(loginDataCache)
        return true
    }

    fun logout() {
        loginDataCache = null
        UserConfig.putUserToken("")
        UserConfig.putUserLoggedIn(false)
        loginStateLiveData.postValue(null)
    }

    fun updateLoginData(loginData: LoginData?) {
        loginDataCache = loginData
        loginStateLiveData.postValue(loginDataCache)
    }

    fun loginState(): LiveData<LoginData?> = loginStateLiveData

    fun currentLoginData(): LoginData? = loginDataCache

    fun currentToken(): String = loginDataCache?.token?.takeIf { it.isNotBlank() } ?: UserConfig.getUserToken().orEmpty()

    fun isLoggedIn(): Boolean {
        val cachedToken = loginDataCache?.token?.takeIf { it.isNotBlank() }
        if (cachedToken != null) {
            return true
        }

        return UserConfig.isUserLoggedIn() && UserConfig.getUserToken()?.isNotBlank() == true
    }
}
