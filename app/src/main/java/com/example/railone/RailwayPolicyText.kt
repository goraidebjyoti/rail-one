package com.example.railone

internal const val OFFICIAL_RAILONE_PRIVACY_URL = "https://devaaikyam.indianrailways.gov.in/privacy-policy"
internal const val PRIVACY_INTRO = "This page describes how this local Rail One app handles the information you enter. The official RailOne service has its own privacy policy, available through the link above."
internal val PRIVACY_POINTS = listOf(
    "Profile details, saved passengers, journey templates, ticket records and the wallet display value are saved in this app's private storage on your device.",
    "The app imports a selected profile photo into private storage. Removing or replacing it deletes the old local copy.",
    "The optional mPIN is stored as a salted verifier. The device biometric prompt performs authentication; this app does not receive fingerprint or facial images.",
    "App lock settings are excluded from Android backups. Other app data may be included in device backups according to your Android settings.",
    "Tickets remain saved until you delete them. Deleting a profile does not automatically delete tickets, saved passengers, templates or the wallet display value.",
    "Social links open in an external application or browser. Their privacy practices apply after you leave this app.",
    "Write Email opens an external mail composer with the recipient, subject and a message placeholder. You choose the sending account, edit the message and decide whether to send it.",
    "Call 139 opens your dialler. The app does not place a call automatically.",
    "This project does not connect profile, ticket or wallet information to an Indian Railways booking or payment service."
)
internal const val TERMS_INTRO = "These terms describe the local features of this Rail One project. Official Indian Railways services and their policies are managed separately by their providers."
internal val TERMS_SECTIONS = listOf(
    "Using the App" to "You can manage local profile details, passengers, journey templates and ticket previews. The app keeps information entered on this device for your own use.",
    "Ticket Records" to "Ticket records created here are local previews. Creating a record does not reserve travel, purchase a railway ticket or make a payment. Use an official authorised service for travel bookings.",
    "Saved Data" to "A ticket becomes Completed twelve hours after its Booked On time. Records stay saved until you delete them. A confirmed deletion cannot be undone within the app.",
    "External Services" to "Social media, email and call buttons hand control to another app. You remain in control of any message or call. The external service's own terms apply when you use it.",
    "App Login" to "You may enable a six-digit mPIN and available device biometrics. Keep your device screen lock secure. Recovery uses the device's screen-lock confirmation.",
    "Local Profile" to "Editing or deleting your local profile changes this project's data only. It does not edit or close an official railway account.",
    "Availability" to "External links depend on installed applications and network availability. Local features depend on your device and available storage."
)
