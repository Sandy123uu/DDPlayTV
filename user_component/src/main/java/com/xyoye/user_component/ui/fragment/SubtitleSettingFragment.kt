package com.xyoye.user_component.ui.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.xyoye.common_component.base.BasePreferenceFragmentCompat
import com.xyoye.common_component.config.SubtitleConfig
import com.xyoye.common_component.enums.SubtitleRendererBackend
import com.xyoye.common_component.preference.MappingPreferenceDataStore
import com.xyoye.user_component.R

/**
 * Created by xyoye on 2021/2/6.
 */

class SubtitleSettingFragment : BasePreferenceFragmentCompat() {
    companion object {
        private const val KEY_AUTO_LOAD_SAME_NAME_SUBTITLE = "auto_load_same_name_subtitle"
        private const val KEY_AUTO_MATCH_SUBTITLE = "auto_match_subtitle"
        private const val KEY_SUBTITLE_SHADOW_ENABLED = "subtitle_shadow_enabled"
        private const val KEY_SAME_NAME_SUBTITLE_PRIORITY = "same_name_subtitle_priority"
        private const val KEY_SUBTITLE_RENDERER_BACKEND = "subtitle_renderer_backend"
        private const val KEY_SUBTITLE_RENDERER_BACKEND_NOTE = "subtitle_renderer_backend_note"

        fun newInstance() = SubtitleSettingFragment()
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        preferenceManager.preferenceDataStore = SubtitleSettingDataStore()
        addPreferencesFromResource(R.xml.preference_subtitle_setting)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        val loadSameSubtitleSwitch = findPreference<SwitchPreference>(KEY_AUTO_LOAD_SAME_NAME_SUBTITLE)
        val sameSubtitlePriority = findPreference<EditTextPreference>(KEY_SAME_NAME_SUBTITLE_PRIORITY)
        val backendPreference = findPreference<ListPreference>(KEY_SUBTITLE_RENDERER_BACKEND)
        val backendNote = findPreference<Preference>(KEY_SUBTITLE_RENDERER_BACKEND_NOTE)
        val shadowPreference = findPreference<SwitchPreference>(KEY_SUBTITLE_SHADOW_ENABLED)
        backendPreference?.isVisible = false
        backendNote?.isVisible = false
        shadowPreference?.isVisible = false

        loadSameSubtitleSwitch?.setOnPreferenceChangeListener { _, newValue ->
            sameSubtitlePriority?.isVisible = newValue as Boolean
            return@setOnPreferenceChangeListener true
        }

        sameSubtitlePriority?.apply {
            isVisible = loadSameSubtitleSwitch?.isChecked ?: false
            summary = if (TextUtils.isEmpty(this.text)) "未设置" else text
            setOnPreferenceChangeListener { _, newValue ->
                summary = if (TextUtils.isEmpty(newValue as String?)) "未设置" else newValue
                return@setOnPreferenceChangeListener true
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private class SubtitleSettingDataStore : MappingPreferenceDataStore(
        dataStoreName = "SubtitleSettingDataStore",
        booleanReaders =
            mapOf(
                KEY_AUTO_LOAD_SAME_NAME_SUBTITLE to { SubtitleConfig.isAutoLoadSameNameSubtitle() },
                KEY_AUTO_MATCH_SUBTITLE to { SubtitleConfig.isAutoMatchSubtitle() },
                KEY_SUBTITLE_SHADOW_ENABLED to { SubtitleConfig.isShadowEnabled() },
            ),
        booleanWriters =
            mapOf(
                KEY_AUTO_LOAD_SAME_NAME_SUBTITLE to { value -> SubtitleConfig.putAutoLoadSameNameSubtitle(value) },
                KEY_AUTO_MATCH_SUBTITLE to { value -> SubtitleConfig.putAutoMatchSubtitle(value) },
                KEY_SUBTITLE_SHADOW_ENABLED to { value -> SubtitleConfig.putShadowEnabled(value) },
            ),
        stringReaders =
            mapOf(
                KEY_SAME_NAME_SUBTITLE_PRIORITY to { SubtitleConfig.getSubtitlePriority() },
                KEY_SUBTITLE_RENDERER_BACKEND to { SubtitleRendererBackend.LIBASS.name },
            ),
        stringWriters =
            mapOf(
                KEY_SAME_NAME_SUBTITLE_PRIORITY to { value -> SubtitleConfig.putSubtitlePriority(value ?: "") },
                KEY_SUBTITLE_RENDERER_BACKEND to { _ -> Unit },
            ),
    )
}
