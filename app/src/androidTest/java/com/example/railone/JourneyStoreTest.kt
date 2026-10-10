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
    @Test fun aboutContactIntentsOpenMailComposerAndDiallerWithoutSendingOrCalling() {
        val mail = railwayMailIntent()
        assertEquals(android.content.Intent.ACTION_SENDTO, mail.action)
        assertTrue(mail.data.toString().startsWith("mailto:railone.support@cris.org.in?"))
        assertEquals("SuperApp for Indian Railways :", mail.getStringExtra(android.content.Intent.EXTRA_SUBJECT))
        assertEquals("Write Your Message Here!", mail.getStringExtra(android.content.Intent.EXTRA_TEXT))
        val call = railwayCallIntent()
        assertEquals(android.content.Intent.ACTION_DIAL, call.action)
        assertEquals("tel:139", call.data.toString())
    }
    @Test fun railwaySocialLinksUseBrowsableExternalViewIntents() {
        assertEquals(4, RAILWAY_SOCIAL_LINKS.size)
        RAILWAY_SOCIAL_LINKS.forEach { (_, url) ->
            val intent = railwaySocialIntent(url)
            assertEquals(android.content.Intent.ACTION_VIEW, intent.action)
            assertEquals(url, intent.data.toString())
            assertTrue(intent.hasCategory(android.content.Intent.CATEGORY_BROWSABLE))
            assertNull(intent.component)
            assertNull(intent.`package`)
        }
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
    @Test fun savedRoutesReverseViaAndAllowDirectAndAlternativePaths() {
        val first = SavedRoute(origin = " kharagpur ", destination = "howrah", via = "pku-src", distance = "116")
        val routes = emptyList<SavedRoute>().saveRoutePair(first)
            .saveRoutePair(SavedRoute(origin = "KHARAGPUR", destination = "HOWRAH", distance = "116"))
            .saveRoutePair(SavedRoute(origin = "KHARAGPUR", destination = "HOWRAH", via = "ABC-DEF", distance = "116"))
        assertEquals(6, routes.size)
        assertTrue(routes.any { it.origin == "HOWRAH" && it.destination == "KHARAGPUR" && it.via == "SRC-PKU" })
        assertEquals(2, routes.count { it.via.isBlank() })
        assertEquals(6, routes.saveRoutePair(first.copy(id = "another", pairId = "another")).size)
    }
    @Test fun editingRouteUpdatesReverseWhileRetainingOtherVariants() {
        val first = SavedRoute(origin = "A", destination = "B", via = "X-Y", distance = "116")
        val routes = emptyList<SavedRoute>().saveRoutePair(first)
            .saveRoutePair(SavedRoute(origin = "A", destination = "B", via = "Z", distance = "116"))
        val edited = routes.saveRoutePair(first.copy(via = "P-Q"))
        assertEquals(4, edited.size)
        assertTrue(edited.any { it.pairId == first.pairId && it.via == "Q-P" })
        assertFalse(edited.any { it.via == "X-Y" || it.via == "Y-X" })
        assertEquals(2, edited.filterNot { it.pairId == first.pairId }.size)
    }
    @Test fun applyingRouteOnlyChangesStationsViaAndDistance() {
        val draft = validDraft()
        val route = SavedRoute(origin = "DELHI", destination = "AGRA", via = "ABC", distance = "116")
        val applied = draft.withRoute(route)
        assertEquals(draft, applied.copy(origin = draft.origin, destination = draft.destination, via = draft.via, distance = draft.distance, fare = draft.fare))
        assertEquals("DELHI", applied.origin)
        assertEquals("ABC", applied.via)
    }
    @Test fun oldSnapshotMigratesRoutesWithoutChangingExistingUserData() {
        val original = JourneyState(tickets = listOf(ticket()),
            passengers = listOf(Passenger(name = "Saved person", mobile = "9123456789")),
            templates = listOf(templateFromDraft(validDraft().copy(via = "PKU-SRC"))),
            profile = UserProfile("Owner", "9876543210"), walletPaise = 855)
        assertTrue(store.save(original))
        val prefs = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
        val old = JSONObject(prefs.getString("journey_state_v1", null)!!).apply { remove("routes") }
        assertTrue(prefs.edit().putString("journey_state_v1", old.toString()).commit())
        val migrated = store.load(testNow)
        assertEquals(original, migrated.copy(routes = emptyList()))
        assertEquals(2, migrated.routes.size)
        assertEquals(migrated, JourneyStore(context).load(testNow))
    }
    @Test fun routePairsRoundTripAndRejectMalformedVia() {
        val routes = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "A", destination = "B", via = "X-Y", distance = "116"))
        val original = JourneyState(routes = routes)
        assertTrue(store.save(original))
        assertEquals(original, JourneyStore(context).load(testNow))
        assertNotNull(routeError(SavedRoute(origin = "A", destination = "B", via = "X--Y", distance = "116")))
        assertNotNull(routeError(SavedRoute(origin = " a ", destination = "A", distance = "116")))
    }

    @Test fun routeDistanceAndUppercaseAreSavedInBothDirections() {
        val routes = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "kharagpur", destination = "howrah", via = "pku-src", distance = "116 km"))
        assertEquals(listOf("116", "116"), routes.map { it.distance })
        assertEquals("KHARAGPUR", routes.first().origin)
        assertEquals("PKU-SRC", routes.first().via)
        assertEquals("SRC-PKU", routes.last().via)
        assertTrue(store.save(JourneyState(routes = routes)))
        assertEquals(routes, store.load(testNow).routes)
        assertNotNull(routeError(routes.first().copy(distance = "0")))
        assertNotNull(routeError(routes.first().copy(distance = "NaN")))
    }
    @Test fun stationSuggestionsAreUniqueCaseInsensitiveAndIncludeBothStationRoles() {
        val routes = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "KHARAGPUR", destination = "HOWRAH", distance = "116"))
            .saveRoutePair(SavedRoute(origin = "KHARAGPUR", destination = "HOWRAH", via = "PKU-SRC", distance = "116"))
        assertEquals(listOf("KHARAGPUR"), stationSuggestions(routes, " khar "))
        assertEquals(listOf("HOWRAH"), stationSuggestions(routes, "how"))
        assertTrue(stationSuggestions(routes, "HOWRAH").isEmpty())
        assertTrue(stationSuggestions(routes, "DELHI").isEmpty())
    }
    @Test fun stationPairAutofillsSinglePathAndNeverChoosesAmbiguousVia() {
        val routes = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "KHARAGPUR", destination = "HOWRAH", via = "PKU-SRC", distance = "116"))
        val draft = validDraft().copy(origin = "howrah", destination = "kharagpur", distance = "", via = "")
        val filled = draft.fillUniqueSavedRoute(routes)
        assertEquals("SRC-PKU", filled.via)
        assertEquals("116", filled.distance)
        assertEquals(draft.passengerName, filled.passengerName)
        val variants = routes.saveRoutePair(SavedRoute(origin = "KHARAGPUR", destination = "HOWRAH", via = "OTHER", distance = "125"))
        assertEquals(draft, draft.fillUniqueSavedRoute(variants))
        val selected = draft.withRoute(variants.last())
        assertEquals("OTHER", selected.via)
        assertEquals("125", selected.distance)
        assertEquals(draft, draft.copy(destination = "UNKNOWN").fillUniqueSavedRoute(routes).copy(destination = draft.destination))
    }
    @Test fun preDistanceRoutesRecoverBothDirectionsFromOldTemplateAndRetainUnknownRoutes() {
        val route = SavedRoute(origin = "HOWRAH", destination = "KHARAGPUR", via = "SRC-PKU", distance = "116")
        val routes = emptyList<SavedRoute>().saveRoutePair(route) + SavedRoute(origin = "A", destination = "B")
        val original = JourneyState(routes = routes, tickets = listOf(ticket()),
            templates = listOf(templateFromDraft(validDraft().copy(via = "SRC-PKU"))),
            passengers = listOf(Passenger(name = "Saved", mobile = "9876543210")), walletPaise = 855)
        assertTrue(store.save(original))
        val prefs = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
        val old = JSONObject(prefs.getString("journey_state_v1", null)!!)
        val array = old.getJSONArray("routes")
        for (i in 0 until array.length()) array.getJSONObject(i).remove("distance")
        assertTrue(prefs.edit().putString("journey_state_v1", old.toString()).commit())
        val migrated = store.load(testNow)
        assertEquals(listOf("116", "116", ""), migrated.routes.map { it.distance })
        assertEquals(original.copy(routes = migrated.routes), migrated)
        assertEquals(migrated, JourneyStore(context).load(testNow))
    }

    @Test fun removingViaCanUpdateBothDirectionsWithoutChangingDistance() {
        val route = SavedRoute(origin = "A", destination = "B", via = "X-Y", distance = "116")
        val original = emptyList<SavedRoute>().saveRoutePair(route)
        val direct = original.editRoute(route.copy(via = ""), updateReverse = true)
        assertEquals(2, direct.size)
        assertTrue(direct.all { it.via.isBlank() && it.distance == "116" })
        assertEquals(original.map { it.id }.toSet(), direct.map { it.id }.toSet())
    }
    @Test fun reverseDirectionCanEditOrRemoveViaIndependentlyAndLaterSynchronise() {
        val original = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "A", destination = "B", via = "X-Y", distance = "116"))
        val reverse = original.last()
        val edited = original.editRoute(reverse.copy(via = "q-p"), updateReverse = false)
        assertEquals("X-Y", edited.first().via)
        assertEquals("Q-P", edited.last().via)
        val removed = edited.editRoute(edited.last().copy(via = ""), updateReverse = false)
        assertEquals("X-Y", removed.first().via)
        assertEquals("", removed.last().via)
        val synced = removed.editRoute(removed.last().copy(via = "SRC-PKU"), updateReverse = true)
        assertEquals("PKU-SRC", synced.single { it.origin == "A" }.via)
        assertEquals("SRC-PKU", synced.single { it.origin == "B" }.via)
        assertTrue(store.save(JourneyState(routes = synced)))
        assertEquals(synced, store.load(testNow).routes)
    }
    @Test fun independentRouteEditsRejectConflictsWithoutRemovingOtherPaths() {
        val first = SavedRoute(origin = "A", destination = "B", via = "X", distance = "10")
        val routes = emptyList<SavedRoute>().saveRoutePair(first)
            .saveRoutePair(SavedRoute(origin = "A", destination = "B", distance = "10"))
        assertTrue(runCatching { routes.editRoute(first.copy(via = ""), updateReverse = false) }.isFailure)
        assertTrue(runCatching { routes.editRoute(first.copy(via = ""), updateReverse = true) }.isFailure)
        assertEquals(4, routes.size)
    }

    @Test fun trainTypeFaresRoundTripAndReverseWithTheRoute() {
        val route = SavedRoute(origin = "A", destination = "B", via = "X-Y", distance = "116",
            fares = mapOf("ORDINARY" to "30", "MAIL/EXPRESS" to "60.50", "SUPERFAST" to "80"))
        val routes = emptyList<SavedRoute>().saveRoutePair(route)
        assertEquals(mapOf("ORDINARY" to "30.00", "MAIL/EXPRESS" to "60.50", "SUPERFAST" to "80.00"), routes.first().fares)
        assertEquals(routes.first().fares, routes.last().fares)
        assertTrue(store.save(JourneyState(routes = routes)))
        assertEquals(routes, store.load(testNow).routes)
    }
    @Test fun selectingRouteOrTrainTypeUsesMatchingFareAndClearsMissingTypes() {
        val route = SavedRoute(origin = "A", destination = "B", via = "X", distance = "116",
            fares = mapOf("ORDINARY" to "30.00", "MAIL/EXPRESS" to "60.00"))
        val routes = emptyList<SavedRoute>().saveRoutePair(route)
        val selected = validDraft().copy(trainType = "ORDINARY").withRoute(routes.first())
        assertEquals("30.00", selected.fare)
        assertEquals("60.00", selected.withTrainTypeFare("MAIL/EXPRESS", routes).fare)
        assertEquals("", selected.withTrainTypeFare("SUPERFAST", routes).fare)
        assertEquals("60.00", selected.copy(origin = "B", destination = "A", via = "X").withTrainTypeFare("MAIL/EXPRESS", routes).fare)
    }
    @Test fun editingFaresIndependentlyAndSavingFromBookingKeepsOtherTrainTypes() {
        val route = SavedRoute(origin = "A", destination = "B", distance = "116",
            fares = mapOf("ORDINARY" to "30.00", "MAIL/EXPRESS" to "60.00"))
        val routes = emptyList<SavedRoute>().saveRoutePair(route)
        val draft = validDraft().withRoute(routes.first()).copy(trainType = "MAIL/EXPRESS", fare = "65")
        val edited = draft.routeForSaving(routes)
        assertEquals(route.id, edited.id)
        assertEquals("30.00", edited.fares["ORDINARY"])
        val changed = routes.editRoute(edited, updateReverse = false)
        assertEquals("65.00", changed.first().fares["MAIL/EXPRESS"])
        assertEquals("60.00", changed.last().fares["MAIL/EXPRESS"])
        assertNotNull(routeError(route.copy(fares = mapOf("ORDINARY" to "-1"))))
        assertNotNull(routeError(route.copy(fares = mapOf("ORDINARY" to "30.123"))))
    }
    @Test fun preFareRoutesMigrateWithoutChangingExistingBookingsOrOtherData() {
        val routes = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "A", destination = "B", distance = "116"))
        val original = JourneyState(routes = routes, tickets = listOf(ticket()), profile = UserProfile("Owner", "9876543210"), walletPaise = 855)
        assertTrue(store.save(original))
        val prefs = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
        val old = JSONObject(prefs.getString("journey_state_v1", null)!!)
        val array = old.getJSONArray("routes")
        for (i in 0 until array.length()) array.getJSONObject(i).remove("fares")
        assertTrue(prefs.edit().putString("journey_state_v1", old.toString()).commit())
        assertEquals(original, store.load(testNow))
        assertEquals(original, JourneyStore(context).load(testNow))
    }

    @Test fun savedAdultFareScalesForCountsAndOnlyOrdinaryReturn() {
        val route = SavedRoute(origin = "A", destination = "B", distance = "116", fares = mapOf("ORDINARY" to "30.00", "MAIL/EXPRESS" to "60.00"))
        val routes = emptyList<SavedRoute>().saveRoutePair(route)
        for (adults in 1..4) {
            val draft = validDraft().copy(adults = adults.toString(), trainType = "ORDINARY", ticketType = "JOURNEY").withRoute(route)
            assertEquals(formatFare((30 * adults).toString()), draft.fare)
            assertEquals(formatFare((60 * adults).toString()), draft.withTicketTypeFare("RETURN", routes).fare)
            val express = draft.withTicketTypeFare("RETURN", routes).withTrainTypeFare("MAIL/EXPRESS", routes)
            assertEquals(formatFare((60 * adults).toString()), express.fare)
        }
        assertEquals("120.00", validDraft().copy(trainType = "ORDINARY", ticketType = "JOURNEY").withRoute(route).withAdultCount("4", routes).fare)
    }
    @Test fun savingMultipliedFareStoresOneAdultOneWayAmount() {
        val routes = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "A", destination = "B", distance = "116", fares = mapOf("ORDINARY" to "30.00")))
        val draft = validDraft().copy(adults = "4", trainType = "ORDINARY", ticketType = "RETURN").withRoute(routes.first())
        assertEquals("240.00", draft.fare)
        assertEquals("30.00", draft.routeForSaving(routes).fares["ORDINARY"])
        assertEquals("40.00", draft.copy(fare = "320.00").routeForSaving(routes).fares["ORDINARY"])
    }
    @Test fun bookingRejectsMoreThanFourAdults() {
        assertNull(draftError(validDraft().copy(adults = "4")))
        assertNotNull(draftError(validDraft().copy(adults = "5")))
        assertNotNull(draftError(validDraft().copy(adults = "0")))
    }

    @Test fun stationCatalogPreservesCodesAndFindsUniqueRouteStations() {
        val routes = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "KHARAGPUR", destination = "HOWRAH", distance = "116"))
        val state = JourneyState(routes = routes, stations = listOf(Station("KHARAGPUR", "KGP"), Station("DELHI", "DLI")), tickets = listOf(ticket()))
        assertTrue(store.save(state))
        assertEquals(state, store.load(testNow))
        assertEquals(3, stationCatalog(state).size)
        assertEquals("KGP", stationCatalog(state).single { it.name == "KHARAGPUR" }.code)
    }
    @Test fun codeLookupUsesFullNamesAndRejectsDuplicateStationCodes() {
        val catalog = listOf(Station("KHARAGPUR", "KGP"), Station("HOWRAH", "HWH"))
        assertEquals("KHARAGPUR", stationName("kgp", catalog))
        assertEquals("NORTH ", stationName("North ", catalog))
        assertEquals("KHARAGPUR", stationMatches(catalog, "kgp").single().name)
        assertEquals("HOWRAH", stationMatches(catalog, "howr").single().name)
        assertNotNull(stationError(Station("DELHI", "KGP"), catalog))
        val data = validDraft().copy(origin = stationName("KGP", catalog), destination = stationName("HWH", catalog))
        assertFalse(ticketJson(data).toString().contains("KGP"))
        assertFalse(ticketJson(data).toString().contains("HWH"))
    }
    @Test fun passengerLimitIncludesChildren() {
        val draft = validDraft().copy(adults = "2", children = "2")
        assertNull(draftError(draft))
        assertNotNull(draftError(draft.copy(children = "3")))
        assertEquals(draft, draft.withPassengerCount("3", true, emptyList()))
        assertEquals(draft, draft.withPassengerCount("3", false, emptyList()))
        assertEquals("1", draft.withPassengerCount("1", false, emptyList()).children)
        assertEquals("65.00", draft.copy(fare = "65.00").withPassengerCount("1", false, emptyList()).fare)
    }
    @Test fun returnRestrictedToOrdinaryAndManualFareDoublesOnlyOnce() {
        val draft = validDraft().copy(trainType = "ORDINARY", ticketType = "JOURNEY", adults = "2", fare = "60.00")
        val returned = draft.withTicketTypeFare("RETURN", emptyList())
        assertEquals("120.00", returned.fare)
        assertEquals("120.00", returned.withTicketTypeFare("RETURN", emptyList()).fare)
        assertEquals("60.00", returned.withTicketTypeFare("JOURNEY", emptyList()).fare)
        assertEquals("JOURNEY", returned.withTrainTypeFare("MAIL/EXPRESS", emptyList()).ticketType)
        assertNotNull(draftError(draft.copy(trainType = "SUPERFAST", ticketType = "RETURN")))
    }
    @Test fun routeSwapKeepsMatchingViaVariantAndReverseFare() {
        val route = SavedRoute(origin = "HOWRAH", destination = "KHARAGPUR", via = "SRC-PKU", distance = "116", fares = mapOf("ORDINARY" to "30.00"))
        val routes = emptyList<SavedRoute>().saveRoutePair(route).saveRoutePair(route.copy(id = "other", pairId = "other", via = "OTHER", distance = "130"))
        val draft = validDraft().copy(trainType = "ORDINARY").withRoute(routes.first { it.id == route.id })
        val swapped = draft.swappedRoute(routes)
        assertEquals("KHARAGPUR", swapped.origin)
        assertEquals("PKU-SRC", swapped.via)
        assertEquals("116", swapped.distance)
        assertEquals("30.00", swapped.fare)
        assertEquals(draft, swapped.swappedRoute(routes))
        assertEquals("", draft.copy(via = "UNSAVED").swappedRoute(routes).fare)
    }

}
