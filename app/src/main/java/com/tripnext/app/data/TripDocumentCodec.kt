package com.tripnext.app.data

import com.tripnext.app.data.local.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TripDocument(val trip: TripEntity, val itinerary: List<ItineraryEventEntity>, val ideas: List<TripIdeaEntity>, val options: List<TripOptionEntity>, val checklist: List<ChecklistItemEntity>, val participants: List<TripParticipantEntity>, val budgets: List<CategoryBudgetEntity>)

object TripDocumentCodec {
    private val zone get() = ZoneId.systemDefault()
    private fun date(epoch: Long) = Instant.ofEpochMilli(epoch).atZone(zone).toLocalDate().toString()
    private fun time(epoch: Long) = Instant.ofEpochMilli(epoch).atZone(zone).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    private fun epoch(date: String, time: String = "00:00") = runCatching { java.time.LocalDate.parse(date).atTime(java.time.LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli() }.getOrDefault(0)
    fun encode(document: TripDocument): JSONObject = JSONObject().apply {
        val trip = document.trip
        put("id", trip.id); put("name", trip.name); put("destination", trip.destination); put("start", date(trip.startDate)); put("end", date(trip.endDate)); put("travelers", trip.travelers); put("budget", trip.totalBudgetMinor / 100.0); put("currency", trip.currency); put("contingencyPercent", trip.contingencyPercent); put("archived", trip.archived); put("updatedAt", Instant.ofEpochMilli(trip.updatedAt).toString())
        put("participants", JSONArray(document.participants.map { JSONObject().put("id", it.id).put("name", it.name) }))
        put("itinerary", JSONArray(document.itinerary.map { event -> JSONObject().put("id", event.id).put("type", typeToWeb(event.type)).put("title", event.title).put("date", date(event.startsAt)).put("time", time(event.startsAt)).put("duration", event.endsAt?.let { (it - event.startsAt) / 60_000 } ?: 0).put("location", event.location).put("notes", event.notes).put("sortOrder", event.sortOrder).put("status", statusToWeb(event.planningStatus)).put("cost", event.estimatedCostMinor / 100.0).put("booking", event.bookingCode).put("link", event.sourceUrl).put("latitude", event.latitude).put("longitude", event.longitude).put("placeId", event.placeId) }))
        put("ideas", JSONArray(document.ideas.map { idea -> JSONObject().put("id", idea.id).put("type", typeToWeb(idea.type)).put("title", idea.title).put("location", idea.location).put("notes", idea.notes).put("cost", idea.estimatedCostMinor / 100.0).put("url", idea.sourceUrl).put("latitude", idea.latitude).put("longitude", idea.longitude).put("placeId", idea.placeId) }))
        put("options", JSONArray(document.options.map { option -> JSONObject().put("id", option.id).put("decision", option.decisionGroup).put("kind", typeToWeb(option.type)).put("title", option.title).put("provider", option.provider).put("location", option.location).put("price", option.estimatedCostMinor / 100.0).put("costCurrency", option.currency).put("cancellation", option.cancellationPolicy).put("baggage", option.inclusions).put("pros", option.pros).put("cons", option.cons).put("url", option.sourceUrl).put("chosen", option.chosen).put("observedAt", Instant.ofEpochMilli(option.observedAt).toString()) }))
        put("checklist", JSONArray(document.checklist.map { item -> JSONObject().put("id", item.id).put("name", item.name).put("category", item.category.name).put("done", item.checked) }))
        put("categoryBudgets", JSONArray(document.budgets.map { item -> JSONObject().put("category", item.category.name).put("limitMinor", item.limitMinor) }))
    }
    fun decode(value: JSONObject): TripDocument {
        val id = value.getString("id")
        val now = System.currentTimeMillis()
        val trip = TripEntity(id = id, name = value.optString("name", "Viagem"), destination = value.optString("destination"), startDate = epoch(value.optString("start")), endDate = epoch(value.optString("end")), totalBudgetMinor = (value.optDouble("budget") * 100).toLong(), currency = value.optString("currency", "BRL"), archived = value.optBoolean("archived"), travelers = value.optInt("travelers", 1).coerceAtLeast(1), updatedAt = now, contingencyPercent = value.optInt("contingencyPercent"))
        val itinerary = value.optJSONArray("itinerary").objects().map { item -> val start = epoch(item.optString("date"), item.optString("time", "09:00")); ItineraryEventEntity(id = item.optString("id", java.util.UUID.randomUUID().toString()), tripId = id, title = item.optString("title"), type = typeFromWeb(item.optString("type")), startsAt = start, endsAt = item.optInt("duration").takeIf { it > 0 }?.let { start + it * 60_000L }, location = item.optString("location"), notes = item.optString("notes"), sortOrder = item.optInt("sortOrder"), planningStatus = statusFromWeb(item.optString("status")), estimatedCostMinor = (item.optDouble("cost") * 100).toLong(), bookingCode = item.optString("booking"), sourceUrl = item.optString("link"), latitude = item.optNullableDouble("latitude"), longitude = item.optNullableDouble("longitude"), placeId = item.optString("placeId")) }
        val ideas = value.optJSONArray("ideas").objects().map { item -> TripIdeaEntity(id = item.optString("id", java.util.UUID.randomUUID().toString()), tripId = id, title = item.optString("title"), type = typeFromWeb(item.optString("type")), location = item.optString("location"), notes = item.optString("notes"), estimatedCostMinor = (item.optDouble("cost") * 100).toLong(), sourceUrl = item.optString("url"), latitude = item.optNullableDouble("latitude"), longitude = item.optNullableDouble("longitude"), placeId = item.optString("placeId")) }
        val options = value.optJSONArray("options").objects().map { item -> TripOptionEntity(id = item.optString("id", java.util.UUID.randomUUID().toString()), tripId = id, decisionGroup = item.optString("decision"), title = item.optString("title"), type = typeFromWeb(item.optString("kind")), provider = item.optString("provider"), location = item.optString("location"), estimatedCostMinor = (item.optDouble("price") * 100).toLong(), currency = item.optString("costCurrency", "BRL"), cancellationPolicy = item.optString("cancellation"), inclusions = item.optString("baggage"), pros = item.optString("pros"), cons = item.optString("cons"), sourceUrl = item.optString("url"), chosen = item.optBoolean("chosen")) }
        val checklist = value.optJSONArray("checklist").objects().map { item -> ChecklistItemEntity(id = item.optString("id", java.util.UUID.randomUUID().toString()), tripId = id, name = item.optString("name"), category = runCatching { ChecklistCategory.valueOf(item.optString("category")) }.getOrDefault(ChecklistCategory.OTHER), checked = item.optBoolean("done")) }
        val participants = value.optJSONArray("participants").objects().map { item -> TripParticipantEntity(id = item.optString("id", java.util.UUID.randomUUID().toString()), tripId = id, name = item.optString("name", "Viajante"), email = item.optString("email", "${item.optString("id")}@local.tripnext")) }
        val budgets = value.optJSONArray("categoryBudgets").objects().mapNotNull { item -> runCatching { CategoryBudgetEntity(id, ExpenseCategory.valueOf(item.optString("category")), item.optLong("limitMinor")) }.getOrNull() }
        return TripDocument(trip, itinerary, ideas, options, checklist, participants, budgets)
    }
    private fun JSONArray?.objects() = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
    private fun JSONObject.optNullableDouble(key: String) = if (isNull(key) || !has(key)) null else optDouble(key).takeUnless { it.isNaN() }
    private fun typeToWeb(type: ItineraryType) = when (type) { ItineraryType.FLIGHT, ItineraryType.TRANSFER -> "transporte"; ItineraryType.CHECK_IN, ItineraryType.CHECK_OUT -> "hospedagem"; ItineraryType.RESTAURANT -> "alimentacao"; ItineraryType.ACTIVITY -> "passeio"; else -> "outro" }
    private fun typeFromWeb(type: String) = when (type) { "transporte" -> ItineraryType.FLIGHT; "hospedagem" -> ItineraryType.CHECK_IN; "alimentacao" -> ItineraryType.RESTAURANT; "passeio" -> ItineraryType.ACTIVITY; "deslocamento" -> ItineraryType.TRANSFER; else -> ItineraryType.OTHER }
    private fun statusToWeb(value: String) = when (value) { "TO_BOOK" -> "reservar"; "BOOKED" -> "reservado"; else -> "pesquisar" }
    private fun statusFromWeb(value: String) = when (value) { "reservar" -> "TO_BOOK"; "reservado" -> "BOOKED"; else -> "RESEARCHING" }
}
