package com.mybetlive.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.mybetlive.data.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-rMX-w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardNavigationTest {
    @Test fun leagueIconsExpandTheSelectionAndFilterLocallyWithoutSportRequests() {
        val first=EventView("league-a",event("SOCCER","SCHEDULED").copy(leagueId="262",leagueName="Liga MX"),false)
        val second=EventView("league-b",event("SOCCER","SCHEDULED").copy(leagueId="39",leagueName="Premier League"),false)
        state.value=state.value.copy(selectedSport="SOCCER",events=listOf(first,second))
        launch()
        compose.onNodeWithTag("nav-EVENTS").performClick()
        compose.onNodeWithTag("league-ALL").assertIsSelected()
        compose.onNodeWithContentDescription("Liga MX").assertExists()
        compose.onAllNodesWithText("Liga MX",substring=false).assertCountEquals(0)
        compose.onNodeWithTag("league-262").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("league-262").assertIsSelected()
        compose.onNodeWithText("Liga MX",substring=false).assertExists()
        compose.onNodeWithTag("event-league-b").assertDoesNotExist()
        assertTrue(selectedSports.isEmpty())
        screenshot("partidos-selector-ligas")
        compose.onNodeWithTag("league-ALL").performClick()
        compose.onNodeWithTag("event-list").performScrollToNode(hasTestTag("event-league-b"))
        compose.onNodeWithTag("event-league-b").assertExists()
    }
    @Test fun gamesExpandOnlyTheSelectedSportAndKeepIconButtonsAccessible() {
        state.value=state.value.copy(sports=sports+listOf(SportInfo("NBA",true,listOf("MONEYLINE")),SportInfo("NHL",true,listOf("MONEYLINE"))))
        launch()
        compose.onNodeWithTag("nav-EVENTS").performClick()
        compose.onNodeWithTag("sport-NFL").assertIsSelected()
        compose.onAllNodesWithText("Fútbol",substring=false).assertCountEquals(0)
        compose.onNodeWithContentDescription("Fútbol").assertExists()
        compose.onNodeWithTag("sport-SOCCER").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("sport-SOCCER").assertIsSelected()
        compose.onNodeWithText("Fútbol",substring=false).assertExists()
        compose.onAllNodesWithText("NFL",substring=false).assertCountEquals(0)
        screenshot("partidos-selector-iconos")
    }
    @Test @Config(qualifiers="en-rUS-w411dp-h891dp-xxhdpi")
    fun englishDeviceUsesEnglishNavigationAndGameLabels() {
        launch()
        compose.onNodeWithTag("nav-EVENTS").performClick()
        compose.onNodeWithText("Choose the game you want to track").assertIsDisplayed()
        compose.onNodeWithContentDescription("Choose date").assertIsDisplayed()
        compose.onNodeWithText("Add tracker").assertIsDisplayed()
        compose.onAllNodesWithText("En juego",substring=true).assertCountEquals(0)
        compose.onNodeWithText("NFL · Live · Q3 05:20").assertIsDisplayed()
        compose.onAllNodesWithText("Partidos",substring=false).assertCountEquals(0)
        screenshot("partidos-english")
    }
    @Test fun finishedGamesDisappearFromEventsWhileExistingResultsStayInHistory() {
        state.value=state.value.copy(events=listOf(
            EventView("finished",event("NFL","FINAL"),false),
            EventView("cancelled",event("NFL","CANCELLED"),false),
            EventView("scheduled",event("NFL","SCHEDULED"),false),
            EventView("live",event("NFL","LIVE"),false)))
        launch()
        compose.onNodeWithText("Partidos",substring=false).performClick()
        compose.onNodeWithTag("event-finished").assertDoesNotExist()
        compose.onNodeWithTag("event-cancelled").assertDoesNotExist()
        compose.onNodeWithTag("event-list").performScrollToNode(hasTestTag("event-scheduled"))
        compose.onNodeWithTag("event-scheduled").assertExists()
        compose.runOnIdle { state.value=state.value.copy(events=state.value.events.map { if(it.id=="live") it.copy(event=it.event.copy(status="FINAL")) else it }) }
        compose.onNodeWithTag("event-live").assertDoesNotExist()
        compose.runOnIdle { state.value=state.value.copy(events=state.value.events.filter { it.id!="scheduled" }) }
        compose.onNodeWithText("Sin partidos disponibles en esta jornada").assertExists()
        assertTrue(state.value.bets.any { it.id=="nfl-final" && it.status=="WON" })
    }
    @Test fun settledPicksAppearOnlyInHistoryAndRemainAccessible() {
        launch()
        compose.runOnIdle {
            state.value=state.value.copy(bets=listOf(bet("new-result","SOCCER","LOST",1.0).copy(
                event=event("SOCCER","FINAL"),updatedAt=today.toString()+"T23:00:00Z")),offline=true)
        }
        compose.onNodeWithTag("latest-result").assertDoesNotExist()
        compose.onNodeWithTag("bet-new-result").assertDoesNotExist()
        compose.onNodeWithTag("pinned-tab").assertDoesNotExist()
        compose.onNodeWithTag("latest-result").assertDoesNotExist()
        compose.onNodeWithTag("bet-new-result").assertDoesNotExist()
        compose.onNodeWithTag("history-tab").performClick()
        compose.onNodeWithTag("bet-new-result").assertIsDisplayed().performClick()
        compose.onNodeWithText("Perdida",substring=false).assertIsDisplayed()
        compose.onNodeWithText("Resultado final del proveedor",substring=false).assertExists()
    }
    @get:Rule val compose = createComposeRule()
    private val restoration by lazy { StateRestorationTester(compose) }
    private val today = LocalDate.now()
    private val sports = listOf(SportInfo("NFL", true, listOf("MONEYLINE", "PLAYER_RECEPTIONS")),
        SportInfo("SOCCER", true, listOf("TOTAL")), SportInfo("MLB", true, listOf("MONEYLINE", "SPREAD", "TOTAL", "TEAM_TOTAL")))
    private fun event(sport: String, status: String = "LIVE") = EventSnapshot(sport, "DEMO_UI", sport,
        if(status=="SCHEDULED")java.time.Instant.now().plusSeconds(3600).toString() else today.toString() + "T19:00:00Z", Team("home", when(sport) { "MLB" -> "Atlanta Braves"; "SOCCER" -> "Novorizontino"; else -> "Cleveland Browns" }),
        Team("away", when(sport) { "MLB" -> "Philadelphia Phillies"; "SOCCER" -> "Goiás"; else -> "Pittsburgh Steelers" }),
        if(status == "SCHEDULED") null else 6, if(status == "SCHEDULED") null else 2,
        status, if(status == "SCHEDULED") "NS" else if(sport == "MLB") "IN6" else "Q3", if(status == "SCHEDULED") null else if(sport == "MLB") "Entrada 6" else "05:20",
        today.toString() + "T19:00:00Z", "1", if(sport == "SOCCER") "Serie B" else sport, if(sport == "SOCCER") "Brasil" else "USA")
    private fun bet(id: String, sport: String, status: String, value: Double? = 3.0) = BetView(id, sport,
        event(sport, if(status in listOf("WON", "PUSH")) "FINAL" else "LIVE"),
        if(sport == "NFL") "PLAYER_RECEPTIONS" else "TOTAL", "OVER", if(status == "PUSH") 8.0 else if(sport == "MLB") 7.5 else 4.5,
        null, if(sport == "NFL") "1645" else null, if(sport == "NFL") "DK Metcalf" else null,
        null, null, null, status, value, null, today.toString() + "T19:00:00Z", false)
    private val initialBets = listOf(bet("nfl-live", "NFL", "LIVE_LOSING"), bet("mlb-live", "MLB", "LIVE_WINNING", 8.0),
        bet("soccer-wait", "SOCCER", "AWAITING_DATA", null), bet("nfl-final", "NFL", "WON", 5.0), bet("mlb-final", "MLB", "PUSH", 8.0))
    private val free = Plan("free", "FREE", "Gratis", "", true, null, null, 3, true, true)
    private val subscription = MySubscription(free, PlanAccess(3, true, true), 3, emptyList(), listOf(free), false, "test-account")
    private val state = mutableStateOf(AppState(loggedIn = true, username = "demo", bets = initialBets, sports = sports,
        events = listOf(EventView("nfl-game", event("NFL"), false)), subscription = subscription))
    private val created = mutableStateOf<BetView?>(null)
    private val selectedSports = mutableListOf<String>()
    private val deleted = mutableListOf<String>()
    private var refreshed = 0
    private var loggedOut = 0
    private val openedEvents = mutableListOf<String>()
    private var fontScale = 1f

    private fun launch() {
        restoration.setContent {
            MyBetLiveTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                DashboardScreen(state.value, emptyMap(), onRefresh = { refreshed++ },
                    onSport = { sport -> selectedSports += sport; state.value = state.value.copy(selectedSport = sport,
                        events = listOf(EventView("$sport-game", event(sport, "SCHEDULED"), false))) },
                    onDate = { date -> state.value = state.value.copy(date = date) }, onOpenEvent = { openedEvents += it },
                    onDelete = { id -> deleted += id; state.value = state.value.copy(bets = state.value.bets.filter { it.id != id }) },
                    onLogout = { loggedOut++ }, buy = {}, restore = {}, manage = {}, createdBet = created.value)
                }
            }
        }
    }
    private fun screenshot(name: String) {
        restoration.emulateSavedInstanceStateRestore()
        compose.mainClock.advanceTimeBy(250)
        compose.waitForIdle()
        compose.onNodeWithContentDescription(com.mybetlive.i18n.AppStrings.translate("Actualizar datos")).assertIsDisplayed()
        compose.runOnIdle {
            val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
            val view = activity.window.decorView
            view.forceLayout()
            view.measure(View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(view.height, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/navigation-screenshots/$name.png")
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun trackerFiltersStayInTrackersAndDoNotRequestAnEventSport() {
        launch()
        compose.onNodeWithTag("sport-ALL").assertIsSelected()
        compose.onNodeWithContentDescription("Fútbol").assertExists()
        compose.onAllNodesWithText("Fútbol",substring=false).assertCountEquals(0)
        compose.onNodeWithTag("bet-nfl-live").assertIsDisplayed()
        compose.onNodeWithTag("bet-nfl-final").assertDoesNotExist()
        screenshot("seguimientos-activos")
        compose.onNodeWithTag("sport-MLB").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("MLB",substring=false).assertExists()
        compose.onAllNodesWithText("Todos",substring=false).assertCountEquals(0)
        compose.onNodeWithTag("nav-TRACKERS").assertIsSelected()
        compose.onNodeWithTag("bet-mlb-live").assertIsDisplayed()
        compose.onNodeWithTag("bet-nfl-live").assertDoesNotExist()
        assertTrue(selectedSports.isEmpty())
        screenshot("seguimientos-selector-iconos")
        compose.onNodeWithTag("nav-EVENTS").performClick()
        compose.onNodeWithTag("sport-SOCCER").performClick()
        assertEquals(listOf("SOCCER"), selectedSports)
        compose.onNodeWithTag("nav-TRACKERS").performClick()
        compose.onNodeWithTag("sport-MLB").assertIsSelected()
        compose.onNodeWithTag("bet-mlb-live").assertIsDisplayed()
    }

    @Test fun historySeparatesFinalResultsAndDetailsPreserveTheFullSelection() {
        launch()
        compose.onNodeWithTag("sport-SOCCER").performClick()
        compose.onNodeWithTag("bet-soccer-wait").assertIsDisplayed()
        compose.onNodeWithTag("history-tab").performClick()
        compose.onNodeWithTag("bet-soccer-wait").assertDoesNotExist()
        compose.onNodeWithTag("sport-ALL").performClick()
        compose.onNodeWithTag("bet-nfl-final").assertIsDisplayed()
        compose.onNodeWithTag("bet-mlb-final").assertIsDisplayed()
        screenshot("historial")
        compose.onNodeWithTag("bet-nfl-final").performClick()
        compose.onNodeWithText("DK Metcalf · Recepciones · Over · 4.5").assertIsDisplayed()
        compose.onNodeWithText("Ganada", substring = false).assertIsDisplayed()
        screenshot("detalle-seguimiento")
        compose.onNodeWithContentDescription("Volver a seguimientos").performClick()
        compose.onNodeWithTag("history-tab").assertIsSelected()
    }

    @Test fun deletionIsOnlyAvailableInDetailsAndRequiresConfirmation() {
        launch()
        compose.onNodeWithText("Eliminar seguimiento").assertDoesNotExist()
        compose.onNodeWithTag("bet-nfl-live").performClick()
        compose.onNodeWithTag("bet-detail-list").performScrollToNode(hasText("Eliminar seguimiento"))
        compose.onNodeWithText("Eliminar seguimiento").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        assertTrue(deleted.isEmpty())
        compose.onNodeWithText("Eliminar seguimiento").performClick()
        compose.onNodeWithText("Eliminar", substring = false).performClick()
        assertEquals(listOf("nfl-live"), deleted)
        compose.onNodeWithTag("tracker-list").assertExists()
        compose.onNodeWithTag("bet-nfl-live").assertDoesNotExist()
    }

    @Test fun profileKeepsAccountPlanDeviceNotificationsAndLogoutTogether() {
        launch()
        compose.onNodeWithTag("nav-PROFILE").performClick()
        compose.onNodeWithText("demo").assertIsDisplayed()
        compose.onNodeWithText("Mi plan").assertIsDisplayed()
        compose.onNodeWithTag("sport-NFL").assertDoesNotExist()
        screenshot("perfil")
        compose.onNodeWithText("Configurar notificaciones").performScrollTo().performClick()
        val intent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(RuntimeEnvironment.getApplication().packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        compose.onNodeWithTag("profile-list").performScrollToNode(hasText("Cerrar sesión"))
        compose.onNodeWithText("Cerrar sesión").performClick()
        assertEquals(1, loggedOut)
    }

    @Test fun scheduledGamesKeepUnknownScoresAndOpenTheChosenEvent() {
        launch()
        compose.onNodeWithTag("nav-EVENTS").performClick()
        compose.onNodeWithText("Hoy", substring = false).assertDoesNotExist()
        compose.onNodeWithText("· local", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("sport-MLB").performClick()
        compose.onNodeWithText("Programado", substring = true).assertIsDisplayed()
        compose.onAllNodesWithText("—").assertCountEquals(2)
        compose.onNodeWithTag("team-logo-home").assertIsDisplayed()
        compose.onNodeWithTag("team-logo-away").assertIsDisplayed()
        screenshot("partidos-mlb")
        compose.onNodeWithText("Añadir seguimiento").performClick()
        assertEquals(listOf("MLB-game"), openedEvents)
        compose.onNodeWithContentDescription("Jornada siguiente").performClick()
        compose.runOnIdle { assertEquals(today.plusDays(1), state.value.date) }
        compose.onNodeWithText("Hoy", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Hoy", substring = false).performClick()
        compose.runOnIdle { assertEquals(today, state.value.date) }
        compose.onNodeWithText("Hoy", substring = false).assertDoesNotExist()
        compose.onNodeWithContentDescription("Elegir jornada").performClick()
        compose.onNodeWithText("Ver jornada").performClick()
        compose.runOnIdle { assertEquals(today, state.value.date) }
    }

    @Test fun savingAFinalGameSelectsHistorySoTheNewTrackerIsVisible() {
        launch()
        compose.onNodeWithTag("nav-PROFILE").performClick()
        compose.runOnIdle {
            val saved = initialBets.last().copy(id = "mlb-created")
            state.value = state.value.copy(bets = listOf(saved) + state.value.bets)
            created.value = saved
        }
        compose.onNodeWithTag("nav-TRACKERS").assertIsSelected()
        compose.onNodeWithTag("history-tab").assertIsSelected()
        compose.onNodeWithTag("sport-MLB").assertIsSelected()
        compose.onNodeWithTag("bet-mlb-created").assertIsDisplayed()
    }

    @Test fun moneylineAndNotificationNavigationOpenTheSelectedTracker() {
        val ml = initialBets[1].copy(id = "ml-notification", market = "MONEYLINE", selection = "AWAY", line = null,
            status = "LIVE_LOSING", currentValue = -4.0)
        state.value = state.value.copy(bets = listOf(ml) + initialBets)
        launch()
        compose.onNodeWithTag("nav-PROFILE").performClick()
        compose.runOnIdle { state.value = state.value.copy(notificationBetId = ml.id, notificationRequest = 1) }
        compose.onNodeWithTag("moneyline-score").assertIsDisplayed()
        compose.onNodeWithText("6 : 2").assertIsDisplayed()
        compose.onNodeWithText("Va perdiendo · desfavorable").assertIsDisplayed()
        screenshot("detalle-moneyline")
        compose.onNodeWithContentDescription("Volver a seguimientos").performClick()
        compose.onNodeWithTag("nav-PROFILE").performClick()
        compose.runOnIdle { state.value = state.value.copy(notificationRequest = 2) }
        compose.onNodeWithTag("moneyline-score").assertIsDisplayed()
    }

    @Test fun planPagesUseTheServerPermissionsAndLeavePurchasesDisabled() {
        state.value = state.value.copy(subscription = subscription.copy(access = PlanAccess(3, false, true)))
        launch()
        compose.onNodeWithTag("nav-PROFILE").performClick()
        compose.onNodeWithText("Ver mi plan").performClick()
        compose.onNodeWithText("Props de jugadores: no incluidos").assertIsDisplayed()
        screenshot("mi-plan-free")
        compose.onNodeWithText("Ver otros planes").performScrollTo().performClick()
        compose.onNodeWithText("Nuevos planes próximamente").assertIsDisplayed()
        compose.onNodeWithText("Suscripción aún no disponible").assertIsNotEnabled()
        screenshot("planes-no-disponibles")
    }

    @Test fun pullToRefreshAndAccessibleRefreshUseTheSameAction() {
        launch()
        compose.onNodeWithTag("refresh-content").performTouchInput { swipeDown(durationMillis = 700) }
        compose.waitForIdle()
        assertTrue(refreshed > 0)
        val before = refreshed
        compose.onNodeWithContentDescription("Actualizar datos").performClick()
        assertEquals(before + 1, refreshed)
    }

    @Test @Config(qualifiers = "es-rMX-w320dp-h640dp-xhdpi")
    fun smallScreensAndLargerTextKeepNavigationAndDateActionsAccessible() {
        fontScale = 1.3f
        launch()
        compose.onNodeWithTag("nav-TRACKERS").assertIsDisplayed()
        compose.onNodeWithTag("nav-EVENTS").assertIsDisplayed()
        compose.onNodeWithTag("nav-PROFILE").assertIsDisplayed()
        screenshot("seguimientos-pantalla-pequena")
        compose.onNodeWithContentDescription("Añadir seguimiento").performClick()
        compose.onNodeWithTag("nav-EVENTS").assertIsSelected()
        compose.onNodeWithContentDescription("Jornada siguiente").assertIsDisplayed().performClick()
        compose.onNodeWithText("Hoy", substring = false).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(today, state.value.date) }
        screenshot("partidos-pantalla-pequena")
        compose.onNodeWithTag("nav-PROFILE").performClick()
        compose.onNodeWithTag("profile-list").performScrollToNode(hasText("Cerrar sesión"))
        compose.onNodeWithText("Cerrar sesión").assertIsDisplayed()
    }

    private fun nba() = initialBets.first().copy(id="nba-prop",event=event("NBA").copy(home=Team("lal","Los Angeles Lakers"),
        away=Team("bos","Boston Celtics"),homeScore=98,awayScore=104,period="Q4",clock="05:42"),
        market="PLAYER_POINTS",playerName="LeBron James",line=24.5,currentValue=26.0,status="THRESHOLD_REACHED")
    private fun prepareNba(pinned: Boolean=true) {
        state.value=state.value.copy(bets=listOf(nba())+initialBets,sports=sports+SportInfo("NBA",true,listOf("MONEYLINE","PLAYER_POINTS")),
            pinnedBets=if(pinned) setOf("nba-prop") else emptySet())
    }
    @Test fun activeTrackersHaveAutomaticNotificationsWithoutAPinnedCategory() {
        prepareNba(); launch()
        compose.onNodeWithTag("pinned-tab").assertDoesNotExist()
        compose.onNodeWithTag("sport-NBA").performClick()
        compose.onNodeWithTag("bet-nba-prop").assertIsDisplayed().performClick()
        compose.onNodeWithTag("bet-detail-list").performScrollToNode(hasTestTag("notification-state"))
        compose.onNodeWithTag("notification-state").assertTextEquals("Notificación activa")
        compose.onNodeWithTag("unpin-notification").assertDoesNotExist()
        compose.onNodeWithTag("pin-notification").assertDoesNotExist()
        compose.onNodeWithContentDescription("Volver a seguimientos").performClick()
        compose.onNodeWithTag("active-tab").assertIsSelected()
        compose.onNodeWithTag("bet-nba-prop").assertIsDisplayed()
        screenshot("seguimientos-activos-unificados")
        compose.runOnIdle { assertEquals(6,state.value.bets.size) }
    }
    @Test fun disabledDeviceNotificationsShowSettingsAndNoFalsePinnedCount() {
        prepareNba();state.value=state.value.copy(deviceNotificationsEnabled=false);launch()
        compose.onNodeWithTag("pinned-tab").assertDoesNotExist()
        compose.onNodeWithTag("active-tab").performClick()
        compose.onNodeWithTag("sport-NBA").performClick()
        compose.onNodeWithTag("bet-nba-prop").performClick()
        compose.onNodeWithTag("bet-detail-list").performScrollToNode(hasText("Habilitar notificaciones en Android"))
        compose.onNodeWithTag("notification-state").assertTextEquals("Notificaciones bloqueadas en Android")
        compose.onNodeWithTag("pin-notification").assertDoesNotExist()
        compose.onNodeWithText("Habilitar notificaciones en Android").performClick()
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS,shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity.action)
    }
    @Test fun notificationPreviewShowsRealValuesAndPrivateVariantHidesTheSelection() {
        prepareNba();launch()
        compose.onNodeWithTag("sport-NBA").performClick()
        compose.onNodeWithTag("bet-nba-prop").performClick()
        compose.onNodeWithTag("bet-detail-list").performScrollToNode(hasTestTag("notification-preview"))
        compose.onNodeWithTag("notification-preview").performClick()
        compose.onNodeWithTag("preview-0").performClick()
        compose.onNode(hasText("26 / 24.5") and hasAnyAncestor(hasTestTag("notification-preview-dialog"))).assertIsDisplayed()
        previewScreenshot("nba-vista-previa-compacta")
        compose.onNodeWithTag("preview-1").performClick()
        previewScreenshot("nba-vista-previa-ampliada")
        compose.onNodeWithTag("preview-2").performClick()
        compose.onNodeWithTag("private-preview").assertIsDisplayed()
        compose.onNode(hasText("LeBron",substring=true) and hasAnyAncestor(hasTestTag("notification-preview-dialog"))).assertDoesNotExist()
        previewScreenshot("nba-vista-previa-privada")
        compose.onNodeWithText("Cerrar",substring=false).performClick()
    }
    @Test fun serverPlanWithoutNotificationsCannotPinEvenWhenAndroidAllowsThem() {
        prepareNba(false);state.value=state.value.copy(subscription=subscription.copy(access=PlanAccess(3,true,false)));launch()
        compose.onNodeWithTag("sport-NBA").performClick()
        compose.onNodeWithTag("bet-nba-prop").performClick()
        compose.onNodeWithTag("bet-detail-list").performScrollToNode(hasTestTag("notification-state"))
        compose.onNodeWithTag("notification-state").assertTextEquals("Notificaciones no incluidas en tu plan")
        compose.onNodeWithTag("pin-notification").assertDoesNotExist()
    }
    @Test @Config(qualifiers="es-rMX-w320dp-h640dp-xhdpi")
    fun nbaPendingAndFinalViewsRemainUsableWithLargeText() {
        fontScale=1.3f;prepareNba(false)
        state.value=state.value.copy(bets=listOf(nba().copy(status="PENDING",currentValue=null, event=nba().event.copy(homeScore=null,awayScore=null,status="SCHEDULED"))))
        launch()
        compose.onNodeWithTag("tracker-list").performScrollToNode(hasTestTag("bet-nba-prop"))
        compose.onNodeWithTag("bet-nba-prop").performClick()
        compose.onNodeWithTag("bet-detail-list").performScrollToNode(hasTestTag("notification-state"))
        compose.onNodeWithTag("notification-state").assertTextEquals("Notificación automática")
        compose.onNodeWithTag("pin-notification").assertDoesNotExist()
        compose.runOnIdle { state.value=state.value.copy(bets=listOf(nba().copy(status="WON",event=nba().event.copy(status="FINAL")))) }
        compose.onNodeWithTag("notification-state").assertTextEquals("Notificación de resultado")
        compose.onNodeWithTag("unpin-notification").assertDoesNotExist()
        screenshot("nba-resultado-pantalla-pequena")
    }
    private fun previewScreenshot(name: String) {
        compose.mainClock.advanceTimeBy(300);compose.waitForIdle()
        compose.runOnIdle {
            val view=WindowInspector.getGlobalWindowViews().last()
            view.forceLayout()
            view.measure(View.MeasureSpec.makeMeasureSpec(view.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(view.height,View.MeasureSpec.EXACTLY))
            view.layout(0,0,view.measuredWidth,view.measuredHeight)
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file=File("build/navigation-screenshots/$name.png");file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        }
    }
}
