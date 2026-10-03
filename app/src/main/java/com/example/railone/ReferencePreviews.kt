package com.example.railone

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

private fun previewState(): JourneyState {
    val data = freshDraft().copy(passengerName = "Traveller", mobile = "9876543210", origin = "HOWRAH",
        destination = "KHARAGPUR", distance = "116 km", fare = "30.00", journeyTicket = "X123456789")
    return JourneyState(profile = UserProfile("Traveller", "9876543210"),
        passengers = listOf(Passenger(name = "Traveller", mobile = "9876543210", age = "26", gender = "Male", meal = "Non-vegetarian")),
        tickets = listOf(StoredTicket(data = data, createdAt = System.currentTimeMillis(),
            countdownEndsAt = System.currentTimeMillis() + 300000L, accentIndex = 0)))
}
@Preview(name = "Home · reference layout", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HomeReferencePreview() {
    val state = previewState()
    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { HomePage(state, System.currentTimeMillis(), {}, {}, {}, {}, {}) }
            ReferenceBottomBar("Home") {}
        }
    }
}
@Preview(name = "Bookings · reference layout", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun BookingsReferencePreview() {
    val state = previewState(); val now = System.currentTimeMillis()
    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { BookingsPage(state.tickets, now, "Upcoming", true, {}, {}, {}, {}, {}, {}, {}, {}) }
            ReferenceBookingFilters(state.tickets, now, "Upcoming") {}
        }
    }
}
@Preview(name = "You · reference layout", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ProfileReferencePreview() {
    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { ProfilePage(previewState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) }
            ReferenceBottomBar("You") {}
        }
    }
}
