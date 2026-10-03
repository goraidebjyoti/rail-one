@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.railone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal const val DEFAULT_SERVICE_NO = "R28199"
internal const val DEFAULT_IR_NO = "19AAAGMO289C1ZC"

internal val TRAIN_TYPE_OPTIONS = listOf("ORDINARY", "MAIL/EXPRESS", "SUPERFAST", "AC EMU TRAIN")
internal val TICKET_TYPE_OPTIONS = listOf("JOURNEY", "RETURN")
internal val CLASS_OPTIONS = listOf("SECOND", "FIRST")

internal val DATE_TIME_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).apply { isLenient = false }
internal val TICKET_DATE_TIME_FORMAT = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US)

/** Current device date & time as "dd/MM/yyyy HH:mm" (24-hour clock). */
internal fun currentDateTimeString(): String = DATE_TIME_FORMAT.format(Calendar.getInstance().time)

/** Adds [hours] to a "dd/MM/yyyy HH:mm" string; returns null if [dateTime] isn't fully valid yet. */
internal fun addHours(dateTime: String, hours: Int): String? = try {
    val parsed = DATE_TIME_FORMAT.parse(dateTime)
    if (parsed == null) {
        null
    } else {
        val cal = Calendar.getInstance()
        cal.time = parsed
        cal.add(Calendar.HOUR_OF_DAY, hours)
        DATE_TIME_FORMAT.format(cal.time)
    }
} catch (e: Exception) {
    null
}

/** Converts "dd/MM/yyyy HH:mm" to the ticket's display format, e.g. "21 Sep 2026, 11:07". */
internal fun toTicketDisplayDateTime(dateTime: String): String = try {
    val parsed = DATE_TIME_FORMAT.parse(dateTime)
    if (parsed == null) dateTime else TICKET_DATE_TIME_FORMAT.format(parsed)
} catch (e: Exception) {
    dateTime
}

/** Formats a fare string to always show exactly two decimal places. */
internal fun formatFare(value: String): String {
    val number = value.trim().toDoubleOrNull() ?: return "0.00"
    return String.format(Locale.US, "%.2f", number)
}

/** Keeps only digits and a single decimal point while typing a fare amount. */
internal fun sanitizeFareInput(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    if (firstDot == -1) return filtered
    return filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
}

internal data class TicketData(
    val passengerName: String,
    val mobile: String,
    val origin: String,
    val distance: String,
    val destination: String,
    val bookingDateTime: String,
    val via: String,
    val adults: String,
    val children: String,
    val bookedOn: String,
    val validTill: String,
    val className: String,
    val trainType: String,
    val ticketType: String,
    val fare: String,
    val irNumber: String,
    val journeyTicket: String,
    val serviceNo: String,
)

/** A saved journey's re-usable booking details (timing fields are intentionally
 * excluded, since those always default to "now"). One passenger can have several
 * of these — one per distinct route — since the id is passenger + route. */
internal data class SavedJourney(
    val id: String,
    val passengerName: String,
    val mobile: String,
    val origin: String,
    val distance: String,
    val destination: String,
    val via: String,
    val adults: String,
    val children: String,
    val className: String,
    val trainType: String,
    val ticketType: String,
    val fare: String,
    val irNumber: String,
    val serviceNo: String,
) {
    val label: String get() = "$passengerName  —  $origin → $destination"
}

internal const val PROFILES_PREFS = "rail_one_profiles"
internal const val SAVED_JOURNEYS_KEY = "saved_journeys_json"

internal fun loadSavedJourneys(context: Context): List<SavedJourney> {
    val prefs = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(SAVED_JOURNEYS_KEY, null) ?: return emptyList()
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { i ->
            val json = array.optJSONObject(i) ?: return@mapNotNull null
            SavedJourney(
                id = json.optString("id"),
                passengerName = json.optString("passengerName"),
                mobile = json.optString("mobile"),
                origin = json.optString("origin"),
                distance = json.optString("distance"),
                destination = json.optString("destination"),
                via = json.optString("via"),
                adults = json.optString("adults"),
                children = json.optString("children"),
                className = json.optString("className"),
                trainType = json.optString("trainType"),
                ticketType = json.optString("ticketType"),
                fare = json.optString("fare"),
                irNumber = json.optString("irNumber"),
                serviceNo = json.optString("serviceNo"),
            )
        }.sortedBy { it.label.lowercase(java.util.Locale.ROOT) }
    } catch (e: Exception) {
        emptyList()
    }
}

internal val HeaderBlue = Color(0xFF0166FF)
internal val PageBg = Color(0xFFE9EDF9)
internal val TextBlue = Color(0xFF303C68)
// Two near-black tones of the harlequin texture (dark = base fill, light = diamonds).
internal val TicketBlack = Color(0xFF121217)
internal val DiamondLight = Color(0xFF1B1B21)
internal val Yellow = Color(0xFFFFF52D)
internal val RedOrange = Color(0xFFFF4A28)
internal val DateOrange = Color(0xFFFFA21A)
// Accent colours for the ticket's strips. One is picked at random every time
// a ticket is generated.
internal val AccentBlue = Color(0xFF67CDE6)
internal val AccentGreen = Color(0xFFA4D67E)
internal val AccentViolet = Color(0xFFB8A0F0)
internal val ACCENT_COLORS = listOf(AccentBlue, AccentGreen, AccentViolet)
internal const val TICKET_COUNTDOWN_SECONDS = 300
internal val RailwayGrey = Color(0xFF9A9AA5)
internal val BookingGrey = Color(0xFFB9BAC1)
internal val ViaBoxBg = Color(0xFFF8F7F8)
internal val NoteBg = Color(0xFFFFF0F2)
internal val TicketBody = Color(0xFFFFFBFB)
internal val GreenBg = Color(0xFFE0F2E3)
internal val GreenText = Color(0xFF34C264)

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        val splashDeadline = android.os.SystemClock.elapsedRealtime() + 350
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { savedInstanceState == null && android.os.SystemClock.elapsedRealtime() < splashDeadline }
        val session = androidx.lifecycle.ViewModelProvider(this)[LoginSession::class.java]
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { MaterialTheme { LaunchGate(session) } }
    }
}

internal fun sanitizeAlphaNumeric15(value: String): String =
    value.uppercase(java.util.Locale.ROOT).filter { it in '0'..'9' || it in 'A'..'Z' }.take(15)

internal fun generateJourneyTicket(): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".filter { it != 'X' }
    val counts = mutableMapOf<Char, Int>()
    val output = StringBuilder("X")
    var previous: Char? = null
    while (output.length < 10) {
        val candidates = alphabet.filter { candidate ->
            candidate != previous && (counts[candidate] ?: 0) < 2
        }
        if (candidates.isEmpty()) break
        val next = candidates[Random.nextInt(candidates.length)]
        output.append(next)
        counts[next] = (counts[next] ?: 0) + 1
        previous = next
    }
    return output.toString().padEnd(10, '0').take(10)
}

@Composable
internal fun InputScreen(
    data: TicketData,
    savedJourneys: List<SavedJourney>,
    setPassengerName: (String) -> Unit,
    setMobile: (String) -> Unit,
    setOrigin: (String) -> Unit,
    setDistance: (String) -> Unit,
    setDestination: (String) -> Unit,
    setVia: (String) -> Unit,
    setAdults: (String) -> Unit,
    setChildren: (String) -> Unit,
    setBookedOn: (String) -> Unit,
    setValidTill: (String) -> Unit,
    resetBookedOnToNow: () -> Unit,
    setClassName: (String) -> Unit,
    setTrainType: (String) -> Unit,
    setTicketType: (String) -> Unit,
    setFare: (String) -> Unit,
    onFareFocusLost: () -> Unit,
    setIrNumber: (String) -> Unit,
    setServiceNo: (String) -> Unit,
    onLoadJourney: (String) -> Unit,
    onSaveJourney: () -> Unit,
    onDeleteJourney: (String) -> Unit,
    onGenerateTicket: () -> Unit,
    onBack: () -> Unit,
    passengers: List<Passenger> = emptyList(),
    onLoadPassenger: (Passenger) -> Unit = {},
    templateSaveLabel: String = "Save As Journey Template",
    error: String? = null,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        BookingHeader(onBack = onBack)

        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (savedJourneys.isNotEmpty()) {
                SectionCard(title = "Saved Journeys", icon = Icons.Default.History) {
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        "Load a reusable journey template. " +
                            "Passenger, route and preferences are copied; dates reset to now.",
                        fontSize = 12.sp,
                        color = Color(0xFF7A7B84)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = "Select a saved journey",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Load saved journey") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            colors = fieldColors()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            savedJourneys.forEach { journey ->
                                DropdownMenuItem(
                                    text = { Text(journey.label, fontSize = 13.sp) },
                                    onClick = {
                                        onLoadJourney(journey.id)
                                        expanded = false
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                onDeleteJourney(journey.id)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete saved journey",
                                                tint = Color(0xFFB3261E),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            SectionCard(title = "Passenger Details") {
                if (passengers.isNotEmpty()) {
                    var pickPassenger by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { pickPassenger = true }) { Text("Use Saved Passenger") }
                        DropdownMenu(expanded = pickPassenger, onDismissRequest = { pickPassenger = false }) {
                            passengers.forEach { passenger ->
                                DropdownMenuItem(text = { Text(passenger.name) }, onClick = {
                                    onLoadPassenger(passenger)
                                    pickPassenger = false
                                })
                            }
                        }
                    }
                }
                Text("Lead passenger / contact. Adult and child counts are set below.", fontSize = 12.sp, color = TextBlue)
                Field("Passenger Name", data.passengerName, setPassengerName, Modifier.fillMaxWidth())
                Field("Mobile Number", data.mobile, setMobile, Modifier.fillMaxWidth(), KeyboardType.Phone)
            }

            SectionCard(title = "Journey Route") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Field("From Station", data.origin, setOrigin, Modifier.weight(1f))
                    Field("To Station", data.destination, setDestination, Modifier.weight(1f))
                }
                Field("Distance (km)", data.distance, setDistance, Modifier.fillMaxWidth(), KeyboardType.Decimal)
                Text(
                    "From/To station and Via are always saved in UPPERCASE.",
                    fontSize = 11.sp,
                    color = Color(0xFF7A7B84)
                )
                Field("Via", data.via, setVia, Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Field("Adults", data.adults, setAdults, Modifier.weight(1f), KeyboardType.Number)
                    Field("Children", data.children, setChildren, Modifier.weight(1f), KeyboardType.Number)
                }
            }

            SectionCard(title = "Booking Time") {
                Text(
                    "Defaults to your device's current date & time. Valid Till auto-fills 3 hours later — both stay editable.",
                    fontSize = 11.sp,
                    color = Color(0xFF7A7B84)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Field("Booked On (dd/MM/yyyy HH:mm)", data.bookedOn, setBookedOn, Modifier.weight(1f))
                    OutlinedButton(
                        onClick = resetBookedOnToNow,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text("Now", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Field("Valid Till (dd/MM/yyyy HH:mm)", data.validTill, setValidTill, Modifier.fillMaxWidth())
            }

            SectionCard(title = "Ticket Details") {
                DropdownField("Class", data.className, CLASS_OPTIONS, setClassName, Modifier.fillMaxWidth())
                DropdownField("Train Type", data.trainType, TRAIN_TYPE_OPTIONS, setTrainType, Modifier.fillMaxWidth())
                DropdownField("Ticket Type", data.ticketType, TICKET_TYPE_OPTIONS, setTicketType, Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Field(
                        "Fare (₹)",
                        data.fare,
                        setFare,
                        Modifier.weight(1f).onFocusChanged { if (!it.isFocused) onFareFocusLost() },
                        KeyboardType.Decimal
                    )
                    Field("IR No. (15 characters)", data.irNumber, setIrNumber, Modifier.weight(1f))
                }
                Field("Service No.", data.serviceNo, setServiceNo, Modifier.fillMaxWidth())
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(
                onClick = onSaveJourney,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.4.dp, HeaderBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HeaderBlue)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(templateSaveLabel, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = onGenerateTicket,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HeaderBlue)
            ) {
                Text("CREATE TICKET", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
        }
    }
}

@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = HeaderBlue,
    unfocusedBorderColor = Color(0xFFB7C4E2),
    focusedLabelColor = HeaderBlue,
    unfocusedLabelColor = TextBlue
)

@Composable
internal fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE7E7EC), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = HeaderBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextBlue)
        }
        content()
    }
}

@Composable
internal fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            colors = fieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        colors = fieldColors()
    )
}

@Composable
private fun BookingHeader(onBack: () -> Unit, mobile: String? = null) {
    Box(Modifier.fillMaxWidth().background(HeaderBlue).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack,
                modifier = Modifier.size(40.dp).border(1.dp, Color.White, CircleShape)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Booking Details", color = Color.White, fontSize = 20.sp,
                    lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
                if (mobile != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Mobile: $mobile", color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
internal fun TicketScreen(data: TicketData, secondsLeft: Int, accent: Color, onBack: () -> Unit, status: String, onConnecting: () -> Unit) {
    val qrPayload = "Status=$status\n" + buildQrPayload(data)
    val qrBitmap = remember(qrPayload) { generateQrBitmap(qrPayload, 560) }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        BookingHeader(onBack = onBack, mobile = data.mobile)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {

        // Flush, square-edged strip — no card margin and no gap from the header.
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                "Thank You ${data.passengerName}, Happy Journey !",
                fontSize = 14.sp,
                color = TextBlue
            )
        }

        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            DynamicTicket(data, secondsLeft, accent)
            TicketBody(data, accent)

            // The refund note is a separate element, outside the journey-ticket card.
            Spacer(Modifier.height(14.dp))
            TicketNote()

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onConnecting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.4.dp, HeaderBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = HeaderBlue
                )
            ) {
                Text("Book Connecting Journey", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }

        // QR panel has square edges and reaches both screen edges. No cutout
        // notches here — those only belong on the ticket card above.
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Ticket QR code",
                    modifier = Modifier.size(250.dp)
                )
            }
        }

        // Soft shadow-like band so the white QR panel's bottom margin reads
        // clearly above the "Do you know?" panel — a bit more breathing
        // room than a bare divider line.
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFD7D9DE), Color.White)))
        )

        // Informational panel has square edges and reaches both screen edges.
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Text("Do you know?", fontSize = 17.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("IR recovers only 57% of cost of travel on an average.", fontSize = 17.sp, color = Color(0xFF6B6C75), lineHeight = 23.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "This ticket is booked on a personal user ID. It’s sale/purchase is an offence u/s 143 of the Railways Act, 1989",
                fontSize = 17.sp,
                color = Color(0xFF6B6C75),
                lineHeight = 23.sp
            )
            Spacer(Modifier.height(10.dp))
            Text("For enquiry and integrated railway helpline, please dial 139.", fontSize = 17.sp, color = Color(0xFF6B6C75), lineHeight = 23.sp)
        }
        Spacer(Modifier.height(18.dp))
        }
    }
}

// Harlequin (argyle) texture across the black preview panel, matched to the
// reference photo: hard-edged, TALL diamonds (about 37dp wide x 64dp high, a
// 1 : sqrt(3) rhombus) in two near-black tones that alternate like a checkerboard.
// The base fill (TicketBlack) is the dark tone; only the lighter diamonds are
// drawn here, all in one Path. The phase (where the first diamond sits) was
// measured from the reference so the pattern lines up the same way.
internal fun Modifier.diamondWatermark(
    tileWidth: Dp = 37.dp,
    tileHeight: Dp = 64.dp,
    color: Color = DiamondLight
): Modifier = this.drawWithCache {
    val w = tileWidth.toPx()
    val h = tileHeight.toPx()
    // Centre of one light diamond, measured from the panel's top-left corner.
    val originX = w * (20.5f / 37f)
    val originY = h * (10f / 64f)
    val path = Path()
    var j = -1
    while (originY + j * h - h / 2f < size.height) {
        var i = -1
        while (originX + i * w - w / 2f < size.width) {
            val cx = originX + i * w
            val cy = originY + j * h
            path.moveTo(cx, cy - h / 2f)
            path.lineTo(cx + w / 2f, cy)
            path.lineTo(cx, cy + h / 2f)
            path.lineTo(cx - w / 2f, cy)
            path.close()
            i++
        }
        j++
    }
    // Compose does not clip drawing to the layout bounds, and the first row/column
    // of diamonds start outside the panel — clip so nothing spills onto the
    // coloured strips above and below the black box.
    onDrawBehind { clipRect { drawPath(path, color = color) } }
}

@Composable
internal fun DynamicTicket(data: TicketData, secondsLeft: Int, accent: Color) {
    // The strip under the black box is a progress bar: it grows from the left
    // edge as the countdown runs down, reaching full width at 00:00.
    val elapsedFraction = (TICKET_COUNTDOWN_SECONDS - secondsLeft).coerceIn(0, TICKET_COUNTDOWN_SECONDS) /
        TICKET_COUNTDOWN_SECONDS.toFloat()
    val progress by animateFloatAsState(
        targetValue = elapsedFraction,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "countdownProgress"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(PageBg)
    ) {
        // Full-width blue tint above the black preview — same thickness and
        // curve as the blue strip at the end of the ticket card. Its height
        // matches the corner radius above so the rounding never reaches into
        // the black box.
        Box(Modifier.fillMaxWidth().height(14.dp).background(accent))

        Row(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(TicketBlack)
                .diamondWatermark(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exactly one dashed divider on each side of the black panel.
            RailwaySideBrand("INDIAN RAILWAYS", drawDividerOnRight = true)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Dynamic preview will close in",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(1.dp))
                FallingCountdown(secondsLeft)
                Spacer(Modifier.height(0.dp))
                Text("Ticket Booking Date & Time", color = BookingGrey, fontSize = 13.sp)
                Spacer(Modifier.height(1.dp))
                Text(
                    data.bookingDateTime,
                    color = DateOrange,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(data.serviceNo, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text("Ticket is Non-Transferable", color = Color.White, fontSize = 13.sp)
            }
            RailwaySideBrand("भारतीय रेल", drawDividerOnRight = false, textSizeSp = 21.5f)
        }

        // Progress bar under the black preview: starts at the left edge and
        // grows with the countdown; the rest of the row shows the page
        // background. Half as thick as the strip above the black preview.
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(5.dp)
                .background(accent)
        )
    }
}

/** Countdown text whose digits fall: the new value drops in from above while the old one falls away. */
@Composable
internal fun FallingCountdown(secondsLeft: Int) {
    val minutes = (secondsLeft / 60).coerceAtLeast(0)
    val seconds = (secondsLeft % 60).coerceAtLeast(0)
    Row(verticalAlignment = Alignment.CenterVertically) {
        FallingDigits("%02d".format(minutes))
        CountdownText(":")
        FallingDigits("%02d".format(seconds))
    }
}

@Composable
internal fun FallingDigits(value: String) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (slideInVertically(animationSpec = tween(450, easing = LinearOutSlowInEasing)) { fullHeight -> -fullHeight } +
                fadeIn(animationSpec = tween(300))) togetherWith
                (slideOutVertically(animationSpec = tween(450, easing = FastOutLinearInEasing)) { fullHeight -> fullHeight } +
                    fadeOut(animationSpec = tween(300))) using SizeTransform(clip = false)
        },
        label = "fallingDigits"
    ) { text ->
        CountdownText(text)
    }
}

@Composable
internal fun CountdownText(text: String) {
    Text(text, color = RedOrange, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
internal fun RailwaySideBrand(text: String, drawDividerOnRight: Boolean, textSizeSp: Float = 19f) {
    Box(
        Modifier
            .width(48.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // The reference has only two dashed lines total: one inner divider
            // on each side of the central ticket content.
            val effect = PathEffect.dashPathEffect(floatArrayOf(28f, 12f), 0f)
            val x = if (drawDividerOnRight) size.width - 1.5f else 1.5f
            drawLine(
                color = RailwayGrey,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2.4f,
                pathEffect = effect
            )

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                native.rotate(-90f, size.width / 2f, size.height / 2f)
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(154, 154, 165)
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    textAlign = AndroidPaint.Align.CENTER
                    textSize = textSizeSp.sp.toPx()
                }
                val maxTextWidth = size.height * 0.90f
                val measured = paint.measureText(text)
                if (measured > maxTextWidth) {
                    paint.textSize *= (maxTextWidth / measured)
                }
                val xText = size.width / 2f
                val yText = size.height / 2f - (paint.ascent() + paint.descent()) / 2f
                native.drawText(text, xText, yText, paint)
                native.restore()
            }
        }
    }
}

@Composable
internal fun TicketBody(data: TicketData, accent: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(TicketBody)
    ) {
        // All the regular field content stays inset from the card edges...
        Column(Modifier.padding(horizontal = 12.dp)) {
            Spacer(Modifier.height(13.5.dp))
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(0.7f)) {
                    Text(
                        "Journey Ticket",
                        fontSize = 11.sp,
                        color = Color(0xFF7A7B84),
                        fontWeight = FontWeight.Bold,
                        lineHeight = 12.sp
                    )
                    Spacer(Modifier.height(2.5.dp))
                    Text(
                        data.journeyTicket,
                        fontSize = 14.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        lineHeight = 17.sp
                    )
                }
                // Compact pill pinned to the top-right, level with the "Journey
                // Ticket" line. It is shorter than the label+code block, so it never
                // adds height and the gap below the code stays the same as between
                // the other rows.
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .height(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(GreenBg)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "● ACTIVE",
                        color = GreenText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            TwoColumnField("Source", data.origin, "Destination", data.destination, boldValues = true)
            Spacer(Modifier.height(12.dp))
            TwoColumnField("Distance", data.distance, "Passenger", "${data.adults} Adult, ${data.children} Child", boldValues = true)
            Spacer(Modifier.height(12.dp))
            TwoColumnField("Ticket Type", data.ticketType, "Train Types", data.trainType, boldValues = true)
            Spacer(Modifier.height(12.dp))
            TwoColumnField("Class", data.className, "Fare", "₹${data.fare}", boldValues = true)

            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ViaBoxBg)
                    .border(1.dp, Color(0xFFE3E1E3), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ViaRouteIcon()
                    Spacer(Modifier.width(7.dp))
                    Text("Via: ${data.via}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(4.5.dp))
            Text("IR:${data.irNumber}", color = Color.Black, fontSize = 13.sp, letterSpacing = 0.6.sp)
        }

        // Cutout notches sit right at the card's true edges, so this divider
        // spans the full card width rather than the inset content width.
        TicketCutoutDivider()

        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(18.dp))
            Text(
                "*Valid for start of journey within 1 hour or until departure of the first train.",
                fontSize = 11.sp,
                color = Color(0xFF7A7B84),
                lineHeight = 15.sp
            )
            Spacer(Modifier.height(10.dp))
        }

        // Blue border below "Valid for..." spans edge to edge of the white card,
        // as a solid color — the same blue used around the black preview box.
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(accent)
        )
    }
}

@Composable
internal fun TicketNote() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(NoteBg)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(
            "Note: This ticket is non refundable. Ticket is stored locally on the device. Please do not change your handset or perform factory reset.",
            fontSize = 13.sp,
            color = Color(0xFFFF3B4E),
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
internal fun ViaRouteIcon() {
    Canvas(Modifier.size(18.dp)) {
        val stroke = 1.7.dp.toPx()
        val c = TextBlue
        // Small branching route/track mark inspired by the reference icon.
        drawLine(c, Offset(2f, size.height * 0.66f), Offset(size.width * 0.48f, size.height * 0.66f), strokeWidth = stroke)
        drawLine(c, Offset(size.width * 0.48f, size.height * 0.66f), Offset(size.width * 0.80f, size.height * 0.38f), strokeWidth = stroke)
        drawLine(c, Offset(size.width * 0.48f, size.height * 0.66f), Offset(size.width * 0.80f, size.height * 0.84f), strokeWidth = stroke)
        drawCircle(c, radius = 1.8.dp.toPx(), center = Offset(2f, size.height * 0.66f))
        drawCircle(c, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.80f, size.height * 0.38f))
        drawCircle(c, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.80f, size.height * 0.84f))
    }
}

@Composable
internal fun TwoColumnField(leftTitle: String, leftValue: String, rightTitle: String, rightValue: String, boldValues: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(leftTitle, fontSize = 11.sp, color = Color(0xFF7A7B84), fontWeight = FontWeight.Bold, lineHeight = 13.sp)
            Spacer(Modifier.height(3.5.dp))
            Text(leftValue, fontSize = 14.sp, color = Color.Black, fontWeight = if (boldValues) FontWeight.Bold else FontWeight.Normal, lineHeight = 17.sp)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(rightTitle, fontSize = 11.sp, color = Color(0xFF7A7B84), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, lineHeight = 13.sp)
            Spacer(Modifier.height(3.5.dp))
            Text(rightValue, fontSize = 14.sp, color = Color.Black, fontWeight = if (boldValues) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.End, lineHeight = 17.sp)
        }
    }
}

@Composable
internal fun TicketCutoutDivider() {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        val r = 12.dp.toPx()
        val cy = size.height / 2f
        // Only the inner half of each circle is visible, creating the ticket
        // notches right at both true edges of the white card.
        drawCircle(
            color = PageBg,
            radius = r,
            center = Offset(0f, cy)
        )
        drawCircle(
            color = PageBg,
            radius = r,
            center = Offset(size.width, cy)
        )
        val effect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
        drawLine(
            color = Color(0xFFD7D9E0),
            start = Offset(r, cy),
            end = Offset(size.width - r, cy),
            strokeWidth = 1.3f,
            pathEffect = effect
        )
    }
}

internal fun buildQrPayload(data: TicketData): String = buildString {
    appendLine("RAIL ONE TICKET")
    appendLine("NOT A REAL TICKET / NOT VALID FOR TRAVEL")
    appendLine("Journey Ticket=${data.journeyTicket}")
    appendLine("Service No=${data.serviceNo}")
    appendLine("Passenger=${data.passengerName}")
    appendLine("Mobile=${data.mobile}")
    appendLine("Source=${data.origin}")
    appendLine("Destination=${data.destination}")
    appendLine("Distance=${data.distance}")
    appendLine("Booking Date Time=${data.bookingDateTime}")
    appendLine("Via=${data.via}")
    appendLine("Adults=${data.adults}")
    appendLine("Children=${data.children}")
    appendLine("Booked On=${data.bookedOn}")
    appendLine("Valid Till=${data.validTill}")
    appendLine("Class=${data.className}")
    appendLine("Train Type=${data.trainType}")
    appendLine("Ticket Type=${data.ticketType}")
    appendLine("Fare=${data.fare}")
    appendLine("IR=${data.irNumber}")
    appendLine("Status=ACTIVE")
    appendLine("Non-Transferable=true")
}

internal fun generateQrBitmap(payload: String, size: Int): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
