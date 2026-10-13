package com.mybetlive.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="es-rMX")
class ScrollableChoicesTest {
    @get:Rule val compose=createComposeRule()

    @Test fun additionalMarketsScrollAndRemainSelectableOnNarrowScreens() {
        val selected=mutableStateOf("0")
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f,1.4f)) {
                MaterialTheme { Box(Modifier.width(240.dp)) {
                    ScrollableChoices(selected.value,(0..12).map { "$it" to "Mercado $it" },
                        {selected.value=it},tagPrefix="market")
                } }
            }
        }
        compose.onNodeWithTag("market-0").assertIsSelected()
        compose.onNodeWithTag("market-12").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("market-12").assertIsSelected()
        compose.runOnIdle {assertEquals("12",selected.value)}
    }

    @Test fun overUnderChoicesShowBothLabelsAndRespectBusyState() {
        val busy=mutableStateOf(false)
        val selected=mutableStateOf("OVER")
        compose.setContent { MaterialTheme {
            ScrollableChoices(selected.value,listOf("OVER" to "Más de (Over)","UNDER" to "Menos de (Under)"),
                {selected.value=it},enabled=!busy.value,tagPrefix="selection")
        } }
        compose.onNodeWithTag("selection-UNDER").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Menos de (Under)").assertIsDisplayed()
        compose.runOnIdle {busy.value=true}
        compose.onNodeWithTag("selection-OVER").assertIsNotEnabled()
        compose.onNodeWithTag("selection-UNDER").assertIsNotEnabled()
    }
}
