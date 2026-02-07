package com.xyoye.common_component.media3

import com.xyoye.data_component.media3.entity.Media3BackgroundMode
import com.xyoye.data_component.media3.entity.PlaybackSession
import com.xyoye.data_component.media3.entity.PlayerCapabilityContract
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface describing the interactions activities/fragments need from the Media3 session service.
 * Defined in common_component so feature modules can depend on it without referencing the app module.
 */
interface Media3SessionClient {
    fun updateSession(
        session: PlaybackSession?,
        capability: PlayerCapabilityContract?
    )

    fun session(): StateFlow<PlaybackSession?>

    fun capability(): StateFlow<PlayerCapabilityContract?>

    fun backgroundModes(): StateFlow<Set<Media3BackgroundMode>>

    fun sessionCommands(): StateFlow<Set<String>>
}
