package com.mybetlive.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.mybetlive.data.BankView
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="es-rMX-w411dp-h891dp-xxhdpi")
class TrackingBankDestinationTest {
    @get:Rule val compose=createComposeRule()
    private val bank=BankView("a","Personal","MXN","UTC","2026-01-01T00:00:00Z","1000",writable=true)
    @Test fun oneWritableBankIsAutomaticButMultipleRequireExplicitChoice(){
        assertEquals(bank,trackingBankDestination(listOf(bank),null))
        val second=bank.copy(id="b",name="Deportes",currency="USD")
        assertNull(trackingBankDestination(listOf(bank,second),null))
        assertEquals(second,trackingBankDestination(listOf(bank,second),"b"))
        assertNull(trackingBankDestination(listOf(bank,second),"deleted"))
    }
    @Test fun archivedAndReadOnlyBanksCannotReceiveAnAutomaticRegistration(){
        assertNull(trackingBankDestination(listOf(bank.copy(archived=true)),bank.id))
        assertNull(trackingBankDestination(listOf(bank.copy(writable=false)),bank.id))
        assertEquals(bank,trackingBankDestination(listOf(bank,bank.copy(id="b",writable=false)),"b"))
    }
    @Test fun multipleBanksShowASelectorWithTheirCurrencies(){
        val selected=mutableStateOf<String?>(null)
        compose.setContent{MyBetLiveTheme{TrackingBankDestination(listOf(bank,bank.copy(id="b",name="Deportes",currency="USD")),selected.value,false,null,true,{selected.value=it},{})}}
        compose.onNodeWithText("¿A qué bank va esta apuesta?",substring=true).assertExists()
        compose.onNodeWithText("Selecciona un bank",substring=true).performClick()
        compose.onNodeWithText("Deportes · USD").performClick()
        compose.runOnIdle{assertEquals("b",selected.value)}
        compose.onNodeWithText("El importe se registrará en USD. La apuesta quedará pendiente de liquidación.").assertExists()
    }
    @Test fun aSingleBankIsVisibleAndAnUnavailableBankCannotBeSelected(){
        compose.setContent{MyBetLiveTheme{TrackingBankDestination(listOf(bank),null,false,null,true,{fail("Single bank should be automatic")},{})}}
        compose.onNodeWithText("Personal · MXN").assertExists()
        compose.onNodeWithText("¿A qué bank va esta apuesta?").assertDoesNotExist()
    }
}
