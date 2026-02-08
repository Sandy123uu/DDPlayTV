package com.xyoye.anime_component.ui.dialog.date_picker

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberPickerBehaviorTest {
    @Test
    fun canChangeValueByDpadRespectsBoundaryWhenNoWrap() {
        assertTrue(
            NumberPicker.canChangeValueByDpad(
                false,
                KeyEvent.KEYCODE_DPAD_DOWN,
                1,
                0,
                2,
            ),
        )

        assertFalse(
            NumberPicker.canChangeValueByDpad(
                false,
                KeyEvent.KEYCODE_DPAD_DOWN,
                2,
                0,
                2,
            ),
        )

        assertTrue(
            NumberPicker.canChangeValueByDpad(
                false,
                KeyEvent.KEYCODE_DPAD_UP,
                1,
                0,
                2,
            ),
        )

        assertFalse(
            NumberPicker.canChangeValueByDpad(
                false,
                KeyEvent.KEYCODE_DPAD_UP,
                0,
                0,
                2,
            ),
        )
    }

    @Test
    fun canChangeValueByDpadAlwaysTrueWhenWrapEnabled() {
        assertTrue(
            NumberPicker.canChangeValueByDpad(
                true,
                KeyEvent.KEYCODE_DPAD_DOWN,
                2,
                0,
                2,
            ),
        )
        assertTrue(
            NumberPicker.canChangeValueByDpad(
                true,
                KeyEvent.KEYCODE_DPAD_UP,
                0,
                0,
                2,
            ),
        )
    }

    @Test
    fun canRequestFocusSafelyRequiresAttachAndParent() {
        assertFalse(NumberPicker.canRequestFocusSafely(false, true))
        assertFalse(NumberPicker.canRequestFocusSafely(true, false))
        assertTrue(NumberPicker.canRequestFocusSafely(true, true))
    }
}
