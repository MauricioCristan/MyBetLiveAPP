package com.mybetlive.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rMX")
class FittingScoreTest {
    @get:Rule val compose = createComposeRule()

    @Test fun missingScoresFitNarrowSpaceWithLargeDeviceText() {
        var layout: TextLayoutResult? = null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 1.5f)) {
                MaterialTheme { FittingScore("— : —", 36.sp, Modifier.width(80.dp)) { layout = it } }
            }
        }
        compose.onNodeWithText("— : —").assertIsDisplayed()
        compose.runOnIdle {
            assertNotNull(layout)
            assertFalse(layout!!.hasVisualOverflow)
            assertEquals(5, layout!!.getLineEnd(0))
        }
    }

    @Test fun threeDigitScoresRemainComplete() {
        var layout: TextLayoutResult? = null
        compose.setContent {
            MaterialTheme { FittingScore("123 : 118", 36.sp, Modifier.width(85.dp)) { layout = it } }
        }
        compose.onNodeWithText("123 : 118").assertIsDisplayed()
        compose.runOnIdle { assertFalse(layout!!.hasVisualOverflow) }
    }
    @Test fun longPropValuesFitTheSmallBubbleWithLargeDeviceText() {
        var layout: TextLayoutResult? = null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 1.3f)) {
                MaterialTheme { FittingScore("333.5 / 275.5", 18.sp, Modifier.width(64.dp),minFontSize=8.sp) { layout = it } }
            }
        }
        compose.onNodeWithText("333.5 / 275.5").assertIsDisplayed()
        compose.runOnIdle { assertFalse(layout!!.hasVisualOverflow) }
    }
}
