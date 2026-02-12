package com.xyoye.common_component.log

import com.xyoye.common_component.log.model.LogLevel
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.model.LogPolicy
import com.xyoye.common_component.log.model.SamplingRule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LogPolicyTest {
    @Test
    fun defaultReleasePolicyUsesInfoLevel() {
        val policy = LogPolicy.defaultReleasePolicy()
        assertEquals(LogPolicy.DEFAULT_RELEASE_POLICY_NAME, policy.name)
        assertEquals(LogLevel.INFO, policy.defaultLevel)
    }

    @Test
    fun debugSessionPolicyUsesConfiguredLevel() {
        val policy = LogPolicy.debugSessionPolicy(minLevel = LogLevel.DEBUG)
        assertEquals(LogPolicy.DEBUG_SESSION_POLICY_NAME, policy.name)
        assertEquals(LogLevel.DEBUG, policy.defaultLevel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun samplingRuleRequiresValidRange() {
        SamplingRule(LogModule.CORE, LogLevel.INFO, sampleRate = 1.5)
    }
}
