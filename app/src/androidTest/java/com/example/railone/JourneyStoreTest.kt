package com.example.railone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JourneyStoreTest {
    private lateinit var context: Context
    private val testNow = parseBookingTime("03/10/2026 10:00")!!
    private lateinit var store: JourneyStore
    @Before fun resetStorage() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        store = JourneyStore(context)
    }
    private fun validDraft() = freshDraft().copy(passengerName = "Passenger", mobile = "9876543210",
        origin = "HOWRAH", destination = "KHARAGPUR", distance = "116 km", fare = "30.00",
        bookedOn = "03/10/2026 09:00", validTill = "03/10/2026 12:00",
        bookingDateTime = "3 Oct 2026, 09:00")
    private fun ticket(draft: TicketData = validDraft(), created: Long = 1000L) = StoredTicket(
        data = draft.copy(journeyTicket = generateJourneyTicket()), createdAt = created,
        countdownEndsAt = created + 300_000, accentIndex = 1)

    @Test fun completedTicketsRemainSavedAfter24HoursAndAcrossReloads() {
        val one = ticket()
        val original = JourneyState(tickets = listOf(one, one.copy(id = "cancelled", cancelled = true)))
        assertTrue(store.save(original))
        val later = parseBookingTime(one.data.bookedOn)!! + 365L * 24 * 60 * 60 * 1000
        assertEquals("Completed", one.status(later))
        assertEquals(original, store.load(later))
        assertEquals(original, store.load(later + 1000))
    }
    @Test fun savedPassengerUsesDisplayAbbreviationsWithoutChangingStoredOptions() {
        val passenger = Passenger(name = "Traveller", mobile = "", age = "26", gender = "Male", berth = "No Preference", meal = "Non Veg")
        assertEquals("26 Y, M, NC | Non Veg", passengerSummary(passenger))
        val preferences = mapOf("Lower" to "LB", "Middle" to "MB", "Upper" to "UB", "Side Lower" to "SL", "Side Middle" to "SM", "Side Upper" to "SU", "Window Side" to "WS", "Coupe" to "CP")
        preferences.forEach { (label, abbreviation) -> assertEquals("26 Y, F, $abbreviation | Veg", passengerSummary(passenger.copy(gender = "Female", berth = label, meal = "Veg"))) }
        assertEquals("26 Y, T, NC | Non Veg", passengerSummary(passenger.copy(gender = "Trans Gender")))
        assertEquals("No Preference", passenger.berth)
    }
    @Test fun walletAndPhotoPersistAndOlderSnapshotsKeepTickets() {
        val one = ticket()
        val profile = UserProfile("Traveller", "9876543210", "profile-1234.jpg")
        val state = JourneyState(tickets = listOf(one), profile = profile, walletPaise = 12345)
        assertTrue(store.save(state))
        assertEquals(state, store.load(testNow))
        val prefs = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
        val raw = JSONObject(prefs.getString("journey_state_v1", null)!!)
        raw.remove("walletPaise"); raw.getJSONObject("profile").remove("photoFile")
        prefs.edit().putString("journey_state_v1", raw.toString()).commit()
        assertEquals(listOf(one), store.load(testNow).tickets)
        assertEquals(0L, store.load(testNow).walletPaise)
        assertEquals("", store.load(testNow).profile.photoFile)
    }
    @Test fun walletAmountsUseExactPaiseAndRejectInvalidValues() {
        assertEquals(12345L, walletAmountPaise("123.45"))
        assertEquals(110L, walletAmountPaise("1.1"))
        assertEquals("124.55", walletDisplay(walletAmountPaise("123.45")!! + walletAmountPaise("1.1")!!))
        listOf("0", "-1", "NaN", "1.234", "1e3", "1000000.01", "").forEach { assertNull(walletAmountPaise(it)) }
    }
    @Test fun extendedPassengerAndProfileFieldsSurviveReloadWithoutChangingTickets() {
        val one = ticket()
        val passenger = Passenger(name = "Traveller", mobile = "", age = "26", gender = "Male", meal = "Veg",
            dob = "28/12/1999", concession = "General", berth = "Lower", idType = "Passport/Travel Document", idNumber = "TEST123")
        val profile = UserProfile(name = "Traveller", mobile = "9876543210", photoFile = "profile-1234.jpg", dob = "28/12/1999",
            gender = "Male", address1 = "Street", address2 = "Area", pin = "700114", district = "North 24 Parganas",
            stateName = "West Bengal", username = "traveller", email = "traveller@example.com", divyangjan = true, menuVersion = "2.5-101",
            postOffice = "Panihati S.O", city = "North 24 Parganas")
        val state = JourneyState(tickets = listOf(one), passengers = listOf(passenger), profile = profile)
        assertTrue(store.save(state))
        assertEquals(state, store.load(testNow))
        val prefs = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
        val raw = JSONObject(prefs.getString("journey_state_v1", null)!!)
        listOf("dob", "concession", "berth", "idType", "idNumber").forEach { raw.getJSONArray("passengers").getJSONObject(0).remove(it) }
        listOf("dob", "gender", "idType", "idNumber", "address1", "address2", "pin", "district", "stateName", "country", "username", "email", "divyangjan", "menuVersion", "postOffice", "city")
            .forEach { raw.getJSONObject("profile").remove(it) }
        prefs.edit().putString("journey_state_v1", raw.toString()).commit()
        assertEquals(one, store.load(testNow).tickets.single())
        assertEquals("General", store.load(testNow).passengers.single().concession)
        assertEquals("No Preference", store.load(testNow).passengers.single().berth)
        assertEquals("profile-1234.jpg", store.load(testNow).profile.photoFile)
        assertEquals("1.0", store.load(testNow).profile.menuVersion)
        assertEquals("", store.load(testNow).profile.postOffice)
        assertEquals("", store.load(testNow).profile.city)
    }
    @Test fun mealMarkersAndDobValidationHandleLegacyAndNewPreferences() {
        assertEquals("Veg", mealMarker("Vegetarian"))
        assertEquals("Non Veg", mealMarker("Non-vegetarian"))
        assertEquals("Veg", mealMarker("Diabetic Veg"))
        assertEquals("Non Veg", mealMarker("Diabetic Non Veg"))
        assertNull(mealMarker("Tea/Coffee"))
        assertNotNull(dobError("31/02/2000"))
        assertNotNull(dobError("01/01/2999"))
        assertNull(dobError("28/12/1999"))
    }
    @Test fun photoImportCreatesBoundedPrivateCopyAndRejectsInvalidFiles() {
        val source = java.io.File(context.cacheDir, "photo-test.png")
        val bitmap = android.graphics.Bitmap.createBitmap(2048, 512, android.graphics.Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val name = importProfilePhoto(context, android.net.Uri.fromFile(source))
        val saved = profilePhotoFile(context, name)!!
        try {
            source.delete()
            val image = requireNotNull(android.graphics.BitmapFactory.decodeFile(saved.path))
            assertTrue(image.width <= 1024 && image.height <= 1024)
            image.recycle()
            assertNull(profilePhotoFile(context, "../other-file.jpg"))
            source.writeText("not an image")
            assertTrue(runCatching { importProfilePhoto(context, android.net.Uri.fromFile(source)) }.isFailure)
        } finally { source.delete(); saved.delete() }
    }

    @Test fun differentJourneysSurviveReload() {
        val one = ticket()
        val two = ticket(validDraft().copy(origin = "DELHI", destination = "AGRA"), 2000L)
        assertTrue(store.save(JourneyState(tickets = listOf(one, two))))
        assertEquals(listOf(one, two), JourneyStore(context).load(testNow).tickets)
    }
    @Test fun sameRouteCreatesSeparateTickets() {
        val one = ticket(); val two = ticket()
        assertNotEquals(one.id, two.id)
        assertTrue(store.save(JourneyState(tickets = listOf(one, two))))
        assertEquals(2, store.load(testNow).tickets.size)
    }
    @Test fun changingProfilesAndTemplatesDoesNotChangeTickets() {
        val one = ticket()
        val passenger = Passenger(name = "Passenger", mobile = "9876543210")
        val template = templateFromDraft(one.data)
        assertTrue(store.save(JourneyState(tickets = listOf(one), passengers = listOf(passenger), templates = listOf(template))))
        val before = store.load(testNow)
        val changed = before.copy(passengers = listOf(passenger.copy(name = "Changed")),
            templates = listOf(template.copy(destination = "DIFFERENT")))
        assertTrue(store.save(changed))
        assertEquals(one, store.load(testNow).tickets.single())
        assertTrue(store.save(changed.copy(passengers = emptyList(), templates = emptyList())))
        assertEquals(one, store.load(testNow).tickets.single())
    }
    @Test fun bookAgainRefreshesTimesAndRemovesReference() {
        val original = validDraft().copy(journeyTicket = "X123456789")
        val copy = renewedDraft(original)
        assertEquals("", copy.journeyTicket)
        assertEquals(original.origin, copy.origin)
        assertEquals(original.passengerName, copy.passengerName)
        assertNotNull(parseBookingTime(copy.bookedOn))
        assertEquals(3 * 60 * 60 * 1000L, parseBookingTime(copy.validTill)!! - parseBookingTime(copy.bookedOn)!!)
        assertEquals("X123456789", original.journeyTicket)
    }
    @Test fun countdownDoesNotResetOnReload() {
        val one = ticket(created = 1000L)
        assertEquals(300, one.secondsLeft(1000L))
        assertTrue(store.save(JourneyState(tickets = listOf(one))))
        val reloaded = store.load(testNow).tickets.single()
        assertEquals(120, reloaded.secondsLeft(181000L))
        assertEquals(0, reloaded.secondsLeft(400000L))
        assertEquals(one.countdownEndsAt, reloaded.countdownEndsAt)
    }
    @Test fun statusUsesTwelveHoursFromBookedOnAndCancellationRatherThanValidityOrCountdown() {
        val one = ticket()
        val expiry = parseBookingTime(one.data.bookedOn)!! + TICKET_COMPLETION_MILLIS
        assertEquals("Upcoming", one.status(expiry - 1))
        assertEquals("Completed", one.status(expiry))
        assertEquals("Upcoming", one.copy(data = one.data.copy(validTill = "01/01/2000 00:00")).status(expiry - 1))
        assertEquals("Completed", one.copy(data = one.data.copy(validTill = "01/01/2999 00:00")).status(expiry))
        assertEquals("Cancelled", one.copy(cancelled = true).status(expiry - 1))
        assertEquals("Cancelled", one.copy(cancelled = true).status(expiry + 1))
    }
    @Test fun bookingSortUsesEnteredBookingTimestampRatherThanCreationTime() {
        val earlier = ticket(validDraft().copy(bookedOn = "03/10/2026 08:00"), created = 999999)
        val later = ticket(validDraft().copy(bookedOn = "03/10/2026 10:00"), created = 1)
        assertEquals(listOf(later, earlier), sortedBookings(listOf(earlier, later), true, "Booking Date"))
        assertEquals(listOf(earlier, later), sortedBookings(listOf(later, earlier), false, "Journey Date"))
    }
    @Test fun legacyJourneysMigrateWithoutDeletingLegacyBytes() {
        val old = ticketJson(validDraft()).apply { put("id", "passenger|HOWRAH|KHARAGPUR") }
        val raw = JSONArray().put(old).put(JSONObject(old.toString()).apply {
            put("id", "passenger|DELHI|AGRA"); put("origin", "DELHI"); put("destination", "AGRA")
        }).toString()
        val prefs = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(SAVED_JOURNEYS_KEY, raw).commit()
        val first = store.load(testNow)
        assertEquals(2, first.templates.size)
        assertEquals(1, first.passengers.size)
        assertEquals(0, first.tickets.size)
        assertEquals(raw, prefs.getString(SAVED_JOURNEYS_KEY, null))
        assertEquals(first, JourneyStore(context).load(testNow))
    }
    @Test fun corruptStorageFailsWithoutDeletingData() {
        val prefs = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString("journey_state_v1", "broken json").commit()
        assertTrue(runCatching { store.load(testNow) }.isFailure)
        assertEquals("broken json", prefs.getString("journey_state_v1", null))
    }
    @Test fun invalidInputAndDatesAreRejected() {
        val draft = validDraft()
        assertNull(draftError(draft))
        assertNotNull(draftError(draft.copy(origin = draft.destination)))
        assertNotNull(draftError(draft.copy(adults = "0", children = "0")))
        assertNotNull(draftError(draft.copy(fare = "NaN")))
        assertNotNull(draftError(draft.copy(distance = "-1")))
        assertNull(parseBookingTime("31/02/2026 09:00"))
        assertNull(parseBookingTime("03/10/2026 09:00 trailing"))
        assertNotNull(draftError(draft.copy(validTill = draft.bookedOn)))
    }
    @Test fun profileAndPassengerPreferencesRoundTrip() {
        val state = JourneyState(profile = UserProfile("Owner", "9876543210"), showServices = false,
            passengers = listOf(Passenger(name = "Traveller", mobile = "9123456789", age = "26", gender = "Male", meal = "Vegetarian")),
            templates = listOf(templateFromDraft(validDraft())))
        assertTrue(store.save(state))
        assertEquals(state, store.load(testNow))
    }
}
