package com.xyoye.user_component.ui.activities.login

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.config.DeveloperCredentialStore
import com.xyoye.common_component.extension.toResString
import com.xyoye.common_component.log.privacy.SensitiveDataSanitizer
import com.xyoye.common_component.network.repository.UserRepository
import com.xyoye.common_component.network.request.NetworkException
import com.xyoye.common_component.session.UserSessionManager
import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.utils.SecurityHelper
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.LoginData
import com.xyoye.user_component.R
import kotlinx.coroutines.launch

class LoginViewModel : BaseViewModel() {
    val accountField = ObservableField("")
    val passwordField = ObservableField("")

    val accountErrorLiveData = MutableLiveData<String>()
    val passwordErrorLiveData = MutableLiveData<String>()
    val loginLiveData = MutableLiveData<LoginData>()
    val openDeveloperAuthDialogLiveData = MutableLiveData<Unit>()

    fun login() {
        try {
            val account = accountField.get()?.trim()
            val password = passwordField.get()

            val allowLogin = checkAccount(account) && checkPassword(password)
            if (!allowLogin) {
                return
            }

            val appId = DeveloperCredentialStore.getAppId()?.trim().orEmpty()
            val appSecret = DeveloperCredentialStore.getAppSecret()?.trim().orEmpty()
            if (appId.isEmpty() || appSecret.isEmpty()) {
                ToastCenter.showWarning(R.string.login_error_developer_credential_required.toResString())
                openDeveloperAuthDialogLiveData.postValue(Unit)
                return
            }

            val accountFingerprint = SensitiveDataSanitizer.fingerprint(account.orEmpty())
            val unixTimestamp = System.currentTimeMillis() / 1000
            val hashInfo = appId + password + unixTimestamp + account
            val hash = SecurityHelper.getInstance().buildHash(hashInfo)

            viewModelScope.launch {
                try {
                    showLoading()
                    val result =
                        UserRepository.login(
                            account!!,
                            password!!,
                            appId,
                            unixTimestamp.toString(),
                            hash,
                        )
                    hideLoading()

                    if (result.isFailure) {
                        val exception = result.exceptionOrNull()
                        exception?.let {
                            ErrorReportHelper.postCatchedExceptionWithContext(
                                it,
                                "LoginViewModel",
                                "login",
                                "Login network request failed (user_fp=$accountFingerprint)",
                            )
                        }
                        ToastCenter.showError(resolveLoginErrorMessage(exception))
                        return@launch
                    }

                    val data =
                        result.getOrNull() ?: run {
                            ErrorReportHelper.postException(
                                "Login response is null",
                                "LoginViewModel",
                                null,
                            )
                            ToastCenter.showError("登录错误，请稍后再试")
                            return@launch
                        }

                    if (UserSessionManager.login(data)) {
                        ToastCenter.showSuccess("登录成功")
                        loginLiveData.postValue(data)
                    } else {
                        ErrorReportHelper.postException(
                            "Login successful but UserSessionManager.login failed",
                            "LoginViewModel",
                            null,
                        )
                        ToastCenter.showError("登录错误，请稍后再试")
                    }
                } catch (e: Exception) {
                    hideLoading()
                    ErrorReportHelper.postCatchedExceptionWithContext(
                        e,
                        "LoginViewModel",
                        "login",
                        "Unexpected error during login process (user_fp=$accountFingerprint)",
                    )
                    ToastCenter.showError("登录过程中发生错误，请稍后再试")
                }
            }
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "LoginViewModel",
                "login",
                "Error in login method initialization",
            )
            ToastCenter.showError("登录初始化失败，请稍后再试")
        }
    }

    private fun resolveLoginErrorMessage(exception: Throwable?): String {
        val message = exception?.message.orEmpty()
        if (exception is NetworkException && exception.code == INVALID_PARAMETER_ERROR_CODE) {
            return R.string.login_error_invalid_params.toResString()
        }
        if (message.contains("一个或多个参数不符合规则")) {
            return R.string.login_error_invalid_params.toResString()
        }
        return message.ifBlank { R.string.login_error_default.toResString() }
    }

    private fun checkAccount(account: String?): Boolean {
        if (account.isNullOrEmpty()) {
            accountErrorLiveData.postValue("请输入帐号")
            return false
        }
        return true
    }

    private fun checkPassword(password: String?): Boolean {
        if (password.isNullOrEmpty()) {
            passwordErrorLiveData.postValue("请输入密码")
            return false
        }
        return true
    }

    companion object {
        private const val INVALID_PARAMETER_ERROR_CODE = 2
    }
}
