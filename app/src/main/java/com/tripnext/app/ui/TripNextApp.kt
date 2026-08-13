package com.tripnext.app.ui

import androidx.compose.foundation.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import com.tripnext.app.data.local.*
import com.tripnext.app.ui.theme.TripVisualTheme
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

private enum class AppTab(val label: String) { HOME("Início"), ITINERARY("Itinerário"), CHECKLIST("Checklist"), GROUP("Grupo"), SETTINGS("Ajustes") }

private val PrototypeCard: Color @Composable get() = MaterialTheme.colorScheme.surface
private val PrototypeBorder: Color @Composable get() = MaterialTheme.colorScheme.outline

@Composable
fun TripNextApp(viewModel: AppViewModel, startWithQuickExpense: Boolean, visualTheme: TripVisualTheme, onVisualThemeChange: (TripVisualTheme) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var quickExpense by remember { mutableStateOf(startWithQuickExpense) }
    var featureCenter by remember { mutableStateOf(false) }
    var planner by remember { mutableStateOf(false) }
    var copilot by remember { mutableStateOf(false) }
    var newTrip by remember { mutableStateOf(false) }
    var newEvent by remember { mutableStateOf(false) }
    var newChecklistItem by remember { mutableStateOf(false) }
    var hideValues by remember { mutableStateOf(false) }
    BackHandler(enabled = featureCenter) { featureCenter = false }
    BackHandler(enabled = planner) { planner = false }
    BackHandler(enabled = copilot) { copilot = false }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (!featureCenter && !planner && !copilot) PrototypeBottomBar(tab) { tab = it } },
        floatingActionButton = { if (!featureCenter && !planner && !copilot && state.activeTrip != null) FloatingActionButton(onClick = { quickExpense = true }, modifier = Modifier.size(52.dp), shape = CircleShape, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, "Nova despesa") } },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (copilot) AiPlannerScreen(state, viewModel.aiPlan, viewModel.aiProposal, viewModel.aiLoading, viewModel.aiError, viewModel.session != null, onClose = { copilot = false }, onGenerate = viewModel::generateAiPlan, onImport = viewModel::importAiProposal)
            else if (planner) TripPlannerScreen(state, onClose = { planner = false }, onFeatureCenter = { planner = false; featureCenter = true }, onAi = { planner = false; copilot = true })
            else if (featureCenter) TripFeatureCenter(onClose = { featureCenter = false }, onQuickExpense = { quickExpense = true }) else when (tab) {
                AppTab.HOME -> HomeScreen(state, viewModel::selectTrip, onFeatureCenter = { featureCenter = true }, onPlanner = { planner = true }, onNewTrip = { newTrip = true }, hideValues = hideValues)
                AppTab.ITINERARY -> ItineraryScreen(state, onAdd = { newEvent = true })
                AppTab.CHECKLIST -> ChecklistScreen(state, viewModel::toggleChecklist, onAdd = { newChecklistItem = true })
                AppTab.GROUP -> GroupScreen()
                AppTab.SETTINGS -> SettingsScreen(hideValues, { hideValues = it }, visualTheme, onVisualThemeChange, viewModel.session, viewModel.sessionLoading, viewModel.sessionError, viewModel.syncStatus, viewModel.syncConflicts.size, viewModel::login, viewModel::register, viewModel::logout, viewModel::syncNow, viewModel::resolveSyncConflicts)
            }
        }
    }
    if (quickExpense) QuickExpenseDialog(onDismiss = { quickExpense = false }) { amount, category, description -> viewModel.addExpense(amount, category, description); quickExpense = false }
    if (newTrip) NewTripSheet(onDismiss = { newTrip = false }) { name, destination, start, end, budget -> viewModel.createTrip(name, destination, start, end, budget); newTrip = false }
    if (newEvent) NewItineraryEventSheet(state.activeTrip, onDismiss = { newEvent = false }) { title, location, date, time, type -> viewModel.addItinerary(title, location, date, time, type); newEvent = false }
    if (newChecklistItem) NewChecklistItemSheet(onDismiss = { newChecklistItem = false }) { name, category -> viewModel.addChecklist(name, category); newChecklistItem = false }
}

private fun tabIcon(tab: AppTab) = when (tab) { AppTab.HOME -> Icons.Default.Home; AppTab.ITINERARY -> Icons.Default.CalendarMonth; AppTab.CHECKLIST -> Icons.Default.CheckCircle; AppTab.GROUP -> Icons.Default.Group; AppTab.SETTINGS -> Icons.Default.Settings }

@Composable private fun PrototypeBottomBar(selected: AppTab, select: (AppTab) -> Unit) {
    Box(Modifier.fillMaxWidth().background(Color.Transparent).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .96f), shadowElevation = 10.dp, border = BorderStroke(1.dp, PrototypeBorder.copy(alpha = .7f))) {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.SpaceAround) {
                AppTab.entries.forEach { tab ->
                    val active = selected == tab
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(if (active) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent).clickable { select(tab) }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(tabIcon(tab), tab.label, Modifier.size(20.dp), tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable private fun HomeScreen(state: AppUiState, select: (String) -> Unit, onFeatureCenter: () -> Unit, onPlanner: () -> Unit, onNewTrip: () -> Unit, hideValues: Boolean) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        state.activeTrip?.let { trip ->
            item { HomeHeader(trip.name, onNewTrip) }
            item { BoardingPass(trip) }
            item { PlanningProgressCard(onPlanner) }
            item { BudgetCard(state, hideValues) }
            item { QuickTravelActions(onFeatureCenter) }
            state.itinerary.minByOrNull { it.startsAt }?.let { event -> item { NextCommitmentCard(event) } }
            if (state.categoryBudgets.isNotEmpty()) item {
                SectionTitle("Categorias")
                val order = listOf(ExpenseCategory.ACCOMMODATION, ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD, ExpenseCategory.ACTIVITIES, ExpenseCategory.INSURANCE, ExpenseCategory.GIFTS, ExpenseCategory.DOCUMENTS, ExpenseCategory.UNEXPECTED)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { state.categoryBudgets.sortedBy { order.indexOf(it.category) }.forEach { budget -> CategoryBudgetRow(budget, state.spentByCategory.firstOrNull { it.category == budget.category }?.amountMinor ?: 0, hideValues) } }
            }
            item { SectionTitle("Últimas despesas") }
            if (state.expenses.isEmpty()) item { EmptyCard("Nenhuma despesa ainda. Use + para registrar a primeira.") }
            val prototypeOrder = listOf("Hotel Alfama", "Trem LIS → Porto", "Jantar Time Out Market", "Seguro viagem (apólice)", "Passagem GRU-LIS (parcela 1/6)")
            item { ExpensesCard(state.expenses.filter { it.description in prototypeOrder }.sortedBy { prototypeOrder.indexOf(it.description) }, hideValues) }
        } ?: item { if (state.loading) EmptyCard("Carregando viagens…") else EmptyTripState(onNewTrip) }
        if (state.trips.size > 1) item {
            SectionTitle("Outras viagens")
            state.trips.filter { it.id != state.activeTrip?.id }.forEach { trip -> TextButton(onClick = { select(trip.id) }) { Text("${trip.name} · ${trip.destination}") } }
        }
    }
}

@Composable private fun EmptyTripState(onNewTrip: () -> Unit) {
    Column(Modifier.fillMaxWidth().heightIn(min = 620.dp).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(82.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f), RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.TravelExplore, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary) }
        Text("Comece pela próxima viagem", Modifier.padding(top = 22.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Defina destino, datas e orçamento. O TripNext cria a estrutura inicial para você planejar roteiro, reservas, documentos e mala.", Modifier.padding(top = 9.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Button(onClick = onNewTrip, Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp)) { Icon(Icons.Default.AddLocationAlt, null); Spacer(Modifier.width(8.dp)); Text("Planejar nova viagem") }
    }
}

@Composable private fun HomeHeader(tripName: String, onNewTrip: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Boa viagem,", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Jefferson", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(tripName, style = MaterialTheme.typography.labelMedium, color = Color(0xFF78716C))
        }
        FilledTonalIconButton(onClick = onNewTrip, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.AddLocationAlt, "Planejar nova viagem") }
    }
}

@Composable private fun PlanningProgressCard(onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, PrototypeBorder)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary) }
                Column(Modifier.weight(1f).padding(horizontal = 11.dp)) { Text("Plano da viagem", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium); Text("6 de 9 etapas encaminhadas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("67%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { .67f }, Modifier.fillMaxWidth().padding(top = 13.dp).height(6.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Próxima decisão", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Definir roteiro do Dia 4", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

private data class PlanningStage(val title: String, val detail: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val done: Boolean, val urgent: Boolean = false)

@Composable private fun TripPlannerScreen(state: AppUiState, onClose: () -> Unit, onFeatureCenter: () -> Unit, onAi: () -> Unit) {
    val stages = listOf(
        PlanningStage("Destino e datas", "Lisboa e Porto · 12–20 Out", Icons.Default.Place, true),
        PlanningStage("Orçamento", "Total e 8 categorias definidos", Icons.Default.AccountBalanceWallet, state.categoryBudgets.isNotEmpty()),
        PlanningStage("Transporte", "Voos e trem adicionados", Icons.Default.Flight, state.itinerary.any { it.type == ItineraryType.FLIGHT }),
        PlanningStage("Hospedagem", "Lisboa confirmada · revisar Porto", Icons.Default.Hotel, true, true),
        PlanningStage("Documentos", "Passaporte, seguro e comprovantes", Icons.Default.Badge, state.checklist.any { it.category == ChecklistCategory.DOCUMENTS }),
        PlanningStage("Roteiro diário", "3 dias completos · 6 dias pendentes", Icons.Default.Route, false, true),
        PlanningStage("Reservas", "Organize confirmações e parcelas", Icons.Default.ConfirmationNumber, true),
        PlanningStage("Viagem em grupo", "Convidar e definir rateios", Icons.Default.Group, false),
        PlanningStage("Mala e preparação", "${state.checklist.count { it.checked }}/${state.checklist.size} itens prontos", Icons.Default.Luggage, false)
    )
    val done = stages.count { it.done }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Voltar") }; Column(Modifier.weight(1f)) { Text("Planejar viagem", style = MaterialTheme.typography.titleLarge); Text(state.activeTrip?.name.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; FilledTonalButton(onClick = onAi, contentPadding = PaddingValues(horizontal = 12.dp)) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Copiloto") } } }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) { Column(Modifier.fillMaxWidth().padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("PREPARAÇÃO GERAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer); Text("$done/${stages.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer) }; Text("Sua viagem está tomando forma", Modifier.padding(top = 7.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer); LinearProgressIndicator(progress = { done.toFloat() / stages.size }, Modifier.fillMaxWidth().padding(top = 13.dp).height(7.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .15f)) } }
        }
        item { SectionTitle("Prioridades agora") }
        item { PlannerAlert("Completar o roteiro", "Defina manhã, tarde e noite dos dias 15–20 Out", Color(0xFFF59E0B)); Spacer(Modifier.height(8.dp)); PlannerAlert("Revisar hospedagem no Porto", "Confirme endereço, check-in e política de cancelamento", Color(0xFFEA580C)) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { SectionTitle("Etapas do planejamento"); TextButton(onClick = onFeatureCenter) { Text("Todos os módulos") } } }
        items(stages) { stage ->
            Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, PrototypeBorder)) { Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).background(if (stage.done) Color(0x1F0D9488) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(stage.icon, null, Modifier.size(20.dp), tint = if (stage.done) Color(0xFF0D9488) else MaterialTheme.colorScheme.onSurfaceVariant) }; Column(Modifier.weight(1f).padding(horizontal = 11.dp)) { Text(stage.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); Text(stage.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; when { stage.done -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF0D9488)); stage.urgent -> StatusBadge("Atenção", Color(0xFFF59E0B)); else -> Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable private fun PlannerAlert(title: String, detail: String, color: Color) { Surface(shape = RoundedCornerShape(14.dp), color = color.copy(alpha = .10f), border = BorderStroke(1.dp, color.copy(alpha = .35f))) { Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PriorityHigh, null, tint = color); Column(Modifier.padding(start = 10.dp)) { Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }

@Composable private fun AiPlannerScreen(state: AppUiState, result: String, proposal: AiTravelProposal?, loading: Boolean, error: String?, sessionAvailable: Boolean, onClose: () -> Unit, onGenerate: () -> Unit, onImport: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Voltar") }; Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .14f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) }; Column(Modifier.padding(start = 11.dp)) { Text("Copiloto de viagem", style = MaterialTheme.typography.titleLarge); Text("Gemini + pesquisa Google", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        item { Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text("PLANO PARA ${state.activeTrip?.name.orEmpty().uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer); Text("Sugestões atuais para decidir mais rápido", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer); Text("O Gemini considera destino, datas, orçamento, roteiro e checklist já cadastrados.", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f)) } } }
        item { Column { Button(onClick = onGenerate, enabled = !loading && sessionAvailable, modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(if (result.isBlank()) "Gerar proposta" else "Atualizar proposta") }; if (!sessionAvailable) Text("Entre em Ajustes para usar o copiloto com segurança. A chave da IA fica somente no servidor.", Modifier.padding(top = 7.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) } }
        if (loading) item { Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp); Column(Modifier.padding(start = 12.dp)) { Text("Pesquisando e organizando…", style = MaterialTheme.typography.bodyMedium); Text("Isso pode levar alguns segundos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        error?.let { item { PlannerAlert("Não foi possível gerar", it, MaterialTheme.colorScheme.error) } }
        if (result.isNotBlank()) item { SectionTitle("Plano sugerido pela IA") }
        if (result.isNotBlank()) item { Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, PrototypeBorder)) { SelectionContainer { Text(result, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) } } }
        proposal?.let { plan ->
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ProposalCount(Icons.Default.Route, "${plan.itinerary.size} eventos", Modifier.weight(1f)); ProposalCount(Icons.Default.Checklist, "${plan.checklist.size} tarefas", Modifier.weight(1f)); ProposalCount(Icons.Default.PieChart, "${plan.budgets.size} categorias", Modifier.weight(1f)) } }
            if (plan.itinerary.isNotEmpty()) item { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { SectionTitle("O que será adicionado ao roteiro"); plan.itinerary.forEach { suggestion -> Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surface) { Column(Modifier.fillMaxWidth().padding(12.dp)) { Text("Dia ${suggestion.dayOffset + 1} · ${suggestion.time} · ${suggestion.title}", fontWeight = FontWeight.Medium); Text(listOf(suggestion.location, suggestion.reason, money(suggestion.estimatedCostMinor)).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } }
            if (plan.checklist.isNotEmpty()) item { Text("Checklist: ${plan.checklist.joinToString(" · ") { it.name }}", style = MaterialTheme.typography.bodySmall) }
            if (plan.budgets.isNotEmpty()) item { Text("Orçamento sugerido: ${plan.budgets.joinToString(" · ") { "${it.category} ${it.percent}%" }}", style = MaterialTheme.typography.bodySmall) }
            item { Button(onClick = onImport, Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Confirmar e adicionar ao planejamento") } }
            if (plan.sources.isNotEmpty()) item { Text("Fontes: ${plan.sources.joinToString(" · ") { "${it.title} (${it.checkedAt})" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item { Text("Revise preços, horários, exigências migratórias e disponibilidade diretamente nas fontes antes de reservar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun ProposalCount(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier) { Surface(modifier, shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary); Text(text, Modifier.padding(top = 5.dp), style = MaterialTheme.typography.labelSmall) } } }

@Composable private fun QuickTravelActions(onFeatureCenter: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Planejamento")
            TextButton(onClick = onFeatureCenter, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Ver tudo") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction(Icons.Default.ConfirmationNumber, "Reservas", Color(0xFFF59E0B), Modifier.weight(1f), onFeatureCenter)
            QuickAction(Icons.Default.Savings, "Metas", Color(0xFF0D9488), Modifier.weight(1f), onFeatureCenter)
            QuickAction(Icons.Default.GridView, "Mais", Color(0xFF6366F1), Modifier.weight(1f), onFeatureCenter)
        }
    }
}

@Composable private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = PrototypeCard, border = BorderStroke(1.dp, PrototypeBorder)) {
        Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(34.dp).background(color.copy(alpha = .14f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(18.dp), tint = color) }
            Text(label, Modifier.padding(top = 7.dp), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun NextCommitmentCard(event: ItineraryEventEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Próximo compromisso")
        Surface(shape = RoundedCornerShape(16.dp), color = PrototypeCard, border = BorderStroke(1.dp, PrototypeBorder)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Color(0x1F0D9488), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(eventIcon(event.type), null, tint = Color(0xFF2DD4BF)) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(event.location.ifBlank { "Lisboa & Porto" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(time(event.startsAt), style = MaterialTheme.typography.labelMedium, color = Color(0xFFF8FAFC))
                    Text("12 OUT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF78716C))
                }
            }
        }
    }
}

@Composable private fun BoardingPass(trip: TripEntity) {
    val month = date(trip.startDate).format(DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("pt-BR"))).replaceFirstChar { it.uppercase() }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("PRÓXIMA VIAGEM", color = Color(0xFF78716C), style = MaterialTheme.typography.labelSmall); Text(trip.name, color = Color(0xFF1C1917), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; Icon(Icons.Default.Flight, null, tint = Color(0xFFE11D48), modifier = Modifier.rotate(45f)) }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(trip.destination, color = Color(0xFF57534E), style = MaterialTheme.typography.labelMedium); Text("${date(trip.startDate).dayOfMonth} — ${date(trip.endDate).dayOfMonth} $month 2026", color = Color(0xFF57534E), style = MaterialTheme.typography.labelMedium) }
            DashedSeparator(Modifier.padding(top = 12.dp, bottom = 7.dp))
            Barcode()
        }
    }
}

@Composable private fun Barcode() {
    Canvas(Modifier.fillMaxWidth().height(24.dp)) { var x = 0f; val widths = listOf(2,1,3,1,2,4,1,2,3,1,1,4); var i = 0; while (x < size.width) { val width = widths[i % widths.size].dp.toPx(); drawRect(Color(0xB31C1917), topLeft = Offset(x, 0f), size = androidx.compose.ui.geometry.Size(width, size.height)); x += width + 3.dp.toPx(); i++ } }
}

@Composable private fun DashedSeparator(modifier: Modifier = Modifier) { Canvas(modifier.fillMaxWidth().height(1.dp)) { drawLine(Color(0xFF78716C), Offset.Zero, Offset(size.width, 0f), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))) } }

@Composable private fun BudgetCard(state: AppUiState, hideValues: Boolean) {
    val trip = state.activeTrip ?: return; val pct = if (trip.totalBudgetMinor > 0) state.spentMinor.toFloat() / trip.totalBudgetMinor else 0f
    Card(colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccountBalanceWallet, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(7.dp)); Text("Orçamento da viagem", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.weight(1f)); Text(if (hideValues) "—" else "${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelMedium) }; Text(if (hideValues) "R$ •••• / R$ ••••" else "${prototypeMoney(state.spentMinor)} / ${prototypeMoney(trip.totalBudgetMinor)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp)); LinearProgressIndicator(progress = { if (hideValues) 0f else pct.coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(6.dp), color = MaterialTheme.colorScheme.primary, trackColor = PrototypeBorder) } }
}

@Composable private fun FeatureCenterEntry(onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(Color(0x1F6366F1), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.GridView, null, tint = Color(0xFF6366F1)) }
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) { Text("Central da viagem", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); Text("Reservas, metas, transporte, importação e widgets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0x1A0D9488)) { Text("8 módulos", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF0D9488)) }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class TripFeature(val id: String, val title: String, val subtitle: String, val status: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color, val items: List<Pair<String, String>>)

@Composable private fun TripFeatureCenter(onClose: () -> Unit, onQuickExpense: () -> Unit) {
    val modules = remember { listOf(
        TripFeature("expenses", "Despesas", "Histórico, filtros e captura", "5 registros", Icons.Default.ReceiptLong, Color(0xFFE11D48), listOf("Hotel Alfama" to "R$ 620,00", "Trem LIS → Porto" to "R$ 95,00", "Jantar Time Out Market" to "R$ 68,00")),
        TripFeature("goals", "Metas de economia", "Aportes antes da viagem", "68%", Icons.Default.Savings, Color(0xFF0D9488), listOf("Portugal 2026" to "R$ 5.800 / R$ 8.500")),
        TripFeature("reservations", "Reservas parceladas", "Passagens, hotéis e carro", "2 ativas", Icons.Default.ConfirmationNumber, Color(0xFFF59E0B), listOf("Passagem GRU-LIS" to "1/6 parcelas", "Hotel Alfama" to "Confirmada")),
        TripFeature("transport", "Transporte", "Quilometragem, combustível e custo", "Sem veículo", Icons.Default.DirectionsCar, Color(0xFF6366F1), emptyList()),
        TripFeature("calendar", "Agenda completa", "Calendário mensal e próximos eventos", "6 eventos", Icons.Default.CalendarMonth, Color(0xFF0D9488), listOf("Voo GRU → LIS" to "12 Out · 09:40", "Check-in Hotel Alfama" to "12 Out · 22:15")),
        TripFeature("imports", "Importar reservas", "PDF, texto, OCR e revisão", "Pronto", Icons.Default.UploadFile, Color(0xFF6366F1), emptyList()),
        TripFeature("widgets", "Widgets", "Contagem, agenda e orçamento", "1 disponível", Icons.Default.Widgets, Color(0xFFF59E0B), emptyList()),
        TripFeature("sync", "Sincronização", "Fila offline e estado remoto", "Offline pronto", Icons.Default.Sync, Color(0xFFEA580C), emptyList())
    ) }
    var selected by remember { mutableStateOf<TripFeature?>(null) }
    BackHandler(enabled = selected != null) { selected = null }
    if (selected != null) {
        FeatureModuleScreen(selected!!, onBack = { selected = null }, onQuickExpense = onQuickExpense)
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Voltar") }; Column { Text("Central da viagem", style = MaterialTheme.typography.titleLarge); Text("Módulos, dados e sincronização", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        items(modules, key = { it.id }) { module -> Card(Modifier.fillMaxWidth().clickable { selected = module }, colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(module.color.copy(alpha = .12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(module.icon, null, Modifier.size(21.dp), tint = module.color) }; Column(Modifier.weight(1f).padding(horizontal = 11.dp)) { Text(module.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); Text(module.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusBadge(module.status, module.color); Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
    }
}

@Composable private fun FeatureModuleScreen(module: TripFeature, onBack: () -> Unit, onQuickExpense: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }; Box(Modifier.size(40.dp).background(module.color.copy(alpha = .12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(module.icon, null, tint = module.color) }; Column(Modifier.padding(start = 11.dp)) { Text(module.title, style = MaterialTheme.typography.titleLarge); Text(module.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        if (module.items.isEmpty()) item { Card(colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(14.dp)) { Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(module.icon, null, Modifier.size(30.dp), tint = module.color); Text("Nenhum item cadastrado", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium); Text("Adicione o primeiro registro deste módulo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = onQuickExpense, Modifier.padding(top = 16.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Adicionar") } } } }
        items(module.items) { item -> PrototypePanel { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(item.first, style = MaterialTheme.typography.bodyMedium); Text(module.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(item.second, style = MaterialTheme.typography.labelMedium, color = module.color) } } }
    }
}

@Composable private fun StatusBadge(text: String, color: Color) { Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = .12f)) { Text(text, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = color) } }

@Composable private fun CategoryBudgetRow(budget: CategoryBudgetEntity, spent: Long, hideValues: Boolean) { Card(colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(12.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(categoryIcon(budget.category), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(8.dp)); Text(categoryLabel(budget.category), style = MaterialTheme.typography.bodyMedium) }; Text(if (hideValues) "R$ •••• / R$ ••••" else "${money(spent)} / ${money(budget.limitMinor)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { if (hideValues || budget.limitMinor == 0L) 0f else (spent.toFloat() / budget.limitMinor).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(6.dp), color = categoryColor(budget.category), trackColor = PrototypeBorder) } } }
@Composable private fun ExpenseRow(expense: ExpenseEntity, currency: String) { val displayDate = when (expense.description) { "Seguro viagem (apólice)" -> "01 Set"; "Passagem GRU-LIS (parcela 1/6)" -> "01 Ago"; else -> "${date(expense.date).dayOfMonth} Out" }; Card(colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(expense.description.ifBlank { categoryLabel(expense.category) }, style = MaterialTheme.typography.bodyMedium); Text(displayDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(money(expense.amountMinor), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun ExpensesCard(expenses: List<ExpenseEntity>, hideValues: Boolean) { Card(colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(12.dp)) { Column { expenses.forEachIndexed { index, expense -> val displayDate = when (expense.description) { "Seguro viagem (apólice)" -> "01 Set"; "Passagem GRU-LIS (parcela 1/6)" -> "01 Ago"; else -> "${date(expense.date).dayOfMonth} Out" }; Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(expense.description, style = MaterialTheme.typography.bodyMedium); Text(displayDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(if (hideValues) "R$ ••••" else money(expense.amountMinor), style = MaterialTheme.typography.labelMedium, color = Color(0xFFD6D3D1)) }; if (index < expenses.lastIndex) HorizontalDivider(color = PrototypeBorder) } } } }

@Composable private fun ItineraryScreen(state: AppUiState, onAdd: () -> Unit) {
    var selectedDay by remember(state.activeTrip?.id) { mutableStateOf(state.activeTrip?.let { date(it.startDate) } ?: LocalDate.now()) }
    val trip = state.activeTrip
    Column(Modifier.fillMaxSize()) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Itinerário", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(trip?.name.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; FilledTonalIconButton(onClick = onAdd, enabled = trip != null) { Icon(Icons.Default.Add, "Adicionar compromisso") } }; LazyRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { trip?.let { t -> val start = date(t.startDate); val count = minOf(31, (date(t.endDate).toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(1)); items(count) { index -> val day = start.plusDays(index.toLong()); FilterChip(selected = day == selectedDay, onClick = { selectedDay = day }, label = { Text("Dia ${index + 1} · ${day.dayOfMonth} Out") }, colors = FilterChipDefaults.filterChipColors(containerColor = PrototypeCard, selectedContainerColor = MaterialTheme.colorScheme.primary, labelColor = MaterialTheme.colorScheme.onSurfaceVariant, selectedLabelColor = MaterialTheme.colorScheme.onPrimary), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = day == selectedDay, borderColor = PrototypeBorder, selectedBorderColor = MaterialTheme.colorScheme.primary)) } } } }
        val events = state.itinerary.filter { date(it.startsAt) == selectedDay }
        if (events.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhum compromisso neste dia", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else LazyColumn(Modifier.drawBehind { val x = 16.dp.toPx() + 29.dp.toPx(); drawLine(Color(0xFF1E293B), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()) }, contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { items(events, key = { it.id }) { event -> Row(verticalAlignment = Alignment.Top) { Column(Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(time(event.startsAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Box(Modifier.padding(top = 6.dp).size(32.dp).background(PrototypeCard, CircleShape).border(1.dp, Color(0xFF334155), CircleShape), contentAlignment = Alignment.Center) { Icon(eventIcon(event.type), null, Modifier.size(15.dp), tint = Color(0xFF0D9488)) } }; Spacer(Modifier.width(12.dp)); Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(13.dp)) { Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); if (event.location.isNotBlank()) Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Text(event.location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } } }
    }
}

@Composable private fun ChecklistScreen(state: AppUiState, toggle: (String) -> Unit, onAdd: () -> Unit) {
    val done = state.checklist.count { it.checked }
    val categories = listOf(ChecklistCategory.DOCUMENTS, ChecklistCategory.CLOTHES, ChecklistCategory.ELECTRONICS)
    val namesOrder = listOf("Passaporte", "Reserva do hotel impressa", "Seguro viagem", "Cartão de vacina", "Casaco leve", "Sapato confortável", "2 mudas extras", "Roupa de banho", "Carregador", "Adaptador de tomada", "Power bank")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Checklist da mala", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("$done/${state.checklist.size} empacotado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; FilledTonalIconButton(onClick = onAdd, enabled = state.activeTrip != null) { Icon(Icons.Default.Add, "Adicionar item") } } }
        item { LinearProgressIndicator(progress = { if (state.checklist.isEmpty()) 0f else done.toFloat() / state.checklist.size }, Modifier.fillMaxWidth().height(5.dp), color = MaterialTheme.colorScheme.secondary, trackColor = PrototypeBorder) }
        categories.forEach { category ->
            val group = state.checklist.filter { it.category == category }.sortedBy { namesOrder.indexOf(it.name) }
            if (group.isNotEmpty()) {
                item { Row(verticalAlignment = Alignment.CenterVertically) { Icon(checklistIcon(category), null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(6.dp)); SectionTitle(checklistLabel(category)) } }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(12.dp)) {
                        Column { group.forEachIndexed { index, item -> Row(Modifier.fillMaxWidth().clickable { toggle(item.id) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(item.checked, { toggle(item.id) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0D9488))); Text(item.name, style = MaterialTheme.typography.bodyMedium, textDecoration = if (item.checked) TextDecoration.LineThrough else null, color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface) }; if (index < group.lastIndex) HorizontalDivider(color = PrototypeBorder) } }
                    }
                }
            }
        }
    }
}

@Composable private fun TripsScreen(state: AppUiState, select: (String) -> Unit, open: () -> Unit) { LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("Minhas viagens", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; items(state.trips, key = { it.id }) { trip -> Card(Modifier.fillMaxWidth().clickable { select(trip.id); open() }) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(horizontal = 12.dp).weight(1f)) { Text(trip.name, fontWeight = FontWeight.Bold); Text(trip.destination, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight, null) } } } } }

@Composable private fun GroupScreen() {
    data class Person(val name: String, val role: String, val balance: Long)
    val people = listOf(Person("Você", "Organizador", 18_000), Person("Marina", "Editor", -9_500), Person("Diego", "Convidado", -8_500))
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Group, null, tint = Color(0xFF6366F1)); Spacer(Modifier.width(8.dp)); Text("Viagem em grupo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }; Text("Divisão de despesas entre participantes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp)) }
        item { SectionTitle("Participantes") }
        items(people) { person -> PrototypePanel { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(32.dp).background(PrototypeBorder, CircleShape), contentAlignment = Alignment.Center) { Text(person.name.first().toString(), fontWeight = FontWeight.Bold) }; Column(Modifier.padding(start = 10.dp)) { Text(person.name, style = MaterialTheme.typography.bodyMedium); Text(person.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Text((if (person.balance >= 0) "+" else "") + money(person.balance), color = if (person.balance >= 0) Color(0xFF0D9488) else Color(0xFFEA580C), style = MaterialTheme.typography.labelMedium) } } }
        item { SectionTitle("Acertos pendentes"); Spacer(Modifier.height(2.dp)) }
        item { SettlementPanel("Marina", "Você", 9_500) }
        item { SettlementPanel("Diego", "Você", 8_500) }
    }
}

@Composable private fun PrototypePanel(content: @Composable () -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(14.dp)) { Box(Modifier.fillMaxWidth().padding(12.dp)) { content() } } }
@Composable private fun SettlementPanel(from: String, to: String, amount: Long) { PrototypePanel { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Text(from, style = MaterialTheme.typography.bodyMedium); Icon(Icons.Default.ArrowForward, null, Modifier.padding(horizontal = 8.dp).size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Text(to, style = MaterialTheme.typography.bodyMedium) }; Text(money(amount), color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelMedium) } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SettingsScreen(hideValues: Boolean, onHideValuesChange: (Boolean) -> Unit, visualTheme: TripVisualTheme, onVisualThemeChange: (TripVisualTheme) -> Unit, session: com.tripnext.app.data.TripSession?, sessionLoading: Boolean, sessionError: String?, syncStatus: String, conflictCount: Int, login: (String, String, String) -> Unit, register: (String, String, String, String) -> Unit, logout: () -> Unit, sync: () -> Unit, resolveConflicts: (Boolean) -> Unit) {
    var notifications by remember { mutableStateOf(true) }
    var showThemes by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineSmall) }
        item { Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(54.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) { Text(session?.name?.take(1)?.uppercase() ?: "L", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge) }; Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(session?.name ?: "Modo local", style = MaterialTheme.typography.titleMedium); Text(session?.email ?: "Seu plano permanece neste aparelho", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; sessionError?.let { Text(it, Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }; Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (session == null) Button({ showAuth = true }, Modifier.weight(1f)) { Icon(Icons.Default.Login, null); Spacer(Modifier.width(6.dp)); Text("Entrar") } else { Button(sync, Modifier.weight(1f), enabled = syncStatus != "Sincronizando…") { Icon(Icons.Default.Sync, null); Spacer(Modifier.width(6.dp)); Text("Sincronizar") }; OutlinedButton(logout) { Text("Sair") } } }; Text(syncStatus, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        if (conflictCount > 0) item { Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer) { Column(Modifier.padding(16.dp)) { Text("Conflito de sincronização", style = MaterialTheme.typography.titleMedium); Text("O plano mudou neste aparelho e em outro lugar. Escolha qual versão continuar usando.", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodySmall); Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ resolveConflicts(false) }, Modifier.weight(1f)) { Text("Usar servidor") }; Button({ resolveConflicts(true) }, Modifier.weight(1f)) { Text("Manter o meu") } } } } }
        item { ModernSettingsGroup("CONTA") { ModernSettingsRow(Icons.Default.Person, "Seu perfil", session?.name ?: "Não conectado") { if (session == null) showAuth = true }; HorizontalDivider(color = PrototypeBorder); ModernSettingsRow(Icons.Default.Group, "Viagem compartilhada", "Participantes") } }
        item { ModernSettingsGroup("PLANEJAMENTO") { ModernSettingsRow(Icons.Default.AccountBalanceWallet, "Orçamento da viagem", "R$ 8.500"); HorizontalDivider(color = PrototypeBorder); ModernSettingsRow(Icons.Default.CalendarMonth, "Agenda", "6 eventos") } }
        item { ModernSettingsGroup("APARÊNCIA") { ModernSettingsRow(Icons.Default.Palette, "Tema", visualTheme.label) { showThemes = true }; HorizontalDivider(color = PrototypeBorder); ModernSwitchRow(Icons.Default.VisibilityOff, "Ocultar valores", hideValues, onHideValuesChange) } }
        item { ModernSettingsGroup("CAPTURA RÁPIDA") { ModernSwitchRow(Icons.Default.Notifications, "Notificações", notifications) { notifications = it }; HorizontalDivider(color = PrototypeBorder); ModernSettingsRow(Icons.Default.PushPin, "Fixar atalho", "Tela inicial") } }
        item { ModernSettingsGroup("DADOS") { ModernSettingsRow(Icons.Default.Sync, "Sincronização", syncStatus) { if (session == null) showAuth = true else sync() }; HorizontalDivider(color = PrototypeBorder); ModernSettingsRow(Icons.Default.Security, "Segurança", if (session == null) "Somente local" else "Sessão ativa") } }
    }
    if (showThemes) ThemePickerSheet(visualTheme, onDismiss = { showThemes = false }) { onVisualThemeChange(it); showThemes = false }
    if (showAuth) AuthSheet(sessionLoading, sessionError, onDismiss = { showAuth = false }, login = login, register = register)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AuthSheet(loading: Boolean, error: String?, onDismiss: () -> Unit, login: (String, String, String) -> Unit, register: (String, String, String, String) -> Unit) {
    var create by remember { mutableStateOf(false) }; var apiUrl by remember { mutableStateOf("http://10.0.2.2:8787") }; var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) { Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) { Text(if (create) "Criar conta" else "Entrar no TripNext", style = MaterialTheme.typography.titleLarge); Text("O modo offline continua disponível sem conta.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedTextField(apiUrl, { apiUrl = it }, Modifier.fillMaxWidth(), label = { Text("Endereço da API") }, singleLine = true); if (create) OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Seu nome") }, singleLine = true); OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("E-mail") }, singleLine = true); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Senha (10+ caracteres)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true); error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }; Button(onClick = { if (create) register(apiUrl, name, email, password) else login(apiUrl, email, password) }, enabled = !loading && email.isNotBlank() && password.length >= 10 && (!create || name.length >= 2), modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(if (create) "Criar conta e entrar" else "Entrar") }; TextButton({ create = !create }, Modifier.align(Alignment.CenterHorizontally)) { Text(if (create) "Já tenho conta" else "Ainda não tenho conta") } } }
}

@Composable private fun ModernSettingsGroup(label: String, content: @Composable ColumnScope.() -> Unit) { Column { SectionTitle(label); Spacer(Modifier.height(7.dp)); Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) { Column(content = content) } } }
@Composable private fun ModernSettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 15.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface); Text(label, Modifier.weight(1f).padding(start = 11.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun ModernSwitchRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface); Text(label, Modifier.weight(1f).padding(start = 11.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); Switch(checked, onChange, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary)) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ThemePickerSheet(selected: TripVisualTheme, onDismiss: () -> Unit, onSelect: (TripVisualTheme) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 28.dp)) {
            Text("Escolha o visual", style = MaterialTheme.typography.titleLarge)
            Text("Temas do Finanza Next e o Embarque original", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            TripVisualTheme.entries.forEach { theme ->
                val colors = themePreview(theme)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onSelect(theme) }.background(if (theme == selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.width(76.dp).height(42.dp).clip(RoundedCornerShape(11.dp)).background(colors.first).padding(7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(colors.second, RoundedCornerShape(5.dp)))
                        Box(Modifier.width(12.dp).height(12.dp).background(colors.third, CircleShape))
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(theme.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); Text(themeDescription(theme), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (theme == selected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) else Icon(Icons.Default.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

private fun themePreview(theme: TripVisualTheme): Triple<Color, Color, Color> = when (theme) {
    TripVisualTheme.BOARDING -> Triple(Color(0xFF020617), Color(0xFF0F172A), Color(0xFFE11D48))
    TripVisualTheme.MODERN_DARK -> Triple(Color.Black, Color(0xFF1C1C1E), Color(0xFFF7F7FA))
    TripVisualTheme.MODERN_LIGHT -> Triple(Color(0xFFF2F2F7), Color.White, Color(0xFF1C1C1E))
    TripVisualTheme.CLASSIC_DARK -> Triple(Color(0xFF08090D), Color(0xFF12151E), Color(0xFFC8F55A))
    TripVisualTheme.CLASSIC_LIGHT -> Triple(Color(0xFFF0F4FF), Color.White, Color(0xFF3F7D00))
    TripVisualTheme.WEB_DARK -> Triple(Color(0xFF08090D), Color(0xFF1C202E), Color(0xFF5AF5C8))
    TripVisualTheme.WEB_LIGHT -> Triple(Color(0xFFF4F7EF), Color(0xFFF6F9F0), Color(0xFF3F7D00))
}
private fun themeDescription(theme: TripVisualTheme) = when (theme) { TripVisualTheme.BOARDING -> "Bilhete creme e rosa TripNext"; TripVisualTheme.MODERN_DARK, TripVisualTheme.MODERN_LIGHT -> "Minimalista e alto contraste"; TripVisualTheme.CLASSIC_DARK, TripVisualTheme.CLASSIC_LIGHT -> "Lima, teal e superfícies azuladas"; else -> "Paleta orgânica do Finanza Web" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun NewTripSheet(onDismiss: () -> Unit, save: (String, String, LocalDate, LocalDate, Long) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val start = runCatching { LocalDate.parse(startText, formatter) }.getOrNull()
    val end = runCatching { LocalDate.parse(endText, formatter) }.getOrNull()
    val enabled = when (step) { 0 -> name.isNotBlank() && destination.isNotBlank(); 1 -> start != null && end != null && !end.isBefore(start); else -> parseMoney(budget) > 0 }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 18.dp).padding(bottom = 22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .14f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.TravelExplore, null, tint = MaterialTheme.colorScheme.primary) }; Column(Modifier.weight(1f).padding(start = 11.dp)) { Text("Planejar nova viagem", style = MaterialTheme.typography.titleLarge); Text(listOf("Para onde você vai?", "Quando será a viagem?", "Defina um ponto de partida")[step], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusBadge("${step + 1}/3", MaterialTheme.colorScheme.primary) }
            LinearProgressIndicator(progress = { (step + 1) / 3f }, Modifier.fillMaxWidth().padding(vertical = 17.dp).height(5.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            when (step) {
                0 -> Column(verticalArrangement = Arrangement.spacedBy(11.dp)) { OutlinedTextField(name, { name = it.take(50) }, Modifier.fillMaxWidth(), label = { Text("Nome da viagem") }, placeholder = { Text("Ex.: Férias em Portugal") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Luggage, null) }); OutlinedTextField(destination, { destination = it.take(60) }, Modifier.fillMaxWidth(), label = { Text("Destino") }, placeholder = { Text("Ex.: Lisboa e Porto") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Place, null) }) }
                1 -> Column(verticalArrangement = Arrangement.spacedBy(11.dp)) { OutlinedTextField(startText, { startText = formatDateInput(it) }, Modifier.fillMaxWidth(), label = { Text("Data de ida") }, placeholder = { Text("12/10/2026") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), leadingIcon = { Icon(Icons.Default.FlightTakeoff, null) }); OutlinedTextField(endText, { endText = formatDateInput(it) }, Modifier.fillMaxWidth(), label = { Text("Data de volta") }, placeholder = { Text("20/10/2026") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), leadingIcon = { Icon(Icons.Default.FlightLand, null) }); if (start != null && end != null && !end.isBefore(start)) Text("${java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1} dias para planejar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary) }
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(budget, { budget = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(12) }, Modifier.fillMaxWidth(), label = { Text("Orçamento total") }, prefix = { Text("R$ ") }, placeholder = { Text("8.500,00") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)); Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primaryContainer) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer); Text("$destination · $startText — $endText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f)); Text("Ao criar, o TripNext prepara orçamento por categoria e checklist inicial.", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer) } } }
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) { OutlinedButton(onClick = { if (step == 0) onDismiss() else step-- }, Modifier.weight(1f)) { Text(if (step == 0) "Cancelar" else "Voltar") }; Button(onClick = { if (step < 2) step++ else save(name, destination, start!!, end!!, parseMoney(budget)) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text(if (step < 2) "Continuar" else "Criar plano") } }
        }
    }
}

private fun formatDateInput(raw: String): String { val digits = raw.filter(Char::isDigit).take(8); return buildString { digits.forEachIndexed { index, c -> if (index == 2 || index == 4) append('/'); append(c) } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun NewItineraryEventSheet(trip: TripEntity?, onDismiss: () -> Unit, save: (String, String, LocalDate, java.time.LocalTime, ItineraryType) -> Unit) {
    var title by remember { mutableStateOf("") }; var location by remember { mutableStateOf("") }
    val defaultDate = trip?.let { date(it.startDate) } ?: LocalDate.now()
    var dateText by remember { mutableStateOf(defaultDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var timeText by remember { mutableStateOf("09:00") }; var type by remember { mutableStateOf(ItineraryType.ACTIVITY) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val parsedDate = runCatching { LocalDate.parse(dateText, formatter) }.getOrNull()
    val parsedTime = runCatching { java.time.LocalTime.parse(timeText) }.getOrNull()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) { Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 18.dp).padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text("Novo compromisso", style = MaterialTheme.typography.titleLarge); Text("Adicione voos, reservas, passeios e deslocamentos ao roteiro.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(title, { title = it.take(70) }, Modifier.fillMaxWidth(), label = { Text("Título") }, singleLine = true)
        OutlinedTextField(location, { location = it.take(100) }, Modifier.fillMaxWidth(), label = { Text("Local") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(dateText, { dateText = formatDateInput(it) }, Modifier.weight(1f), label = { Text("Data") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); OutlinedTextField(timeText, { timeText = formatTimeInput(it) }, Modifier.weight(.7f), label = { Text("Hora") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(listOf(ItineraryType.ACTIVITY, ItineraryType.FLIGHT, ItineraryType.CHECK_IN, ItineraryType.RESTAURANT, ItineraryType.TRANSFER)) { option -> FilterChip(selected = type == option, onClick = { type = option }, label = { Text(itineraryTypeLabel(option)) }) } }
        Button(onClick = { save(title.trim(), location.trim(), parsedDate!!, parsedTime!!, type) }, enabled = title.isNotBlank() && parsedDate != null && parsedTime != null, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Adicionar ao itinerário") }
    } }
}

private fun formatTimeInput(raw: String): String { val digits = raw.filter(Char::isDigit).take(4); return buildString { digits.forEachIndexed { index, c -> if (index == 2) append(':'); append(c) } } }
private fun itineraryTypeLabel(type: ItineraryType) = when (type) { ItineraryType.FLIGHT -> "Voo"; ItineraryType.CHECK_IN -> "Check-in"; ItineraryType.CHECK_OUT -> "Check-out"; ItineraryType.RESTAURANT -> "Restaurante"; ItineraryType.TRANSFER -> "Deslocamento"; ItineraryType.ACTIVITY -> "Passeio"; else -> "Outro" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun NewChecklistItemSheet(onDismiss: () -> Unit, save: (String, ChecklistCategory) -> Unit) {
    var name by remember { mutableStateOf("") }; var category by remember { mutableStateOf(ChecklistCategory.DOCUMENTS) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) { Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 18.dp).padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Novo item", style = MaterialTheme.typography.titleLarge); OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("O que precisa preparar?") }, singleLine = true); LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(ChecklistCategory.entries) { option -> FilterChip(selected = category == option, onClick = { category = option }, label = { Text(checklistLabel(option)) }) } }; Button(onClick = { save(name.trim(), category) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Adicionar ao checklist") } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun QuickExpenseDialog(onDismiss: () -> Unit, save: (Long, ExpenseCategory, String) -> Unit) {
    var step by remember { mutableIntStateOf(0) }; var amount by remember { mutableStateOf("") }; var category by remember { mutableStateOf<ExpenseCategory?>(null) }; var description by remember { mutableStateOf("") }
    val availableCategories = ExpenseCategory.entries.filter { it !in listOf(ExpenseCategory.SHOPPING, ExpenseCategory.OTHER) }
    val enabled = when (step) { 0 -> parseMoney(amount) > 0; 1 -> category != null; else -> description.isNotBlank() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PrototypeCard, scrimColor = Color.Black.copy(alpha = .58f), shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 18.dp).padding(bottom = 18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Nova despesa", style = MaterialTheme.typography.titleLarge); Text(listOf("Informe o valor", "Escolha a categoria", "Descreva o gasto")[step], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusBadge("${step + 1}/3", Color(0xFFE11D48)) }
            LinearProgressIndicator(progress = { (step + 1) / 3f }, Modifier.fillMaxWidth().padding(vertical = 16.dp).height(4.dp), color = Color(0xFFE11D48), trackColor = PrototypeBorder)
            when (step) {
                0 -> OutlinedTextField(value = amount, onValueChange = { amount = it.filter { char -> char.isDigit() || char == ',' || char == '.' }.take(12) }, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center), prefix = { Text("R$ ", style = MaterialTheme.typography.titleLarge) }, placeholder = { Text("0,00", style = MaterialTheme.typography.titleLarge) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                1 -> LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) { items(availableCategories) { item -> FilterChip(selected = item == category, onClick = { category = item }, label = { Text(categoryLabel(item)) }, leadingIcon = { Icon(categoryIcon(item), null, Modifier.size(17.dp)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = categoryColor(item).copy(alpha = .24f), selectedLabelColor = Color.White), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = item == category, borderColor = PrototypeBorder, selectedBorderColor = categoryColor(item))) } }
                else -> OutlinedTextField(value = description, onValueChange = { description = it.take(80) }, modifier = Modifier.fillMaxWidth(), label = { Text("Descrição") }, placeholder = { Text("Ex.: almoço no centro") }, supportingText = { Text("${description.length}/80") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { if (step > 0) step-- else onDismiss() }, Modifier.weight(1f)) { Text(if (step > 0) "Voltar" else "Cancelar") }; Button(onClick = { if (step < 2) step++ else save(parseMoney(amount), category ?: ExpenseCategory.OTHER, description.trim()) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text(if (step < 2) "Continuar" else "Salvar") } }
        }
    }
}

@Composable private fun SectionTitle(text: String) = Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
@Composable private fun EmptyCard(text: String) = Card(colors = CardDefaults.cardColors(containerColor = PrototypeCard), border = BorderStroke(1.dp, PrototypeBorder), shape = RoundedCornerShape(12.dp)) { Text(text, Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
private fun date(epoch: Long) = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()
private fun time(epoch: Long) = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
private fun money(minor: Long, currency: String = "BRL") = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).apply { this.currency = Currency.getInstance(currency) }.format(minor / 100.0)
private fun prototypeMoney(minor: Long) = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).apply { maximumFractionDigits = 0; minimumFractionDigits = 0 }.format(minor / 100.0)
private fun parseMoney(value: String): Long { val normalized = if (value.contains(',')) value.replace(".", "").replace(',', '.') else value; return ((normalized.toDoubleOrNull() ?: 0.0) * 100).toLong() }
private fun categoryLabel(value: ExpenseCategory) = when (value) { ExpenseCategory.ACCOMMODATION -> "Hospedagem"; ExpenseCategory.TRANSPORT -> "Transporte"; ExpenseCategory.FOOD -> "Alimentação"; ExpenseCategory.ACTIVITIES -> "Passeios"; ExpenseCategory.INSURANCE -> "Seguro viagem"; ExpenseCategory.GIFTS -> "Compras e presentes"; ExpenseCategory.DOCUMENTS -> "Documentos e vistos"; ExpenseCategory.UNEXPECTED -> "Imprevistos"; ExpenseCategory.SHOPPING -> "Compras"; ExpenseCategory.OTHER -> "Outros" }
private fun categoryIcon(value: ExpenseCategory) = when (value) { ExpenseCategory.ACCOMMODATION -> Icons.Default.Hotel; ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar; ExpenseCategory.FOOD -> Icons.Default.Restaurant; ExpenseCategory.ACTIVITIES -> Icons.Default.PhotoCamera; ExpenseCategory.INSURANCE -> Icons.Default.Shield; ExpenseCategory.GIFTS -> Icons.Default.CardGiftcard; ExpenseCategory.DOCUMENTS -> Icons.Default.Description; ExpenseCategory.UNEXPECTED -> Icons.Default.Error; ExpenseCategory.SHOPPING -> Icons.Default.ShoppingBag; ExpenseCategory.OTHER -> Icons.Default.MoreHoriz }
private fun categoryColor(value: ExpenseCategory) = when (value) { ExpenseCategory.ACCOMMODATION -> Color(0xFF0D9488); ExpenseCategory.TRANSPORT -> Color(0xFFF59E0B); ExpenseCategory.FOOD -> Color(0xFFE11D48); ExpenseCategory.ACTIVITIES, ExpenseCategory.GIFTS -> Color(0xFF6366F1); ExpenseCategory.INSURANCE, ExpenseCategory.DOCUMENTS -> Color(0xFF16A34A); ExpenseCategory.UNEXPECTED -> Color(0xFFEA580C); else -> Color(0xFFE11D48) }
private fun checklistLabel(value: ChecklistCategory) = when (value) { ChecklistCategory.DOCUMENTS -> "Documentos"; ChecklistCategory.CLOTHES -> "Roupas"; ChecklistCategory.ELECTRONICS -> "Eletrônicos"; ChecklistCategory.HYGIENE -> "Higiene"; ChecklistCategory.MEDICINES -> "Medicamentos"; ChecklistCategory.OTHER -> "Outros" }
private fun eventIcon(value: ItineraryType) = when (value) { ItineraryType.FLIGHT -> Icons.Default.Flight; ItineraryType.CHECK_IN, ItineraryType.CHECK_OUT -> Icons.Default.Hotel; ItineraryType.ACTIVITY -> Icons.Default.PhotoCamera; ItineraryType.RESTAURANT -> Icons.Default.Restaurant; ItineraryType.TRANSFER -> Icons.Default.Route; else -> Icons.Default.Event }
private fun checklistIcon(value: ChecklistCategory) = when (value) { ChecklistCategory.DOCUMENTS -> Icons.Default.Description; ChecklistCategory.CLOTHES -> Icons.Default.Checkroom; ChecklistCategory.ELECTRONICS -> Icons.Default.Bolt; else -> Icons.Default.Luggage }
