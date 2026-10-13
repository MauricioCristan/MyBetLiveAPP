package com.mybetlive.floating

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.mybetlive.data.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35],qualifiers="es-rMX-w360dp-h780dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FloatingPickViewTest {
    @get:Rule val compose=createComposeRule()
    private fun bet(status:String="LIVE_WINNING",value:Double?=26.0)=BetView("pick","event",EventSnapshot("NBA","fake","event","",Team("h","Lakers"),Team("a","Celtics"),98,104,"LIVE","Q4","05:42","2026-10-10T20:00:00Z"),
        "PLAYER_POINTS","OVER",24.5,null,"player","LeBron James",null,null,null,status,value,null,"2026-10-10T20:00:00Z",false)
    @Test fun cardCanOpenMinimizeCloseAndScrollWithLargeText() {
        var closed=false;var opened=false;var minimized=false
        compose.setContent{CompositionLocalProvider(LocalDensity provides Density(2f,1.3f)){
            FloatingPickView(bet(),false,true,520f,{}, {minimized=true},{closed=true},{opened=true},{},{_,_->},{})
        }}
        compose.onNodeWithTag("floating-next").assertDoesNotExist()
        compose.onNodeWithTag("floating-open").assertIsDisplayed().performClick();assertTrue(opened)
        compose.onNodeWithTag("floating-content").performTouchInput{swipeUp()}
        compose.onNodeWithTag("floating-minimize").assertIsDisplayed().performClick();assertTrue(minimized)
        compose.onNodeWithTag("floating-close").performClick();assertTrue(closed)
    }
    @Test fun compactBubblePreservesMissingZeroAndFinal() {
        val row=mutableStateOf(bet("AWAITING_DATA",null));var expanded=false
        compose.setContent{FloatingPickView(row.value,false,false,520f,{expanded=true},{},{},{},{},{_,_->},{})}
        compose.onNodeWithText("— / 24.5").assertExists()
        compose.runOnIdle{row.value=bet("LIVE_LOSING",0.0)}
        compose.onNodeWithText("0 / 24.5").assertExists()
        compose.runOnIdle{row.value=bet("WON",26.0)}
        compose.onNodeWithText("Ganada").assertExists()
        compose.onNodeWithText("26 / 24.5").assertExists()
        compose.onNodeWithText("LeBron James").assertDoesNotExist()
        compose.onNodeWithText("Over · puntos").assertDoesNotExist()
        val bounds=compose.onNodeWithTag("floating-bubble").getBoundsInRoot()
        assertTrue((bounds.right-bounds.left).value<=80f)
        compose.onNodeWithTag("floating-bubble").performClick();assertTrue(expanded)
    }
    @Test fun compactDrawShowsBothTeamsAndCompleteScore() {
        val row=bet().copy(market="MONEYLINE",selection="DRAW",playerName=null,line=null,
            event=bet().event.copy(sport="SOCCER",homeScore=1,awayScore=1),currentValue=0.0)
        compose.setContent{FloatingPickView(row,false,false,520f,{},{},{},{},{},{_,_->},{})}
        compose.onNodeWithTag("floating-team-home",useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithTag("floating-team-away",useUnmergedTree=true).assertIsDisplayed()
        compose.onAllNodesWithText("1",useUnmergedTree=true).assertCountEquals(2)
        compose.onNodeWithTag("floating-score-home",useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithTag("floating-score-away",useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithText("Empate").assertDoesNotExist()
        val bounds=compose.onNodeWithTag("floating-bubble").getBoundsInRoot()
        assertTrue((bounds.right-bounds.left).value<=80f)
    }
}
