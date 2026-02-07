package com.xyoye.common_component.adapter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdapterDiffCreatorTest {
    @Test
    fun isSameItemReturnsFalseWhenTypeMismatchEvenWithUnsafeComparator() {
        val creator = AdapterDiffCreator()
        creator.areItemsTheSame { old, new ->
            (old as DiffA).id == (new as DiffA).id
        }

        val result = creator.isSameItem(DiffA(1), DiffB(1))

        assertFalse(result)
    }

    @Test
    fun isSameContentReturnsFalseWhenComparatorThrows() {
        val creator = AdapterDiffCreator()
        creator.areContentsTheSame { old, new ->
            val oldItem = old as DiffA
            val newItem = new as DiffA
            if (oldItem.payload == "throw") {
                throw IllegalStateException("boom")
            }
            oldItem.payload == newItem.payload
        }

        val result = creator.isSameContent(DiffA(1, "throw"), DiffA(1, "x"))

        assertFalse(result)
    }

    @Test
    fun isSameItemStillWorksForSameTypeComparator() {
        val creator = AdapterDiffCreator()
        creator.areItemsTheSame { old, new ->
            (old as DiffA).id == (new as DiffA).id
        }

        val result = creator.isSameItem(DiffA(7), DiffA(7))

        assertTrue(result)
    }
}

private data class DiffA(
    val id: Int,
    val payload: String = "",
)

private data class DiffB(
    val id: Int,
)
