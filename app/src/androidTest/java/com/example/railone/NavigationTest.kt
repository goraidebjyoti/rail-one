package com.example.railone

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var first: StoredTicket
    private lateinit var second: StoredTicket
    @Before fun start() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        val draft = freshDraft().copy(passengerName = "Traveller", mobile = "9876543210", origin = "HOWRAH",
            destination = "KHARAGPUR", distance = "116 km", fare = "30.00", journeyTicket = "X123456789")
        val now = System.currentTimeMillis()
        first = StoredTicket(data = draft, createdAt = now, countdownEndsAt = now + 300000, accentIndex = 0)
        second = StoredTicket(data = draft.copy(origin = "DELHI", destination = "AGRA", journeyTicket = "X987654321"),
            createdAt = now + 1, countdownEndsAt = now + 300000, accentIndex = 2)
        JourneyStore(context).save(JourneyState(tickets = listOf(first, second)))
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }
    @After fun stop() { scenario.close() }
    @Test fun emptyHomeHidesUpcomingSectionIncludingWhenOnlyCompletedTicketsRemain() {
        scenario.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = JourneyStore(context)
        store.save(JourneyState())
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.onNodeWithText("Upcoming Journey").assertDoesNotExist()
        compose.onNodeWithText("View All").assertDoesNotExist()
        compose.onNodeWithTag("upcoming-journeys").assertDoesNotExist()
        scenario.close()
        val past = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - 60_000))
        store.save(JourneyState(tickets = listOf(first.copy(data = first.data.copy(validTill = past)))))
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.onNodeWithText("Upcoming Journey").assertDoesNotExist()
        compose.onNodeWithText("View All").assertDoesNotExist()
        compose.onNodeWithTag("upcoming-journeys").assertDoesNotExist()
    }
    @Test fun socialHeadingStaysInPlaceWhenFactsScrollToLongestCaption() {
        compose.onNodeWithTag("railway-facts").performScrollTo()
        val rowHeight = compose.onNodeWithTag("railway-facts").fetchSemanticsNode().size.height
        val before = compose.onNodeWithTag("social-heading").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("railway-facts").performScrollToIndex(3)
        compose.waitForIdle()
        val after = compose.onNodeWithTag("social-heading").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertEquals(rowHeight, compose.onNodeWithTag("railway-facts").fetchSemanticsNode().size.height)
        org.junit.Assert.assertEquals(before, after, 1f)
    }
    @Test fun selectingTicketAndRecreatingActivityKeepsItsDetails() {
        compose.onNodeWithText("My Bookings").performClick()
        compose.onNodeWithText("UTS: ${second.data.journeyTicket}").assertIsDisplayed()
        compose.onAllNodesWithText("View Details")[0].performClick()
        compose.onNodeWithText(second.data.journeyTicket).assertIsDisplayed()
        scenario.recreate()
        compose.onNodeWithText(second.data.journeyTicket).assertIsDisplayed()
        compose.onNodeWithContentDescription("Back", useUnmergedTree = true).performClick()
        compose.onNodeWithText("UTS: ${second.data.journeyTicket}").assertIsDisplayed()
    }
    @Test fun openingDetailsFromBookingsAndHomeResetsOnlyThatCountdown() {
        scenario.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = JourneyStore(context)
        first = first.copy(countdownEndsAt = 1)
        second = second.copy(countdownEndsAt = 1)
        store.save(JourneyState(tickets = listOf(first, second)))
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.onNodeWithText("My Bookings").performClick()
        val beforeBookings = System.currentTimeMillis()
        compose.onAllNodesWithText("View Details")[0].performClick()
        val afterBookings = System.currentTimeMillis()
        val loaded = store.load()
        org.junit.Assert.assertTrue(loaded.tickets.single { it.id == second.id }.countdownEndsAt in
            (beforeBookings + 300000)..(afterBookings + 300000))
        org.junit.Assert.assertEquals(first, loaded.tickets.single { it.id == first.id })
        compose.onNodeWithContentDescription("Back", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Back", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("upcoming-journeys").performScrollTo()
        val beforeHome = System.currentTimeMillis()
        compose.onNode(hasText("View Details") and hasAnyAncestor(hasTestTag("home-ticket-${first.id}"))).performClick()
        val afterHome = System.currentTimeMillis()
        org.junit.Assert.assertTrue(store.load().tickets.single { it.id == first.id }.countdownEndsAt in
            (beforeHome + 300000)..(afterHome + 300000))
        org.junit.Assert.assertEquals(first.data, store.load().tickets.single { it.id == first.id }.data)
    }
    @Test fun deleteConfirmationKeepsOrRemovesOnlySelectedTicket() {
        compose.onNodeWithText("My Bookings").performClick()
        fun requestDelete() {
            val actions: List<CustomAccessibilityAction> = compose.onNodeWithTag("booking-ticket-${second.id}")
                .fetchSemanticsNode().config[SemanticsActions.CustomActions]
            compose.runOnIdle { actions.single { it.label == "Delete ticket" }.action() }
        }
        requestDelete()
        compose.onNodeWithText("Delete ticket?").assertIsDisplayed()
        compose.onNodeWithText("Keep").performClick()
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.junit.Assert.assertEquals(2, JourneyStore(context).load().tickets.size)
        requestDelete()
        compose.onNodeWithText("Delete", substring = false).performClick()
        compose.onNodeWithText("Upcoming (1)").assertIsDisplayed()
        org.junit.Assert.assertEquals(listOf(first), JourneyStore(context).load().tickets)
        compose.onNodeWithContentDescription("Back", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("upcoming-journeys").performScrollTo()
        compose.onNodeWithTag("home-ticket-${second.id}").assertDoesNotExist()
        compose.onNodeWithTag("home-ticket-${first.id}").assertIsDisplayed()
    }
    @Test fun twoSecondHoldShowsDeleteAndEarlyReleaseDoesNot() {
        compose.onNodeWithText("My Bookings").performClick()
        val card = compose.onNodeWithTag("booking-ticket-${second.id}")
        card.performTouchInput { down(center); advanceEventTime(200); up() }
        compose.onNodeWithText("Delete ticket?").assertDoesNotExist()
        card.performTouchInput { down(center) }
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onAllNodesWithText("Delete ticket?").fetchSemanticsNodes().isNotEmpty()
        }
        card.performTouchInput { up() }
        compose.onNodeWithText("Keep").performClick()
    }
    @Test fun bookAgainCreatesThirdTicketAndPreservesOriginals() {
        compose.onNodeWithText("My Bookings").performClick()
        compose.onAllNodesWithText("Book Again")[0].performClick()
        compose.onNodeWithText("CREATE TICKET").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Back", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Upcoming (3)").assertIsDisplayed()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tickets = JourneyStore(context).load().tickets
        org.junit.Assert.assertEquals(3, tickets.size)
        org.junit.Assert.assertEquals(first, tickets.find { it.id == first.id })
        org.junit.Assert.assertEquals(second, tickets.find { it.id == second.id })
    }
    @Test fun sideMenuTogglesHomeServices() {
        compose.onNodeWithText("More Offerings").assertIsDisplayed()
        compose.onNodeWithText("Menu").performClick()
        compose.onNodeWithText("FAQs").assertIsDisplayed()
        compose.onNodeWithText("Show/Hide Services").performClick()
        compose.onNodeWithText("More Offerings").assertDoesNotExist()
    }
    @Test fun walletAddSavesExactAmountAndSurvivesRecreation() {
        compose.onNodeWithText("You").performClick()
        compose.onNodeWithText("Upload Photo").assertIsDisplayed()
        compose.onNodeWithTag("wallet-add").performClick()
        compose.onNode(hasSetTextAction() and hasText("Amount to add (₹)")).performTextInput("123.45")
        compose.onNode(hasText("Add", substring = false) and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("₹ 123.45").assertIsDisplayed()
        scenario.recreate()
        compose.onNodeWithText("₹ 123.45").assertIsDisplayed()
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.junit.Assert.assertEquals(12345L, JourneyStore(context).load().walletPaise)
    }
    @Test fun newTicketStartsWithBlankPassengerAndRoute() {
        compose.onNodeWithContentDescription("New Ticket").performClick()
        compose.onNode(hasSetTextAction() and hasText("Passenger Name")).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        compose.onNode(hasSetTextAction() and hasText("From Station")).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
    }
    @Test fun passengerReferenceSheetSavesMealAndBirthDate() {
        compose.onNodeWithText("You").performClick()
        compose.onNodeWithTag("add-passenger").performScrollTo().performClick()
        compose.onNodeWithText("Male").performClick()
        compose.onNode(hasSetTextAction() and hasText("Full Name")).performTextInput("Saved Traveller")
        compose.onNode(hasSetTextAction() and hasText("DOB (dd/MM/yyyy)")).performScrollTo().performTextInput("28/12/1999")
        compose.onNodeWithContentDescription("Meal Preferences").performScrollTo().performClick()
        compose.onNodeWithText("Veg", substring = false).performClick()
        compose.onNode(hasText("Add Passenger") and hasClickAction()).performScrollTo()
            .assertIsDisplayed().assertHeightIsEqualTo(48.dp).performClick()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val saved = JourneyStore(context).load().passengers.single()
        org.junit.Assert.assertEquals("Veg", saved.meal)
        org.junit.Assert.assertEquals("28/12/1999", saved.dob)
        compose.onNodeWithContentDescription("Veg meal").assertExists()
    }
    @Test fun extendedProfileSaveOpensSeparateAccountSheet() {
        compose.onNodeWithText("You").performClick()
        compose.onNodeWithText("Edit Details").performClick()
        compose.onNodeWithText("Edit Your Details").assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("Full Name")).performTextInput("Profile Traveller")
        compose.onNode(hasSetTextAction() and hasText("Mobile")).performTextInput("9876543210")
        compose.onNode(hasSetTextAction() and hasText("Username")).performTextInput("traveller")
        compose.onNode(hasSetTextAction() and hasText("Email")).performScrollTo().performTextInput("traveller@example.com")
        compose.onNode(hasSetTextAction() and hasText("App version shown in Menu")).performScrollTo()
            .performTextReplacement("2.5-101")
        compose.onNodeWithText("Update", substring = false).performScrollTo()
            .assertIsDisplayed().assertHeightIsEqualTo(48.dp).performClick()
        compose.onNodeWithText("My\nAccount").performScrollTo().performClick()
        compose.onNodeWithText("My Account").assertIsDisplayed()
        compose.onNodeWithText("traveller@example.com").assertExists()
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.junit.Assert.assertEquals("traveller", JourneyStore(context).load().profile.username)
        org.junit.Assert.assertEquals("2.5-101", JourneyStore(context).load().profile.menuVersion)
        compose.onNodeWithContentDescription("Close My Account").performClick()
        compose.onNodeWithText("Menu", substring = false).performClick()
        compose.onNodeWithText("V-2.5-101").performScrollTo().assertIsDisplayed()
    }
    @Test fun profileViewShowsReferenceDetailsAndEditShortcut() {
        scenario.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val profile = UserProfile(name = "Profile Traveller", mobile = "9876543210", dob = "28/12/1999", gender = "Male",
            idType = "Aadhaar ID/Virtual ID", idNumber = "123456789012", address1 = "Street", pin = "700114",
            postOffice = "Panihati S.O", city = "North 24 Parganas")
        JourneyStore(context).save(JourneyState(profile = profile, tickets = listOf(first)))
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.onNodeWithText("You").performClick()
        compose.onNodeWithText("View Details", substring = false).performClick()
        compose.onNodeWithText("Your Details").assertIsDisplayed()
        compose.onNodeWithText("28th December, 1999").assertIsDisplayed()
        compose.onNodeWithText("•••• 9012").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("123456789012").assertDoesNotExist()
        compose.onNodeWithText("Panihati S.O").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("North 24 Parganas").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit your details").performScrollTo().performClick()
        compose.onNodeWithText("Edit Your Details").assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("Full Name")).performTextReplacement("Updated Traveller")
        compose.onNodeWithText("Update", substring = false).performScrollTo().performClick()
        org.junit.Assert.assertEquals("Updated Traveller", JourneyStore(context).load().profile.name)
        org.junit.Assert.assertEquals(listOf(first), JourneyStore(context).load().tickets)
    }
    @Test fun homeCarouselReachesFourthTicketAndOpensItsDetails() {
        scenario.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val third = second.copy(id = "third", data = second.data.copy(journeyTicket = "X333333333"))
        val fourth = second.copy(id = "fourth", data = second.data.copy(journeyTicket = "X444444444"))
        // Keep the complete carousel in view so this test isolates horizontal paging.
        JourneyStore(context).save(JourneyState(tickets = listOf(first, second, third, fourth), showServices = false))
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.onNodeWithTag("upcoming-journeys").performScrollTo()
        compose.onNodeWithText("1 / 4").performScrollTo().assertIsDisplayed()
        repeat(3) { page ->
            compose.onNodeWithTag("upcoming-journeys").performScrollTo()
            compose.onNodeWithTag("upcoming-journeys").performTouchInput {
                // Each card is narrower than the viewport. Drag more than half a
                // card but less than a whole card, slowly, above the action buttons.
                swipe(start = Offset(width * .75f, height * .25f),
                    end = Offset(width * .25f, height * .25f), durationMillis = 600)
            }
            compose.waitForIdle()
            compose.onNodeWithText("${page + 2} / 4").assertExists(
                "After swipe ${page + 1}, expected page ${page + 2}. Current UI:\n${compose.onRoot().printToString()}")
        }
        compose.onNodeWithText("4 / 4").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("View Details") and hasAnyAncestor(hasTestTag("home-ticket-fourth"))).performClick()
        compose.onNodeWithText(fourth.data.journeyTicket).performScrollTo().assertIsDisplayed()
    }
}
