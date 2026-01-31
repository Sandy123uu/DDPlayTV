package com.xyoye.common_component.preference

import android.view.View
import android.widget.EditText
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import com.xyoye.common_component.utils.showKeyboard

/**
 * TV 端部分输入法（尤其是自带/第三方 TV 软键盘）在 [InputMethodManager.showSoftInput] 时可能始终返回 false，
 * 这会触发 AndroidX Preference 默认实现的无限重试，导致用户用遥控器 BACK 隐藏键盘后又被立刻拉起。
 *
 * 这里改为：只在 dialog 已显示（onStart）后做一次“尽力而为”的拉起键盘，避免出现“键盘关不掉”的死循环。
 */
class TvEditTextPreferenceDialogFragmentCompat : EditTextPreferenceDialogFragmentCompat() {
    private var boundEditText: EditText? = null
    private var pendingShowSoftInput = false

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        boundEditText = view.findViewById(android.R.id.edit)
        boundEditText?.setOnClickListener {
            showKeyboard(it)
        }
    }

    override fun scheduleShowSoftInput() {
        pendingShowSoftInput = true
    }

    override fun onStart() {
        super.onStart()
        if (!pendingShowSoftInput) return
        pendingShowSoftInput = false
        boundEditText?.let { showKeyboard(it) }
    }

    override fun onDestroyView() {
        boundEditText = null
        pendingShowSoftInput = false
        super.onDestroyView()
    }

    companion object {
        fun newInstance(key: String): TvEditTextPreferenceDialogFragmentCompat {
            val fragment = TvEditTextPreferenceDialogFragmentCompat()
            fragment.arguments =
                androidx.core.os.bundleOf("key" to key)
            return fragment
        }
    }
}
