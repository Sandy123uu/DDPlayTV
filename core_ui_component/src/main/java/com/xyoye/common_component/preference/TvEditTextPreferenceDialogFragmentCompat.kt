package com.xyoye.common_component.preference

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import com.xyoye.common_component.extension.isTelevisionUiMode
import com.xyoye.common_component.utils.showKeyboard

/**
 * TV 端 EditTextPreference 改为手动触发键盘：
 * - 禁用 AndroidX 默认的自动弹键盘流程（避免 BACK 隐藏后又被系统立即拉起）
 * - 在输入框点击/确认键时按需调用 [showKeyboard]
 */
class TvEditTextPreferenceDialogFragmentCompat : EditTextPreferenceDialogFragmentCompat() {
    private var boundEditText: EditText? = null

    private val isTelevisionMode: Boolean
        get() = context?.isTelevisionUiMode() == true

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        boundEditText = view.findViewById(android.R.id.edit)

        val editText = boundEditText ?: return
        if (!isTelevisionMode) {
            // 手机/平板保持 AndroidX 默认行为（自动拉起键盘）
            editText.showSoftInputOnFocus = true
            return
        }

        editText.showSoftInputOnFocus = false
        editText.setOnClickListener { showKeyboard(it) }
        editText.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            ) {
                showKeyboard(editText)
                return@setOnKeyListener true
            }
            false
        }
    }

    @SuppressLint("RestrictedApi")
    override fun needInputMethod(): Boolean =
        if (isTelevisionMode) {
            false
        } else {
            super.needInputMethod()
        }

    @SuppressLint("RestrictedApi")
    override fun scheduleShowSoftInput() {
        if (!isTelevisionMode) {
            super.scheduleShowSoftInput()
        }
    }

    override fun onDestroyView() {
        boundEditText?.setOnClickListener(null)
        boundEditText?.setOnKeyListener(null)
        boundEditText = null
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
