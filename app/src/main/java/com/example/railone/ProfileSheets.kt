@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.railone

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

private val SheetBlue = Color(0xFF0166FF)
private val SheetInk = Color(0xFF0C2065)
internal val BERTH_PREFERENCES = listOf("No Preference", "Lower", "Middle", "Upper", "Side Lower", "Side Middle", "Side Upper", "Window Side", "Cabin", "Coupe")
internal val MEAL_PREFERENCES = listOf("Veg", "Non Veg", "Tea/Coffee", "Jain Meal", "Diabetic Veg", "Diabetic Non Veg", "Tea with Snacks(Veg Only)")
internal val ID_CARD_TYPES = listOf("Driving Licence", "Passport/Travel Document", "Pan Card", "Voter ID-Card", "Aadhaar ID/Virtual ID", "Govt Issued Id-Card", "Student ID-Card", "No Preference")
internal val CONCESSION_TYPES = listOf("Person with Disability", "Escort", "General")
internal fun normalMeal(value: String): String = when (value) { "Vegetarian" -> "Veg"; "Non-vegetarian" -> "Non Veg"; else -> value }
internal fun mealMarker(value: String): String? = when (normalMeal(value)) {
    "Veg", "Jain Meal", "Diabetic Veg", "Tea with Snacks(Veg Only)" -> "Veg"
    "Non Veg", "Diabetic Non Veg" -> "Non Veg"
    else -> null
}
internal fun dobError(dob: String): String? {
    if (dob.isBlank()) return null
    val time = parseBookingTime("$dob 00:00") ?: return "Use dd/MM/yyyy for DOB."
    if (time > System.currentTimeMillis()) return "DOB cannot be in the future."
    val year = Calendar.getInstance().apply { timeInMillis = time }.get(Calendar.YEAR)
    return if (year < Calendar.getInstance().get(Calendar.YEAR) - 120) "Enter a DOB within the last 120 years." else null
}
internal fun ageFromDob(dob: String, fallback: String): String {
    val time = parseBookingTime("$dob 00:00") ?: return fallback
    val birth = Calendar.getInstance().apply { timeInMillis = time }
    val today = Calendar.getInstance()
    var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    if (today.get(Calendar.MONTH) < birth.get(Calendar.MONTH) ||
        (today.get(Calendar.MONTH) == birth.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))) age--
    return age.coerceAtLeast(0).toString()
}

@Composable
internal fun ReferenceSheet(title: String, onDismiss: () -> Unit, fraction: Float = .83f, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().height(maxHeight * fraction)
                .background(Color.White, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).padding(horizontal = 18.dp, vertical = 18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 20.sp, color = SheetInk, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp).border(2.dp, Color(0xFFCEEDF2), CircleShape)) {
                        Icon(Icons.Default.Close, "Close $title", tint = SheetBlue, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp), content = content)
            }
        }
    }
}
@Composable
private fun SheetField(label: String, value: String, onChange: (String) -> Unit, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, onChange, label = { Text(label, fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SheetBlue, unfocusedBorderColor = Color(0xFF91D4E6)),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp))
}
@Composable
private fun ChoiceField(label: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF91D4E6)), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value.ifBlank { label }, color = if (value.isBlank()) Color.Gray else Color.Black, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowDown, label, tint = SheetBlue, modifier = Modifier.size(23.dp))
        }
    }
}
@Composable
private fun OptionSheet(title: String, values: List<String>, selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    ReferenceSheet(title, onDismiss, .75f) {
        values.forEach { value ->
            Surface(onClick = { onSelect(value) }, shape = CircleShape,
                color = if (value == selected) SheetBlue else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (value == selected) SheetBlue else Color(0xFFE8E8E8))) {
                Text(value, fontSize = 16.sp, color = if (value == selected) Color.White else Color(0xFF505260),
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            }
        }
    }
}
@Composable
private fun GenderChoices(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf("Male" to R.drawable.gender_male, "Female" to R.drawable.gender_female, "Trans Gender" to R.drawable.gender_trans).forEach { (value, icon) ->
            val tint = if (selected == value) SheetBlue else Color(0xFFB4B4B4)
            Column(Modifier.size(64.dp).border(1.dp, tint, RoundedCornerShape(9.dp)).clickable { onSelect(value) }.padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(painterResource(icon), null, Modifier.size(28.dp), colorFilter = ColorFilter.tint(tint))
                Text(value, fontSize = 9.sp, color = tint)
            }
        }
    }
}
@Composable
internal fun PassengerSheet(passenger: Passenger, isNew: Boolean, onDismiss: () -> Unit, onSave: (Passenger) -> Unit) {
    var name by rememberSaveable(passenger.id) { mutableStateOf(passenger.name) }
    var mobile by rememberSaveable(passenger.id) { mutableStateOf(passenger.mobile) }
    var gender by rememberSaveable(passenger.id) { mutableStateOf(passenger.gender) }
    var dob by rememberSaveable(passenger.id) { mutableStateOf(passenger.dob) }
    var concession by rememberSaveable(passenger.id) { mutableStateOf(passenger.concession) }
    var berth by rememberSaveable(passenger.id) { mutableStateOf(passenger.berth) }
    var meal by rememberSaveable(passenger.id) { mutableStateOf(normalMeal(passenger.meal)) }
    var idType by rememberSaveable(passenger.id) { mutableStateOf(passenger.idType) }
    var idNumber by rememberSaveable(passenger.id) { mutableStateOf(passenger.idNumber) }
    var selector by rememberSaveable { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }
    ReferenceSheet(if (isNew) "Add Passenger" else "Edit Passenger", onDismiss) {
        GenderChoices(gender) { gender = it }
        ChoiceField("Select Concession Type", concession) { selector = "concession" }
        SheetField("Full Name", name, { name = it.take(60) })
        Text("Please submit Name (Max. 60 char) and Date of Birth as per Aadhaar.", color = Color.Gray, fontSize = 12.sp)
        SheetField("DOB (dd/MM/yyyy)", dob, { dob = it.take(10) })
        ChoiceField("Berth Preferences", berth) { selector = "berth" }
        ChoiceField("Meal Preferences", meal) { selector = "meal" }
        ChoiceField("ID Card Type (Optional)", idType) { selector = "id" }
        if (idType != "No Preference") SheetField("Enter Card Number", idNumber, { idNumber = it.take(40) })
        SheetField("Mobile (optional)", mobile, { mobile = it.filter(Char::isDigit).take(15) }, KeyboardType.Phone)
        problem?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        Button(onClick = {
            problem = when {
                name.isBlank() -> "Enter the passenger name."
                gender !in listOf("Male", "Female", "Trans Gender") -> "Select a gender."
                dob.isBlank() && isNew -> "Enter the date of birth."
                dobError(dob) != null -> dobError(dob)
                mobile.isNotBlank() && mobile.length !in 7..15 -> "Mobile must have 7–15 digits."
                idType != "No Preference" && idNumber.isBlank() -> "Enter the selected card number or choose No Preference."
                else -> null
            }
            if (problem == null) onSave(passenger.copy(name = name.trim(), mobile = mobile, gender = gender, dob = dob,
                age = ageFromDob(dob, passenger.age), concession = concession, berth = berth, meal = meal,
                idType = idType, idNumber = if (idType == "No Preference") "" else idNumber.trim()))
        }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = SheetBlue), modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(if (isNew) "Add Passenger" else "Update Passenger", fontSize = 16.sp)
        }
    }
    when (selector) {
        "concession" -> OptionSheet("Concession Type", CONCESSION_TYPES, concession, { selector = "" }) { concession = it; selector = "" }
        "berth" -> OptionSheet("Berth Preferences", BERTH_PREFERENCES, berth, { selector = "" }) { berth = it; selector = "" }
        "meal" -> OptionSheet("Meal Preferences", MEAL_PREFERENCES, meal, { selector = "" }) { meal = it; selector = "" }
        "id" -> OptionSheet("ID Card Type (Optional)", ID_CARD_TYPES, idType, { selector = "" }) { idType = it; idNumber = ""; selector = "" }
    }
}

@Composable
internal fun ProfileDetailsSheet(profile: UserProfile, onDismiss: () -> Unit, onSave: (UserProfile) -> Unit) {
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var mobile by rememberSaveable { mutableStateOf(profile.mobile) }
    var dob by rememberSaveable { mutableStateOf(profile.dob) }
    var gender by rememberSaveable { mutableStateOf(profile.gender) }
    var idType by rememberSaveable { mutableStateOf(profile.idType) }
    var idNumber by rememberSaveable { mutableStateOf(profile.idNumber) }
    var address1 by rememberSaveable { mutableStateOf(profile.address1) }
    var address2 by rememberSaveable { mutableStateOf(profile.address2) }
    var pin by rememberSaveable { mutableStateOf(profile.pin) }
    var district by rememberSaveable { mutableStateOf(profile.district) }
    var stateName by rememberSaveable { mutableStateOf(profile.stateName) }
    var country by rememberSaveable { mutableStateOf(profile.country) }
    var postOffice by rememberSaveable { mutableStateOf(profile.postOffice) }
    var city by rememberSaveable { mutableStateOf(profile.city) }
    var username by rememberSaveable { mutableStateOf(profile.username) }
    var email by rememberSaveable { mutableStateOf(profile.email) }
    var menuVersion by rememberSaveable { mutableStateOf(profile.menuVersion) }
    var selector by rememberSaveable { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }
    val edited = profile.copy(name = name.trim(), mobile = mobile, dob = dob, gender = gender, idType = idType,
        idNumber = if (idType == "No Preference") "" else idNumber.trim(), address1 = address1.trim(), address2 = address2.trim(),
        pin = pin, district = district.trim(), stateName = stateName.trim(), country = country.trim(), username = username.trim(), email = email.trim(), menuVersion = menuVersion.trim(),
        postOffice = postOffice.trim(), city = city.trim())
    ReferenceSheet("Edit Your Details", onDismiss, .93f) {
        SheetField("Full Name", name, { name = it.take(60) })
        SheetField("Mobile", mobile, { mobile = it.filter(Char::isDigit).take(15) }, KeyboardType.Phone)
        SheetField("Username", username, { username = it.take(60) })
        SheetField("Email", email, { email = it.take(254) }, KeyboardType.Email)
        SheetField("DOB (dd/MM/yyyy)", dob, { dob = it.take(10) })
        GenderChoices(gender) { gender = it }
        ChoiceField("ID Card Type (Optional)", idType) { selector = "id" }
        if (idType != "No Preference") SheetField("Card Number", idNumber, { idNumber = it.take(40) })
        SheetField("Address Line1", address1, { address1 = it.take(200) })
        SheetField("Address Line2", address2, { address2 = it.take(200) })
        SheetField("PIN Code", pin, { pin = it.filter(Char::isDigit).take(6) }, KeyboardType.Number)
        SheetField("Post Office", postOffice, { postOffice = it.take(100) })
        SheetField("City", city, { city = it.take(100) })
        SheetField("District", district, { district = it.take(80) })
        SheetField("State", stateName, { stateName = it.take(80) })
        SheetField("Country", country, { country = it.take(80) })
        SheetField("App version shown in Menu", menuVersion, { menuVersion = it.take(40) })
        problem?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        Button(onClick = {
            problem = when {
                name.isBlank() || mobile.length !in 7..15 -> "Enter a name and mobile number with 7–15 digits."
                !Regex("[0-9]+(\\.[0-9]+){0,3}([-+][A-Za-z0-9.-]+)?").matches(menuVersion.trim()) -> "Enter a version such as 1.0 or 2.1.66-237."
                dobError(dob) != null -> dobError(dob)
                pin.isNotBlank() && pin.length != 6 -> "PIN code must contain six digits."
                email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address."
                idType != "No Preference" && idNumber.isBlank() -> "Enter the selected card number or choose No Preference."
                else -> null
            }
            if (problem == null) onSave(edited)
        }, enabled = edited != profile, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = SheetBlue), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Update", fontSize = 16.sp) }
    }
    if (selector == "id") OptionSheet("ID Card Type (Optional)", ID_CARD_TYPES, idType, { selector = "" }) { idType = it; idNumber = ""; selector = "" }
}

@Composable
internal fun AccountSheet(profile: UserProfile, onDismiss: () -> Unit, onEdit: () -> Unit, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    ReferenceSheet("My Account", onDismiss, .65f) {
        listOf(profile.username.ifBlank { "Username not set" }, profile.name.ifBlank { "Name not set" },
            profile.mobile.ifBlank { "Mobile not set" }, profile.email.ifBlank { "Email not set" }).forEachIndexed { index, value ->
            Row(Modifier.fillMaxWidth().background(if (index == 0) Color(0xFFF5F5F5) else Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFD5D5D5), RoundedCornerShape(8.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (index == 2 || index == 3) Icon(if (index == 2) Icons.Default.Phone else Icons.Default.Email, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                Text(value, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = if (index >= 2) 10.dp else 0.dp))
                if (index > 0 && !value.endsWith("not set")) Icon(Icons.Default.CheckCircle, "Saved locally", tint = Color(0xFF00BD54), modifier = Modifier.size(18.dp))
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFFE9FFE7), CircleShape), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(profile.divyangjan, onCheckedChange = onToggle)
            Text("Enable Reserved Divyangjan Booking Flow", fontSize = 12.sp, color = SheetInk, modifier = Modifier.weight(1f))
        }
        Text("Account details and this preference are saved locally. No online account verification is connected.", fontSize = 10.sp, color = Color.Gray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onEdit) { Text("Edit Details", color = SheetBlue) }
            TextButton(onClick = onDelete) { Text("Delete Account?", color = SheetBlue) }
        }
    }
}

internal fun profileDobDisplay(value: String): String {
    val time = parseBookingTime("$value 00:00") ?: return value.ifBlank { "Not set" }
    val day = Calendar.getInstance().apply { timeInMillis = time }.get(Calendar.DAY_OF_MONTH)
    val suffix = if (day in 11..13) "th" else when (day % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
    return "$day$suffix " + java.text.SimpleDateFormat("MMMM, yyyy", java.util.Locale.ENGLISH).format(java.util.Date(time))
}
internal fun maskedProfileId(value: String): String = when {
    value.isBlank() -> "Not set"
    value.length <= 4 -> "••••"
    else -> "•••• ${value.takeLast(4)}"
}
@Composable
private fun ProfileValue(label: String, value: String, savedId: Boolean = false) {
    OutlinedTextField(value = value.ifBlank { "Not set" }, onValueChange = {}, readOnly = true,
        label = { Text(label, fontSize = 11.sp, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp), minLines = 1, maxLines = 3,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFF91D4E6), focusedBorderColor = Color(0xFF91D4E6)),
        trailingIcon = { if (savedId) Icon(Icons.Default.CheckCircle, "ID saved locally", tint = Color(0xFF00BD54), modifier = Modifier.size(18.dp)) })
}
@Composable
internal fun ProfileViewSheet(profile: UserProfile, onDismiss: () -> Unit, onEdit: () -> Unit) {
    ReferenceSheet("Your Details", onDismiss, .93f) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(profile.name.ifBlank { "Your Profile" }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black,
                modifier = Modifier.widthIn(max = 240.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "Edit your details", tint = SheetBlue, modifier = Modifier.size(22.dp))
            }
        }
        ProfileValue("DOB", profileDobDisplay(profile.dob))
        ProfileValue("Gender", profile.gender)
        ProfileValue("ID Type", profile.idType)
        ProfileValue("ID Number", maskedProfileId(profile.idNumber), savedId = profile.idNumber.isNotBlank())
        ProfileValue("Address Line1", profile.address1)
        ProfileValue("Address Line2", profile.address2)
        ProfileValue("Pin Code", profile.pin)
        ProfileValue("Post Office", profile.postOffice)
        ProfileValue("City", profile.city.ifBlank { profile.district })
        ProfileValue("Country", profile.country)
    }
}

@Composable
internal fun BookingsSortSheet(sortBy: String, filter: String, onDismiss: () -> Unit, onApply: (String, String) -> Unit) {
    var section by remember { mutableStateOf("Sort By") }
    var selectedSort by remember { mutableStateOf(sortBy) }
    var selectedFilter by remember { mutableStateOf(filter) }
    ReferenceSheet("Sort & Filters", onDismiss, fraction = .45f) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Sort By", "Filter").forEach { label ->
                FilterChip(selected = section == label, onClick = { section = label }, label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF0166FF), selectedLabelColor = Color.White), shape = CircleShape)
            }
        }
        if (section == "Sort By") Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Journey Date", "Booking Date").forEach { label ->
                FilterChip(selected = selectedSort == label, onClick = { selectedSort = label }, label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF0166FF), selectedLabelColor = Color.White), shape = CircleShape)
            }
        } else Column {
            listOf("Upcoming", "Completed", "Cancelled", "All").forEach { label ->
                FilterChip(selected = selectedFilter == label, onClick = { selectedFilter = label }, label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = bookingColour(label), selectedLabelColor = Color.White), shape = CircleShape)
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { onApply(selectedSort, selectedFilter) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0166FF))) { Text("Apply", fontSize = 18.sp) }
    }
}
