package com.xyoye.common_component.services

import androidx.lifecycle.LiveData
import com.xyoye.data_component.data.LoginData

/**
 * 用户登录态只读契约。
 *
 * 由 runtime 层提供实现，feature 层仅依赖该接口读取当前登录态与 token。
 */
interface UserSessionState {
    fun loginState(): LiveData<LoginData?>

    fun currentLoginData(): LoginData?

    fun currentToken(): String

    fun isLoggedIn(): Boolean
}
