package com.xyoye.user_component.ui.dialog

import android.app.Activity
import com.xyoye.common_component.config.BilibiliTvCredentialStore
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.BaseBottomDialog
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.DialogBilibiliTvCredentialBinding

class BilibiliTvCredentialDialog(
    private val activity: Activity,
    private val onChanged: () -> Unit
) : BaseBottomDialog<DialogBilibiliTvCredentialBinding>(activity) {
    private lateinit var binding: DialogBilibiliTvCredentialBinding

    override fun getChildLayoutId(): Int = R.layout.dialog_bilibili_tv_credential

    override fun initView(binding: DialogBilibiliTvCredentialBinding) {
        this.binding = binding

        setTitle(activity.getString(R.string.developer_bilibili_tv_credential_dialog_title))
        setPositiveText("保存")
        setNegativeText("取消")

        binding.inputAppKey.setText(BilibiliTvCredentialStore.getStoredAppKeyForPrefill())
        binding.inputAppSecret.setText(BilibiliTvCredentialStore.getStoredAppSecretForPrefill())

        setNegativeListener { dismiss() }
        setPositiveListener { saveOrWarn() }
        addNeutralButton("清除") { clear() }
    }

    private fun saveOrWarn() {
        val appKey =
            binding.inputAppKey.text
                ?.toString()
                ?.trim()
                .orEmpty()
        val appSecret =
            binding.inputAppSecret.text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (appKey.isBlank() && appSecret.isBlank()) {
            clear()
            return
        }

        if (appKey.isBlank() || appSecret.isBlank()) {
            ToastCenter.showWarning("请同时填写 APP_KEY 与 APP_SEC（或两者都留空以清除）")
            return
        }

        BilibiliTvCredentialStore.putAppKey(appKey)
        BilibiliTvCredentialStore.putAppSecret(appSecret)
        ToastCenter.showSuccess("已保存")
        onChanged.invoke()
        dismiss()
    }

    private fun clear() {
        BilibiliTvCredentialStore.putAppKey("")
        BilibiliTvCredentialStore.putAppSecret("")
        ToastCenter.showSuccess("已清除")
        onChanged.invoke()
        dismiss()
    }
}
