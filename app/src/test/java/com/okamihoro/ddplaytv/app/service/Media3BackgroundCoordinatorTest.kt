package com.okamihoro.ddplaytv.app.service

import com.xyoye.data_component.entity.media3.Media3BackgroundMode
import com.xyoye.data_component.entity.media3.Media3Capability
import com.xyoye.data_component.entity.media3.PlayerCapabilityContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Media3BackgroundCoordinatorTest {
    @Test
    fun syncClearsCommandsAndModes_whenTelevisionUiMode() {
        val bridge = RecordingBridge()
        val coordinator = Media3BackgroundCoordinator(bridge)
        val contract =
            PlayerCapabilityContract(
                sessionId = "session-tv",
                capabilities = listOf(Media3Capability.BACKGROUND, Media3Capability.PIP),
                backgroundModes = listOf(Media3BackgroundMode.NOTIFICATION, Media3BackgroundMode.PIP),
                sessionCommands = listOf("REMOTE_HEARTBEAT"),
            )

        coordinator.sync(contract, isTelevisionUiMode = false)
        coordinator.sync(contract, isTelevisionUiMode = true)

        assertEquals(setOf<String>(), bridge.commandEvents.last())
        assertEquals(setOf<Media3BackgroundMode>(), bridge.backgroundEvents.last())
    }

    @Test
    fun syncPushesNotificationAndPipCommands_whenCapabilitiesAllow() {
        val bridge = RecordingBridge()
        val coordinator = Media3BackgroundCoordinator(bridge)
        val contract =
            PlayerCapabilityContract(
                sessionId = "session-1",
                capabilities =
                    listOf(
                        Media3Capability.PLAY,
                        Media3Capability.BACKGROUND,
                        Media3Capability.PIP
                    ),
                backgroundModes =
                    listOf(
                        Media3BackgroundMode.NOTIFICATION,
                        Media3BackgroundMode.PIP
                    ),
                sessionCommands = listOf("REMOTE_HEARTBEAT")
            )

        coordinator.sync(contract, isTelevisionUiMode = false)

        assertEquals(
            setOf(
                "REMOTE_HEARTBEAT",
                Media3BackgroundCoordinator.COMMAND_BACKGROUND_RESUME,
                Media3BackgroundCoordinator.COMMAND_ENTER_PIP
            ),
            bridge.commandEvents.single()
        )
        assertEquals(
            setOf(
                Media3BackgroundMode.NOTIFICATION,
                Media3BackgroundMode.PIP
            ),
            bridge.backgroundEvents.single()
        )
    }

    @Test
    fun syncOnlyEmitsChanges_whenContractUpdates() {
        val bridge = RecordingBridge()
        val coordinator = Media3BackgroundCoordinator(bridge)
        val baseline =
            PlayerCapabilityContract(
                sessionId = "session-2",
                capabilities = listOf(Media3Capability.BACKGROUND),
                backgroundModes = listOf(Media3BackgroundMode.NOTIFICATION),
                sessionCommands = emptyList()
            )

        coordinator.sync(baseline, isTelevisionUiMode = false)
        bridge.reset()

        val updated =
            baseline.copy(
                backgroundModes = listOf(Media3BackgroundMode.NOTIFICATION, Media3BackgroundMode.PIP),
                capabilities = listOf(Media3Capability.BACKGROUND, Media3Capability.PIP)
            )
        coordinator.sync(updated, isTelevisionUiMode = false)

        assertEquals(
            setOf(Media3BackgroundMode.NOTIFICATION, Media3BackgroundMode.PIP),
            bridge.backgroundEvents.single()
        )
        assertEquals(
            setOf(
                Media3BackgroundCoordinator.COMMAND_BACKGROUND_RESUME,
                Media3BackgroundCoordinator.COMMAND_ENTER_PIP
            ),
            bridge.commandEvents.single()
        )

        bridge.reset()
        coordinator.sync(updated, isTelevisionUiMode = false)
        assertTrue(bridge.commandEvents.isEmpty())
        assertTrue(bridge.backgroundEvents.isEmpty())
    }

    private class RecordingBridge : MediaSessionCommandBridge {
        val commandEvents = mutableListOf<Set<String>>()
        val backgroundEvents = mutableListOf<Set<Media3BackgroundMode>>()

        override fun updateSessionCommands(commands: Set<String>) {
            commandEvents += commands
        }

        override fun updateBackgroundModes(modes: Set<Media3BackgroundMode>) {
            backgroundEvents += modes
        }

        fun reset() {
            commandEvents.clear()
            backgroundEvents.clear()
        }
    }
}
