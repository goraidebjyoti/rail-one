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
    val origin: String = "", val destination: String = "", val via: String = "", val distance: String = "") {
    val label: String get() = "$origin → $destination" + (if (via.isBlank()) " · Direct" else " · Via $via") +
        if (distance.isBlank()) " · Add distance" else " · ${normalDistance(distance)} km"
}
internal fun normalStation(value: String): String = value.trim().uppercase(Locale.ROOT).replace(Regex("\\s+"), " ")
internal fun normalVia(value: String): String = value.split('-').joinToString("-") { normalStation(it) }
internal fun normalDistance(value: String): String = value.trim().replace(Regex("(?i)\\s*km$"), "").trim()
internal fun validRouteDistance(value: String): Boolean = normalDistance(value).toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
internal fun reverseVia(value: String): String = if (value.isBlank()) "" else normalVia(value).split('-').reversed().joinToString("-")
internal fun routeError(route: SavedRoute): String? = when {
    route.origin.isBlank() || route.destination.isBlank() -> "Enter both stations."
    normalStation(route.origin) == normalStation(route.destination) -> "From and To stations must differ."
    route.via.isNotBlank() && route.via.split('-').any { it.isBlank() } -> "Separate Via stops with one hyphen, for example PKU-SRC."
    !validRouteDistance(route.distance) -> "Enter a positive distance in km."
    else -> null
}
private fun routeKey(route: SavedRoute) = listOf(normalStation(route.origin), normalStation(route.destination), normalVia(route.via))
internal fun List<SavedRoute>.saveRoutePair(input: SavedRoute): List<SavedRoute> {
    require(routeError(input) == null)
    val route = input.copy(origin = normalStation(input.origin), destination = normalStation(input.destination), via = normalVia(input.via), distance = normalDistance(input.distance))
    val reverse = route.copy(id = firstOrNull { it.pairId == route.pairId && it.id != route.id }?.id ?: UUID.randomUUID().toString(),
        origin = route.destination, destination = route.origin, via = reverseVia(route.via))
    val keys = setOf(routeKey(route), routeKey(reverse))
    return filterNot { it.pairId == route.pairId || routeKey(it) in keys } + listOf(route, reverse)
}
internal fun List<SavedRoute>.editRoute(input: SavedRoute, updateReverse: Boolean): List<SavedRoute> {
    require(routeError(input) == null) { routeError(input).orEmpty() }
    val normal = input.copy(origin = normalStation(input.origin), destination = normalStation(input.destination),
        via = normalVia(input.via), distance = normalDistance(input.distance))
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
    distance = route.distance.takeIf { it.isNotBlank() }?.let(::normalDistance) ?: distance)
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
        if (isEditing) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = updateReverse, onCheckedChange = { updateReverse = it; error = null })
            Text("Update reverse route too", fontSize = 13.sp)
        }
        error?.let { Text(it, color = Color(0xFFB3261E)) }
        Button(onClick = {
            val edited = route.copy(origin = origin, destination = destination, via = via, distance = distance)
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
    modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val suggestions = stationSuggestions(routes, value)
    val expanded = focused && !dismissed && suggestions.isNotEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { dismissed = !it }, modifier = modifier) {
        OutlinedTextField(value, { dismissed = false; onValue(it.uppercase(Locale.ROOT)) },
            label = { Text(label) }, singleLine = true, colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                .onFocusChanged { focused = it.isFocused })
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { dismissed = true }) {
            suggestions.forEach { station -> DropdownMenuItem(text = { Text(station) }, onClick = {
                dismissed = true; onValue(station)
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
