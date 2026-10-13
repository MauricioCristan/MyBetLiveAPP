package com.mybetlive.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.mybetlive.data.LineRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class LineWheelTest {
    @get:Rule val compose=createComposeRule()
    @Test fun tappingAndChangingFormatRequireARealChoiceWithoutAKeyboard() {
        val value=mutableStateOf("")
        compose.setContent {MyBetLiveTheme {LineWheel(LineRule(0.0,10.5,.5,"goles",.5),value.value,{value.value=it})}}
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.runOnIdle {assertEquals("",value.value)}
        compose.onNodeWithTag("line-choice-1.5").performClick()
        compose.waitForIdle();compose.runOnIdle {assertEquals("1.5",value.value)}
        compose.onNodeWithTag("line-whole").performClick()
        compose.waitForIdle();compose.runOnIdle {assertEquals("",value.value)}
        compose.onNodeWithTag("prop-line-list").performScrollToIndex(1)
        compose.onNodeWithTag("line-choice-1").performClick()
        compose.waitForIdle();compose.runOnIdle {assertEquals("1",value.value)}
    }
    @Test fun nflSupportsExactHighValuesAndDraggingStaysInTheCatalog() {
        val value=mutableStateOf("")
        compose.setContent {MyBetLiveTheme {LineWheel(LineRule(0.0,800.5,.5,"yardas",267.5),value.value,{value.value=it})}}
        compose.onNodeWithTag("line-choice-267.5").performClick()
        compose.waitForIdle();compose.runOnIdle {assertEquals("267.5",value.value)}
        compose.onNodeWithTag("prop-line-list").performTouchInput {swipeUp(durationMillis=500)}
        compose.waitForIdle();compose.runOnIdle {
            val selected=value.value.toDouble();assertTrue(selected>=.5&&selected<=800.5);assertEquals(.5,selected%1,0.0)
        }
    }
    @Test fun aRuleUpdateClearsASelectionThatIsNoLongerAllowed() {
        val value=mutableStateOf("9.5");val rule=mutableStateOf(LineRule(0.0,10.5,.5,"goles",.5))
        compose.setContent {MyBetLiveTheme {LineWheel(rule.value,value.value,{value.value=it})}}
        compose.runOnIdle {rule.value=LineRule(0.0,2.5,.5,"goles",.5)}
        compose.waitForIdle();compose.runOnIdle {assertEquals("",value.value)}
        compose.onNodeWithTag("line-choice-9.5").assertDoesNotExist()
    }
    @Test fun restoringTheScreenPreservesWholeLineFormatAndChoice() {
        val value=mutableStateOf("");val restoration=StateRestorationTester(compose)
        restoration.setContent {MyBetLiveTheme {LineWheel(LineRule(0.0,800.5,.5,"yardas",267.5),value.value,{value.value=it})}}
        compose.onNodeWithTag("line-whole").performClick()
        compose.onNodeWithTag("line-choice-267").performClick();compose.waitForIdle()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("line-whole").assertIsSelected()
        compose.runOnIdle {assertEquals("267",value.value)}
    }
    @Test fun busyStateCannotChangeTheChoice() {
        val value=mutableStateOf("")
        compose.setContent {MyBetLiveTheme {LineWheel(LineRule(0.0,10.5,.5,"goles",.5),value.value,{value.value=it},enabled=false)}}
        compose.onNodeWithTag("line-choice-0.5").assertIsNotEnabled()
        compose.onNodeWithTag("line-whole").assertIsNotEnabled()
        compose.runOnIdle {assertEquals("",value.value)}
    }
    @Test fun negativeHandicapsCanBeChosenWithoutFreeText() {
        val value=mutableStateOf("")
        compose.setContent {MyBetLiveTheme {LineWheel(LineRule(-20.5,20.5,.5,"hándicap",-1.5),value.value,{value.value=it})}}
        compose.onNodeWithTag("line-choice--1.5").performClick();compose.waitForIdle()
        compose.runOnIdle {assertEquals("-1.5",value.value)}
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }
}
