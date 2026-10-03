@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.railone

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

private val AppBlue = Color(0xFF0166FF)
private val Navy = Color(0xFF13275C)
private val Peach = Color(0xFFFFF1E2)
private val Ice = Color(0xFFE2F8FD)
private val Lavender = Color(0xFFF0ECFF)

@Composable
internal fun RailOneApp() {
    val context = LocalContext.current
    val store = remember { JourneyStore(context) }
    val loaded = remember { runCatching { store.load() } }
    var state by remember { mutableStateOf(loaded.getOrDefault(JourneyState())) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf("Home") }
    var page by rememberSaveable { mutableStateOf("Main") }
    var bookingsFilter by rememberSaveable { mutableStateOf("Upcoming") }
    var bookingsNewestFirst by rememberSaveable { mutableStateOf(true) }
    var returnTab by rememberSaveable { mutableStateOf("Home") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorReturnTicketId by rememberSaveable { mutableStateOf<String?>(null) }
    var templateId by rememberSaveable { mutableStateOf<String?>(null) }
    var draftRaw by rememberSaveable { mutableStateOf(ticketJson(freshDraft()).toString()) }
    var draftBaselineRaw by rememberSaveable { mutableStateOf(draftRaw) }
    var pendingDiscard by rememberSaveable { mutableStateOf(false) }
    val draft = remember(draftRaw) { ticketFromJson(JSONObject(draftRaw)) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var profileEditing by rememberSaveable { mutableStateOf(false) }
    var profileViewing by rememberSaveable { mutableStateOf(false) }
    var accountOpen by rememberSaveable { mutableStateOf(false) }
    var walletEditing by rememberSaveable { mutableStateOf(false) }
    var photoBusy by remember { mutableStateOf(false) }
    var passengerEditingId by rememberSaveable { mutableStateOf<String?>(null) }
    val passengerEditing = passengerEditingId?.let { id ->
        state.passengers.find { it.id == id } ?: Passenger(id = id, name = "", mobile = "")
    }
    var confirm by remember { mutableStateOf<Pair<String, String>?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    SideEffect {
        (context as? android.app.Activity)?.window?.let { window ->
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = page == "Main" && tab != "My Bookings"
            controller.isAppearanceLightNavigationBars = page != "Main" || tab == "My Bookings"
        }
    }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    fun notify(text: String) { scope.launch { snackbar.showSnackbar(text) } }
    fun persist(updated: JourneyState): Boolean {
        if (loaded.isFailure) { message = "Stored data could not be read. Nothing has been overwritten. Restore a device backup or contact support with the source project."; return false }
        val retained = updated.withoutExpiredTickets(System.currentTimeMillis())
        if (!runCatching { store.save(retained) }.getOrDefault(false)) {
            notify("Could not save changes. Please retry."); return false
        }
        state = retained
        return true
    }
    LaunchedEffect(now) {
        val retained = state.withoutExpiredTickets(now)
        if (retained != state && persist(retained)) {
            if (page == "Ticket" && retained.tickets.none { it.id == selectedId }) {
                selectedId = null; page = "Main"; tab = returnTab
            }
        }
    }
    fun refreshData() {
        runCatching { store.load() }.onSuccess { state = it; notify("Saved data refreshed") }
            .onFailure { notify("Could not read saved data. Existing records were kept.") }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !photoBusy) scope.launch {
            photoBusy = true
            try {
                val name = withContext(Dispatchers.IO) { importProfilePhoto(context, uri) }
                val old = state.profile.photoFile
                if (persist(state.copy(profile = state.profile.copy(photoFile = name)))) {
                    withContext(Dispatchers.IO) { profilePhotoFile(context, old)?.delete() }
                } else withContext(Dispatchers.IO) { profilePhotoFile(context, name)?.delete() }
            } catch (e: Exception) {
                notify("Could not save the photo. Choose another image and retry.")
            } finally { photoBusy = false }
        }
    }
    fun setDraft(updated: TicketData) { draftRaw = ticketJson(updated).toString(); error = null }
    fun edit(data: TicketData, id: String? = null) {
        editorReturnTicketId = if (page == "Ticket") selectedId else null
        if (page != "Ticket") returnTab = tab
        setDraft(data); draftBaselineRaw = ticketJson(data).toString(); templateId = id; page = "Editor"
    }
    fun openTicket(ticket: StoredTicket) {
        val openedAt = System.currentTimeMillis()
        val updated = state.copy(tickets = state.tickets.map {
            if (it.id == ticket.id) it.copy(countdownEndsAt = openedAt + TICKET_COUNTDOWN_SECONDS * 1000L) else it
        })
        if (persist(updated)) {
            now = openedAt
            selectedId = ticket.id; returnTab = tab; page = "Ticket"
        }
    }
    fun back() {
        if (page == "Editor" && editorReturnTicketId != null) {
            selectedId = editorReturnTicketId; editorReturnTicketId = null; page = "Ticket"
        } else { page = "Main"; tab = returnTab }
        error = null
    }
    fun requestBack() {
        if (page == "Editor" && draftRaw != draftBaselineRaw) pendingDiscard = true else back()
    }
    BackHandler(enabled = page != "Main" || tab != "Home") {
        if (page != "Main") requestBack() else tab = "Home"
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = AppBlue, background = Color.White)) {
        if (loaded.isFailure) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text("Unable to read saved data", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Your saved data has been left untouched. Restore a device backup or investigate the storage file before continuing.", modifier = Modifier.padding(top = 16.dp))
                }
            }
        } else {
            val selected = state.tickets.find { it.id == selectedId }
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (page == "Main") {
                        if (tab == "My Bookings") ReferenceBookingFilters(state.tickets, now, bookingsFilter) { bookingsFilter = it }
                        else ReferenceBottomBar(tab) { name ->
                            if (name == "Menu") menuOpen = true else tab = name
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (page) {
                        "Editor" -> InputScreen(
                            data = draft, savedJourneys = state.templates,
                            setPassengerName = { setDraft(draft.copy(passengerName = it)) },
                            setMobile = { setDraft(draft.copy(mobile = it.filter(Char::isDigit).take(15))) },
                            setOrigin = { setDraft(draft.copy(origin = it.uppercase(java.util.Locale.ROOT))) },
                            setDistance = { setDraft(draft.copy(distance = it)) },
                            setDestination = { setDraft(draft.copy(destination = it.uppercase(java.util.Locale.ROOT))) },
                            setVia = { setDraft(draft.copy(via = it.uppercase(java.util.Locale.ROOT))) },
                            setAdults = { setDraft(draft.copy(adults = it.filter(Char::isDigit))) },
                            setChildren = { setDraft(draft.copy(children = it.filter(Char::isDigit))) },
                            setBookedOn = { setDraft(draft.copy(bookedOn = it, bookingDateTime = toTicketDisplayDateTime(it),
                                validTill = addHours(it, 3) ?: draft.validTill)) },
                            setValidTill = { setDraft(draft.copy(validTill = it)) },
                            resetBookedOnToNow = { setDraft(renewedDraft(draft)) },
                            setClassName = { setDraft(draft.copy(className = it)) },
                            setTrainType = { setDraft(draft.copy(trainType = it)) },
                            setTicketType = { setDraft(draft.copy(ticketType = it)) },
                            setFare = { setDraft(draft.copy(fare = sanitizeFareInput(it))) },
                            onFareFocusLost = {
                                if (draft.fare.isNotBlank()) setDraft(draft.copy(fare = formatFare(draft.fare)))
                            },
                            setIrNumber = { setDraft(draft.copy(irNumber = sanitizeAlphaNumeric15(it))) },
                            setServiceNo = { setDraft(draft.copy(serviceNo = it.uppercase(java.util.Locale.ROOT).take(10))) },
                            onLoadJourney = { id -> state.templates.find { it.id == id }?.let {
                                // Loading a shortcut is not editing the original template.
                                setDraft(draftFromTemplate(it)); templateId = null
                            } },
                            onSaveJourney = {
                                val problem = draftError(draft)
                                if (problem != null) error = problem else {
                                    val template = templateFromDraft(draft, templateId ?: UUID.randomUUID().toString())
                                    if (persist(state.copy(templates = state.templates.filterNot { it.id == template.id } + template))) {
                                        templateId = template.id; draftBaselineRaw = draftRaw; notify("Journey template saved")
                                    }
                                }
                            },
                            onDeleteJourney = { confirm = "template" to it },
                            onGenerateTicket = {
                                val problem = draftError(draft)
                                if (problem != null) error = problem else {
                                    var reference = generateJourneyTicket()
                                    while (state.tickets.any { it.data.journeyTicket == reference }) reference = generateJourneyTicket()
                                    val time = System.currentTimeMillis()
                                    val frozen = draft.copy(passengerName = draft.passengerName.trim(), origin = draft.origin.trim(),
                                        destination = draft.destination.trim(), fare = formatFare(draft.fare),
                                        bookingDateTime = toTicketDisplayDateTime(draft.bookedOn), journeyTicket = reference)
                                    val ticket = StoredTicket(data = frozen, createdAt = time,
                                        countdownEndsAt = time + TICKET_COUNTDOWN_SECONDS * 1000L,
                                        accentIndex = ACCENT_COLORS.indices.random())
                                    if (persist(state.copy(tickets = state.tickets + ticket))) {
                                        selectedId = ticket.id; templateId = null; editorReturnTicketId = null; page = "Ticket"; returnTab = "My Bookings"
                                        setDraft(freshDraft())
                                    }
                                }
                            }, onBack = { requestBack() }, passengers = state.passengers,
                            onLoadPassenger = { setDraft(draft.copy(passengerName = it.name, mobile = it.mobile)) },
                            templateSaveLabel = if (templateId == null) "Save As Journey Template" else "Update Journey Template",
                            error = error,
                        )
                        "Ticket" -> if (selected != null) TicketScreen(selected.data, selected.secondsLeft(now),
                            ACCENT_COLORS[selected.accentIndex.coerceIn(0, ACCENT_COLORS.lastIndex)],
                            onBack = { back() }, status = selected.status(now),
                            onConnecting = {
                                edit(freshDraft().copy(passengerName = selected.data.passengerName,
                                    mobile = selected.data.mobile, adults = selected.data.adults,
                                    children = selected.data.children, origin = selected.data.destination))
                            }) else Column(Modifier.safeDrawingPadding().padding(20.dp)) {
                                Text("Ticket unavailable"); Button(onClick = { back() }) { Text("Back to bookings") }
                            }
                        else -> when (tab) {
                            "Home" -> HomePage(state, now, onNew = { edit(freshDraft()) },
                                onBookings = { tab = "My Bookings" }, onView = { openTicket(it) },
                                onRepeat = { edit(renewedDraft(it.data)) },
                                onService = { /* Service not implemented. */ }, onSocial = { url ->
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                                })
                            "My Bookings" -> BookingsPage(state.tickets, now,
                                filter = bookingsFilter, newestFirst = bookingsNewestFirst,
                                onFilter = { bookingsFilter = it }, onSort = { bookingsNewestFirst = !bookingsNewestFirst }, onNew = { edit(freshDraft()) },
                                onView = { openTicket(it) }, onRepeat = { edit(renewedDraft(it.data)) },
                                onCancel = { confirm = "cancel" to it.id }, onRefresh = { refreshData() }, onBack = { tab = "Home" },
                                onDelete = { confirm = "ticket" to it.id })
                            "You" -> ProfilePage(state, onRefreshPassengers = { refreshData() }, onBack = { tab = "Home" },
                                onPhoto = { if (!photoBusy) runCatching { photoPicker.launch("image/*") }.onFailure { notify("No image picker available") } },
                                onRemovePhoto = {
                                    if (!photoBusy) {
                                        val old = state.profile.photoFile
                                        if (persist(state.copy(profile = state.profile.copy(photoFile = ""))))
                                            scope.launch(Dispatchers.IO) { profilePhotoFile(context, old)?.delete() }
                                    }
                                }, onWalletAdd = { walletEditing = true }, onWalletRefresh = { refreshData() },
                                onViewProfile = { profileViewing = true },
                                onService = { /* Service not implemented. */ },
                                onTransactions = { tab = "My Bookings"; bookingsFilter = "All" }, onProfile = { profileEditing = true },
                                onAccount = { accountOpen = true },
                                onAddPassenger = { passengerEditingId = UUID.randomUUID().toString() },
                                onEditPassenger = { passengerEditingId = it.id },
                                onDeletePassenger = { confirm = "passenger" to it.id },
                                onNewTemplate = { edit(freshDraft()) },
                                onEditTemplate = { edit(draftFromTemplate(it), it.id) },
                                onUseTemplate = { edit(draftFromTemplate(it)) },
                                onDeleteTemplate = { confirm = "template" to it.id })

                        }
                    }
                }
            }
        }
        if (menuOpen) MenuDrawer(state, onDismiss = { menuOpen = false },
            onWalletAdd = { menuOpen = false; walletEditing = true },
            onProfile = { menuOpen = false; tab = "You" },
            onServices = { persist(state.copy(showServices = !state.showServices)); menuOpen = false },
            onShare = {
                menuOpen = false
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT,
                        "Rail One — a local app for creating and managing journey previews. Not valid for travel.")
                }
                runCatching { context.startActivity(Intent.createChooser(intent, "Share app description")) }
                    .onFailure { notify("No sharing app available") }
            })
        if (pendingDiscard) AlertDialog(onDismissRequest = { pendingDiscard = false },
            title = { Text("Discard draft changes?") },
            text = { Text("The changes in this booking form have not been saved as a ticket or template.") },
            confirmButton = { TextButton(onClick = {
                pendingDiscard = false; draftRaw = draftBaselineRaw; back()
            }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { pendingDiscard = false }) { Text("Keep Editing") } })
        message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text("Rail One") },
            text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }) }
        if (profileEditing) ProfileDetailsSheet(state.profile, onDismiss = { profileEditing = false }, onSave = {
            if (persist(state.copy(profile = it.copy(photoFile = state.profile.photoFile)))) profileEditing = false
        })
        if (profileViewing) ProfileViewSheet(state.profile, onDismiss = { profileViewing = false },
            onEdit = { profileViewing = false; profileEditing = true })
        if (walletEditing) WalletDialog(state.walletPaise, onDismiss = { walletEditing = false }, onAdd = { amount ->
            if (amount <= MAX_WALLET_PAISE - state.walletPaise && persist(state.copy(walletPaise = state.walletPaise + amount)))
                walletEditing = false
        })
        if (accountOpen) AccountSheet(state.profile, onDismiss = { accountOpen = false },
            onEdit = { accountOpen = false; profileEditing = true },
            onToggle = { persist(state.copy(profile = state.profile.copy(divyangjan = it))) },
            onDelete = { confirm = "profile" to "profile" })
        passengerEditing?.let { passenger -> PassengerSheet(passenger, isNew = state.passengers.none { it.id == passenger.id }, onDismiss = { passengerEditingId = null }, onSave = {
            if (persist(state.copy(passengers = state.passengers.filterNot { p -> p.id == it.id } + it))) passengerEditingId = null
        }) }
        confirm?.let { (kind, id) ->
            val cancellation = kind == "cancel"
            val deletingTicket = kind == "ticket"
            val deletingProfile = kind == "profile"
            AlertDialog(onDismissRequest = { confirm = null }, title = { Text(if (cancellation) "Cancel ticket?" else if (deletingTicket) "Delete ticket?" else if (deletingProfile) "Delete local profile?" else "Delete saved item?") },
                text = { Text(if (deletingProfile) "Clear your local profile details and photo? Tickets, saved passengers, journey templates and wallet balance will remain. There is no online account to delete."
                    else if (deletingTicket) "Delete this ticket from this device? Other tickets, saved passengers and journey templates will remain. This cannot be undone."
                    else if (cancellation) "This marks only this ticket as cancelled. It does not request a railway cancellation or refund."
                    else "Existing generated tickets will keep their original details.") },
                confirmButton = { TextButton(onClick = {
                    val updated = when (kind) {
                        "profile" -> state.copy(profile = UserProfile())
                        "ticket" -> state.copy(tickets = state.tickets.filterNot { it.id == id })
                        "template" -> state.copy(templates = state.templates.filterNot { it.id == id })
                        "passenger" -> state.copy(passengers = state.passengers.filterNot { it.id == id })
                        else -> state.copy(tickets = state.tickets.map { if (it.id == id) it.copy(cancelled = true) else it })
                    }
                    val oldPhoto = state.profile.photoFile
                    if (persist(updated)) {
                        confirm = null; if (templateId == id) templateId = null
                        if (deletingProfile) {
                            accountOpen = false
                            scope.launch(Dispatchers.IO) { profilePhotoFile(context, oldPhoto)?.delete() }
                        }
                    }
                }) { Text(if (cancellation) "Cancel ticket" else "Delete") } },
                dismissButton = { TextButton(onClick = { confirm = null }) { Text("Keep") } })
        }
    }
}

@Composable
private fun WalletDialog(balance: Long, onDismiss: () -> Unit, onAdd: (Long) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Money") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Current balance: ₹ ${walletDisplay(balance)}")
            Field("Amount to add (₹)", amount, { amount = it; problem = null }, Modifier.fillMaxWidth(), androidx.compose.ui.text.input.KeyboardType.Decimal)
            Text("Updates your local balance. No payment is collected.", fontSize = 11.sp)
            problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        val paise = walletAmountPaise(amount)
        when {
            paise == null -> problem = "Enter a positive amount with up to two decimal places."
            paise > MAX_WALLET_PAISE - balance -> problem = "The balance limit is ₹ 10,00,000.00."
            else -> onAdd(paise)
        }
    }) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
