package com.xyoye.user_component.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.SwitchPreference
import com.xyoye.common_component.base.BasePreferenceFragmentCompat
import com.xyoye.common_component.config.DanmuConfig
import com.xyoye.common_component.preference.MappingPreferenceDataStore
import com.xyoye.data_component.enums.DanmakuLanguage
import com.xyoye.user_component.R

/**
 * Created by xyoye on 2021/2/6.
 */

class DanmuSettingFragment : BasePreferenceFragmentCompat() {
    companion object {
        private const val KEY_SHOW_DIALOG_BEFORE_PLAY = "show_dialog_before_play"
        private const val KEY_AUTO_LAUNCH_DANMU_BEFORE_PLAY = "auto_launch_danmu_before_play"
        private const val KEY_AUTO_LOAD_SAME_NAME_DANMU = "auto_load_same_name_danmu"
        private const val KEY_AUTO_MATCH_DANMU = "auto_match_danmu"
        private const val KEY_DANMU_UPDATE_IN_CHOREOGRAPHER = "danmu_update_in_choreographer"
        private const val KEY_DANMU_CLOUD_BLOCK = "danmu_cloud_block"
        private const val KEY_DANMU_DEBUG = "danmu_debug"
        private const val KEY_DANMU_LANGUAGE = "danmu_language"

        fun newInstance() = DanmuSettingFragment()

        private val languageData =
            mapOf(
                Pair("原始", DanmakuLanguage.ORIGINAL.value.toString()),
                Pair("简体中文", DanmakuLanguage.SC.value.toString()),
                Pair("繁体中文", DanmakuLanguage.TC.value.toString()),
            )
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        preferenceManager.preferenceDataStore = DanmuSettingDataStore()
        addPreferencesFromResource(R.xml.preference_danmu_setting)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        findPreference<SwitchPreference>(KEY_SHOW_DIALOG_BEFORE_PLAY)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                findPreference<SwitchPreference>(KEY_AUTO_LAUNCH_DANMU_BEFORE_PLAY)?.isVisible =
                    !(newValue as Boolean)
                return@setOnPreferenceChangeListener true
            }

            findPreference<SwitchPreference>(KEY_AUTO_LAUNCH_DANMU_BEFORE_PLAY)?.isVisible =
                !isChecked
        }
        // 播放器类型
        findPreference<ListPreference>(KEY_DANMU_LANGUAGE)?.apply {
            entries = languageData.keys.toTypedArray()
            entryValues = languageData.values.toTypedArray()
            summary = "当前配置：$entry，指定播放时的弹幕语言"
            setOnPreferenceChangeListener { _, newValue ->
                val key = languageData.entries.first { it.value == newValue }.key
                summary = "当前配置：$key，指定播放时的弹幕语言"
                return@setOnPreferenceChangeListener true
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    private class DanmuSettingDataStore : MappingPreferenceDataStore(
        dataStoreName = "DanmuSettingDataStore",
        booleanReaders =
            mapOf(
                KEY_AUTO_LOAD_SAME_NAME_DANMU to { DanmuConfig.isAutoLoadSameNameDanmu() },
                KEY_AUTO_MATCH_DANMU to { DanmuConfig.isAutoMatchDanmu() },
                KEY_DANMU_UPDATE_IN_CHOREOGRAPHER to { DanmuConfig.isDanmuUpdateInChoreographer() },
                KEY_DANMU_CLOUD_BLOCK to { DanmuConfig.isCloudDanmuBlock() },
                KEY_DANMU_DEBUG to { DanmuConfig.isDanmuDebug() },
            ),
        booleanWriters =
            mapOf(
                KEY_AUTO_LOAD_SAME_NAME_DANMU to { value -> DanmuConfig.putAutoLoadSameNameDanmu(value) },
                KEY_AUTO_MATCH_DANMU to { value -> DanmuConfig.putAutoMatchDanmu(value) },
                KEY_DANMU_UPDATE_IN_CHOREOGRAPHER to { value -> DanmuConfig.putDanmuUpdateInChoreographer(value) },
                KEY_DANMU_CLOUD_BLOCK to { value -> DanmuConfig.putCloudDanmuBlock(value) },
                KEY_DANMU_DEBUG to { value -> DanmuConfig.putDanmuDebug(value) },
            ),
        stringReaders =
            mapOf(
                KEY_DANMU_LANGUAGE to { DanmuConfig.getDanmuLanguage().toString() },
            ),
        stringWriters =
            mapOf(
                KEY_DANMU_LANGUAGE to { value -> DanmuConfig.putDanmuLanguage(value?.toInt() ?: 0) },
            ),
    )
}
