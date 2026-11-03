package com.example.railone

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryImprovementsTest {
    private lateinit var context: Context
    @Before fun reset() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(APP_LOCK_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }
    @After fun clear() {
        context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(APP_LOCK_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun draft() = freshDraft().copy(passengerName = "Passenger", mobile = "9876543210",
        origin = "SODEPUR", destination = "NAIHATI JN.", distance = "22")

    @Test fun renamingStationUpdatesRoutesAndTemplatesButNotBookedTickets() {
        val routes = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "SODEPUR", destination = "NAIHATI JN.", distance = "22"))
        val ticket = StoredTicket(data = draft(), createdAt = 0L, countdownEndsAt = 0L, accentIndex = 0)
        val state = JourneyState(routes = routes, tickets = listOf(ticket), templates = listOf(templateFromDraft(draft())),
            stations = listOf(Station("SODEPUR", "SOP")))
        val renamed = state.withStationRenamed("SODEPUR", Station("SODEPUR JN.", "SOP"))
        assertTrue(renamed.routes.none { it.origin == "SODEPUR" || it.destination == "SODEPUR" })
        assertTrue(renamed.routes.any { it.origin == "SODEPUR JN." } && renamed.routes.any { it.destination == "SODEPUR JN." })
        assertEquals("SODEPUR JN.", renamed.templates.single().origin)
        assertEquals("SODEPUR", renamed.tickets.single().data.origin)
        assertTrue(renamed.stations.any { it.name == "SODEPUR JN." && it.code == "SOP" })
        assertTrue(renamed.stations.none { it.name == "SODEPUR" })
    }

    @Test fun stationUsedInSavedRouteCannotBeDeletedButUnusedStationCan() {
        val routes = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "A", destination = "B", via = "C", distance = "5"))
        assertTrue(stationInUse(routes, "a"))
        assertTrue(stationInUse(routes, "B"))
        assertTrue(stationInUse(routes, "C"))
        assertFalse(stationInUse(routes, "D"))
        val state = JourneyState(routes = routes, stations = listOf(Station("D", "DD"), Station("E")))
        assertEquals(listOf("E"), state.withoutStation("D").stations.map { it.name })
    }

    @Test fun templateNameAndLastUsedAndRouteLastUsedSurviveSaveAndLoad() {
        val route = emptyList<SavedRoute>().saveRoutePair(SavedRoute(origin = "SODEPUR", destination = "NAIHATI JN.", distance = "22")).first().copy(lastUsed = 1234L)
        val template = templateFromDraft(draft()).copy(title = "Daily commute", lastUsed = 5678L)
        val store = JourneyStore(context)
        assertTrue(store.save(JourneyState(routes = listOf(route), templates = listOf(template))))
        val loaded = JourneyStore(context).load()
        assertEquals("Daily commute", loaded.templates.single().title)
        assertEquals(5678L, loaded.templates.single().lastUsed)
        assertEquals(1234L, loaded.routes.single().lastUsed)
        assertEquals("Daily commute", loaded.templates.single().heading)
    }

    @Test fun lockDelayIsStoredAndInvalidValuesAreIgnored() {
        val store = AppLockStore(context)
        assertEquals(DEFAULT_LOCK_AFTER_SECONDS, store.config().lockAfterSeconds)
        assertTrue(store.save(true, false, "123456", 300))
        assertEquals(300, AppLockStore(context).config().lockAfterSeconds)
        assertTrue(store.save(true, false, "", 7))
        assertEquals(300, store.config().lockAfterSeconds)
    }

    @Test fun legacySha1PinStillWorksAndIsUpgradedToSha256() {
        val salt = ByteArray(24) { it.toByte() }
        val spec = PBEKeySpec("123456".toCharArray(), salt, 150_000, 256)
        val legacyHash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        val prefs = context.getSharedPreferences(APP_LOCK_PREFS, Context.MODE_PRIVATE)
        assertTrue(prefs.edit().putBoolean("enabled", true).putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(legacyHash, Base64.NO_WRAP)).commit())
        assertFalse(AppLockStore(context).verify("000000").accepted)
        assertTrue(AppLockStore(context).verify("123456", 1_000_000L).accepted)
        if (Build.VERSION.SDK_INT >= 26) assertEquals("PBKDF2WithHmacSHA256", prefs.getString("alg", null))
        assertTrue(AppLockStore(context).verify("123456", 2_000_000L).accepted)
    }
}
