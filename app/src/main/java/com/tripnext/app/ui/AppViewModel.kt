package com.tripnext.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripnext.app.data.TripRepository
import com.tripnext.app.data.local.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId

data class AppUiState(
    val trips: List<TripEntity> = emptyList(),
    val archivedTrips: List<TripEntity> = emptyList(),
    val activeTrip: TripEntity? = null,
    val expenses: List<ExpenseEntity> = emptyList(),
    val itinerary: List<ItineraryEventEntity> = emptyList(),
    val ideas: List<TripIdeaEntity> = emptyList(),
    val options: List<TripOptionEntity> = emptyList(),
    val checklist: List<ChecklistItemEntity> = emptyList(),
    val categoryBudgets: List<CategoryBudgetEntity> = emptyList(),
    val spentByCategory: List<CategorySpent> = emptyList(),
    val loading: Boolean = true
) {
    val spentMinor get() = expenses.sumOf { it.amountMinor }
    val remainingMinor get() = (activeTrip?.totalBudgetMinor ?: 0) - spentMinor
}

data class AiItinerarySuggestion(val dayOffset: Int, val time: String, val title: String, val location: String, val type: String)
data class AiChecklistSuggestion(val name: String, val category: String)
data class AiBudgetSuggestion(val category: String, val percent: Int)
data class AiTravelProposal(val overview: String, val itinerary: List<AiItinerarySuggestion>, val checklist: List<AiChecklistSuggestion>, val budgets: List<AiBudgetSuggestion>, val sources: List<String>, val liveSearch: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val repository: TripRepository) : ViewModel() {
    var aiPlan by mutableStateOf(""); private set
    var aiProposal by mutableStateOf<AiTravelProposal?>(null); private set
    var aiLoading by mutableStateOf(false); private set
    var aiError by mutableStateOf<String?>(null); private set
    private val selectedTripId = MutableStateFlow<String?>(null)
    private val selectedTrip = selectedTripId.flatMapLatest { id -> id?.let(repository::trip) ?: flowOf(null) }
    private val expenses = selectedTripId.flatMapLatest { id -> id?.let(repository::expenses) ?: flowOf(emptyList()) }
    private val itinerary = selectedTripId.flatMapLatest { id -> id?.let(repository::itinerary) ?: flowOf(emptyList()) }
    private val ideas = selectedTripId.flatMapLatest { id -> id?.let(repository::ideas) ?: flowOf(emptyList()) }
    private val options = selectedTripId.flatMapLatest { id -> id?.let(repository::options) ?: flowOf(emptyList()) }
    private val checklist = selectedTripId.flatMapLatest { id -> id?.let(repository::checklist) ?: flowOf(emptyList()) }
    private val budgets = selectedTripId.flatMapLatest { id -> id?.let(repository::budgets) ?: flowOf(emptyList()) }
    private val spent = selectedTripId.flatMapLatest { id -> id?.let(repository::spentByCategory) ?: flowOf(emptyList()) }

    val uiState = combine(repository.trips(), repository.archivedTrips(), selectedTrip, expenses, itinerary, ideas, options, checklist, budgets, spent) { values ->
        @Suppress("UNCHECKED_CAST")
        val trips = values[0] as List<TripEntity>
        val active = (values[2] as TripEntity?) ?: trips.firstOrNull { it.isActive } ?: trips.firstOrNull()
        if (selectedTripId.value == null && active != null) selectedTripId.value = active.id
        AppUiState(trips, values[1] as List<TripEntity>, active, values[3] as List<ExpenseEntity>, values[4] as List<ItineraryEventEntity>, values[5] as List<TripIdeaEntity>, values[6] as List<TripOptionEntity>, values[7] as List<ChecklistItemEntity>, values[8] as List<CategoryBudgetEntity>, values[9] as List<CategorySpent>, false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init { viewModelScope.launch { repository.deleteTrip("prototype-lisboa-porto"); repository.deleteTripsNamed("Lisboa & Porto"); selectedTripId.value = null } }
    fun selectTrip(id: String) { selectedTripId.value = id }
    fun archiveTrip(id: String) = viewModelScope.launch { repository.archiveTrip(id); selectedTripId.value = null }
    fun restoreTrip(id: String) = viewModelScope.launch { repository.restoreTrip(id); repository.activateTrip(id); selectedTripId.value = id }
    fun addIdea(title: String, location: String = "", type: ItineraryType = ItineraryType.ACTIVITY, estimatedCostMinor: Long = 0, sourceUrl: String = "", notes: String = "") = viewModelScope.launch {
        selectedTripId.value?.let { repository.saveIdea(TripIdeaEntity(tripId = it, title = title.trim(), location = location.trim(), type = type, estimatedCostMinor = estimatedCostMinor, sourceUrl = sourceUrl.trim(), notes = notes.trim())) }
    }
    fun scheduleIdea(idea: TripIdeaEntity, date: LocalDate, time: java.time.LocalTime) = viewModelScope.launch {
        val startsAt = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.saveEvent(ItineraryEventEntity(tripId = idea.tripId, title = idea.title, type = idea.type, startsAt = startsAt, location = idea.location, notes = idea.notes, estimatedCostMinor = idea.estimatedCostMinor, sourceUrl = idea.sourceUrl, latitude = idea.latitude, longitude = idea.longitude, placeId = idea.placeId))
        repository.deleteIdea(idea.id)
    }
    fun saveOption(decisionGroup: String, title: String, type: ItineraryType, provider: String = "", estimatedCostMinor: Long = 0, cancellationPolicy: String = "", inclusions: String = "", pros: String = "", cons: String = "", sourceUrl: String = "", location: String = "", origin: String = "", destination: String = "", departsAt: Long? = null, arrivesAt: Long? = null, stopCount: Int = 0, roomType: String = "", nightCount: Int = 0, durationMinutes: Int = 0, currency: String = "BRL", estimatedMinMinor: Long = estimatedCostMinor, estimatedMaxMinor: Long = estimatedCostMinor, exchangeRate: Double = 1.0, quoteDate: Long? = null, costScope: String = "GROUP", costClass: String = "FIXED") = viewModelScope.launch {
        selectedTripId.value?.let { repository.saveOption(TripOptionEntity(tripId = it, decisionGroup = decisionGroup.trim(), title = title.trim(), type = type, provider = provider.trim(), estimatedCostMinor = estimatedCostMinor, currency = currency, cancellationPolicy = cancellationPolicy.trim(), inclusions = inclusions.trim(), pros = pros.trim(), cons = cons.trim(), sourceUrl = sourceUrl.trim(), location = location.trim(), origin = origin.trim(), destination = destination.trim(), departsAt = departsAt, arrivesAt = arrivesAt, stopCount = stopCount, roomType = roomType.trim(), nightCount = nightCount, durationMinutes = durationMinutes, estimatedMinMinor = estimatedMinMinor, estimatedMaxMinor = estimatedMaxMinor, exchangeRate = exchangeRate, quoteDate = quoteDate, costScope = costScope, costClass = costClass)) }
    }
    fun chooseOption(option: TripOptionEntity) = viewModelScope.launch { repository.chooseOption(option) }
    fun scheduleOption(option: TripOptionEntity, date: LocalDate, time: java.time.LocalTime) = viewModelScope.launch {
        val startsAt = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.saveEvent(ItineraryEventEntity(tripId = option.tripId, title = option.title, type = option.type, startsAt = option.departsAt ?: startsAt, endsAt = option.arrivesAt ?: option.durationMinutes.takeIf { it > 0 }?.let { startsAt + it * 60_000L }, location = option.location.ifBlank { option.destination }, notes = listOfNotNull(option.provider.takeIf(String::isNotBlank)?.let { "Fornecedor: $it" }, option.cancellationPolicy.takeIf(String::isNotBlank)?.let { "Cancelamento: $it" }, option.origin.takeIf(String::isNotBlank)?.let { "$it → ${option.destination}" }).joinToString(" · "), estimatedCostMinor = option.estimatedCostMinor, sourceUrl = option.sourceUrl, planningStatus = if (option.chosen) "TO_BOOK" else "RESEARCHING", estimatedMinMinor = option.estimatedMinMinor, estimatedMaxMinor = option.estimatedMaxMinor, costCurrency = option.currency, exchangeRate = option.exchangeRate, quoteDate = option.quoteDate, costScope = option.costScope, costClass = option.costClass))
    }
    fun addExpense(amountMinor: Long, category: ExpenseCategory, description: String) = viewModelScope.launch {
        selectedTripId.value?.let { repository.saveExpense(ExpenseEntity(tripId = it, amountMinor = amountMinor, category = category, date = System.currentTimeMillis(), description = description)) }
    }
    fun addChecklist(name: String, category: ChecklistCategory = ChecklistCategory.OTHER) = viewModelScope.launch { selectedTripId.value?.let { repository.saveChecklist(ChecklistItemEntity(tripId = it, name = name, category = category)) } }
    fun toggleChecklist(id: String) = viewModelScope.launch { repository.toggleChecklist(id) }
    fun generateAiPlan(apiKey: String) = viewModelScope.launch {
        val trip = uiState.value.activeTrip ?: return@launch
        aiLoading = true; aiError = null
        runCatching { withContext(Dispatchers.IO) { requestGeminiPlan(apiKey, trip, uiState.value) } }
            .onSuccess { aiProposal = it; aiPlan = it.overview }
            .onFailure { aiError = when { it.message?.contains("401") == true || it.message?.contains("403") == true -> "Chave Gemini inválida ou sem permissão."; else -> it.message ?: "Não foi possível consultar o Gemini." } }
        aiLoading = false
    }
    fun importAiProposal() = viewModelScope.launch {
        val trip = uiState.value.activeTrip ?: return@launch
        val proposal = aiProposal ?: return@launch
        val zone = ZoneId.systemDefault()
        val start = java.time.Instant.ofEpochMilli(trip.startDate).atZone(zone).toLocalDate()
        proposal.itinerary.forEach { suggestion ->
            val parsedTime = runCatching { java.time.LocalTime.parse(suggestion.time) }.getOrDefault(java.time.LocalTime.of(9, 0))
            val instant = start.plusDays(suggestion.dayOffset.coerceAtLeast(0).toLong()).atTime(parsedTime).atZone(zone).toInstant().toEpochMilli()
            val type = runCatching { ItineraryType.valueOf(suggestion.type.uppercase()) }.getOrDefault(ItineraryType.ACTIVITY)
            repository.saveEvent(ItineraryEventEntity(tripId = trip.id, title = suggestion.title, type = type, startsAt = instant, location = suggestion.location, notes = "Sugestão importada do Copiloto"))
        }
        proposal.checklist.forEach { suggestion ->
            val category = runCatching { ChecklistCategory.valueOf(suggestion.category.uppercase()) }.getOrDefault(ChecklistCategory.OTHER)
            repository.saveChecklist(ChecklistItemEntity(tripId = trip.id, name = suggestion.name, category = category))
        }
        proposal.budgets.forEach { suggestion ->
            val category = runCatching { ExpenseCategory.valueOf(suggestion.category.uppercase()) }.getOrNull() ?: return@forEach
            repository.saveBudget(CategoryBudgetEntity(trip.id, category, trip.totalBudgetMinor * suggestion.percent.coerceIn(0, 100) / 100))
        }
    }
    fun addItinerary(title: String, location: String, date: LocalDate, time: java.time.LocalTime, type: ItineraryType) = viewModelScope.launch {
        val tripId = uiState.value.activeTrip?.id ?: return@launch
        val startsAt = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.saveEvent(ItineraryEventEntity(tripId = tripId, title = title, type = type, startsAt = startsAt, location = location))
    }
    fun createTrip(name: String, destination: String, start: LocalDate, end: LocalDate, budgetMinor: Long) = viewModelScope.launch {
        val zone = ZoneId.systemDefault()
        val trip = TripEntity(
            name = name.trim(), destination = destination.trim(),
            startDate = start.atStartOfDay(zone).toInstant().toEpochMilli(),
            endDate = end.atStartOfDay(zone).toInstant().toEpochMilli(),
            totalBudgetMinor = budgetMinor, isActive = true
        )
        repository.saveTrip(trip)
        repository.activateTrip(trip.id)
        val allocations = listOf(
            ExpenseCategory.ACCOMMODATION to 30, ExpenseCategory.TRANSPORT to 25,
            ExpenseCategory.FOOD to 15, ExpenseCategory.ACTIVITIES to 12,
            ExpenseCategory.INSURANCE to 5, ExpenseCategory.GIFTS to 4,
            ExpenseCategory.DOCUMENTS to 4, ExpenseCategory.UNEXPECTED to 5
        )
        allocations.forEach { (category, percent) -> repository.saveBudget(CategoryBudgetEntity(trip.id, category, budgetMinor * percent / 100)) }
        listOf(
            "Verificar validade do passaporte" to ChecklistCategory.DOCUMENTS,
            "Contratar seguro viagem" to ChecklistCategory.DOCUMENTS,
            "Salvar reservas offline" to ChecklistCategory.DOCUMENTS,
            "Separar carregadores e adaptadores" to ChecklistCategory.ELECTRONICS
        ).forEach { (item, category) -> repository.saveChecklist(ChecklistItemEntity(tripId = trip.id, name = item, category = category)) }
        selectedTripId.value = trip.id
    }

    private suspend fun loadPrototypeData() {
        val existing = repository.activeTrip()
        if (existing != null) { selectedTripId.value = existing.id; return }
        val zone = ZoneId.systemDefault()
        fun at(day: Int, hour: Int = 0, minute: Int = 0) = LocalDate.of(2026, 10, day).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        val trip = TripEntity(id = "prototype-lisboa-porto", name = "Lisboa & Porto", destination = "GRU → LIS", startDate = at(12), endDate = at(20), totalBudgetMinor = 850_000, currency = "BRL", isActive = true)
        repository.saveTrip(trip)
        listOf(
            ExpenseCategory.ACCOMMODATION to 260_000L,
            ExpenseCategory.TRANSPORT to 150_000L,
            ExpenseCategory.FOOD to 180_000L,
            ExpenseCategory.ACTIVITIES to 160_000L,
            ExpenseCategory.INSURANCE to 35_000L,
            ExpenseCategory.GIFTS to 50_000L,
            ExpenseCategory.DOCUMENTS to 15_000L,
            ExpenseCategory.UNEXPECTED to 40_000L
        ).forEach { (category, limit) -> repository.saveBudget(CategoryBudgetEntity(trip.id, category, limit)) }
        listOf(
            ExpenseEntity("proto-hotel", trip.id, 62_000, ExpenseCategory.ACCOMMODATION, at(12), "Hotel Alfama"),
            ExpenseEntity("proto-hotel-restante", trip.id, 148_000, ExpenseCategory.ACCOMMODATION, at(11), "Reserva hospedagem"),
            ExpenseEntity("proto-trem", trip.id, 9_500, ExpenseCategory.TRANSPORT, at(14), "Trem LIS → Porto"),
            ExpenseEntity("proto-transporte-restante", trip.id, 111_500, ExpenseCategory.TRANSPORT, at(10), "Passagens aéreas"),
            ExpenseEntity("proto-passagem-parcela", trip.id, 24_000, ExpenseCategory.TRANSPORT, at(1), "Passagem GRU-LIS (parcela 1/6)", installmentCount = 6),
            ExpenseEntity("proto-jantar", trip.id, 6_800, ExpenseCategory.FOOD, at(13), "Jantar Time Out Market"),
            ExpenseEntity("proto-comida-restante", trip.id, 91_200, ExpenseCategory.FOOD, at(9), "Alimentação planejada"),
            ExpenseEntity("proto-passeios", trip.id, 70_000, ExpenseCategory.ACTIVITIES, at(8), "Passeios e ingressos"),
            ExpenseEntity("proto-seguro", trip.id, 35_000, ExpenseCategory.INSURANCE, at(1), "Seguro viagem (apólice)"),
            ExpenseEntity("proto-presentes", trip.id, 12_000, ExpenseCategory.GIFTS, at(7), "Compras e presentes"),
            ExpenseEntity("proto-documentos", trip.id, 15_000, ExpenseCategory.DOCUMENTS, at(6), "Documentos e vistos")
        ).forEach { repository.saveExpense(it) }
        listOf(
            ItineraryEventEntity("proto-voo", trip.id, "Voo GRU → LIS", ItineraryType.FLIGHT, at(12, 9, 40), location = "LATAM 8181"),
            ItineraryEventEntity("proto-checkin-lis", trip.id, "Check-in Hotel Alfama", ItineraryType.CHECK_IN, at(12, 22, 15), location = "Rua de São Pedro, 34"),
            ItineraryEventEntity("proto-tour", trip.id, "Tour a pé — Alfama", ItineraryType.ACTIVITY, at(13, 10), location = "Ponto de encontro: Sé de Lisboa"),
            ItineraryEventEntity("proto-restaurante", trip.id, "Jantar", ItineraryType.RESTAURANT, at(13, 20), location = "Time Out Market"),
            ItineraryEventEntity("proto-trem-evento", trip.id, "Trem para o Porto", ItineraryType.TRANSFER, at(14, 8, 30), location = "Estação Santa Apolónia"),
            ItineraryEventEntity("proto-checkin-porto", trip.id, "Check-in Hotel", ItineraryType.CHECK_IN, at(14, 15), location = "Ribeira do Porto")
        ).forEach { repository.saveEvent(it) }
        val checklist = mapOf(
            ChecklistCategory.DOCUMENTS to listOf("Passaporte", "Reserva do hotel impressa", "Seguro viagem", "Cartão de vacina"),
            ChecklistCategory.CLOTHES to listOf("Casaco leve", "Sapato confortável", "2 mudas extras", "Roupa de banho"),
            ChecklistCategory.ELECTRONICS to listOf("Carregador", "Adaptador de tomada", "Power bank")
        )
        val checked = setOf("Passaporte", "Seguro viagem", "Sapato confortável", "Carregador")
        checklist.forEach { (category, names) -> names.forEachIndexed { index, name -> repository.saveChecklist(ChecklistItemEntity("proto-${category.name}-$index", trip.id, name, category = category, checked = name in checked)) } }
        selectedTripId.value = trip.id
    }

    private fun requestGeminiPlan(apiKey: String, trip: TripEntity, state: AppUiState): AiTravelProposal {
        require(apiKey.isNotBlank()) { "Configure sua chave Gemini em Ajustes." }
        val prompt = """Você é um planejador de viagens. Pesquise informações atuais e crie um plano importável em português do Brasil para ${trip.name}, destino ${trip.destination}, de ${dateForAi(trip.startDate)} a ${dateForAi(trip.endDate)}, orçamento ${trip.totalBudgetMinor / 100.0} BRL. Já existem ${state.itinerary.size} eventos e ${state.checklist.size} tarefas. Retorne SOMENTE JSON válido: {"overview":"resumo com prioridades e estimativas de valores","itinerary":[{"dayOffset":0,"time":"09:00","title":"nome","location":"endereço/bairro","type":"FLIGHT|CHECK_IN|CHECK_OUT|ACTIVITY|RESTAURANT|TRANSFER|OTHER"}],"checklist":[{"name":"tarefa","category":"DOCUMENTS|CLOTHES|ELECTRONICS|HYGIENE|MEDICINES|OTHER"}],"budgets":[{"category":"ACCOMMODATION|TRANSPORT|FOOD|ACTIVITIES|INSURANCE|GIFTS|DOCUMENTS|UNEXPECTED","percent":30}],"sources":["nome ou URL"]}. Use no máximo 12 eventos e 12 tarefas. A soma dos percentuais deve ser 100."""
        val body = JSONObject().apply {
            put("contents", org.json.JSONArray().put(JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt)))))
            put("tools", org.json.JSONArray().put(JSONObject().put("google_search", JSONObject())))
            put("generationConfig", JSONObject().put("maxOutputTokens", 4096).put("responseMimeType", "application/json"))
        }
        var (code, response) = sendGemini(apiKey, body)
        val fellBack = code == 429
        if (fellBack) { body.remove("tools"); val retry = sendGemini(apiKey, body); code = retry.first; response = retry.second }
        if (code !in 200..299) error("Gemini HTTP $code: ${response.take(180)}")
        val json = JSONObject(response)
        val parts = json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        val text = (0 until parts.length()).mapNotNull { parts.getJSONObject(it).optString("text").takeIf(String::isNotBlank) }.joinToString("\n").trim().removePrefix("```json").removeSuffix("```").trim()
        val proposal = JSONObject(text)
        fun array(name: String) = proposal.optJSONArray(name) ?: org.json.JSONArray()
        val itinerary = array("itinerary").let { values -> (0 until values.length()).map { values.getJSONObject(it) }.map { AiItinerarySuggestion(it.optInt("dayOffset"), it.optString("time", "09:00"), it.optString("title"), it.optString("location"), it.optString("type", "ACTIVITY")) }.filter { it.title.isNotBlank() } }
        val checklist = array("checklist").let { values -> (0 until values.length()).map { values.getJSONObject(it) }.map { AiChecklistSuggestion(it.optString("name"), it.optString("category", "OTHER")) }.filter { it.name.isNotBlank() } }
        val budgets = array("budgets").let { values -> (0 until values.length()).map { values.getJSONObject(it) }.map { AiBudgetSuggestion(it.optString("category"), it.optInt("percent")) } }
        val sources = array("sources").let { values -> (0 until values.length()).map { values.optString(it) }.filter { it.isNotBlank() } }
        val warning = if (fellBack) "Plano gerado sem pesquisa ao vivo porque a cota de busca está indisponível.\n\n" else ""
        return AiTravelProposal(warning + proposal.optString("overview"), itinerary, checklist, budgets, sources, !fellBack)
    }

    private fun sendGemini(apiKey: String, body: JSONObject): Pair<Int, String> {
        val connection = (URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20_000; readTimeout = 60_000; doOutput = true
            setRequestProperty("Content-Type", "application/json"); setRequestProperty("x-goog-api-key", apiKey)
        }
        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
        return code to response
    }

    private fun dateForAi(epoch: Long) = java.time.Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    class Factory(private val repository: TripRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repository) as T
    }
}
