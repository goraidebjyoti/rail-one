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
@Composable
internal fun StationEditor(station: Station, onDismiss: () -> Unit, onSave: (Station) -> String?) {
    var name by rememberSaveable(station.name) { mutableStateOf(station.name) }
    var code by rememberSaveable(station.name) { mutableStateOf(station.code) }
    var error by remember { mutableStateOf<String?>(null) }
    ReferenceSheet(if (station.name.isBlank()) "Add Station" else "Assign Station Code", onDismiss) {
        OutlinedTextField(name, { name = it.uppercase(Locale.ROOT) }, label = { Text("Station name") }, readOnly = station.name.isNotBlank(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(code, { code = it.uppercase(Locale.ROOT).filter(Char::isLetterOrDigit).take(10) }, label = { Text("Station code (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Codes are used only for search and suggestions. Tickets and PDFs show the full name only.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { error = onSave(Station(normalStation(name), code.trim())) }, modifier = Modifier.fillMaxWidth()) { Text("Save Station") }
    }
}

internal fun stationName(value: String, stations: List<Station>): String {
    val upper = normalStation(value)
    return stations.singleOrNull { it.code.isNotBlank() && it.code == upper }?.name ?: value.uppercase(Locale.ROOT)
}
