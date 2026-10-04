package com.example.railone

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

internal data class Passenger(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mobile: String,
    val age: String = "",
    val gender: String = "Not specified",
    val meal: String = "No preference",
    val dob: String = "",
    val concession: String = "General",
    val berth: String = "No Preference",
    val idType: String = "No Preference",
    val idNumber: String = "",
)
internal data class UserProfile(val name: String = "", val mobile: String = "", val photoFile: String = "",
    val dob: String = "", val gender: String = "Not specified", val idType: String = "No Preference", val idNumber: String = "",
    val address1: String = "", val address2: String = "", val pin: String = "", val district: String = "",
    val stateName: String = "", val country: String = "India", val username: String = "", val email: String = "",
    val divyangjan: Boolean = false, val menuVersion: String = "1.0", val postOffice: String = "", val city: String = "")
internal const val MAX_WALLET_PAISE = 100000000L
internal fun walletAmountPaise(value: String): Long? {
    if (!Regex("[0-9]{1,7}(\\.[0-9]{1,2})?").matches(value.trim())) return null
    val parts = value.trim().split('.')
    val paise = parts[0].toLong() * 100 + parts.getOrElse(1) { "" }.padEnd(2, '0').toLong()
    return paise.takeIf { it in 1..MAX_WALLET_PAISE }
}
internal fun walletDisplay(paise: Long): String = "${paise / 100}.${(paise % 100).toString().padStart(2, '0')}"
internal data class StoredTicket(
    val id: String = UUID.randomUUID().toString(),
    val data: TicketData,
    val createdAt: Long,
    val countdownEndsAt: Long,
    val accentIndex: Int,
    val cancelled: Boolean = false,
) {
    fun status(now: Long): String = when {
        cancelled -> "Cancelled"
        now >= (parseBookingTime(data.bookedOn) ?: createdAt) + TICKET_COMPLETION_MILLIS -> "Completed"
        else -> "Upcoming"
    }
    fun secondsLeft(now: Long): Int = ((countdownEndsAt - now + 999) / 1000)
        .coerceIn(0L, TICKET_COUNTDOWN_SECONDS.toLong()).toInt()
}
internal data class JourneyState(
    val tickets: List<StoredTicket> = emptyList(),
    val passengers: List<Passenger> = emptyList(),
    val templates: List<SavedJourney> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val showServices: Boolean = true,
    val walletPaise: Long = 0,
)
internal fun parseBookingTime(value: String): Long? {
    if (!Regex("[0-9]{2}/[0-9]{2}/[0-9]{4} [0-9]{2}:[0-9]{2}").matches(value)) return null
    val parser = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).apply { isLenient = false }
    val position = ParsePosition(0)
    val parsed = parser.parse(value, position)
    return if (parsed != null && position.index == value.length) parsed.time else null
}
internal const val TICKET_COMPLETION_MILLIS = 12 * 60 * 60 * 1000L
internal fun freshDraft(): TicketData {
    val now = currentDateTimeString()
    return TicketData("", "", "", "", "", toTicketDisplayDateTime(now), "", "1", "0", now,
        addHours(now, 3) ?: now, CLASS_OPTIONS.first(), TRAIN_TYPE_OPTIONS.first(),
        TICKET_TYPE_OPTIONS.first(), "", DEFAULT_IR_NO, "", DEFAULT_SERVICE_NO)
}
internal fun renewedDraft(data: TicketData): TicketData {
    val now = currentDateTimeString()
    return data.copy(bookedOn = now, bookingDateTime = toTicketDisplayDateTime(now),
        validTill = addHours(now, 3) ?: now, journeyTicket = "")
}
internal fun draftFromTemplate(j: SavedJourney): TicketData = freshDraft().copy(
    passengerName = j.passengerName, mobile = j.mobile, origin = j.origin, distance = j.distance,
    destination = j.destination, via = j.via, adults = j.adults, children = j.children,
    className = j.className, trainType = j.trainType, ticketType = j.ticketType, fare = j.fare,
    irNumber = j.irNumber, serviceNo = j.serviceNo)
internal fun templateFromDraft(data: TicketData, id: String = UUID.randomUUID().toString()): SavedJourney =
    SavedJourney(id, data.passengerName.trim(), data.mobile, data.origin.trim(), data.distance,
        data.destination.trim(), data.via, data.adults, data.children, data.className,
        data.trainType, data.ticketType, formatFare(data.fare), data.irNumber, data.serviceNo)
internal fun draftError(d: TicketData): String? {
    if (d.passengerName.isBlank()) return "Enter the lead passenger name."
    if (d.mobile.length !in 7..15 || !d.mobile.all(Char::isDigit)) return "Enter a mobile number with 7–15 digits."
    if (d.origin.isBlank() || d.destination.isBlank()) return "Enter both stations."
    if (d.origin.trim().equals(d.destination.trim(), true)) return "Origin and destination must differ."
    val distance = d.distance.trim().removeSuffix("km").trim().toDoubleOrNull()
    if (distance == null || !distance.isFinite() || distance <= 0) return "Enter a positive distance in km."
    val adults = d.adults.toIntOrNull()
    val children = d.children.toIntOrNull()
    if (adults == null || children == null || adults < 0 || children < 0 || adults.toLong() + children == 0L)
        return "Enter valid counts with at least one traveller."
    val booked = parseBookingTime(d.bookedOn) ?: return "Booked On must use dd/MM/yyyy HH:mm."
    val until = parseBookingTime(d.validTill) ?: return "Valid Till must use dd/MM/yyyy HH:mm."
    if (until <= booked) return "Valid Till must be later than Booked On."
    val fare = d.fare.toDoubleOrNull()
    if (fare == null || !fare.isFinite() || fare < 0) return "Enter a valid non-negative fare."
    if (d.irNumber.length != 15 || !d.irNumber.all { it in 'A'..'Z' || it in '0'..'9' }) return "IR No. must contain 15 letters or digits."
    if (d.serviceNo.isBlank()) return "Enter a service number."
    return null
}

internal fun ticketJson(d: TicketData): JSONObject = JSONObject().apply {
    put("passengerName", d.passengerName); put("mobile", d.mobile)
    put("origin", d.origin); put("distance", d.distance); put("destination", d.destination)
    put("bookingDateTime", d.bookingDateTime); put("via", d.via)
    put("adults", d.adults); put("children", d.children)
    put("bookedOn", d.bookedOn); put("validTill", d.validTill)
    put("className", d.className); put("trainType", d.trainType); put("ticketType", d.ticketType)
    put("fare", d.fare); put("irNumber", d.irNumber); put("journeyTicket", d.journeyTicket)
    put("serviceNo", d.serviceNo)
}
internal fun ticketFromJson(j: JSONObject): TicketData = TicketData(
    j.getString("passengerName"), j.getString("mobile"), j.getString("origin"),
    j.getString("distance"), j.getString("destination"), j.getString("bookingDateTime"),
    j.getString("via"), j.getString("adults"), j.getString("children"),
    j.getString("bookedOn"), j.getString("validTill"), j.getString("className"),
    j.getString("trainType"), j.getString("ticketType"), j.getString("fare"),
    j.getString("irNumber"), j.getString("journeyTicket"), j.getString("serviceNo"))
private fun <T> readArray(array: JSONArray, read: (JSONObject) -> T): List<T> =
    (0 until array.length()).map { read(array.getJSONObject(it)) }
private fun <T> writeArray(items: List<T>, write: (T) -> JSONObject): JSONArray =
    JSONArray().apply { items.forEach { put(write(it)) } }

/** One atomic snapshot. Legacy storage is left intact; invalid storage is never overwritten. */
internal class JourneyStore(private val context: Context) {
    private val preferences = context.getSharedPreferences(PROFILES_PREFS, Context.MODE_PRIVATE)
    private val key = "journey_state_v1"
    fun load(now: Long = System.currentTimeMillis()): JourneyState {
        val raw = preferences.getString(key, null)
        if (raw == null) {
            // Check legacy syntax before using its older tolerant reader.
            preferences.getString(SAVED_JOURNEYS_KEY, null)?.let { rawLegacy ->
                val legacy = JSONArray(rawLegacy)
                for (index in 0 until legacy.length()) {
                    val item = legacy.getJSONObject(index)
                    require(item.has("id") && item.has("passengerName") && item.has("origin") && item.has("destination")) {
                        "Invalid legacy journey record"
                    }
                }
            }
            val journeys = loadSavedJourneys(context)
            val passengers = journeys.distinctBy { it.passengerName.trim().lowercase(Locale.ROOT) + "|" + it.mobile }
                .map { Passenger(name = it.passengerName, mobile = it.mobile) }
            val migrated = JourneyState(templates = journeys, passengers = passengers)
            check(save(migrated)) { "Unable to save migrated data" }
            return migrated
        }
        val j = JSONObject(raw)
        require(j.getInt("version") == 1) { "Unsupported storage version" }
        val stored = JourneyState(
            tickets = readArray(j.getJSONArray("tickets")) { t -> StoredTicket(
                id = t.getString("id"), data = ticketFromJson(t.getJSONObject("data")),
                createdAt = t.getLong("createdAt"), countdownEndsAt = t.getLong("countdownEndsAt"),
                accentIndex = t.getInt("accentIndex"), cancelled = t.getBoolean("cancelled")) },
            passengers = readArray(j.getJSONArray("passengers")) { p -> Passenger(
                p.getString("id"), p.getString("name"), p.getString("mobile"),
                p.optString("age"), p.optString("gender", "Not specified"), p.optString("meal", "No preference"),
                p.optString("dob"), p.optString("concession", "General"), p.optString("berth", "No Preference"),
                p.optString("idType", "No Preference"), p.optString("idNumber")) },
            templates = readArray(j.getJSONArray("templates")) { t ->
                templateFromDraft(ticketFromJson(t.getJSONObject("data")), t.getString("id")) },
            profile = j.getJSONObject("profile").let { UserProfile(it.getString("name"), it.getString("mobile"), it.optString("photoFile"),
                it.optString("dob"), it.optString("gender", "Not specified"), it.optString("idType", "No Preference"), it.optString("idNumber"),
                it.optString("address1"), it.optString("address2"), it.optString("pin"), it.optString("district"),
                it.optString("stateName"), it.optString("country", "India"), it.optString("username"), it.optString("email"), it.optBoolean("divyangjan"), it.optString("menuVersion", "1.0"),
                it.optString("postOffice"), it.optString("city")) },
            showServices = j.getBoolean("showServices"),
            walletPaise = j.optLong("walletPaise", 0).also { require(it in 0..MAX_WALLET_PAISE) })
        return stored
    }
    fun save(state: JourneyState): Boolean {
        val json = JSONObject().apply {
            put("version", 1)
            put("tickets", writeArray(state.tickets) { t -> JSONObject().apply {
                put("id", t.id); put("data", ticketJson(t.data)); put("createdAt", t.createdAt)
                put("countdownEndsAt", t.countdownEndsAt); put("accentIndex", t.accentIndex)
                put("cancelled", t.cancelled)
            } })
            put("passengers", writeArray(state.passengers) { p -> JSONObject().apply {
                put("id", p.id); put("name", p.name); put("mobile", p.mobile)
                put("age", p.age); put("gender", p.gender); put("meal", p.meal)
                put("dob", p.dob); put("concession", p.concession); put("berth", p.berth); put("idType", p.idType); put("idNumber", p.idNumber)
            } })
            put("templates", writeArray(state.templates) { t -> JSONObject().apply {
                put("id", t.id); put("data", ticketJson(draftFromTemplate(t)))
            } })
            put("profile", JSONObject().apply {
                val p = state.profile
                put("name", p.name); put("mobile", p.mobile); put("photoFile", p.photoFile)
                put("dob", p.dob); put("gender", p.gender); put("idType", p.idType); put("idNumber", p.idNumber)
                put("address1", p.address1); put("address2", p.address2); put("pin", p.pin); put("district", p.district)
                put("stateName", p.stateName); put("country", p.country); put("username", p.username); put("email", p.email); put("divyangjan", p.divyangjan)
                put("menuVersion", p.menuVersion)
                put("postOffice", p.postOffice); put("city", p.city)
            })
            put("showServices", state.showServices)
            put("walletPaise", state.walletPaise)
        }
        return preferences.edit().putString(key, json.toString()).commit()
    }
}
