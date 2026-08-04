package com.houvven.guise.xposed

import com.houvven.guise.xposed.config.HooksValue
import com.houvven.guise.xposed.config.ModuleConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class HookPlanTest {

    @Test
    fun defaultConfigurationInstallsNoHooks() {
        assertEquals(emptyList<HookFeature>(), ModuleConfig().activeHookFeatures())
    }

    @Test
    fun onlyConfiguredGroupsAreActivated() {
        val config = ModuleConfig(
            brand = "Xiaomi",
            densityDpi = 420,
            versionName = "2.0",
            passApplications = true,
        )

        assertEquals(
            listOf(
                HookFeature.OS_BUILD,
                HookFeature.DISPLAY_DENSITY,
                HookFeature.APPLICATION_LIST,
                HookFeature.APP_VERSION,
            ),
            config.activeHookFeatures(),
        )
    }

    @Test
    fun networkSubfieldsActivateTheNetworkGroup() {
        assertEquals(
            listOf(HookFeature.NETWORK),
            ModuleConfig(simOperator = "46000").activeHookFeatures(),
        )
        assertEquals(
            listOf(HookFeature.NETWORK),
            ModuleConfig(networkType = HooksValue.NET_WIFI).activeHookFeatures(),
        )
    }
}
