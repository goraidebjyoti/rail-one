@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.railone

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class LoginSession : ViewModel() {
    var introDone by mutableStateOf(false)
    var unlocked by mutableStateOf(false)
}
private val LoginBlue = Color(0xFF0166FF)
private val LoginInk = Color(0xFF0C2065)

@Composable
internal fun LaunchGate(session: LoginSession) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val store = remember { AppLockStore(context) }
    var config by remember { mutableStateOf(store.config()) }
    var settingsOpen by remember { mutableStateOf(false) }
    var recovery by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentLocked = config.enabled && !session.unlocked
    val locked by rememberUpdatedState(currentLocked)
    val biometrics = remember { BiometricManager.from(context) }
    val available = biometrics.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    // Recreate the callback with the Activity after rotation, as required by BiometricPrompt.
    val biometricPrompt = remember(activity) {
        BiometricPrompt(activity, ContextCompat.getMainExecutor(context), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (locked && store.config().biometric) { session.unlocked = true; pin = ""; error = "" }
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { error = errString.toString() }
            override fun onAuthenticationFailed() { error = "Biometric not recognised. Try again or use your mPIN." }
        })
    }
    val recoveryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && locked) { recovery = true; error = "" }
    }
    fun biometricLogin() {
        if (!config.biometric || !available) { error = "Enable an enrolled device biometric in App Login settings."; return }
        biometricPrompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Rail One")
            .setSubtitle("Unlock with your device biometric")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Use mPIN").build())
    }
    if (!session.introDone) BrandLaunch { session.introDone = true }
    else if (currentLocked) {
        BackHandler { activity.finish() }
        val name = remember { runCatching { JourneyStore(context).load().profile.name }.getOrDefault("") }
        LoginPage(name, pin, { pin = it; error = "" }, error, busy, config.biometric && available,
            onLogin = {
                if (!busy) scope.launch {
                    busy = true
                    try {
                        val result = withContext(Dispatchers.IO) { store.verify(pin) }
                        if (result.accepted) { session.unlocked = true; pin = ""; error = "" } else error = result.error
                    } catch (_: Exception) { error = "Could not verify mPIN. Please retry." }
                    finally { busy = false }
                }
            }, onBiometric = { biometricLogin() }, onReset = {
                val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (!keyguard.isDeviceSecure) error = "Set a device screen lock to use mPIN recovery."
                else {
                    @Suppress("DEPRECATION")
                    val intent = keyguard.createConfirmDeviceCredentialIntent("Reset Rail One mPIN", "Confirm your device screen lock")
                    if (intent != null) runCatching { recoveryLauncher.launch(intent) }.onFailure { error = "Device verification is unavailable." }
                }
            })
    } else RailOneApp(config.enabled, config.biometric, onLoginSettings = { settingsOpen = true }, onBiometricToggle = {
        if (!config.enabled || (!config.biometric && !available)) settingsOpen = true
        else if (store.save(config.enabled, !config.biometric)) config = store.config()
    })
    if (settingsOpen || recovery) LoginSettingsSheet(store, config, available, recovery,
        onDismiss = { settingsOpen = false; recovery = false }, onSaved = {
            config = store.config(); settingsOpen = false
            session.unlocked = true; pin = ""; recovery = false
        })
}

@Composable
private fun BrandLaunch(onFinished: () -> Unit) {
    val scale = remember { Animatable(.8f) }
    LaunchedEffect(Unit) {
        delay(350)
        scale.animateTo(1.25f, tween(250))
        delay(120)
        scale.animateTo(.68f, tween(350))
        delay(120)
        onFinished()
    }
    val activity = LocalContext.current as Activity
    SideEffect {
        val bars = androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        bars.isAppearanceLightStatusBars = true; bars.isAppearanceLightNavigationBars = true
    }
    Box(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.launch_brand), "Rail One", Modifier.width(155.dp).scale(scale.value))
    }
}

@Composable
private fun LoginPage(name: String, pin: String, onPin: (String) -> Unit, error: String, busy: Boolean,
    biometricAvailable: Boolean, onLogin: () -> Unit, onBiometric: () -> Unit, onReset: () -> Unit) {
    val activity = LocalContext.current as Activity
    SideEffect {
        val bars = androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        bars.isAppearanceLightStatusBars = true; bars.isAppearanceLightNavigationBars = true
    }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE2F9FF), Color.White)))
        .safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 23.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.rail_one_logo), "Rail One", Modifier.width(115.dp))
        Spacer(Modifier.height(65.dp))
        Text("Login using mPIN", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B4D5E))
        Spacer(Modifier.height(28.dp))
        Text("Welcome ${name.ifBlank { "Traveller" }}!", fontSize = 15.sp, color = Color(0xFF55576A))
        Spacer(Modifier.height(28.dp))
        Text("Enter mPIN below", color = Color(0xFF55576A), fontSize = 15.sp)
        Spacer(Modifier.height(16.dp))
        BasicTextField(value = pin, onValueChange = { onPin(it.filter { c -> c in '0'..'9' }.take(6)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("login-pin"), decorationBox = { inner ->
                Box {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        repeat(6) { index ->
                            Box(Modifier.weight(1f).height(44.dp).background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF91D4E9), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Text(if (index < pin.length) "●" else "", fontSize = 19.sp)
                            }
                        }
                    }
                    // Keep the editable field focusable and accessible; mask actual digits.
                    Box(Modifier.size(1.dp)) { inner() }
                }
            }, visualTransformation = PasswordVisualTransformation())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { /* No remote password account is configured. */ }) { Text("Forgot Password?", color = LoginInk, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            TextButton(onClick = onReset) { Text("Reset mPIN?", color = LoginInk, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(66.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text("Or login using biometric", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(Modifier.weight(1f))
        }
        Spacer(Modifier.height(44.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBiometric, enabled = biometricAvailable && !busy) { Icon(Icons.Default.Face, "Device face or fingerprint login", Modifier.size(32.dp), tint = Color.Gray) }
            IconButton(onClick = onBiometric, enabled = biometricAvailable && !busy) { Icon(Icons.Default.Fingerprint, "Biometric login", Modifier.size(36.dp), tint = Color.Gray) }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onLogin, enabled = pin.length == 6 && !busy, shape = CircleShape,
                modifier = Modifier.width(108.dp).height(40.dp).testTag("login-submit")) { Text("Login", color = LoginBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        Spacer(Modifier.height(44.dp))
        TextButton(onClick = { /* This app has one local profile. */ }) { Text("Different User?", color = LoginInk, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun LoginSettingsSheet(store: AppLockStore, config: AppLockConfig, biometricAvailable: Boolean,
    recovery: Boolean, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var enabled by remember { mutableStateOf(if (recovery) true else config.enabled) }
    var biometric by remember { mutableStateOf(config.biometric) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ReferenceSheet(if (recovery) "Reset mPIN" else "App Login", onDismiss, fraction = .72f) {
        if (!recovery) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Activate login page", modifier = Modifier.weight(1f), color = LoginInk)
            Switch(checked = enabled, onCheckedChange = { enabled = it }, modifier = Modifier.testTag("login-enable"))
        }
        Text("Set a six-digit mPIN. Device biometrics are optional.", fontSize = 12.sp, color = Color.Gray)
        @Composable fun PinField(label: String, value: String, change: (String) -> Unit) {
            OutlinedTextField(value, { change(it.filter { c -> c in '0'..'9' }.take(6)) }, label = { Text(label) },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
        }
        if (config.enabled && !recovery) PinField("Current mPIN", currentPin) { currentPin = it }
        if (enabled) {
            PinField(if (config.enabled && !recovery) "New mPIN (optional)" else "New mPIN", newPin) { newPin = it }
            PinField("Confirm new mPIN", confirmPin) { confirmPin = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Use device biometric", modifier = Modifier.weight(1f), color = LoginInk)
                Switch(checked = biometric && biometricAvailable, onCheckedChange = { biometric = it }, enabled = biometricAvailable)
            }
            if (!biometricAvailable) Text("No supported enrolled biometric is available. mPIN login still works.", color = Color.Gray, fontSize = 12.sp)
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        Button(onClick = {
            if (!busy) scope.launch {
                error = ""
                val needsPin = enabled && (!config.enabled || recovery || newPin.isNotEmpty())
                if (needsPin && (newPin.length != 6 || newPin != confirmPin)) { error = "Enter and confirm the same six-digit mPIN."; return@launch }
                busy = true
                try {
                    val result = withContext(Dispatchers.IO) {
                        if (config.enabled && !recovery) {
                            val verified = store.verify(currentPin)
                            if (!verified.accepted) return@withContext verified.error
                        }
                        if (store.save(enabled, biometric && biometricAvailable, if (needsPin) newPin else "")) "" else "Could not save login settings."
                    }
                    if (result.isEmpty()) onSaved() else error = result
                } catch (_: Exception) { error = "Could not save login settings. Please retry." }
                finally { busy = false }
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(48.dp), shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = LoginBlue)) { Text(if (recovery) "Reset mPIN" else "Save") }
    }
}
