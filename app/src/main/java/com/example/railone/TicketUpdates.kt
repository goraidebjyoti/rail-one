package com.example.railone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.ArrowForward
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

@Suppress("DEPRECATION")
internal fun ticketHoldVibration(context: android.content.Context): Boolean = runCatching {
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31)
        context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
    else context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
    if (vibrator == null || !vibrator.hasVibrator()) false else {
        if (android.os.Build.VERSION.SDK_INT >= 26) vibrator.vibrate(android.os.VibrationEffect.createOneShot(45, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        else vibrator.vibrate(45L)
        true
    }
}.getOrDefault(false)

@Composable
private fun SummaryField(label: String, value: String, modifier: Modifier = Modifier, right: Boolean = false) {
    Column(modifier, horizontalAlignment = if (right) Alignment.End else Alignment.Start) {
        Text(label, color = Color(0xFF777782), fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = .2.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color(0xFF252529), fontSize = 13.sp, lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold, textAlign = if (right) TextAlign.End else TextAlign.Start)
    }
}
@Composable
private fun SummaryPair(left: String, value: String, right: String, rightValue: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryField(left, value, Modifier.weight(1f))
        SummaryField(right, rightValue, Modifier.weight(1.2f).padding(top = 8.dp), right = true)
    }
}
@Composable
internal fun HeldTicketSummary(data: TicketData, status: String, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val corner = with(density) { 22.dp.toPx() }
    val radius = with(density) { 14.dp.toPx() }
    val footerOffset = with(density) { 86.dp.toPx() }
    val shape = remember(corner, radius, footerOffset) { GenericShape { size, _ ->
        val w = size.width; val h = size.height; val y = h - footerOffset; val r = radius; val c = corner
        moveTo(c, 0f); lineTo(w-c, 0f); quadraticBezierTo(w, 0f, w, c)
        lineTo(w, y-r); cubicTo(w-r*.55f, y-r, w-r, y-r*.55f, w-r, y)
        cubicTo(w-r, y+r*.55f, w-r*.55f, y+r, w, y+r)
        lineTo(w, h-c); quadraticBezierTo(w, h, w-c, h); lineTo(c, h); quadraticBezierTo(0f, h, 0f, h-c)
        lineTo(0f, y+r); cubicTo(r*.55f, y+r, r, y+r*.55f, r, y)
        cubicTo(r, y-r*.55f, r*.55f, y-r, 0f, y-r)
        lineTo(0f, c); quadraticBezierTo(0f, 0f, c, 0f); close()
    } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.matchParentSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onDismiss))
            Column(Modifier.fillMaxWidth(.9f).widthIn(max = 440.dp).verticalScroll(rememberScrollState())
                .clip(shape).background(Color(0xFFFAF8FB))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { /* Keep taps inside the summary. */ }
                .testTag("held-ticket-summary")) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        SummaryField("Journey Ticket", data.journeyTicket, Modifier.weight(1f))
                        Box(Modifier.background(Color(0xFFE8F3E5), CircleShape).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(if (status == "Upcoming") "● ACTIVE" else status.uppercase(), color = Color(0xFF579C58), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    SummaryPair("Source", data.origin, "Destination", data.destination)
                    SummaryPair("Distance", "${normalDistance(data.distance)} km", "Passenger", "${data.adults} Adult, ${data.children} Child")
                    SummaryPair("Ticket Type", data.ticketType, "Train Types", data.trainType)
                    SummaryPair("Class", data.className, "Fare", "₹${data.fare}")
                    SummaryPair("Booked on", data.bookedOn, "*Valid Till", data.validTill)
                    Row(Modifier.fillMaxWidth().border(1.dp, Color(0xFFE6E3E9), RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ViaRouteIcon(); Spacer(Modifier.width(6.dp)); Text("Via: ${data.via.ifBlank { "-" }}", fontSize = 11.sp)
                    }
                    Text("IR: ${data.irNumber}", fontSize = 12.sp, color = Color(0xFF62626A))
                }
                Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                    drawLine(Color(0xFFB7B4BE), Offset(radius, size.height/2), Offset(size.width-radius, size.height/2),
                        strokeWidth = .7.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())))
                }
                Box(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("*Valid for start of journey within 1 hour or until departure of the first train.",
                        fontSize = 12.sp, lineHeight = 17.sp, color = Color(0xFF777782))
                }
            }
        }
    }
}

@Composable
internal fun UpgradeTicketSheet(ticket: StoredTicket, routes: List<SavedRoute>, onDismiss: () -> Unit, onConfirm: (String) -> String?) {
    val suggested = remember(ticket.id, routes) { upgradeSurcharge(ticket.data, routes) }
    var surcharge by rememberSaveable(ticket.id) { mutableStateOf(suggested.orEmpty()) }
    var editing by rememberSaveable(ticket.id) { mutableStateOf(false) }
    var error by rememberSaveable(ticket.id) { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf(false) }
    val amount = surchargeAmount(surcharge)
    val total = amount?.let { ticket.data.fare.toBigDecimalOrNull()?.add(it) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        LaunchedEffect(Unit) { withFrameNanos { }; entered = true }
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            @Suppress("DEPRECATION")
            if (window != null) {
                window.statusBarColor = android.graphics.Color.WHITE
                window.navigationBarColor = android.graphics.Color.WHITE
                window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    if (android.os.Build.VERSION.SDK_INT >= 26) android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
            }
            onDispose { }
        }
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(entered, modifier = Modifier.fillMaxSize(), enter = slideInVertically(tween(420)) { it }, exit = slideOutVertically(tween(300)) { it }) {
                Column(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
                    Box(Modifier.fillMaxWidth().height(68.dp), contentAlignment = Alignment.Center) {
                        Text("Ticket Summary", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF17223B))
                        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd).size(44.dp).border(1.dp, Color(0xFFCDE8EE), CircleShape)) {
                            Icon(Icons.Default.Close, "Close upgrade", tint = Color(0xFF0166FF), modifier = Modifier.size(22.dp))
                        }
                    }
                    Text("Journey Ticket", fontSize = 15.sp, color = Color(0xFF53535B), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp))
                    Row(Modifier.fillMaxWidth().background(Color(0xFFF0F7FC)).padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ticket.data.origin, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ArrowForward, null, tint = Color(0xFF92939B), modifier = Modifier.padding(horizontal = 10.dp).size(20.dp))
                        Text(ticket.data.destination, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                    }
                    val passengerCount = (ticket.data.adults.toIntOrNull() ?: 0) + (ticket.data.children.toIntOrNull() ?: 0)
                    Text("$passengerCount Passenger${if (passengerCount == 1) "" else "s"}", fontSize = 15.sp, color = Color(0xFF53535B), modifier = Modifier.padding(top = 16.dp, bottom = 26.dp))
                    Row(Modifier.fillMaxWidth()) {
                        SummaryField("Class", ticket.data.className, Modifier.weight(1f))
                        SummaryField("Train Type", "SUPERFAST", Modifier.weight(1f), right = true)
                    }
                    Spacer(Modifier.height(18.dp))
                    SummaryField("Duration", "Daily")
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth().background(Color(0xFFF0F7FC)).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        UpgradeFareIcon()
                        Text("Total Fare", Modifier.weight(1f).padding(start = 10.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(amount?.let { "₹${it.setScale(2, java.math.RoundingMode.HALF_UP)}" } ?: "—", fontSize = 15.sp)
                        IconButton(onClick = { editing = true }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Edit, "Edit surcharge", tint = Color(0xFF0166FF), modifier = Modifier.size(16.dp))
                        }
                    }
                    if (editing || suggested == null) {
                        OutlinedTextField(surcharge, { surcharge = it; error = null }, label = { Text("Surcharge (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                    }
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(26.dp))
                    Button(onClick = { error = onConfirm(surcharge) }, enabled = total != null, shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0166FF)), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Book Now", fontSize = 18.sp, fontWeight = FontWeight.Normal) }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun UpgradeFareIcon() {
    Canvas(Modifier.size(30.dp)) {
        val blue = Color(0xFF0166FF); val bg = Color(0xFFF0F7FC)
        drawRoundRect(blue, topLeft = Offset(0f, size.height*.12f), size = androidx.compose.ui.geometry.Size(size.width, size.height*.76f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        drawCircle(bg, size.height*.12f, Offset(0f, size.height*.5f))
        drawCircle(bg, size.height*.12f, Offset(size.width, size.height*.5f))
        drawLine(Color.White, Offset(size.width*.26f, size.height*.2f), Offset(size.width*.26f, size.height*.8f),
            strokeWidth = 1.3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 1.5.dp.toPx())))
        listOf(.3f, .5f, .7f).forEach { y -> drawLine(Color.White, Offset(size.width*.4f, size.height*y), Offset(size.width*.8f, size.height*y), strokeWidth = 1.3.dp.toPx()) }
    }
}
