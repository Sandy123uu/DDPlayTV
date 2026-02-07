package com.xyoye.data_component.media3.entity

data class Media3SessionBundle(
    val session: PlaybackSession,
    val capabilityContract: PlayerCapabilityContract,
    val toggleSnapshot: RolloutToggleSnapshot
)
