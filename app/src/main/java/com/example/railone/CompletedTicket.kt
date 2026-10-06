package com.example.railone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

internal fun invoiceBookingTime(value: String): String = parseBookingTime(value)?.let {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(java.util.Date(it))
} ?: value
internal fun invoiceSummary(data: TicketData): String =
    "${data.className} | ${data.trainType} | ${data.ticketType} | ₹${formatFare(data.fare)}"
internal fun invoicePassengers(data: TicketData): String = "${data.adults} Adult, ${data.children} Child"

@Composable
internal fun CompletedTicketPage(data: TicketData, onBack: () -> Unit, onInvoice: () -> Unit) {
    val background = Color(0xFFF5F5F5)
    Column(Modifier.fillMaxSize().background(background).testTag("completed-ticket-details")) {
        BookingHeader(onBack, onInvoice = onInvoice)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 15.dp).fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)).background(Color(0xFFC8EBF0))
                .drawWithContent {
                    drawContent()
                    val x = size.width * .82f
                    drawCircle(background, 15.dp.toPx(), Offset(x, 0f))
                    drawCircle(background, 15.dp.toPx(), Offset(x, size.height))
                }.padding(horizontal = 15.dp, vertical = 28.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(data.ticketType, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(data.journeyTicket, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(data.origin, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(data.destination, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Via", fontSize = 13.sp, color = Color(0xFF777D80))
                        Text(data.via.ifBlank { "-" }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(Modifier.weight(1.5f), horizontalAlignment = Alignment.End) {
                        Text("Booked on", fontSize = 13.sp, color = Color(0xFF777D80))
                        Text(invoiceBookingTime(data.bookedOn), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("Ticket Expired", fontSize = 14.sp, color = Color(0xFF83858E), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(22.dp))
                Text("Passenger(s) : ${invoicePassengers(data)}", fontSize = 15.sp)
                Spacer(Modifier.height(14.dp))
                Text(invoiceSummary(data), fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
