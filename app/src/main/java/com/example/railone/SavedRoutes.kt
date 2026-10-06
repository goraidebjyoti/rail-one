package com.example.railone

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import java.util.Locale
import java.util.UUID

internal data class SavedRoute(val id: String = UUID.randomUUID().toString(), val pairId: String = id,
    val origin: String = "", val destination: String = "", val via: String = "") {
    val label: String get() = "$origin → $destination" + if (via.isBlank()) " · Direct" else " · Via $via"
}
internal fun normalStation(value: String): String = value.trim().uppercase(Locale.ROOT).replace(Regex("\\s+"), " ")
internal fun normalVia(value: String): String = value.split('-').joinToString("-") { normalStation(it) }
internal fun reverseVia(value: String): String = if (value.isBlank()) "" else normalVia(value).split('-').reversed().joinToString("-")
internal fun routeError(route: SavedRoute): String? = when {
    route.origin.isBlank() || route.destination.isBlank() -> "Enter both stations."
    normalStation(route.origin) == normalStation(route.destination) -> "From and To stations must differ."
    route.via.isNotBlank() && route.via.split('-').any { it.isBlank() } -> "Separate Via stops with one hyphen, for example PKU-SRC."
    else -> null
}
private fun routeKey(route: SavedRoute) = listOf(normalStation(route.origin), normalStation(route.destination), normalVia(route.via))
internal fun List<SavedRoute>.saveRoutePair(input: SavedRoute): List<SavedRoute> {
    require(routeError(input) == null)
    val route = input.copy(origin = normalStation(input.origin), destination = normalStation(input.destination), via = normalVia(input.via))
    val reverse = route.copy(id = firstOrNull { it.pairId == route.pairId && it.id != route.id }?.id ?: UUID.randomUUID().toString(),
        origin = route.destination, destination = route.origin, via = reverseVia(route.via))
    val keys = setOf(routeKey(route), routeKey(reverse))
    return filterNot { it.pairId == route.pairId || routeKey(it) in keys } + listOf(route, reverse)
}
internal fun TicketData.withRoute(route: SavedRoute): TicketData = copy(origin = route.origin, destination = route.destination, via = route.via)
internal fun routesFromTemplates(templates: List<SavedJourney>): List<SavedRoute> = templates.fold(emptyList()) { routes, template ->
    val route = SavedRoute(origin = template.origin, destination = template.destination, via = template.via)
    if (routeError(route) == null) routes.saveRoutePair(route) else routes
}
@Composable
internal fun SavedRouteSheet(route: SavedRoute, onDismiss: () -> Unit, onSave: (SavedRoute) -> Unit) {
    var origin by rememberSaveable(route.id) { mutableStateOf(route.origin) }
    var destination by rememberSaveable(route.id) { mutableStateOf(route.destination) }
    var via by rememberSaveable(route.id) { mutableStateOf(route.via) }
    var error by remember { mutableStateOf<String?>(null) }
    ReferenceSheet("Save Route", onDismiss, fraction = .65f) {
        Text("Save station details independently of passengers. Both directions are saved; Via stops reverse automatically.", fontSize = 13.sp)
        OutlinedTextField(origin, { origin = it; error = null }, label = { Text("From Station") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(destination, { destination = it; error = null }, label = { Text("To Station") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(via, { via = it; error = null }, label = { Text("Via (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Leave Via empty for a direct route. Different Via paths for the same stations can be saved separately.", fontSize = 12.sp)
        error?.let { Text(it, color = Color(0xFFB3261E)) }
        Button(onClick = {
            val edited = route.copy(origin = origin, destination = destination, via = via)
            error = routeError(edited)
            if (error == null) onSave(edited)
        }, modifier = Modifier.fillMaxWidth()) { Text("Save Route and Reverse") }
    }
}
