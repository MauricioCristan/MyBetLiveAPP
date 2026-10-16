package com.mybetlive.ui
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.widget.TimePicker
import org.robolectric.shadows.ShadowDialog
import android.view.inspector.WindowInspector
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.mybetlive.data.*
import com.mybetlive.application.BankOperations
import com.mybetlive.bank.*
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="es-rMX-w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BankScreensTest {
 @get:Rule val compose=createComposeRule()
 private val bank=BankView("sample-bank","Personal","MXN","America/Mexico_City","2026-10-01T06:00:00Z","10000.00",version=1,capital="11250.00",reserved="1500.00",available="9750.00",profit="1250.00",roi="10.00",writable=true,updatedAt="2026-10-13T08:00:00Z")
 private val atlas=BankBet("atlas","sample-bank","2026-10-12T18:00:00Z","BACK","500.00","1.85",bookmaker="Caliente",result="WON",payout="925.00",exposure="500.00",profit="425.00",settledAt="2026-10-13T07:00:00Z",selections=listOf(BankLeg(title="Atlas · Más de 8.5 córners",sport="SOCCER")))
 private val summary=BankSummary(bank,BankAccess(true,null,"sample-bank"),listOf(atlas),listOf(BankPoint("2026-10-01","0"),BankPoint("2026-10-05","-300"),BankPoint("2026-10-12","1250")),listOf(BankDay("2026-10-12",1,"425")),3,40,"2026-10-13T08:00:00Z",mapOf("benefit" to "1250.00","roi" to "10.00","successPercent" to "60.00","bets" to "40"))
 private val model=BankViewModel(object:BankOperations{override fun owner()="sample";override fun cached()=BankCache();override fun remember(cache:BankCache){};override suspend fun <T> call(action:suspend(BankApi)->T):T=error("Screens do not write to an API")})
 private val state=BankState(listOf(bank),summary,BankPage(listOf(atlas),0,1,1,BankHistoryGrouping.entries.map{BankHistoryGroup(it.name,bankHistoryGroup(bankPlacedDate(atlas.placedAt,bank.timezone),it).toString(),1,0,"425.00","0.00","425.00")}+BankHistoryGroup("MONTH_WEEK",bankMonthWeekStart(bankPlacedDate(atlas.placedAt,bank.timezone)).toString(),1,0,"425.00","0.00","425.00")),observedAt=summary.observedAt,from="2026-10-01",to="2026-10-13")
 private fun launch(scale:Float=1f,offline:Boolean=false){
  com.mybetlive.i18n.AppStrings.resources=RuntimeEnvironment.getApplication().resources
  compose.setContent{CompositionLocalProvider(LocalDensity provides Density(2f,scale)){MyBetLiveTheme{
   DashboardScreen(AppState(loggedIn=true,username="sample"),emptyMap(),{},{},{},{},{},{},{},{},{},bankContent={_,_->BankScreen(state.copy(offline=offline),model,emptyList(),{})})
  }}}
  compose.onNodeWithTag("nav-BANK").performClick();compose.waitForIdle()
 }
 @Test fun summaryUsesConfirmedValuesAndItsPrimaryActionIsVisible(){
  launch();compose.onNodeWithText("Últimas apuestas").assertDoesNotExist();compose.onNodeWithText("Ver todas").assertDoesNotExist();compose.onNodeWithText("Atlas · Más de 8.5 córners").assertDoesNotExist();compose.onNodeWithText(bank.name).assertIsDisplayed();compose.onNodeWithText("Tu dinero, en un vistazo").assertDoesNotExist();compose.onNodeWithText("MXN · Privado").assertDoesNotExist();compose.onNodeWithText("Apuestas del periodo").assertIsDisplayed();compose.onNodeWithText("Progresión").assertIsDisplayed();compose.onNodeWithText("Personalizado").performScrollTo();compose.onNodeWithText("Personalizado").assertIsDisplayed()
  compose.onNodeWithText("Saldo actual").assertDoesNotExist()
  compose.onNodeWithText("Apuestas por fecha de registro, incluidas pendientes. Beneficio y ROI por liquidación confirmada.").assertDoesNotExist()
  assertTrue(compose.onNodeWithText("Progresión").getBoundsInRoot().bottom < compose.onNodeWithTag("bank-home-chart").getBoundsInRoot().top)
  compose.onNodeWithContentDescription("Añadir apuesta").assertIsDisplayed().assertIsEnabled();screenshot("bank-resumen")
  compose.onNodeWithContentDescription("Añadir apuesta").performClick();compose.onNodeWithText("Vincular seguimiento: Registro manual").assertExists()
  compose.onNodeWithText("Guardar apuesta").assertExists();screenshot("bank-registro")
 }
 @Test fun homeChartStartsCompactExpandsOnTapAndCustomPeriodLivesWithPresets(){
  launch()
  compose.onNodeWithText("Evolución del beneficio").assertDoesNotExist()
  val compact=compose.onNodeWithTag("bank-home-chart").getBoundsInRoot().let{it.bottom-it.top}
  compose.onNodeWithContentDescription("Ampliar gráfica").performClick();compose.waitForIdle()
  val expanded=compose.onNodeWithTag("bank-home-chart").getBoundsInRoot().let{it.bottom-it.top}
  assertTrue(expanded>compact)
  compose.onNodeWithContentDescription("Reducir gráfica").performClick();compose.waitForIdle()
  assertEquals(compact,compose.onNodeWithTag("bank-home-chart").getBoundsInRoot().let{it.bottom-it.top})
  compose.onNodeWithText("Personalizado").performScrollTo()
  compose.onNodeWithText("Personalizado").performClick()
  compose.onNodeWithText("Desde").assertExists();compose.onNodeWithText("Hasta").assertExists()
 }
 @Test fun bankNameOpensSelectorAndCreateBankForm(){
  launch()
  compose.onNodeWithText("Mi bank").assertDoesNotExist()
  compose.onNodeWithTag("bank-selector").assertIsDisplayed().performClick()
  compose.onNodeWithText("Personal · MXN").assertIsDisplayed()
  compose.onNodeWithText("Crear otro bank").performClick()
  compose.onNodeWithText("Nombre del bank").assertExists()
 }
 @Test @Config(qualifiers="es-rMX-w320dp-h640dp-xhdpi")
 fun betSectionsKeepDraftsAndSaveVisibleWithLargeText(){
  launch(1.3f)
  compose.onNodeWithContentDescription("Añadir apuesta").performClick()
  compose.onNodeWithText("Guardar apuesta").assertIsDisplayed()
  compose.onNodeWithText("Título de la apuesta").performTextInput("Madrid gana")
  compose.onNodeWithText("02 · Selección").performClick()
  compose.onNodeWithText("Título de la apuesta").assertDoesNotExist()
  compose.onNodeWithText("Madrid gana").assertExists()
  compose.onNodeWithText("02 · Selección").performClick()
  compose.onNodeWithText("Título de la apuesta").assertTextContains("Madrid gana")
  compose.onNodeWithText("Guardar apuesta").assertIsDisplayed()
  screenshot("bank-registro-opcion-4-texto-grande")
 }
 @Test fun bookmakerIsSelectedAndPersonalHouseCanBeRegistered(){
  launch();compose.onNodeWithContentDescription("Añadir apuesta").performClick()
  compose.onNodeWithText("01 · Datos del boleto").performClick()
  compose.onNodeWithText("Casa de apuestas: Sin especificar").performClick()
  compose.onNodeWithText("Caliente").performClick()
  compose.onNodeWithText("Casa de apuestas: Caliente").assertExists().performClick()
  compose.onNodeWithText("+ Añadir casa de apuestas").performClick()
  compose.onNodeWithText("Nombre de la casa").performTextInput("   ")
  compose.onNodeWithText("Guardar casa").performClick()
  compose.onNodeWithText("Introduce un nombre válido de hasta 80 caracteres.").assertExists()
  compose.onNodeWithText("Cancelar").performClick()
  compose.onNodeWithText("Casa de apuestas: Caliente").assertExists()
 }
 @Test fun dateCalendarConfirmsIsoDateAndCancelKeepsPreviousSelection(){
  var selected="2026-10-13"
  com.mybetlive.i18n.AppStrings.resources=RuntimeEnvironment.getApplication().resources
  compose.setContent{MyBetLiveTheme{BankDateField("Fecha",selected,{selected=it})}}
  compose.onNodeWithText("Fecha").performClick()
  compose.runOnIdle{
   val picker=ShadowDialog.getLatestDialog() as DatePickerDialog
   picker.datePicker.updateDate(2026,10,5)
   assertEquals(2026,picker.datePicker.year)
   assertEquals(10,picker.datePicker.month)
   assertEquals(5,picker.datePicker.dayOfMonth)
   assertTrue("Calendar confirmation should be enabled",picker.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled)
   picker.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
  }
  org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
  compose.waitForIdle()
  assertEquals("2026-11-05",selected)
  compose.onNodeWithText("Fecha").performClick()
  compose.runOnIdle{
   val picker=ShadowDialog.getLatestDialog() as DatePickerDialog
   picker.datePicker.updateDate(2027,0,1)
   picker.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
  }
  org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
  compose.waitForIdle()
  assertEquals("2026-11-05",selected)
 }
 @Test fun clockPickerConfirmsTwentyFourHourWireValue(){
  var selected="17:47"
  com.mybetlive.i18n.AppStrings.resources=RuntimeEnvironment.getApplication().resources
  compose.setContent{MyBetLiveTheme{BankTimeField("Hora",selected,{selected=it})}}
  compose.onNodeWithText("Hora").performClick()
  compose.runOnIdle{
   val dialog=ShadowDialog.getLatestDialog() as TimePickerDialog
   fun find(view:View):TimePicker?=if(view is TimePicker)view else if(view is ViewGroup)(0 until view.childCount).firstNotNullOfOrNull{find(view.getChildAt(it))}else null
   val picker=find(dialog.window!!.decorView)!!
   assertEquals(17,picker.hour);assertEquals(47,picker.minute)
   picker.hour=23;picker.minute=5
   dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
  }
  assertEquals("23:05",selected)
  assertEquals("2026-10-14T05:05:00Z",bankFormTime("2026-10-13",selected,"America/Mexico_City"))
 }
 @Test @Config(qualifiers="es-rMX-w320dp-h640dp-xhdpi")
 fun offlineRemainsReadableWithLargeTextAndDisablesWrites(){
  launch(1.3f,true);compose.onNodeWithText("Sin conexión · consulta de datos guardados").assertExists();compose.onNodeWithTag("bank-floating-navigation").assertIsDisplayed();compose.onNodeWithContentDescription("Añadir apuesta").assertIsDisplayed().assertIsNotEnabled();screenshot("bank-offline-texto-grande")
 }
 @Test fun floatingNavigationSwitchesSectionsAndReturnsToTheMainApp(){
  launch(offline=true)
  compose.onNodeWithTag("bank-nav-SUMMARY").assertIsSelected()
  compose.onNodeWithTag("nav-PROFILE").assertDoesNotExist()
  compose.onNodeWithTag("bank-nav-HISTORY").performClick().assertIsSelected()
  compose.onNodeWithText("Añadir apuesta").assertDoesNotExist()
  compose.onNodeWithContentDescription("Añadir apuesta").assertIsDisplayed().assertIsNotEnabled()
  compose.onNodeWithTag("bank-content").performScrollToNode(hasText("Historial por fecha de colocación de la apuesta"))
  compose.onNodeWithTag("bank-floating-navigation").assertIsDisplayed()
  compose.onNodeWithTag("bank-nav-STATISTICS").performClick().assertIsSelected()
  compose.onNodeWithText("Aciertos").assertIsDisplayed()
  compose.onNodeWithTag("bank-nav-SUMMARY").performClick().assertIsSelected()
  compose.onNodeWithTag("bank-home-chart").assertIsDisplayed()
  compose.onNodeWithText("Saldo actual").assertDoesNotExist()
  compose.onNodeWithText("Disponible").assertDoesNotExist()
  compose.onNodeWithContentDescription("Volver a seguimientos").performClick()
  compose.onNodeWithTag("nav-TRACKERS").assertIsSelected()
 }
 @Test fun historyUsesSearchQuickFiltersDateGroupsAndTheSingleFloatingAction(){
  launch()
  compose.onNodeWithTag("bank-nav-HISTORY").performClick()
  compose.onNodeWithText("Tus apuestas").assertIsDisplayed()
  compose.onNodeWithText("Anterior").assertDoesNotExist()
  compose.onNodeWithText("Siguiente").assertDoesNotExist()
  compose.onNodeWithText("Buscar apuesta").assertExists()
  compose.onNodeWithText("Todas · 40").assertIsDisplayed()
  compose.onNodeWithText("Pendientes · 3").assertIsDisplayed()
  compose.onNodeWithText("Filtros").assertDoesNotExist()
  compose.onNodeWithText("Aplicar filtros").assertDoesNotExist()
  compose.onNodeWithText("Deporte · código opcional").assertDoesNotExist()
  compose.onNodeWithText("Añadir apuesta").assertDoesNotExist()
  compose.onNodeWithContentDescription("Añadir apuesta").assertIsDisplayed()
  screenshot("bank-apuestas-opcion-3")
 }
 @Test fun monthAndWeekBoundariesUseTheBankTimezone(){
  assertEquals(java.time.LocalDate.of(2025,12,29),bankHistoryGroup(java.time.LocalDate.of(2026,1,4),BankHistoryGrouping.WEEK))
  assertEquals("+"+bankMoney("425.00","MXN"),bankSignedMoney("425.00","MXN"))
  assertEquals(bankMoney("-125.00","MXN"),bankSignedMoney("-125.00","MXN"))
  assertEquals(bankMoney("0.00","MXN"),bankSignedMoney("0.00","MXN"))
  assertEquals(java.time.LocalDate.of(2026,1,1),bankMonthWeekStart(java.time.LocalDate.of(2026,1,4)))
  assertEquals(java.time.LocalDate.of(2026,7,27),bankMonthWeekStart(java.time.LocalDate.of(2026,7,31)))
  assertEquals(java.time.LocalDate.of(2026,8,1),bankMonthWeekStart(java.time.LocalDate.of(2026,8,1)))
  assertEquals(java.time.LocalDate.of(2026,10,13),bankPlacedDate("2026-10-14T01:00:00Z",bank.timezone))
 }
 @Test fun onlyMonthsCollapseAndEachDayKeepsItsBetsInOneGrid(){
  val other=atlas.copy(id="second",selections=listOf(BankLeg(title="Segundo pick del día",sport="MLB")))
  val current=state.copy(offline=true,history=state.history!!.copy(content=listOf(atlas,other),totalElements=2))
  com.mybetlive.i18n.AppStrings.resources=RuntimeEnvironment.getApplication().resources
  compose.setContent{MyBetLiveTheme{BankScreen(current,model,emptyList(),{})}}
  compose.onNodeWithTag("bank-nav-HISTORY").performClick()
  compose.onNodeWithText("Agrupar por").assertDoesNotExist()
  val day=bankPlacedDate(atlas.placedAt,bank.timezone)
  val monthKey="bank-period-MONTH"+day.withDayOfMonth(1)
  val weekKey="bank-period-MONTH_WEEK"+bankMonthWeekStart(day)
  compose.onNodeWithTag("bank-content").performScrollToNode(hasTestTag(weekKey))
  compose.onNodeWithTag(weekKey).assertHasNoClickAction()
  compose.onNodeWithTag("bank-content").performScrollToNode(hasTestTag("bank-day-"+day))
  compose.onAllNodesWithTag("bank-day-"+day).assertCountEquals(1)
  compose.onNodeWithTag("bank-bet-atlas").assertExists();compose.onNodeWithTag("bank-bet-second").assertExists()
  compose.onNodeWithTag("bank-content").performScrollToNode(hasTestTag(monthKey))
  compose.onNodeWithTag(monthKey).performClick()
  compose.onNodeWithTag("bank-day-"+day).assertDoesNotExist();compose.onNodeWithTag(weekKey).assertDoesNotExist()
  compose.onNodeWithText("Ganado").assertDoesNotExist();compose.onNodeWithText("Perdido").assertDoesNotExist();compose.onNodeWithText("Balance").assertDoesNotExist()
  compose.onNodeWithText("+"+bankMoney("425.00",bank.currency)).assertIsDisplayed()
  compose.onNodeWithTag(monthKey).performClick()
  compose.onNodeWithTag("bank-content").performScrollToNode(hasTestTag(weekKey))
  compose.onNodeWithTag(weekKey).assertHasNoClickAction()
 }
 @Test fun scrollingLoadsTenMoreUntilAllTwentyOneAreVisible(){
  val rows=(1..21).map{atlas.copy(id="scroll-$it",selections=listOf(BankLeg(title="Pick de prueba $it",sport="SOCCER")))}
  val requested=mutableListOf<Int>()
  val api=java.lang.reflect.Proxy.newProxyInstance(BankApi::class.java.classLoader,arrayOf(BankApi::class.java)){_,method,args->when(method.name){
   "bookmakers"->emptyList<BankBookmaker>();"list"->listOf(bank);"summary"->summary
   "history"->{val page=args!![1] as Int;requested.add(page);BankPage(rows.drop(page*10).take(10),page,3,21)}
   else->error(method.name)
  }} as BankApi
  val pagingModel=BankViewModel(object:BankOperations{
   override fun owner()="scroll-fixture";override fun cached()=BankCache(listOf(bank),summary);override fun remember(cache:BankCache){}
   override suspend fun <T> call(action:suspend(BankApi)->T):T=action(api)
  })
  com.mybetlive.i18n.AppStrings.resources=RuntimeEnvironment.getApplication().resources
  compose.setContent{MyBetLiveTheme{val current by pagingModel.state.collectAsState();LaunchedEffect(Unit){pagingModel.reload()};BankScreen(current,pagingModel,emptyList(),{})}}
  compose.waitUntil(5000){!pagingModel.state.value.offline&&!pagingModel.state.value.busy}
  compose.onNodeWithTag("bank-nav-HISTORY").performClick()
  compose.waitForIdle();assertEquals(10,pagingModel.state.value.history!!.content.size)
  var swipes=0
  while(pagingModel.state.value.history!!.content.size<21&&swipes<30){
   compose.onNodeWithTag("bank-content").performTouchInput{swipeUp()}
   compose.waitForIdle();swipes++
  }
  assertEquals(21,pagingModel.state.value.history!!.content.size)
  compose.onNodeWithTag("bank-content").performScrollToNode(hasText("Has visto todas las apuestas"))
  compose.onNodeWithText("Has visto todas las apuestas").assertIsDisplayed()
  assertEquals(listOf(0,1,2),requested)
  compose.onNodeWithText("Anterior").assertDoesNotExist();compose.onNodeWithText("Siguiente").assertDoesNotExist()
 }
 @Test fun bankMenuKeepsToolsAndDoesNotExposeMovements(){
  launch(offline=true)
  compose.onNodeWithContentDescription("Opciones del bank").performClick()
  compose.onNodeWithText("Movimientos").assertDoesNotExist()
  compose.onNodeWithText("Compartir imagen").performClick()
  compose.onNodeWithText("Vista previa del resumen").assertIsDisplayed()
  compose.onNodeWithContentDescription("Resumen del bank para compartir").assertExists()
  compose.onNodeWithText("Cerrar").performClick()
  compose.onNodeWithContentDescription("Opciones del bank").performClick()
  compose.onNodeWithText("Herramientas").performClick()
  compose.onNodeWithText("Tu bank es privado").assertIsDisplayed()
  compose.onNodeWithTag("bank-floating-navigation").assertIsDisplayed()
 }
 @Test fun reportCanBePreviewedWithoutIncludingAccountIdentifiers(){
  val image=BankReport.render(RuntimeEnvironment.getApplication(),summary,state.from,state.to);assertEquals(1080,image.width);assertEquals(1440,image.height)
  val path=File("build/bank-screenshots/bank-resumen-compartido.png");path.parentFile!!.mkdirs();path.outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
 }
 @Test fun pendingDetailKeepsSuggestionSeparateAndSettlementActionVisible(){
  val pending=atlas.copy(result="PENDING",payout=null,profit=null,settledAt=null,suggestedResult="WON",potentialReturn="925.00")
  var edited=false
  com.mybetlive.i18n.AppStrings.resources=RuntimeEnvironment.getApplication().resources
  compose.setContent{MyBetLiveTheme{BankBetDetail(bank,pending,state,model,{},{edited=true},{})}}
  compose.onNodeWithText("Pendiente").assertIsDisplayed()
  compose.onNodeWithText("Retorno potencial").assertIsDisplayed()
  compose.onNodeWithText("Retorno recibido").assertDoesNotExist()
  compose.onNodeWithText("Beneficio neto").assertDoesNotExist()
  compose.onNodeWithText("Confirmar liquidación").assertIsDisplayed().assertIsEnabled()
  screenshot("bank-detalle-pendiente")
  compose.onNodeWithContentDescription("Acciones de la apuesta").performClick()
  compose.onNodeWithText("Modificar apuesta").performClick()
  assertTrue(edited)
 }
 @Test @Config(qualifiers="es-rMX-w320dp-h640dp-xhdpi")
 fun settledDetailShowsConfirmedProfitAndDisablesOfflineActionsWithLargeText(){
  com.mybetlive.i18n.AppStrings.resources=RuntimeEnvironment.getApplication().resources
  compose.setContent{CompositionLocalProvider(LocalDensity provides Density(2f,1.3f)){MyBetLiveTheme{
   BankBetDetail(bank,atlas.copy(suggestedResult="LOST"),state.copy(offline=true),model,{},{},{})
  }}}
  compose.onNodeWithText("Retorno recibido").assertExists()
  compose.onNodeWithText("Beneficio neto").assertExists()
  compose.onNodeWithText("Retorno potencial").assertDoesNotExist()
  compose.onNodeWithText("Propuesta deportiva: Perdida").assertDoesNotExist()
  compose.onNodeWithText("Corregir liquidación").assertIsDisplayed().assertIsNotEnabled()
  screenshot("bank-detalle-liquidado-texto-grande")
  compose.onNodeWithContentDescription("Acciones de la apuesta").performClick()
  compose.onNodeWithText("Modificar apuesta").assertIsNotEnabled()
  compose.onNodeWithText("Copiar como apuesta nueva").assertIsNotEnabled()
  compose.onNodeWithText("Eliminar apuesta").assertIsNotEnabled()
 }
 @Test fun combinedDetailKeepsEverySelectionAndOptionalNotes(){
  val combined=atlas.copy(format="COMBINED",notes="Boleto combinado",closingOdds="1.70",
   selections=atlas.selections+BankLeg(title="Lakers · Ganador",sport="NBA",odds="1.50"))
  com.mybetlive.i18n.AppStrings.resources=RuntimeEnvironment.getApplication().resources
  compose.setContent{MyBetLiveTheme{BankBetDetail(bank,combined,state,model,{},{},{})}}
  compose.onNodeWithText("Atlas · Más de 8.5 córners").assertExists()
  compose.onNodeWithText("Lakers · Ganador").assertExists()
  compose.onNodeWithText("Notas y detalles").performScrollTo().performClick()
  compose.onNodeWithText("Boleto combinado").assertExists()
  compose.onNodeWithText("Cuota de cierre").assertExists()
  compose.onNodeWithText("Corregir liquidación").assertIsDisplayed()
 }
 private fun screenshot(name:String){
  compose.mainClock.advanceTimeBy(200);compose.waitForIdle();compose.runOnIdle{
   val activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first()
   val view=WindowInspector.getGlobalWindowViews().lastOrNull()?:activity.window.decorView;val width=view.width.takeIf{it>0}?:780;val height=view.height.takeIf{it>0}?:1688
   view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height)
   val image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);view.draw(Canvas(image));val file=File("build/bank-screenshots/$name.png");file.parentFile!!.mkdirs();file.outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
  }
 }
}

