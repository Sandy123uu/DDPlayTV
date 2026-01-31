package com.xyoye.common_component.base

import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.xyoye.common_component.preference.TvEditTextPreferenceDialogFragmentCompat

/**
 * PreferenceFragmentCompat 的统一封装：
 * - TV/遥控器场景下，EditTextPreference 的软键盘显示需要做特殊兼容（避免无限重试导致键盘反复拉起）。
 */
abstract class BasePreferenceFragmentCompat : PreferenceFragmentCompat() {
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is EditTextPreference) {
            val fragmentManager = parentFragmentManager
            if (fragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) return

            val dialogFragment =
                TvEditTextPreferenceDialogFragmentCompat.newInstance(preference.key)

            @Suppress("DEPRECATION")
            dialogFragment.setTargetFragment(this, 0)

            dialogFragment.show(fragmentManager, DIALOG_FRAGMENT_TAG)
            return
        }

        super.onDisplayPreferenceDialog(preference)
    }

    private companion object {
        private const val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"
    }
}

