package com.example.railone

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import java.util.Locale
import java.util.UUID

internal data class SavedRoute(val id: String = UUID.randomUUID().toString(), val pairId: String = id,
    val origin: String = "", val destination: String = "", val via: String = "", val distance: String = "", val fares: Map<String, String> = emptyMap()) {
    val label: String get() = "$origin → $destination" + (if (via.isBlank()) " · Direct" else " · Via $via") +
        if (distance.isBlank()) " · Add distance" else " · ${normalDistance(distance)} km"
}
internal fun normalStation(value: String): String = value.trim().uppercase(Locale.ROOT).replace(Regex("\\s+"), " ")
internal fun normalVia(value: String): String = value.split('-').joinToString("-") { normalStation(it) }
internal fun normalDistance(value: String): String = value.trim().replace(Regex("(?i)\\s*km$"), "").trim()
internal fun validRouteFare(value: String): Boolean = Regex("[0-9]{1,7}(\\.[0-9]{1,2})?").matches(value.trim())
internal fun normalFares(fares: Map<String, String>): Map<String, String> = fares.mapValues { formatFare(it.value.trim()) }
internal fun validRouteDistance(value: String): Boolean = normalDistance(value).toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
internal fun reverseVia(value: String): String = if (value.isBlank()) "" else normalVia(value).split('-').reversed().joinToString("-")
internal fun routeError(route: SavedRoute): String? = when {
    route.origin.isBlank() || route.destination.isBlank() -> "Enter both stations."
    normalStation(route.origin) == normalStation(route.destination) -> "From and To stations must differ."
    route.via.isNotBlank() && route.via.split('-').any { it.isBlank() } -> "Separate Via stops with one hyphen, for example PKU-SRC."
    !validRouteDistance(route.distance) -> "Enter a positive distance in km."
    route.fares.any { (type, fare) -> type !in TRAIN_TYPE_OPTIONS || !validRouteFare(fare) } -> "Enter valid fares with at most two decimal places, or leave them empty."
    else -> null
}
private fun routeKey(route: SavedRoute) = listOf(normalStation(route.origin), normalStation(route.destination), normalVia(route.via))
internal fun List<SavedRoute>.saveRoutePair(input: SavedRoute): List<SavedRoute> {
    require(routeError(input) == null)
    val route = input.copy(origin = normalStation(input.origin), destination = normalStation(input.destination), via = normalVia(input.via), distance = normalDistance(input.distance), fares = normalFares(input.fares))
    val reverse = route.copy(id = firstOrNull { it.pairId == route.pairId && it.id != route.id }?.id ?: UUID.randomUUID().toString(),
        origin = route.destination, destination = route.origin, via = reverseVia(route.via))
    val keys = setOf(routeKey(route), routeKey(reverse))
    return filterNot { it.pairId == route.pairId || routeKey(it) in keys } + listOf(route, reverse)
}
internal fun List<SavedRoute>.editRoute(input: SavedRoute, updateReverse: Boolean): List<SavedRoute> {
    require(routeError(input) == null) { routeError(input).orEmpty() }
    val normal = input.copy(origin = normalStation(input.origin), destination = normalStation(input.destination),
        via = normalVia(input.via), distance = normalDistance(input.distance), fares = normalFares(input.fares))
    val existing = any { it.id == input.id }
    if (updateReverse) {
        if (existing) {
            val reverseKey = routeKey(normal.copy(origin = normal.destination, destination = normal.origin, via = reverseVia(normal.via)))
            require(none { it.pairId != normal.pairId && routeKey(it) in setOf(routeKey(normal), reverseKey) }) {
                "That path is already saved separately. Edit that saved route instead."
            }
        }
        return saveRoutePair(normal)
    }
    require(existing) { "New routes must include the reverse direction." }
    require(none { it.id != normal.id && routeKey(it) == routeKey(normal) }) {
        "That path is already saved separately. Edit that saved route instead."
    }
    return map { if (it.id == normal.id) normal else it }
}

internal fun TicketData.withRoute(route: SavedRoute): TicketData = copy(origin = normalStation(route.origin), destination = normalStation(route.destination), via = normalVia(route.via),
    distance = route.distance.takeIf { it.isNotBlank() }?.let(::normalDistance) ?: distance,
    fare = calculatedRouteFare(route.fares[trainType].orEmpty()))
internal fun routesFromTemplates(templates: List<SavedJourney>): List<SavedRoute> = templates.fold(emptyList()) { routes, template ->
    val route = SavedRoute(origin = template.origin, destination = template.destination, via = template.via, distance = template.distance)
    if (routeError(route) == null) routes.saveRoutePair(route) else routes
}
@Composable
internal fun SavedRouteSheet(route: SavedRoute, isEditing: Boolean = false, onDismiss: () -> Unit, onSave: (SavedRoute, Boolean) -> String?) {
    var origin by rememberSaveable(route.id) { mutableStateOf(route.origin) }
    var destination by rememberSaveable(route.id) { mutableStateOf(route.destination) }
    var via by rememberSaveable(route.id) { mutableStateOf(route.via) }
    var distance by rememberSaveable(route.id) { mutableStateOf(route.distance) }
    var ordinaryFare by rememberSaveable(route.id) { mutableStateOf(route.fares["ORDINARY"].orEmpty()) }
    var expressFare by rememberSaveable(route.id) { mutableStateOf(route.fares["MAIL/EXPRESS"].orEmpty()) }
    var superfastFare by rememberSaveable(route.id) { mutableStateOf(route.fares["SUPERFAST"].orEmpty()) }
    var acFare by rememberSaveable(route.id) { mutableStateOf(route.fares["AC EMU TRAIN"].orEmpty()) }
    var updateReverse by rememberSaveable(route.id) { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    ReferenceSheet(if (isEditing) "Edit Route" else "Save Route", onDismiss, fraction = .75f) {
        Text(if (isEditing) "Edit this direction, or update its reverse too. Existing tickets keep their booked route." else "Save station details independently of passengers. Both directions are saved; Via stops reverse automatically.", fontSize = 13.sp)
        OutlinedTextField(origin, { origin = it.uppercase(Locale.ROOT); error = null }, label = { Text("From Station") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(destination, { destination = it.uppercase(Locale.ROOT); error = null }, label = { Text("To Station") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(via, { via = it.uppercase(Locale.ROOT); error = null }, label = { Text("Via (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        TextButton(onClick = { via = ""; error = null }, enabled = via.isNotBlank()) { Text("Remove Via") }
        OutlinedTextField(distance, { distance = it; error = null }, label = { Text("Distance (km)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Leave Via empty for a direct route. Different Via paths for the same stations can be saved separately.", fontSize = 12.sp)
        Text("Fare for one adult by train type (₹)", fontSize = 14.sp)
        Text("These are one-adult, one-way fares. Leave a fare empty if it is not stored. Selecting that train type will require a manual fare.", fontSize = 12.sp)
        OutlinedTextField(ordinaryFare, { ordinaryFare = it; error = null }, label = { Text("Ordinary fare (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
        OutlinedTextField(expressFare, { expressFare = it; error = null }, label = { Text("Mail/Express fare (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
        OutlinedTextField(superfastFare, { superfastFare = it; error = null }, label = { Text("Superfast fare (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
        OutlinedTextField(acFare, { acFare = it; error = null }, label = { Text("AC EMU fare (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
        if (isEditing) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = updateReverse, onCheckedChange = { updateReverse = it; error = null })
            Text("Update reverse route too", fontSize = 13.sp)
        }
        error?.let { Text(it, color = Color(0xFFB3261E)) }
        Button(onClick = {
            val fares = mapOf("ORDINARY" to ordinaryFare, "MAIL/EXPRESS" to expressFare,
                "SUPERFAST" to superfastFare, "AC EMU TRAIN" to acFare).filterValues { it.isNotBlank() }
            val edited = route.copy(origin = origin, destination = destination, via = via, distance = distance, fares = fares)
            error = routeError(edited)
            if (error == null) error = onSave(edited, updateReverse)
        }, modifier = Modifier.fillMaxWidth()) { Text(if (!isEditing) "Save Route and Reverse" else if (updateReverse) "Save Both Directions" else "Save This Direction") }
    }
}


internal fun matchingRoutes(routes: List<SavedRoute>, origin: String, destination: String): List<SavedRoute> =
    routes.filter { normalStation(it.origin) == normalStation(origin) && normalStation(it.destination) == normalStation(destination) }

// Called only on a station edit. Editing Via or distance later never re-applies the saved values.
internal fun TicketData.fillUniqueSavedRoute(routes: List<SavedRoute>): TicketData =
    matchingRoutes(routes, origin, destination).singleOrNull()?.let { withRoute(it) } ?: this

internal fun stationSuggestions(routes: List<SavedRoute>, query: String): List<String> {
    val normal = normalStation(query)
    if (normal.isBlank()) return emptyList()
    return routes.flatMap { listOf(normalStation(it.origin), normalStation(it.destination)) }
        .filter { it.isNotBlank() && it.contains(normal) && it != normal }.distinct()
        .sortedWith(compareBy<String> { !it.startsWith(normal) }.thenBy { it }).take(8)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StationSuggestionField(label: String, value: String, routes: List<SavedRoute>, onValue: (String) -> Unit,
    modifier: Modifier = Modifier, stations: List<Station> = emptyList()) {
    var focused by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val suggestions = remember(stations, routes, value) { stationMatches(stations.ifEmpty { routes.flatMap { listOf(Station(it.origin), Station(it.destination)) }.distinctBy { it.name } }, value) }
    val expanded = focused && !dismissed && suggestions.isNotEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { dismissed = !it }, modifier = modifier) {
        OutlinedTextField(value, { dismissed = false; onValue(it.uppercase(Locale.ROOT)) },
            label = { Text(label) }, singleLine = true, colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                .onFocusChanged { focused = it.isFocused })
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { dismissed = true }) {
            suggestions.forEach { station -> DropdownMenuItem(text = { Text(station.display) }, onClick = {
                dismissed = true; onValue(station.name)
            }) }
        }
    }
}


internal fun recoverRouteDistance(origin: String, destination: String, via: String, records: List<TicketData>): String =
    records.firstOrNull { data ->
        validRouteDistance(data.distance) && (
            (normalStation(data.origin) == normalStation(origin) && normalStation(data.destination) == normalStation(destination) && normalVia(data.via) == normalVia(via)) ||
            (normalStation(data.destination) == normalStation(origin) && normalStation(data.origin) == normalStation(destination) && reverseVia(data.via) == normalVia(via)))
    }?.distance?.let(::normalDistance).orEmpty()


internal fun TicketData.savedPath(routes: List<SavedRoute>): SavedRoute? = matchingRoutes(routes, origin, destination)
    .filter { normalVia(it.via) == normalVia(via) }.singleOrNull()
internal fun TicketData.withTrainTypeFare(type: String, routes: List<SavedRoute>): TicketData {
    val route = savedPath(routes)
    val changed = copy(trainType = type, ticketType = if (type != "ORDINARY") "JOURNEY" else ticketType)
    return changed.copy(fare = if (route == null) if (type == trainType) fare else "" else changed.calculatedRouteFare(route.fares[type].orEmpty()))
}
internal fun TicketData.routeForSaving(routes: List<SavedRoute>): SavedRoute {
    val existing = savedPath(routes)
    val storedFares = existing?.fares.orEmpty()
    val updatedFares = if (trainType in TRAIN_TYPE_OPTIONS && validRouteFare(fare)) storedFares + (trainType to singleAdultFare()) else storedFares
    return (existing ?: SavedRoute()).copy(origin = origin, destination = destination, via = via, distance = distance, fares = updatedFares)
}

internal fun TicketData.fareMultiplier(): Int = (adults.toIntOrNull()?.coerceIn(1, 4) ?: 1) *
    if (trainType == "ORDINARY" && ticketType == "RETURN") 2 else 1
internal fun TicketData.calculatedRouteFare(base: String): String = base.toBigDecimalOrNull()?.let {
    it.multiply(fareMultiplier().toBigDecimal()).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
}.orEmpty()
internal fun TicketData.singleAdultFare(): String = fare.toBigDecimal().divide(fareMultiplier().toBigDecimal(), 2, java.math.RoundingMode.HALF_UP).toPlainString()
internal fun TicketData.withAdultCount(value: String, routes: List<SavedRoute>): TicketData =
    copy(adults = value).withTrainTypeFare(trainType, routes)
internal fun TicketData.withTicketTypeFare(value: String, routes: List<SavedRoute>): TicketData {
    val changed = copy(ticketType = if (trainType == "ORDINARY") value else "JOURNEY")
    return changed.copy(fare = fareForChange(changed, routes))
}
internal fun TicketData.fareForChange(changed: TicketData, routes: List<SavedRoute>): String {
    val route = changed.savedPath(routes)
    if (route != null) return changed.calculatedRouteFare(route.fares[changed.trainType].orEmpty())
    if (fare.toBigDecimalOrNull() == null) return ""
    return fare.toBigDecimal().multiply(changed.fareMultiplier().toBigDecimal()).divide(fareMultiplier().toBigDecimal(), 2, java.math.RoundingMode.HALF_UP).toPlainString()
}

internal fun TicketData.withPassengerCount(value: String, adult: Boolean, routes: List<SavedRoute>): TicketData {
    val count = value.toIntOrNull() ?: return this
    val other = (if (adult) children else adults).toIntOrNull() ?: return this
    if (count < (if (adult) 1 else 0) || count + other > 4) return this
    val changed = if (adult) copy(adults = count.toString()) else copy(children = count.toString())
    return if (adult) changed.copy(fare = fareForChange(changed, routes)) else changed
}
internal fun TicketData.swappedRoute(routes: List<SavedRoute>): TicketData {
    val changed = copy(origin = destination, destination = origin, via = reverseVia(via))
    val paths = matchingRoutes(routes, changed.origin, changed.destination)
    val exact = paths.singleOrNull { normalVia(it.via) == normalVia(changed.via) }
    return (exact ?: paths.singleOrNull())?.let { changed.withRoute(it) } ?: if (paths.size > 1) changed.copy(distance = "", fare = "") else changed
}
