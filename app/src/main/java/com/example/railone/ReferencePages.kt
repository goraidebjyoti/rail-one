package com.example.railone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Measurements use a 360dp canvas derived from the 921px reference screenshots.
private val Blue = Color(0xFF0166FF)
private val Ink = Color(0xFF0C2065)
private val PaleBlue = Color(0xFFE2F9FF)
private val PalePeach = Color(0xFFFFF1E4)
private val PaleViolet = Color(0xFFEFEEFE)
private val Gold = Color(0xFFF9BF53)
private val Orange = Color(0xFFEBA95F)
private val Muted = Color(0xFF8B8B94)
private val MenuViolet = Color(0xFF9288F4)
private val NavMuted = Color(0xFF99BCEB)
internal const val TICKET_DELETE_HOLD_MILLIS = 1000L

@Composable
private fun Picture(resource: Int, description: String?, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit) {
    Image(painterResource(resource), description, modifier, contentScale = scale)
}
@Composable
private fun ProfileAvatar(profile: UserProfile, description: String, modifier: Modifier) {
    val context = LocalContext.current
    val photo = remember(profile.photoFile) {
        runCatching { profilePhotoFile(context, profile.photoFile)?.let { android.graphics.BitmapFactory.decodeFile(it.path) }?.asImageBitmap() }.getOrNull()
    }
    if (photo == null) Picture(R.drawable.profile_avatar, description, modifier)
    else Image(photo, description, modifier, contentScale = ContentScale.Crop)
}
@Composable
private fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
}
@Composable
private fun CircleBack(onBack: () -> Unit, light: Boolean = false) {
    IconButton(onClick = onBack, modifier = Modifier.size(40.dp).border(1.dp,
        if (light) Color.White else Color(0xFF8CD7F1), CircleShape)) {
        Icon(Icons.Default.ArrowBack, "Back", tint = if (light) Color.White else Blue, modifier = Modifier.size(23.dp))
    }
}
@Composable
internal fun ReferenceBottomBar(tab: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Blue).navigationBarsPadding().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("Home" to R.drawable.nav_home, "My Bookings" to R.drawable.nav_bookings,
            "You" to R.drawable.nav_you, "Menu" to R.drawable.nav_menu).forEach { (label, icon) ->
            val tint = if (tab == label) Color.White else NavMuted
            Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(label) }.padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Image(painterResource(icon), null, modifier = Modifier.size(25.dp), colorFilter = ColorFilter.tint(tint))
                Text(label, color = tint, fontSize = 12.sp, lineHeight = 15.sp)
            }
        }
    }
}
@Composable
private fun HomeHeader(onService: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).statusBarsPadding().height(60.dp).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Picture(R.drawable.language_button, "Language", Modifier.size(37.dp).clickable { onService("Language selection") })
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Picture(R.drawable.rail_one_logo, "Rail One", Modifier.width(112.dp).height(28.dp))
        }
        IconButton(onClick = { onService("Notifications") }, modifier = Modifier.size(37.dp).border(1.dp, Color(0xFFECECEC), CircleShape)) {
            Icon(Icons.Default.NotificationsNone, "Notifications", tint = Color.Black, modifier = Modifier.size(25.dp))
        }
    }
}
@Composable
private fun PlannerTile(label: String, resource: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Picture(resource, label, Modifier.fillMaxWidth().aspectRatio(267f / 224f))
        Text(label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 4.dp))
    }
}
@Composable
private fun Offering(label: String, resource: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Picture(resource, label, Modifier.fillMaxWidth().aspectRatio(171f / 155f))
        Text(label, color = Ink, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 16.sp,
            modifier = Modifier.padding(top = 6.dp))
    }
}
@Composable
internal fun HomePage(state: JourneyState, now: Long, onNew: () -> Unit, onBookings: () -> Unit,
    onView: (StoredTicket) -> Unit, onRepeat: (StoredTicket) -> Unit, onService: (String) -> Unit,
    onSocial: (String) -> Unit = {}) {
    val upcoming = state.tickets.filter { it.status(now) == "Upcoming" }.sortedBy { parseBookingTime(it.data.bookedOn) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        HomeHeader(onService)
        LazyColumn(modifier = Modifier.testTag("home-content"),
            contentPadding = PaddingValues(top = 32.dp, bottom = 28.dp)) {
            item {
                Text(if (state.profile.name.isBlank()) "Hi, Traveller!" else "Hi, ${state.profile.name}!",
                    color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 9.dp))
                Spacer(Modifier.height(17.dp))
                Heading("Journey Planner", Modifier.padding(horizontal = 9.dp))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.padding(horizontal = 9.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PlannerTile("Reserved", R.drawable.planner_reserved, Modifier.weight(1f)) { onService("Reserved bookings") }
                    PlannerTile("Unreserved", R.drawable.planner_unreserved, Modifier.weight(1f).semantics { contentDescription = "New Ticket" }, onNew)
                    PlannerTile("Platform", R.drawable.planner_platform, Modifier.weight(1f)) { onService("Platform tickets") }
                }
            }
            if (state.showServices) item {
                Spacer(Modifier.height(17.dp))
                Heading("More Offerings", Modifier.padding(horizontal = 9.dp))
                Spacer(Modifier.height(20.dp))
                val offerings = listOf("Search\nTrains" to R.drawable.service_search, "PNR\nStatus" to R.drawable.service_pnr,
                    "Coach\nPosition" to R.drawable.service_coach, "Track Your\nTrain" to R.drawable.service_track,
                    "Order\nFood" to R.drawable.service_food, "File\nRefund" to R.drawable.service_refund,
                    "Rail\nMadad" to R.drawable.service_help, "Go To\nWAVES" to R.drawable.service_waves)
                offerings.chunked(4).forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) Spacer(Modifier.height(36.dp))
                    Row(Modifier.padding(horizontal = 9.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { (label, art) -> Offering(label, art, Modifier.weight(1f)) { onService(label.replace('\n', ' ')) } }
                    }
                }
            }
            if (upcoming.isNotEmpty()) item {
                Spacer(Modifier.height(32.dp))
                Row(Modifier.padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Heading("Upcoming Journey", Modifier.weight(1f))
                    if (upcoming.size > 1) Text("View All", fontSize = 11.sp, color = Blue, modifier = Modifier.clickable(onClick = onBookings).padding(vertical = 4.dp))
                }
                Spacer(Modifier.height(14.dp))

            }
            if (upcoming.isNotEmpty()) item {
                val pager = rememberPagerState(pageCount = { upcoming.size })
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    HorizontalPager(state = pager, pageSize = PageSize.Fixed((maxWidth - 90.dp).coerceAtLeast(1.dp)),
                        contentPadding = PaddingValues(horizontal = 45.dp), pageSpacing = 12.dp,
                        key = { upcoming[it].id }, modifier = Modifier.fillMaxWidth().testTag("upcoming-journeys")) { page ->
                        val ticket = upcoming[page]
                        Box(Modifier.padding(vertical = 6.dp).testTag("home-ticket-${ticket.id}")) {
                            HomeJourneyCard(ticket, onView, onRepeat)
                        }
                    }
                }
                if (upcoming.size > 1) Text("${pager.currentPage + 1} / ${upcoming.size}",
                    color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
            item {
                Spacer(Modifier.height(20.dp))
                Heading("Do You Know?", Modifier.padding(horizontal = 9.dp))
                Spacer(Modifier.height(12.dp))
                FactsCarousel()
                Spacer(Modifier.height(40.dp))
                Heading("Follow Us On Social Media Platforms", Modifier.padding(horizontal = 9.dp).testTag("social-heading"))
                Spacer(Modifier.height(18.dp))
                SocialBanner(onSocial)
            }
        }
    }
}
internal data class RailwaySocialLink(val label: String, val url: String, val left: Float, val width: Float)
internal val railwaySocialLinks = listOf(
    RailwaySocialLink("X", "https://x.com/RailMinIndia", 198f, 84f),
    RailwaySocialLink("Facebook", "https://www.facebook.com/RailMinIndia/", 309f, 84f),
    RailwaySocialLink("Instagram", "https://www.instagram.com/railminindia/", 432f, 76f),
    RailwaySocialLink("YouTube", "https://www.youtube.com/user/RailMinIndia", 548f, 84f),
)
@Composable
private fun SocialBanner(onOpen: (String) -> Unit) {
    // The four logos are part of the 830 x 390 artwork. Scale each individual
    // hit region with the image so that tapping a logo opens its own account.
    BoxWithConstraints(Modifier.padding(horizontal = 18.dp).fillMaxWidth()
        .aspectRatio(830f / 390f).clip(RoundedCornerShape(9.dp))) {
        Picture(R.drawable.social_banner, null, Modifier.matchParentSize(), ContentScale.FillBounds)
        railwaySocialLinks.forEach { link ->
            Box(Modifier.offset(x = maxWidth * (link.left / 830f), y = maxHeight * (148f / 390f))
                .size(width = maxWidth * (link.width / 830f), height = maxHeight * (94f / 390f))
                .semantics { contentDescription = "Ministry of Railways on ${link.label}" }
                .testTag("social-${link.label.lowercase(Locale.ROOT)}")
                .clickable { onOpen(link.url) })
        }
    }
}
private val railwayFacts = listOf(
    R.drawable.fact_first_train to "First ever passenger train was run between Bori Bandar to Thane on April 16, 1853.",
    R.drawable.fact_chenab to "Chenab Railway Bridge in Dharot, Jammu & Kashmir is the World's highest Railway Bridge.",
    R.drawable.fact_noney to "Noney Bridge is going to be world's tallest railway bridge pier at a height of 141 meters.",
    R.drawable.fact_hubballi to "Shree Siddharoodha Swamiji Railway Station Hubballi is world's longest Railway Platform with length of 1505 meters.",
    R.drawable.fact_electrification to "99% Electrification is achieved in Indian Railways.",
)
@Composable
private fun FactsCarousel() {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = LocalTextStyle.current.copy(color = Ink, fontSize = 12.sp,
        lineHeight = 16.sp, fontWeight = FontWeight.Light)
    // Measure every caption, including off-screen facts, at the actual font scale.
    val captionHeight = with(density) {
        railwayFacts.maxOf { (_, caption) ->
            measurer.measure(caption, style, constraints = Constraints(maxWidth = 145.dp.roundToPx() - 4.dp.roundToPx())).size.height
        }.toDp()
    }
    LazyRow(modifier = Modifier.height(145.dp * (327f / 369f) + 5.dp + captionHeight).testTag("railway-facts"),
        contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(railwayFacts) { (resource, caption) -> FactCard(resource, caption) }
    }
}
@Composable
private fun FactCard(resource: Int, text: String) {
    Column(Modifier.width(145.dp)) {
        Picture(resource, null, Modifier.fillMaxWidth().aspectRatio(369f / 327f).clip(RoundedCornerShape(11.dp)), ContentScale.Crop)
        Text(text, color = Ink, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
    }
}
private fun referenceDate(data: TicketData): String = parseBookingTime(data.bookedOn)?.let {
    SimpleDateFormat("EEE, dd MMM yy", Locale.US).format(Date(it))
} ?: data.bookedOn
@Composable
private fun HomeJourneyCard(ticket: StoredTicket, onView: (StoredTicket) -> Unit, onRepeat: (StoredTicket) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF514797), Color(0xFFBC80D4))), RoundedCornerShape(20.dp))
        .drawWithContent {
            drawContent()
            val x = size.width * .765f
            drawCircle(Color.White, 9.dp.toPx(), Offset(x, 0f))
            drawCircle(Color.White, 9.dp.toPx(), Offset(x, size.height))
        }.padding(horizontal = 10.dp, vertical = 16.dp)) {
        Text(referenceDate(ticket.data), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Light)
        HorizontalDivider(Modifier.padding(vertical = 9.dp), color = Color.White.copy(alpha = .35f))
        Row {
            Text(ticket.data.origin, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text(ticket.data.destination, color = Color.White, fontSize = 10.sp)
        }
        HorizontalDivider(Modifier.padding(vertical = 9.dp), color = Color.White.copy(alpha = .35f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Unreserved", color = Color(0xFFBCEDBC), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { onRepeat(ticket) }, border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                shape = CircleShape, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 9.dp)) {
                Text("Book Again", color = Color.White, fontSize = 10.sp, lineHeight = 12.sp)
            }
            OutlinedButton(onClick = { onView(ticket) }, border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                shape = CircleShape, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 9.dp)) {
                Text("View Details", color = Color.White, fontSize = 10.sp, lineHeight = 12.sp)
            }
        }
    }
}
// Outer edge incorporates the ticket's semicircular side cutouts; border follows them.
private fun BookingShape(footerHeightPx: Float): Shape = GenericShape { size, _ ->
    val w = size.width; val h = size.height; val corner = w * .027f; val radius = w * .047f
    val y = h - footerHeightPx
    moveTo(corner, 0f); lineTo(w - corner, 0f); quadraticBezierTo(w, 0f, w, corner)
    lineTo(w, y - radius); cubicTo(w - radius * 1.33f, y - radius, w - radius * 1.33f, y + radius, w, y + radius)
    lineTo(w, h - corner); quadraticBezierTo(w, h, w - corner, h); lineTo(corner, h)
    quadraticBezierTo(0f, h, 0f, h - corner); lineTo(0f, y + radius)
    cubicTo(radius * 1.33f, y + radius, radius * 1.33f, y - radius, 0f, y - radius)
    lineTo(0f, corner); quadraticBezierTo(0f, 0f, corner, 0f); close()
}
@Composable
private fun BookingCard(ticket: StoredTicket, now: Long, onView: (StoredTicket) -> Unit,
    onRepeat: (StoredTicket) -> Unit, onCancel: (StoredTicket) -> Unit, onDelete: (StoredTicket) -> Unit) {
    val status = ticket.status(now)
    var menu by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val shape = remember(density) { BookingShape(with(density) { 51.dp.toPx() }) }
    val deleteCallback by rememberUpdatedState(onDelete)
    val view = LocalView.current
    Column(Modifier.fillMaxWidth().heightIn(min = 170.dp).clip(shape).background(Color(0xFFF6F6F6)).border(.8.dp, bookingColour(status), shape)
        .testTag("booking-ticket-${ticket.id}")
        .semantics { customActions = listOf(CustomAccessibilityAction("Delete ticket") { deleteCallback(ticket); true }) }
        .pointerInput(ticket.id, view) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                val wasKeepingScreenOn = view.keepScreenOn
                view.keepScreenOn = true
                try {
                    // A release or scroll cancels the hold. Only a full one-second hold triggers it.
                    val finished = withTimeoutOrNull(TICKET_DELETE_HOLD_MILLIS) {
                        waitForUpOrCancellation()
                        true
                    }
                    if (finished == null) {
                        deleteCallback(ticket)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                } finally { view.keepScreenOn = wasKeepingScreenOn }
            }
        }) {
        Column(Modifier.fillMaxWidth().heightIn(min = 119.dp).padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFEAD9F0), shape = RoundedCornerShape(8.dp)) {
                    Text("Unreserved", color = Color(0xFFB769D0),
                        fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                }
                Spacer(Modifier.weight(1f))
                Box {
                    Column(Modifier.clickable { menu = true }, horizontalAlignment = Alignment.End) {
                        Text("UTS: ${ticket.data.journeyTicket}", color = Color(0xFF282828), fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(ticket.data.passengerName) }, onClick = { menu = false; onView(ticket) })
                        if (status == "Upcoming") DropdownMenuItem(text = { Text("Cancel ticket") }, onClick = { menu = false; onCancel(ticket) })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Ticket Type", color = Color(0xFFB0B0B0), fontSize = 12.sp, lineHeight = 14.sp)
                    Text(ticket.data.ticketType, color = Color(0xFF282828), fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Booking Date", color = Color(0xFFB0B0B0), fontSize = 12.sp, lineHeight = 14.sp)
                    Text(referenceDate(ticket.data), color = Color(0xFF282828), fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ticket.data.origin, modifier = Modifier.weight(1f), color = Color.Black, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 2)
                Text("— ${ticket.data.distance.trim().removeSuffix("km").trim()} km —", color = Color(0xFFB3B3BD), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp))
                Text(ticket.data.destination, modifier = Modifier.weight(1f), color = Color.Black, fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.End, maxLines = 2)
            }
        }
        Canvas(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 15.dp)) {
            drawLine(bookingColour(status), Offset.Zero, Offset(size.width, 0f), strokeWidth = .8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())))
        }
        Row(Modifier.fillMaxWidth().height(51.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).fillMaxHeight().clickable { onRepeat(ticket) }, contentAlignment = Alignment.Center) {
                Text("Book Again", color = Blue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.width(1.dp).height(16.dp).background(Color(0xFFAFB5BD)))
            Box(Modifier.weight(1f).fillMaxHeight().clickable { onView(ticket) }, contentAlignment = Alignment.Center) {
                Text("View Details", color = Blue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
@Composable
internal fun ReferenceBookingFilters(tickets: List<StoredTicket>, now: Long, filter: String, onFilter: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().border(2.dp, Color(0xFFD0D2D3), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        .background(Color(0xFFE3F5FD), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).navigationBarsPadding()
        .padding(horizontal = 6.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Upcoming", "Completed", "Cancelled", "All").forEach { label ->
            val selected = filter == label
            Column(Modifier.weight(1f).height(52.dp).background(if (selected) Color(0xFFF8FBFC) else Color(0xFFDFF2FD), RoundedCornerShape(9.dp))
                .border(if (selected) 1.dp else 0.dp, if (selected) Color.White else Color.Transparent, RoundedCornerShape(9.dp))
                .clickable { onFilter(label) }.semantics { contentDescription = "$label, ${tickets.count { label == "All" || it.status(now) == label }} tickets" },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Picture(if (selected) when (label) {
                    "Completed" -> R.drawable.booking_filter_completed
                    "Cancelled" -> R.drawable.booking_filter_cancelled
                    "All" -> R.drawable.booking_filter_all
                    else -> R.drawable.booking_filter_active
                } else R.drawable.booking_filter_inactive, null, Modifier.size(23.dp))
                Text(label, color = if (selected) bookingColour(label) else Muted, fontSize = 12.sp)
            }
        }
    }
}
internal fun bookingColour(status: String): Color = when (status) {
    "Completed" -> Color(0xFF299D5A)
    "Cancelled" -> Color(0xFFF2636A)
    "All" -> Blue
    else -> Orange
}
// Unreserved tickets have no separately scheduled journey date: their Booked On
// timestamp is also their journey timestamp.
internal fun sortedBookings(tickets: List<StoredTicket>, newestFirst: Boolean, sortBy: String): List<StoredTicket> {
    val comparator = compareBy<StoredTicket> {
        when (sortBy) {
            "Journey Date" -> parseBookingTime(it.data.bookedOn) ?: it.createdAt
            else -> parseBookingTime(it.data.bookedOn) ?: it.createdAt
        }
    }.thenBy { it.createdAt }.thenBy { it.id }
    return tickets.sortedWith(if (newestFirst) comparator.reversed() else comparator)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookingsPage(tickets: List<StoredTicket>, now: Long, filter: String, newestFirst: Boolean,
    onFilter: (String) -> Unit, onSort: () -> Unit, onNew: () -> Unit,
    onView: (StoredTicket) -> Unit, onRepeat: (StoredTicket) -> Unit, onCancel: (StoredTicket) -> Unit, onRefresh: () -> Unit, onBack: () -> Unit,
    onDelete: (StoredTicket) -> Unit = {}, sortBy: String = "Booking Date") {
    val matching = tickets.filter { filter == "All" || it.status(now) == filter }
    val sorted = sortedBookings(matching, newestFirst, sortBy)
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().background(Blue).statusBarsPadding().height(68.dp).padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleBack(onBack, true)
            Text("My Bookings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 18.dp))
            IconButton(onClick = onSort) { Picture(R.drawable.booking_sort, "Sort & Filters", Modifier.size(24.dp)) }
        }
        if (sorted.isNotEmpty()) Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(42.dp))
            Text("$filter (${matching.size})", color = bookingColour(filter), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Sync, "Refresh bookings", tint = Muted, modifier = Modifier.size(21.dp)) }
        }
        PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (sorted.isEmpty()) Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Picture(R.drawable.booking_empty, null, Modifier.width(90.dp).height(60.dp))
                    Spacer(Modifier.height(18.dp))
                    Text("No Tickets Found. Swipe down to refresh.", color = Color(0xFFB0B0B0), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            } else LazyColumn(modifier = Modifier.fillMaxSize().testTag("bookings-list"), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(sorted, key = { it.id }) { BookingCard(it, now, onView, onRepeat, onCancel, onDelete) }
            }
        }
    }
}

@Composable
private fun WalletRow(balance: Long, drawer: Boolean = false, onAdd: () -> Unit, onRefresh: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().background(if (drawer) PaleViolet else Color.White, CircleShape).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Picture(if (drawer) R.drawable.wallet_violet else R.drawable.wallet_green, null, Modifier.size(29.dp))
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text("R-Wallet", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Light)
            Text("₹ ${walletDisplay(balance)}", color = if (drawer) Color.Black else Color(0xFF005C49), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Local balance", color = Muted, fontSize = 8.sp)
        }
        if (!drawer) IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Sync, "Refresh wallet", tint = Blue)
        }
        Button(onClick = onAdd, shape = CircleShape,
            modifier = Modifier.height(if (drawer) 43.dp else 30.dp).testTag(if (drawer) "drawer-wallet-add" else "wallet-add"), contentPadding = PaddingValues(horizontal = if (drawer) 15.dp else 17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
            Text(if (drawer) "Add Money" else "Add", color = Color.White, fontSize = if (drawer) 14.sp else 13.sp)
        }
    }
}
@Composable
private fun AccountTile(label: String, resource: Int?, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(100.dp).background(color, RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(top = 14.dp, bottom = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (resource != null) Picture(resource, null, Modifier.size(31.dp)) else {
            Icon(Icons.Default.Fingerprint, null, tint = Blue, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(5.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(label, color = Ink, fontSize = 13.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
    }
}
@Composable
internal fun ProfilePage(state: JourneyState, onRefreshPassengers: () -> Unit, onBack: () -> Unit, onViewProfile: () -> Unit,
    onService: (String) -> Unit, onTransactions: () -> Unit, onProfile: () -> Unit, onAddPassenger: () -> Unit,
    onEditPassenger: (Passenger) -> Unit, onDeletePassenger: (Passenger) -> Unit,
    onNewTemplate: () -> Unit, onEditTemplate: (SavedJourney) -> Unit,
    onUseTemplate: (SavedJourney) -> Unit, onDeleteTemplate: (SavedJourney) -> Unit,
    onPhoto: () -> Unit = {}, onRemovePhoto: () -> Unit = {}, onWalletAdd: () -> Unit = {}, onWalletRefresh: () -> Unit = {}, onAccount: () -> Unit = onProfile,
    loginEnabled: Boolean = false, biometricEnabled: Boolean = false, onLoginSettings: () -> Unit = {}, onBiometricToggle: () -> Unit = {}, onAddRoute: () -> Unit = {}, onUseRoute: (SavedRoute) -> Unit = {},
    onEditRoute: (SavedRoute) -> Unit = {}, onDeleteRoute: (SavedRoute) -> Unit = {}, onSwitchUser: () -> Unit = {}) {
    var routesExpanded by rememberSaveable { mutableStateOf(false) }
    var templatesExpanded by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(Color.White).testTag("profile-content"), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(PaleBlue, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .statusBarsPadding().padding(top = 26.dp, bottom = 20.dp)) {
                Box(Modifier.padding(start = 15.dp)) { CircleBack(onBack) }
                Spacer(Modifier.height(20.dp))
                ProfileAvatar(state.profile, "Change profile picture", Modifier.size(72.dp).clip(CircleShape)
                    .align(Alignment.CenterHorizontally).clickable(onClick = onPhoto))
                Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onPhoto) { Text(if (state.profile.photoFile.isBlank()) "Upload Photo" else "Change Photo", color = Blue, fontSize = 11.sp) }
                    if (state.profile.photoFile.isNotBlank()) TextButton(onClick = onRemovePhoto) { Text("Remove", color = Blue, fontSize = 11.sp) }
                }
                Spacer(Modifier.height(13.dp))
                Text(state.profile.name.ifBlank { "Your Profile" }, fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.height(22.dp).clickable(onClick = onViewProfile), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, null, tint = Blue, modifier = Modifier.size(14.dp))
                        Text("View Details", fontSize = 14.sp, color = Blue, fontWeight = FontWeight.SemiBold)
                    }
                    Text(" | ", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 7.dp))
                    Row(Modifier.height(22.dp).clickable(onClick = onProfile), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, tint = Blue, modifier = Modifier.size(14.dp))
                        Text("Edit Details", fontSize = 14.sp, color = Blue, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.padding(horizontal = 29.dp)) { WalletRow(state.walletPaise, onAdd = onWalletAdd, onRefresh = onWalletRefresh) }
            }
        }
        item {
            val completion = listOf(state.profile.name.isNotBlank(), state.profile.mobile.isNotBlank()).count { it } / 2f
            Column(Modifier.padding(start = 19.dp, end = 19.dp, top = 10.dp).fillMaxWidth()
                .border(2.dp, PaleBlue, RoundedCornerShape(10.dp)).padding(horizontal = 11.dp, vertical = 10.dp)) {
                Text("Profile Complete", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Light)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(progress = { completion }, color = Color(0xFF3B8B3F), trackColor = Color(0xFFE4ECE2),
                        modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape))
                    Text("${(completion * 100).toInt()}%", fontSize = 16.sp, color = Color.Black, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 17.dp, vertical = 20.dp).fillMaxWidth().background(Color(0xFFFFFCF6), RoundedCornerShape(12.dp))) {
                Row(Modifier.fillMaxWidth().background(PalePeach, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, null, tint = Gold, modifier = Modifier.size(28.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("Saved Passengers", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                        Text("Add/Edit Passenger info", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 6.dp))
                    }
                    IconButton(onClick = onRefreshPassengers, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Sync, "Refresh passengers", tint = Gold) }
                    Button(onClick = onAddPassenger, shape = CircleShape, contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(44.dp).testTag("add-passenger"), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF886000))) {
                        Text("Add", fontSize = 14.sp)
                    }
                }
                if (state.passengers.isEmpty()) Text("No saved passengers. Tap Add to save one.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(18.dp))
                state.passengers.forEach { passenger ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.Top) {
                        Picture(R.drawable.passenger_avatar, null, Modifier.size(29.dp))
                        Column(Modifier.weight(1f).padding(start = 9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(passenger.name, color = Color(0xFF555560), fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false))
                                mealMarker(passenger.meal)?.let { marker ->
                                    val color = if (marker == "Veg") Color(0xFF008C40) else Color(0xFFE21C29)
                                    Box(Modifier.size(13.dp).border(1.dp, color).semantics { contentDescription = "$marker meal" }, contentAlignment = Alignment.Center) {
                                        Box(Modifier.size(6.dp).background(color, CircleShape))
                                    }
                                }
                            }
                            Text(passengerSummary(passenger),
                                color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 7.dp))
                        }
                        IconButton(onClick = { onEditPassenger(passenger) }, modifier = Modifier.size(25.dp)) { Icon(Icons.Default.Edit, "Edit ${passenger.name}", tint = Gold, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = { onDeletePassenger(passenger) }, modifier = Modifier.size(25.dp)) { Icon(Icons.Default.DeleteOutline, "Delete ${passenger.name}", tint = Gold, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    AccountTile("Change\nPassword", R.drawable.profile_password, PaleBlue, Modifier.weight(1f)) { onService("Change password") }
                    AccountTile("My\nAccount", R.drawable.profile_account, Color(0xFFE9FFE9), Modifier.weight(1f), onAccount)
                    BiometricTile(biometricEnabled, Modifier.weight(1f), onBiometricToggle)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    AccountTile("Transfer\nTicket", R.drawable.profile_transfer, Color(0xFFF0F8FC), Modifier.weight(1f)) { onService("Ticket transfer") }
                    AccountTile("My\nTransaction", R.drawable.profile_transactions, PalePeach, Modifier.weight(1f), onTransactions)
                    AccountTile("DeLink\nAadhar", R.drawable.profile_aadhaar, Color(0xFFF0F1EC), Modifier.weight(1f)) { onService("Aadhaar linking") }
                }
            }
        }
        item {
            Spacer(Modifier.height(22.dp))
            ProfileOptionCard("Saved Routes", "${state.routes.size} directions saved · independent of passengers", Icons.Default.Route, PaleBlue) { routesExpanded = !routesExpanded }
            if (routesExpanded) {
                TextButton(onClick = onAddRoute, modifier = Modifier.padding(horizontal = 17.dp)) { Text("Add Route") }
                if (state.routes.isEmpty()) Text("No saved routes.", modifier = Modifier.padding(17.dp), color = Muted)
            }
        }
        if (routesExpanded) items(state.routes, key = { "route-${it.id}" }) { route ->
            Column(Modifier.padding(horizontal = 17.dp, vertical = 6.dp).fillMaxWidth().background(PaleBlue, RoundedCornerShape(12.dp)).padding(14.dp)) {
                Text(route.label, color = Ink, fontSize = 13.sp)
                if (route.fares.isNotEmpty()) Text(route.fares.entries.joinToString(" · ") { "${it.key}: ₹${it.value}" }, color = Muted, fontSize = 11.sp)
                Row {
                    TextButton(onClick = { onUseRoute(route) }) { Text("Use") }
                    TextButton(onClick = { onEditRoute(route) }) { Text("Edit") }
                    TextButton(onClick = { onDeleteRoute(route) }) { Text("Delete") }
                }
            }
        }
        item {
            Spacer(Modifier.height(22.dp))
            ProfileOptionCard("Users", "${state.profile.username.ifBlank { "Username not set" }} · switch or add a local user", Icons.Default.People, PaleBlue, onSwitchUser)
            Spacer(Modifier.height(12.dp))
            ProfileOptionCard("App Login", if (loginEnabled) if (biometricEnabled) "Enabled · mPIN and device biometrics" else "Enabled · mPIN" else "Off · set a six-digit mPIN", Icons.Default.Lock, PaleBlue, onLoginSettings)
            Spacer(Modifier.height(12.dp))
            ProfileOptionCard("Saved Journey Templates", "${state.templates.size} saved · tap to ${if (templatesExpanded) "hide" else "open"}", Icons.Default.Bookmark, PaleViolet) { templatesExpanded = !templatesExpanded }
            if (templatesExpanded) {
                Row(Modifier.padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Reusable journey details", color = Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onNewTemplate) { Text("Add", fontSize = 12.sp) }
                }
                if (state.templates.isEmpty()) Text("No saved templates.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(17.dp))
            }
        }
        if (templatesExpanded)        items(state.templates, key = { it.id }) { template ->
            Column(Modifier.padding(horizontal = 17.dp, vertical = 6.dp).fillMaxWidth().background(PaleViolet, RoundedCornerShape(12.dp)).padding(14.dp)) {
                Text(template.passengerName, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("${template.origin} → ${template.destination}", color = Ink, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Row {
                    TextButton(onClick = { onUseTemplate(template) }) { Text("Use", fontSize = 12.sp) }
                    TextButton(onClick = { onEditTemplate(template) }) { Text("Edit", fontSize = 12.sp) }
                    TextButton(onClick = { onDeleteTemplate(template) }) { Text("Delete", fontSize = 12.sp) }
                }
            }
        }
    }
}
@Composable
private fun ProfileOptionCard(title: String, subtitle: String, icon: ImageVector, colour: Color, onClick: () -> Unit) {
    Row(Modifier.padding(horizontal = 17.dp).fillMaxWidth().background(colour, RoundedCornerShape(14.dp))
        .clickable(onClick = onClick).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Blue, modifier = Modifier.size(32.dp))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = Blue)
    }
}
@Composable
private fun DrawerRow(text: String, icon: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(42.dp).clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Picture(icon, null, Modifier.size(19.dp))
        Text(text, color = Color.Black, fontSize = 15.sp, modifier = Modifier.padding(start = 15.dp))
    }
}
@Composable
internal fun MenuDrawer(state: JourneyState, onDismiss: () -> Unit, onProfile: () -> Unit,
    onServices: () -> Unit, onShare: () -> Unit, onWalletAdd: () -> Unit = {}, onAbout: () -> Unit = {}) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val dialogView = LocalView.current
        SideEffect { (dialogView.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .55f)).clickable(onClick = onDismiss))
            Column(Modifier.fillMaxHeight().fillMaxWidth(.79f).align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp)).background(Color.White)
                .verticalScroll(rememberScrollState()).navigationBarsPadding()) {
                Column(Modifier.fillMaxWidth().background(PaleViolet, RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                    .statusBarsPadding().padding(top = 42.dp, bottom = 16.dp).clickable(onClick = onProfile), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileAvatar(state.profile, "Open profile", Modifier.size(72.dp).clip(CircleShape))
                    Spacer(Modifier.height(20.dp))
                    Text(state.profile.name.ifBlank { "Your Profile" }, fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(28.dp))
                WalletRow(state.walletPaise, drawer = true, onAdd = onWalletAdd)
                Spacer(Modifier.height(19.dp))
                DrawerRow("Show/Hide Services", R.drawable.menu_services, onServices)
                DrawerRow("FAQs", R.drawable.menu_faq) { /* Service not implemented. */ }
                DrawerRow("Help & Support", R.drawable.menu_support) { /* Service not implemented. */ }
                DrawerRow("Reserved Services - Counter", R.drawable.menu_counter) { /* Service not implemented. */ }
                DrawerRow("About", R.drawable.menu_about, onAbout)
                DrawerRow("Rate Us", R.drawable.menu_rate) { /* Service not implemented. */ }
                DrawerRow("Share", R.drawable.menu_share, onShare)
                Spacer(Modifier.height(30.dp))
                HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = Color(0xFFD7D0EC))
                Spacer(Modifier.height(12.dp))
                DrawerRow("Log Out", R.drawable.menu_logout) { /* Service not implemented. */ }
                Text("V-${state.profile.menuVersion}", color = Color(0xFFA8A8A8), fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 28.dp, bottom = 36.dp))
            }
        }
    }
}

internal fun passengerSummary(passenger: Passenger): String {
    val gender = when (passenger.gender) { "Male" -> "M"; "Female" -> "F"; "Trans", "Trans Gender", "Transgender" -> "T"; else -> passenger.gender }
    val berth = when (passenger.berth) {
        "No Preference" -> "NC"; "Lower" -> "LB"; "Middle" -> "MB"; "Upper" -> "UB"
        "Side Lower" -> "SL"; "Side Middle" -> "SM"; "Side Upper" -> "SU"; "Window Side" -> "WS"; "Coupe" -> "CP"
        else -> passenger.berth
    }
    return listOf(ageFromDob(passenger.dob, passenger.age).takeIf { it.isNotBlank() }?.let { "$it Y" }, gender, berth)
        .filterNotNull().joinToString(", ") + " | " + normalMeal(passenger.meal)
}
@Composable
private fun BiometricTile(enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colour = if (enabled) Blue else Color(0xFF808189)
    val thumb by androidx.compose.animation.core.animateDpAsState(if (enabled) 30.dp else 0.dp, label = "biometric thumb")
    Column(modifier.height(100.dp).background(Color(0xFFFFF6FC), RoundedCornerShape(10.dp))
        .clickable(onClick = onClick).padding(top = 26.dp, bottom = 9.dp)
        .semantics { contentDescription = "Biometric ${if (enabled) "On" else "Off"}" }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(48.dp).height(19.dp).background(Color.White, CircleShape).border(.9.dp, colour, CircleShape)) {
            Text(if (enabled) "On" else "Off", color = colour, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(if (enabled) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 4.dp))
            Box(Modifier.offset(x = thumb).size(18.dp).background(colour, CircleShape))
        }
        Spacer(Modifier.height(7.dp))
        Text("Biometric", color = Ink, fontSize = 13.sp, lineHeight = 17.sp)
    }
}
