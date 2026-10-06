package com.example.railone

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import org.json.JSONArray
import java.util.UUID

internal const val ORIGINAL_USER_ID = "original"
private const val USERS_KEY = "local_user_ids_v1"
private const val ACTIVE_USER_KEY = "active_local_user_v1"
internal fun activeUserId(context: Context): String = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
    .getString(ACTIVE_USER_KEY, ORIGINAL_USER_ID) ?: ORIGINAL_USER_ID
internal data class LocalUser(val id: String, val profile: UserProfile)
internal fun bookingProfileError(profile: UserProfile): String? = when {
    profile.name.isBlank() -> "Set your name in You → Edit Details before booking."
    profile.username.isBlank() -> "Set your username in You → Edit Details before booking."
    !profile.mobile.trim().matches(Regex("[0-9]{7,15}")) -> "Set a valid mobile number in You → Edit Details before booking."
    else -> null
}
internal class UserAccounts(private val context: Context) {
    private val prefs = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
    fun ids(): List<String> {
        val raw = prefs.getString(USERS_KEY, null) ?: return listOf(ORIGINAL_USER_ID)
        val array = JSONArray(raw)
        val ids = (0 until array.length()).map { array.getString(it) }
        require(ORIGINAL_USER_ID in ids && ids.distinct().size == ids.size)
        require(ids.all { it == ORIGINAL_USER_ID || runCatching { UUID.fromString(it) }.isSuccess })
        return ids
    }
    fun users(): List<LocalUser> = ids().map { LocalUser(it, JourneyStore(context, it).load().profile) }
    fun usernameTaken(value: String, exceptId: String? = null): Boolean = value.isNotBlank() &&
        users().any { it.id != exceptId && it.profile.username.trim().equals(value.trim(), ignoreCase = true) }
    fun select(id: String): Boolean {
        require(id in ids())
        return prefs.edit().putString(ACTIVE_USER_KEY, id).commit()
    }
    fun create(profile: UserProfile): String {
        require(bookingProfileError(profile) == null)
        require(!usernameTaken(profile.username)) { "This username is already in use on this device." }
        val id = UUID.randomUUID().toString()
        val oldIds = ids()
        check(JourneyStore(context, id).save(JourneyState(profile = profile.copy(name = profile.name.trim(),
            username = profile.username.trim(), mobile = profile.mobile.trim())))) { "Could not save this user." }
        check(prefs.edit().putString(USERS_KEY, JSONArray(oldIds + id).toString()).commit()) { "Could not register this user." }
        return id
    }
}

@Composable
internal fun UserPickerSheet(accounts: UserAccounts, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val users = remember(accounts) { runCatching { accounts.users() } }
    ReferenceSheet("Different User", onDismiss, fraction = .8f) {
        Text("Each local user has separate tickets, passengers, routes, wallet and login settings.")
        if (users.isFailure) Text("User data could not be read. Nothing has been replaced.", color = MaterialTheme.colorScheme.error)
        users.getOrDefault(emptyList()).forEach { user ->
            OutlinedButton(onClick = { onSelect(user.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("${user.profile.name.ifBlank { "Original user" }} · ${user.profile.username.ifBlank { "Username not set" }}")
            }
        }
        TextButton(onClick = { adding = !adding }, enabled = users.isSuccess) { Text("Add User") }
        if (adding) {
            OutlinedTextField(name, { name = it.take(60) }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(username, { username = it.take(60) }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(mobile, { mobile = it.filter(Char::isDigit).take(15) }, label = { Text("Mobile Number") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val profile = UserProfile(name = name, username = username, mobile = mobile)
                error = bookingProfileError(profile)
                if (error == null) runCatching { accounts.create(profile) }.onSuccess(onSelect)
                    .onFailure { error = it.message ?: "Could not create this user." }
            }, modifier = Modifier.fillMaxWidth()) { Text("Create User") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
