package com.example.railone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserAccountsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before @After fun clear() {
        context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(APP_LOCK_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun bookingRequiresNameUsernameAndValidMobile() {
        val valid = UserProfile(name = "Owner", username = "owner", mobile = "9876543210")
        assertNull(bookingProfileError(valid))
        assertNotNull(bookingProfileError(valid.copy(name = " ")))
        assertNotNull(bookingProfileError(valid.copy(username = " ")))
        assertNotNull(bookingProfileError(valid.copy(mobile = "123")))
        assertNotNull(bookingProfileError(valid.copy(mobile = "98765abcde")))
    }
    @Test fun addingAndSwitchingUserRetainsOriginalDataAndSeparatesAllCollections() {
        val original = JourneyState(profile = UserProfile("Owner", "9876543210", username = "owner"),
            passengers = listOf(Passenger(name = "Saved", mobile = "9876543210")), walletPaise = 855,
            routes = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "A", destination = "B", distance = "10")))
        val store = JourneyStore(context)
        assertTrue(store.save(original))
        val accounts = UserAccounts(context)
        val id = accounts.create(UserProfile("Second", "9123456789", username = "second"))
        assertTrue(accounts.select(id))
        val second = JourneyStore(context)
        assertEquals("second", second.load().profile.username)
        assertTrue(second.load().routes.isEmpty())
        assertTrue(second.load().tickets.isEmpty())
        assertTrue(second.load().passengers.isEmpty())
        assertTrue(second.save(second.load().copy(walletPaise = 200)))
        assertTrue(accounts.select(ORIGINAL_USER_ID))
        assertEquals(original, JourneyStore(context).load())
        assertEquals(200L, JourneyStore(context, id).load().walletPaise)
    }
    @Test fun duplicateUsernamesAreRejectedWithoutReplacingAccounts() {
        JourneyStore(context).save(JourneyState(profile = UserProfile("Owner", "9876543210", username = "owner")))
        val accounts = UserAccounts(context)
        assertTrue(runCatching { accounts.create(UserProfile("Other", "9123456789", username = " OWNER ")) }.isFailure)
        assertEquals(listOf(ORIGINAL_USER_ID), accounts.ids())
        assertEquals("Owner", JourneyStore(context).load().profile.name)
    }
    @Test fun mPinAndBiometricSettingsAreScopedToTheSelectedUser() {
        val accounts = UserAccounts(context)
        val second = accounts.create(UserProfile("Second", "9123456789", username = "second"))
        val firstLock = AppLockStore(context, ORIGINAL_USER_ID)
        val secondLock = AppLockStore(context, second)
        assertTrue(firstLock.save(true, true, "123456"))
        assertFalse(secondLock.config().enabled)
        assertTrue(secondLock.save(true, false, "654321"))
        assertTrue(firstLock.verify("123456").accepted)
        assertFalse(firstLock.verify("654321").accepted)
        assertTrue(secondLock.verify("654321").accepted)
        assertFalse(secondLock.verify("123456").accepted)
        assertTrue(firstLock.config().biometric)
        assertFalse(secondLock.config().biometric)
    }
    @Test fun legacyTicketOwnersMigrateAndLaterProfileChangesDoNotReassignThem() {
        val data = freshDraft().copy(passengerName = "Owner", mobile = "9876543210")
        val ticket = StoredTicket(data = data, createdAt = 1, countdownEndsAt = 2, accentIndex = 0)
        val store = JourneyStore(context)
        assertTrue(store.save(JourneyState(tickets = listOf(ticket), profile = UserProfile("Owner", "9876543210", username = "owner"))))
        val prefs = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
        val raw = JSONObject(prefs.getString("journey_state_v1", null)!!)
        raw.getJSONArray("tickets").getJSONObject(0).apply { remove("ownerUserId"); remove("ownerUsername") }
        assertTrue(prefs.edit().putString("journey_state_v1", raw.toString()).commit())
        val migrated = store.load()
        assertEquals(ORIGINAL_USER_ID, migrated.tickets.single().ownerUserId)
        assertEquals("owner", migrated.tickets.single().ownerUsername)
        assertTrue(store.save(migrated.copy(profile = migrated.profile.copy(name = "Changed", username = "changed", mobile = "9123456789"))))
        val old = store.load().tickets.single()
        assertEquals("owner", old.ownerUsername)
        assertEquals("Owner", old.data.passengerName)
        assertEquals("9876543210", old.data.mobile)
    }
}
