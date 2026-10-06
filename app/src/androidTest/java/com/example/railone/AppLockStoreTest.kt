package com.example.railone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockStoreTest {
    private lateinit var context: Context
    private lateinit var store: AppLockStore
    @Before fun reset() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(APP_LOCK_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        store = AppLockStore(context)
    }
    @After fun clear() { context.getSharedPreferences(APP_LOCK_PREFS, Context.MODE_PRIVATE).edit().clear().commit() }
    @Test fun loginIsOffByDefaultAndOnlySaltedVerifierIsPersisted() {
        assertEquals(AppLockConfig(), store.config())
        assertTrue(store.save(true, false, "001234"))
        val prefs = context.getSharedPreferences(APP_LOCK_PREFS, Context.MODE_PRIVATE)
        assertFalse(prefs.all.values.contains("001234"))
        val salt = prefs.getString("salt", null)
        assertTrue(AppLockStore(context).verify("001234").accepted)
        assertEquals(AppLockConfig(true, false), AppLockStore(context).config())
        assertTrue(store.save(true, true, "001234"))
        assertNotEquals(salt, prefs.getString("salt", null))
        assertEquals(AppLockConfig(true, true), store.config())
    }
    @Test fun failedAttemptDelayPersistsAcrossReloadAndCorrectPinWorksAfterCooldown() {
        store.save(true, false, "123456")
        repeat(5) { assertFalse(store.verify("000000", 1000).accepted) }
        assertFalse(AppLockStore(context).verify("123456", 30_999).accepted)
        assertTrue(AppLockStore(context).verify("123456", 31_000).accepted)
    }
    @Test fun setupRejectsInvalidPinsAndDisablingLoginPreservesJourneyData() {
        assertTrue(runCatching { store.save(true, false, "12345") }.isFailure)
        assertTrue(runCatching { store.save(true, false, "12345a") }.isFailure)
        assertTrue(runCatching { store.save(true, false) }.isFailure)
        assertEquals(AppLockConfig(), store.config())
        val journeys = JourneyStore(context)
        val state = JourneyState(profile = UserProfile("Traveller"), walletPaise = 1234)
        journeys.save(state)
        store.save(true, false, "123456")
        assertTrue(store.save(false, true))
        assertEquals(AppLockConfig(false, false), store.config())
        assertEquals(state, journeys.load())
    }
}
