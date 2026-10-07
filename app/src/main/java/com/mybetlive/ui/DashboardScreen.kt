package com.mybetlive.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.mybetlive.R
import com.mybetlive.data.*
import com.mybetlive.ads.AdsPolicy
import com.mybetlive.ads.NativeAdCard
import java.time.*
import java.time.format.DateTimeFormatter

private enum class Section(val label: String, val icon: Int) {
    TRACKERS("Seguimientos", R.drawable.ic_nav_trackers),
    EVENTS("Partidos", R.drawable.ic_nav_games),
    PROFILE("Perfil", R.drawable.ic_nav_profile)
}
private val finalStatuses = setOf("WON", "LOST", "PUSH", "VOID")
private fun BetView.settled() = status in finalStatuses
private enum class TrackerTab { ACTIVE, PINNED, HISTORY }

/** Navigation and tracker filters are local; only the event catalog selects a provider sport. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun DashboardScreen(
    state: AppState, prices: Map<String, StorePrice>, onRefresh: () -> Unit,
    onSport: (String) -> Unit, onDate: (LocalDate) -> Unit, onOpenEvent: (String) -> Unit,
    onDelete: (String) -> Unit, onLogout: () -> Unit, buy: (Plan) -> Unit,
    restore: () -> Unit, manage: () -> Unit, createdBet: BetView? = null,
    onUnpin: (String) -> Unit = {}, onPin: (String) -> Unit = {}
) {
    var section by rememberSaveable { mutableStateOf(Section.TRACKERS) }
    var trackerTab by rememberSaveable { mutableStateOf(TrackerTab.ACTIVE) }
    var trackerSport by rememberSaveable { mutableStateOf("ALL") }
    var selectedBetId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedBet = state.bets.find { it.id == selectedBetId }
    var handledNotification by rememberSaveable { mutableLongStateOf(-1) }
    LaunchedEffect(state.notificationRequest, state.bets.map { it.id }) {
        state.notificationBetId?.takeIf { id -> state.notificationRequest != handledNotification && state.bets.any { it.id == id } }?.let { id ->
            section = Section.TRACKERS; selectedBetId = id; handledNotification = state.notificationRequest
        }
    }
    LaunchedEffect(createdBet?.id) {
        createdBet?.let { section = Section.TRACKERS; trackerTab = if(it.settled()) TrackerTab.HISTORY else TrackerTab.ACTIVE; trackerSport = it.event.sport; selectedBetId = null }
    }
    BackHandler(enabled = selectedBetId != null || section != Section.TRACKERS) {
        if (selectedBetId != null) selectedBetId = null else section = Section.TRACKERS
    }
    val context = LocalContext.current
    val notificationSettings = {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        runCatching { context.startActivity(intent) }.getOrElse {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
        }
        Unit
    }
    Scaffold(containerColor = Ink, topBar = {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (selectedBet != null) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedBetId = null }) { AppIcon(R.drawable.ic_back, "Volver a seguimientos") }
                    Text("Detalle del seguimiento", fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else BrandLogo()
            IconButton(onClick = onRefresh, enabled = !state.busy) { AppIcon(R.drawable.ic_refresh, "Actualizar datos") }
        }
    }, bottomBar = {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compactLabels = maxWidth < 360.dp && LocalDensity.current.fontScale > 1.1f
        NavigationBar(containerColor = Ink, tonalElevation = 0.dp) {
            Section.entries.forEach { destination ->
                NavigationBarItem(selected = section == destination,
                    onClick = { section = destination; selectedBetId = null },
                    icon = { AppIcon(destination.icon) }, label = { Text(destination.label, maxLines = 1,
                        fontSize = if (compactLabels) 11.sp else 12.sp, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("nav-${destination.name}"),
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green,
                        indicatorColor = Green.copy(alpha = .13f), unselectedIconColor = Muted, unselectedTextColor = Muted))
            }
        }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.offline) Notice("Sin conexión · mostrando la última actualización guardada", Muted)
            state.error?.let { Notice(it, Red) }
            if (state.events.any { it.event.provider.startsWith("DEMO") } || state.bets.any { it.event.provider.startsWith("DEMO") })
                Notice("Demo · datos deportivos simulados", Green)
            PullToRefreshBox(isRefreshing = state.busy, onRefresh = { if (!state.busy) onRefresh() },
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("refresh-content")) {
                when {
                    selectedBet != null -> BetDetail(selectedBet, state.offline, state.busy,
                        state = state, onUnpin = { onUnpin(selectedBet.id) }, onPin = { onPin(selectedBet.id) }, onSettings=notificationSettings,
                        onDelete = { onDelete(selectedBet.id); selectedBetId = null })
                    section == Section.TRACKERS -> TrackersScreen(state, trackerTab, trackerSport,
                        onTab = { trackerTab = it }, onFilter = { trackerSport = it },
                        onAdd = { section = Section.EVENTS }, onOpen = { selectedBetId = it }, onSettings=notificationSettings)
                    section == Section.EVENTS -> EventsScreen(state, onSport, onDate, onOpenEvent)
                    else -> ProfileScreen(state, prices, buy, restore, manage, onLogout, onNotifications = notificationSettings)
                }
            }
        }
    }
}

@Composable private fun AppIcon(resource: Int, description: String? = null) {
    Icon(painterResource(resource), contentDescription = description, modifier = Modifier.size(24.dp))
}

@Composable private fun Notice(message: String, color: Color) {
    Text(message, color = color, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
}

@Composable private fun SectionHeading(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, color = Muted, fontSize = 13.sp) }
        }
        action?.invoke()
    }
}

@Composable private fun SportFilters(selected: String, sports: List<String>, onSelected: (String) -> Unit,
    enabled: Boolean = true, all: Boolean = false) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        (if (all) listOf("ALL") + sports else sports).forEach { sport ->
            FilterChip(selected = selected == sport, onClick = { onSelected(sport) }, enabled = enabled,
                label = { Text(if (sport == "ALL") "Todos" else sportLabel(sport)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Green.copy(alpha = .13f),
                    selectedLabelColor = Green, labelColor = Muted), modifier = Modifier.testTag("sport-$sport"))
        }
    }
}

@Composable private fun TrackersScreen(state: AppState, tab: TrackerTab, sport: String,
    onTab: (TrackerTab) -> Unit, onFilter: (String) -> Unit, onAdd: () -> Unit, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val bySport = state.bets.filter { sport == "ALL" || it.event.sport == sport }
    val activeCount = bySport.count { !it.settled() }
    val pinned=if(state.deviceNotificationsEnabled && state.subscription?.access?.pushNotifications == true) state.pinnedBets else emptySet()
    val pinnedCount=bySport.count { !it.settled() && it.id in pinned }
    val visible = bySport.filter { when(tab) { TrackerTab.ACTIVE -> !it.settled(); TrackerTab.PINNED -> !it.settled() && it.id in pinned; TrackerTab.HISTORY -> it.settled() } }
    val sports = (state.sports.filter { it.enabled }.map { it.sport } + state.bets.map { it.event.sport }).distinct()
    LazyColumn(Modifier.fillMaxSize().testTag("tracker-list"), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionHeading("Seguimientos", "El avance de tus selecciones", action = {
                FilledTonalIconButton(onClick = onAdd) { AppIcon(R.drawable.ic_add, "Añadir seguimiento") }
            })
        }
        state.subscription?.let { plan -> item { PlanUsage(plan) } }
        item {
            PrimaryTabRow(selectedTabIndex = tab.ordinal, containerColor = Ink, contentColor = Green) {
                listOf(TrackerTab.ACTIVE to "Activos ($activeCount)", TrackerTab.PINNED to "Anclados ($pinnedCount)",
                    TrackerTab.HISTORY to "Historial (${bySport.size-activeCount})").forEach { (destination, label) ->
                    Tab(selected=tab==destination, onClick={onTab(destination)}, text={Text(label,fontSize=12.sp,maxLines=2)},
                        selectedContentColor=Green, unselectedContentColor=Muted, modifier=Modifier.testTag("${destination.name.lowercase()}-tab"))
                }
            }
            SportFilters(sport, sports, onFilter, all = true)
        }
        if(tab==TrackerTab.PINNED) item {
            Text("Avisos activos en este dispositivo. Desanclar conserva el seguimiento y su historial.", color=Muted, fontSize=12.sp)
            if(!state.deviceNotificationsEnabled && state.subscription?.access?.pushNotifications==true)
                OutlinedButton(onClick=onSettings) { Text("Habilitar notificaciones en Android") }
        }
        if (visible.isEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(when(tab) { TrackerTab.HISTORY -> "Aún no hay resultados aquí"; TrackerTab.PINNED -> "No hay notificaciones ancladas"; else -> "No tienes seguimientos activos" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(when(tab) { TrackerTab.HISTORY -> "Los seguimientos ganados, perdidos, empatados o cancelados aparecerán en Historial."
                        TrackerTab.PINNED -> "Abre un seguimiento activo para anclar su aviso. Los partidos por comenzar aparecerán cuando haya datos."
                        else -> "Elige un partido y añade la selección que quieres seguir." }, color = Muted, fontSize = 14.sp)
                    Button(onClick = { if(tab==TrackerTab.PINNED) onTab(TrackerTab.ACTIVE) else onAdd() }) { Text(if(tab==TrackerTab.PINNED) "Ver seguimientos activos" else "Ver partidos") }
                }
            }
        }
        if(visible.isEmpty() && tab!=TrackerTab.HISTORY && state.subscription?.ads?.trackersEnabled==true) {
            item(key="tracker-empty-ad-${tab.name}") { NativeAdCard(state.subscription,state.offline) }
        }
        val adPositions = state.subscription?.ads?.takeIf { if(tab == TrackerTab.HISTORY) it.historyEnabled else it.trackersEnabled }
            ?.let { AdsPolicy.positions(visible.size,it.everyItems,it.maxAdsPerScreen) }.orEmpty()
        visible.forEachIndexed { index, bet ->
            item(key=bet.id) { CompactBetCard(bet, state.offline, bet.id in pinned) { onOpen(bet.id) } }
            if(index+1 in adPositions) item(key="tracker-ad-${tab.name}-$index") { NativeAdCard(state.subscription,state.offline) }
        }
        if (visible.isNotEmpty()) item {
            Text(if (tab==TrackerTab.HISTORY) "Resultados informativos según los datos deportivos. Toca una tarjeta para consultar el detalle."
                else "Las líneas alcanzadas son provisionales hasta el resultado final. Toca una tarjeta para ver el detalle.",
                color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable private fun CompactBetCard(bet: BetView, offline: Boolean, pinned: Boolean, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth().testTag("bet-${bet.id}"),
        colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (bet.market.startsWith("PLAYER_")) PlayerAvatar(bet.playerImageUrl, size = 40.dp)
                Text(bet.title(), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                AppIcon(R.drawable.ic_next)
            }
            if(pinned) Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(6.dp), modifier=Modifier.testTag("pinned-${bet.id}")) {
                Icon(painterResource(R.drawable.ic_pin),contentDescription=null,tint=Green,modifier=Modifier.size(14.dp))
                Text("Anclada en este dispositivo", color=Green, fontSize=11.sp)
            }
            MarketCardContent(bet, offline, compact = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TeamLogo(bet.event.home, size = 22.dp)
                    TeamLogo(bet.event.away, size = 22.dp)
                }
                Text("${sportLabel(bet.event.sport)} · ${bet.event.home.name} vs ${bet.event.away.name}",
                    modifier = Modifier.weight(1f), fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${bet.event.homeScore ?: "—"}:${bet.event.awayScore ?: "—"}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun shortStatus(status: String) = when (status) {
    "WON" -> "Ganada"; "LOST" -> "Perdida"; "PUSH" -> "Empate en la línea"; "VOID" -> "Cancelada"
    else -> statusLabels[status] ?: "Esperando información"
}

@Composable private fun BetDetail(bet: BetView, offline: Boolean, busy: Boolean,
    state: AppState, onUnpin: () -> Unit, onPin: () -> Unit, onSettings: () -> Unit, onDelete: () -> Unit) {
    var deleting by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().testTag("bet-detail-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (bet.market.startsWith("PLAYER_")) PlayerAvatar(bet.playerImageUrl, size = 56.dp)
                        Text(bet.title(), fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    MarketCardContent(bet, offline)
                }
            }
        }
        item { TrackingNotificationControls(bet,state,onPin,onUnpin,onSettings) }
        item { Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Scoreboard(bet.event); Text(kickoff(bet.event.startsAt), color = Muted, fontSize = 12.sp) } } }
        if (bet.stake != null || bet.decimalOdds != null || bet.bookmaker != null) item {
            Card { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Datos de tu apuesta", fontWeight = FontWeight.Bold)
                bet.stake?.let { Text("Monto: ${it.display()}") }
                bet.decimalOdds?.let { Text("Momio decimal: ${it.display()}") }
                bet.bookmaker?.let { Text("Casino: $it") }
            } }
        }
        item { Text(when (bet.event.sport) {
            "SOCCER" -> "90 minutos más compensación, sin prórroga ni penales."
            "MLB" -> "Juego completo con entradas extra, sin condición de pitcher ni primeras entradas."
            else -> "Partido completo, incluido tiempo extra."
        } + " El resultado informativo puede diferir de la liquidación del casino.", color = Muted, fontSize = 12.sp) }
        item { OutlinedButton(onClick = { deleting = true }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("Eliminar seguimiento") } }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Eliminar seguimiento") },
        text = { Text("Se quitará este seguimiento y su notificación.") },
        confirmButton = { TextButton(onClick = { deleting = false; onDelete() }, enabled = !busy) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancelar") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun EventsScreen(state: AppState, onSport: (String) -> Unit, onDate: (LocalDate) -> Unit, onOpenEvent: (String) -> Unit) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    var league by rememberSaveable(state.selectedSport, state.date.toString()) { mutableStateOf("ALL") }
    var showLeagues by remember { mutableStateOf(false) }
    val today = LocalDate.now(ZoneOffset.UTC)
    val leagues = state.events.mapNotNull { view -> view.event.leagueId?.let { id -> id to (view.event.leagueName ?: id) } }.distinctBy { it.first }
    val visible = state.events.filter { league == "ALL" || it.event.leagueId == league }
    LazyColumn(Modifier.fillMaxSize().testTag("event-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeading("Partidos", "Elige el encuentro que quieres seguir") }
        item { SportFilters(state.selectedSport, state.sports.filter { it.enabled }.map { it.sport }, onSport, enabled = !state.busy) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { onDate(state.date.minusDays(1)) }, enabled = !state.busy && state.date > today.minusDays(7)) { AppIcon(R.drawable.ic_back, "Jornada anterior") }
                TextButton(onClick = { showDate = true }, enabled = !state.busy,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Elegir jornada" }) {
                    Text(state.date.format(DateTimeFormatter.ofPattern("dd MMM")) + " · UTC")
                }
                TextButton(onClick = { onDate(today) }, enabled = !state.busy && state.date != today) { Text("Hoy") }
                IconButton(onClick = { onDate(state.date.plusDays(1)) }, enabled = !state.busy && state.date < today.plusDays(14)) { AppIcon(R.drawable.ic_next, "Jornada siguiente") }
            }
            if (state.selectedSport == "SOCCER" && leagues.size > 1) Box {
                OutlinedButton(onClick = { showLeagues = true }, modifier = Modifier.fillMaxWidth()) { Text(if (league == "ALL") "Todas las ligas" else leagues.find { it.first == league }?.second ?: "Todas las ligas") }
                DropdownMenu(expanded = showLeagues, onDismissRequest = { showLeagues = false }) {
                    (listOf("ALL" to "Todas las ligas") + leagues).forEach { (id, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { league = id; showLeagues = false })
                    }
                }
            }
        }
        if (visible.isEmpty()) item {
            Card { Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sin partidos en esta jornada", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(if (state.selectedSport == "SOCCER") "No hay partidos de las ligas habilitadas para esta fecha. Puedes elegir otra jornada."
                    else "Prueba otra fecha o desliza hacia abajo para actualizar.", color = Muted)
            } }
        }
        val adPositions = state.subscription?.ads?.takeIf { it.eventsEnabled }
            ?.let { AdsPolicy.positions(visible.size,it.everyItems,it.maxAdsPerScreen) }.orEmpty()
        visible.forEachIndexed { index, event ->
        item(key=event.id) {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.testTag("event-${event.id}")) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Scoreboard(event.event)
                    Text(kickoff(event.event.startsAt), color = Muted, fontSize = 12.sp)
                    Button(onClick = { onOpenEvent(event.id) }, enabled = !state.busy && event.event.status != "CANCELLED", modifier = Modifier.fillMaxWidth()) { Text("Añadir seguimiento") }
                }
            }
        }
        if(index+1 in adPositions) item(key="events-ad-$index") { NativeAdCard(state.subscription,state.offline) }
        }
    }
    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return date >= today.minusDays(7) && date <= today.plusDays(14)
                }
            })
        DatePickerDialog(onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = {
                picker.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }; showDate = false
            }, enabled = picker.selectedDateMillis != null && !state.busy) { Text("Ver jornada") } },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancelar") } }) { DatePicker(picker) }
    }
}
