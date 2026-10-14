package com.example.railone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.math.BigDecimal
import java.util.Calendar

internal fun completionDeadline(data: TicketData, fallback: Long): Long {
    val booked = parseBookingTime(data.bookedOn) ?: fallback
    if (data.trainType != "ORDINARY" || data.ticketType != "RETURN") return booked + TICKET_COMPLETION_MILLIS
    return Calendar.getInstance().apply {
        timeInMillis = booked
        add(Calendar.DAY_OF_MONTH, 1)
        if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
internal fun upgradeSurcharge(data: TicketData, routes: List<SavedRoute>): String? {
    val route = data.savedPath(routes) ?: return null
    val mail = route.fares["MAIL/EXPRESS"]?.toBigDecimalOrNull() ?: return null
    val fast = route.fares["SUPERFAST"]?.toBigDecimalOrNull() ?: return null
    val adults = data.adults.toIntOrNull()?.takeIf { it in 1..4 } ?: return null
    val difference = fast.subtract(mail)
    if (difference.signum() < 0) return null
    return difference.multiply(adults.toBigDecimal()).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
}
internal fun surchargeAmount(raw: String): BigDecimal? = raw.trim().takeIf {
    Regex("[0-9]{1,8}(\\.[0-9]{1,2})?").matches(it)
}?.toBigDecimalOrNull()
internal fun StoredTicket.upgradedTicket(surcharge: String, now: Long): StoredTicket? {
    if (status(now) != "Upcoming" || data.trainType != "MAIL/EXPRESS") return null
    val amount = surchargeAmount(surcharge) ?: return null
    val original = data.fare.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 } ?: return null
    return copy(data = data.copy(trainType = "SUPERFAST", fare = original.add(amount).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()))
}

@Composable
internal fun HeldTicketSummary(data: TicketData, status: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(22.dp))
            .verticalScroll(rememberScrollState()).padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Journey Ticket", color = Color.Gray, fontSize = 12.sp); Text(data.journeyTicket, fontWeight = FontWeight.Bold) }
                Text(if (status == "Upcoming") "ACTIVE" else status.uppercase(), color = if (status == "Upcoming") Color(0xFF31905A) else Color.Gray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            TwoColumnField("Source", data.origin, "Destination", data.destination, boldValues = true)
            TwoColumnField("Distance", data.distance, "Passenger", "${data.adults} Adult, ${data.children} Child", boldValues = true)
            TwoColumnField("Ticket Type", data.ticketType, "Train Types", data.trainType, boldValues = true)
            TwoColumnField("Class", data.className, "Fare", "₹${data.fare}", boldValues = true)
            TwoColumnField("Booked on", data.bookedOn, "*Valid Till", data.validTill, boldValues = true)
            Text("Via: ${data.via.ifBlank { "-" }}", modifier = Modifier.padding(vertical = 12.dp))
            Text("IR: ${data.irNumber}", fontSize = 12.sp)
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("*Valid for start of journey within 1 hour or until departure of the first train.", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpgradeTicketSheet(ticket: StoredTicket, routes: List<SavedRoute>, onDismiss: () -> Unit, onConfirm: (String) -> String?) {
    val suggested = remember(ticket.id, routes) { upgradeSurcharge(ticket.data, routes) }
    var surcharge by rememberSaveable(ticket.id) { mutableStateOf(suggested.orEmpty()) }
    var editing by rememberSaveable(ticket.id) { mutableStateOf(suggested == null) }
    var error by rememberSaveable(ticket.id) { mutableStateOf<String?>(null) }
    val amount = surchargeAmount(surcharge)
    val total = amount?.let { ticket.data.fare.toBigDecimalOrNull()?.add(it)?.setScale(2, java.math.RoundingMode.HALF_UP)?.toPlainString() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), dragHandle = null) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ticket Summary", modifier = Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                OutlinedIconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close upgrade") }
            }
            Text("Journey Ticket", modifier = Modifier.padding(vertical = 16.dp))
            Row(Modifier.fillMaxWidth().background(Color(0xFFEAF6FB)).padding(16.dp)) {
                Text(ticket.data.origin, Modifier.weight(1f)); Text("→", Modifier.padding(horizontal = 10.dp)); Text(ticket.data.destination, Modifier.weight(1f))
            }
            Text("${ticket.data.adults} Adult, ${ticket.data.children} Child", Modifier.padding(vertical = 18.dp))
            TwoColumnField("Class", ticket.data.className, "Train Type", "SUPERFAST", boldValues = true)
            Text("Duration: Daily", Modifier.padding(vertical = 16.dp))
            Row(Modifier.fillMaxWidth().background(Color(0xFFEAF6FB)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Additional Fare", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text(if (amount == null) "Enter amount" else "₹${amount.setScale(2, java.math.RoundingMode.HALF_UP)}")
                IconButton(onClick = { editing = !editing }) { Icon(Icons.Default.Edit, "Edit surcharge") }
            }
            if (editing) OutlinedTextField(surcharge, { surcharge = it; error = null }, label = { Text("Total surcharge for this ticket (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Updated ticket fare: ${total?.let { "₹$it" } ?: "—"}", Modifier.padding(vertical = 16.dp))
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Button(onClick = { error = onConfirm(surcharge) }, enabled = total != null, shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Upgrade to Superfast") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
