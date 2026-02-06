package com.xyoye.common_component.preference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingPreferenceDataStoreTest {
    @Test
    fun getReturnsMappedValue() {
        val store =
            MappingPreferenceDataStore(
                dataStoreName = "TestStore",
                booleanReaders = mapOf("bool_key" to { true }),
                stringReaders = mapOf("string_key" to { "value" }),
                intReaders = mapOf("int_key" to { 42 }),
            )

        assertTrue(store.getBoolean("bool_key", false))
        assertEquals("value", store.getString("string_key", "default"))
        assertEquals(42, store.getInt("int_key", -1))
    }

    @Test
    fun getFallsBackToDefaultWhenReaderThrows() {
        val store =
            MappingPreferenceDataStore(
                dataStoreName = "TestStore",
                booleanReaders = mapOf("bool_key" to { throw IllegalStateException("boom") }),
                stringReaders = mapOf("string_key" to { throw IllegalStateException("boom") }),
                intReaders = mapOf("int_key" to { throw IllegalStateException("boom") }),
            )

        assertFalse(store.getBoolean("bool_key", false))
        assertEquals("default", store.getString("string_key", "default"))
        assertEquals(-1, store.getInt("int_key", -1))
    }

    @Test
    fun putCallsMappedWriters() {
        var booleanValue = false
        var stringValue: String? = null
        var intValue = 0

        val store =
            MappingPreferenceDataStore(
                dataStoreName = "TestStore",
                booleanWriters = mapOf("bool_key" to { value -> booleanValue = value }),
                stringWriters = mapOf("string_key" to { value -> stringValue = value }),
                intWriters = mapOf("int_key" to { value -> intValue = value }),
            )

        store.putBoolean("bool_key", true)
        store.putString("string_key", "updated")
        store.putInt("int_key", 7)

        assertTrue(booleanValue)
        assertEquals("updated", stringValue)
        assertEquals(7, intValue)
    }
}
