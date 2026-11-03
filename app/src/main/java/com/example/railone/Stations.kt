package com.example.railone

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

internal data class Station(val name: String, val code: String = "") {
    val display: String get() = if (code.isBlank()) name else "$code — $name"
}
internal fun stationCatalog(state: JourneyState): List<Station> =
    (state.stations + state.routes.flatMap { listOf(Station(normalStation(it.origin)), Station(normalStation(it.destination))) })
        .distinctBy { normalStation(it.name) }.sortedBy { it.name }
internal fun stationMatches(stations: List<Station>, value: String): List<Station> {
    val query = normalStation(value)
    if (query.isBlank()) return emptyList()
    return stations.filter { normalStation(it.name) != query && (it.name.contains(query, true) || (it.code.isNotBlank() && it.code.contains(query, true))) }
        .sortedWith(compareBy<Station> { it.code != query }.thenBy { !it.name.startsWith(query) }.thenBy { it.name }).take(8)
}
internal fun stationError(station: Station, catalog: List<Station>): String? = when {
    station.name.isBlank() -> "Enter a station name."
    station.code.isNotBlank() && !Regex("[A-Z0-9]{1,10}").matches(station.code) -> "Use up to ten letters or digits for the code."
    station.code.isNotBlank() && catalog.any { it.name != station.name && it.code.equals(station.code, true) } -> "That code belongs to another station."
    else -> null
}
/** True when a saved route starts, ends or passes through the station. */
internal fun stationInUse(routes: List<SavedRoute>, name: String): Boolean {
    val target = normalStation(name)
    return routes.any { normalStation(it.origin) == target || normalStation(it.destination) == target ||
        it.via.split('-').any { stop -> normalStation(stop) == target } }
}
private fun renameVia(via: String, old: String, new: String): String =
    if (via.isBlank()) via else via.split('-').joinToString("-") { if (normalStation(it) == old) new else it }
/** Renames a station in saved routes and templates. Booked tickets are never touched. */
internal fun JourneyState.withStationRenamed(oldName: String, station: Station): JourneyState {
    val old = normalStation(oldName)
    val new = normalStation(station.name)
    fun swap(value: String) = if (normalStation(value) == old) new else value
    return copy(
        stations = (stationCatalog(this).filterNot { normalStation(it.name) == old } + station.copy(name = new)).sortedBy { it.name },
        routes = routes.map { it.copy(origin = swap(it.origin), destination = swap(it.destination), via = renameVia(it.via, old, new)) },
        templates = templates.map { it.copy(origin = swap(it.origin), destination = swap(it.destination), via = renameVia(it.via, old, new)) })
}
internal fun JourneyState.withoutStation(name: String): JourneyState =
    copy(stations = stations.filterNot { normalStation(it.name) == normalStation(name) })
@Composable
internal fun StationEditor(station: Station, onDismiss: () -> Unit, onSave: (Station) -> String?) {
    var name by rememberSaveable(station.name) { mutableStateOf(station.name) }
    var code by rememberSaveable(station.name) { mutableStateOf(station.code) }
    var error by remember { mutableStateOf<String?>(null) }
    ReferenceSheet(if (station.name.isBlank()) "Add Station" else "Edit Station", onDismiss) {
        OutlinedTextField(name, { name = it.uppercase(Locale.ROOT) }, label = { Text("Station name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(code, { code = it.uppercase(Locale.ROOT).filter(Char::isLetterOrDigit).take(10) }, label = { Text("Station code (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Codes are used only for search and suggestions. Tickets and PDFs show the full name only.")
        if (station.name.isNotBlank()) Text("Renaming also updates saved routes and journey templates. Booked tickets keep their original station names.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { error = onSave(Station(normalStation(name), code.trim())) }, modifier = Modifier.fillMaxWidth()) { Text("Save Station") }
    }
}

internal fun stationName(value: String, stations: List<Station>): String {
    val upper = normalStation(value)
    return stations.singleOrNull { it.code.isNotBlank() && it.code == upper }?.name ?: value.uppercase(Locale.ROOT)
}
