package com.xyoye.common_component.adapter

import org.junit.Assert.assertFalse
import org.junit.Test

class AdapterDiffCallBackSafetyTest {
    @Test
    fun areItemsTheSameReturnsFalseForMixedTypesWithUnsafeComparator() {
        val creator = AdapterDiffCreator().apply {
            areItemsTheSame { old, new ->
                (old as MixedA).id == (new as MixedA).id
            }
        }
        val callback =
            AdapterDiffCallBack(
                oldData = listOf(MixedA(1)),
                newData = listOf(MixedB(1)),
                diffCreator = creator,
            )

        val same = callback.areItemsTheSame(0, 0)

        assertFalse(same)
    }

    @Test
    fun areContentsTheSameReturnsFalseWhenComparatorThrows() {
        val creator = AdapterDiffCreator().apply {
            areContentsTheSame { old, new ->
                val oldItem = old as MixedA
                val newItem = new as MixedA
                if (oldItem.payload == "throw") {
                    throw IllegalStateException("boom")
                }
                oldItem.payload == newItem.payload
            }
        }
        val callback =
            AdapterDiffCallBack(
                oldData = listOf(MixedA(1, "throw")),
                newData = listOf(MixedA(1, "ok")),
                diffCreator = creator,
            )

        val same = callback.areContentsTheSame(0, 0)

        assertFalse(same)
    }
}

private data class MixedA(
    val id: Int,
    val payload: String = "",
)

private data class MixedB(
    val id: Int,
)
