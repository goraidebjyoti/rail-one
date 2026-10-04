# Rail One — multiple tickets

App name: **Rail One**. Version: **1.0** (`versionCode = 1`).

A Kotlin / Jetpack Compose Android prototype. All tickets and QR codes are **ticket previews, not valid for travel**.

## Included

- Home with journey planner, optional service tiles, a swipeable carousel of all upcoming ticket cards, View All, and railway information.
- My Bookings with independent ticket records, Upcoming / Completed / Cancelled / All filters, counts, and newest/oldest sorting.
- You with an editable user profile and photo picker, saved passenger management, and editable journey templates.
- Right-side menu drawer with a persistent service-visibility preference, FAQs, help, About, and Android sharing.
- Existing ticket design retained; the ribbon above Thank You is removed. QR payloads identify previews as not valid for travel.
- Book Again copies a ticket into a new editable form. Generating creates another ticket; originals remain intact.
- Connecting Journey prefills the previous destination as the new origin, and retains the contact and traveller counts. The new destination, distance, fare, via and preferences need to be supplied.
- Optional saved passenger and journey-template selectors in the booking form.
- Input validation, delete/cancel confirmation, and an unsaved-booking-change prompt on Back.

## What can be edited

| Location | Editable data | Effect on existing tickets |
| --- | --- | --- |
| Booking Details | Lead passenger/contact, stations, distance, via, counts, dates, class, train/ticket type, fare, IR/service number | None; creating appends a separate ticket |
| You → Edit Details | Name, mobile, username, email, DOB, gender, optional ID and address | None; profile controls the Home greeting |
| You → Upload/Change Photo | Profile picture; Remove restores the default avatar | None; used in You and Menu |
| R-Wallet → Add / Add Money | Rupee amount to add to the saved local balance | None; updates the balance in You and Menu |
| You → Saved Passengers → Add/Edit | Name, optional mobile, DOB, gender, concession, berth, meal and optional ID | None; selecting copies name/mobile into a draft |
| You → Saved Journey Templates → Edit | Reusable booking form details | Updates only the chosen template after Update Journey Template |
| My Bookings → Book Again | An editable copy of the selected ticket, with dates refreshed and reference removed | Original stays intact |
| Ticket Details | Read-only ticket snapshot | No ticket field editing; Connecting Journey opens a separate draft |
| Menu | Visibility of Home service tiles | None |

The existing ticket format has one lead passenger/contact plus adult/child counts. Saved DOB, gender, concession, berth, ID and meal preferences are reusable passenger data; this update does not add a named passenger manifest to the ticket.

## Persistence and migration

Generated tickets, passengers, templates, profile and preferences use one versioned JSON snapshot in the existing app-private SharedPreferences file. Ticket IDs use UUIDs, so the same route can be stored repeatedly. The displayed reference is generated independently and checked against existing references.

The fully renamed package is `com.example.railone`, and storage uses `rail_one_profiles`. Android treats this as a separate app from the earlier package. Existing data in the old app is not automatically transferred. Keep the old app installed if you need its data. Subsequent updates of this Rail One package must keep the same application ID and signing key to retain its local data. On first launch, existing saved journeys migrate into templates and reusable passenger contacts. The old `saved_journeys_json` bytes remain untouched. The previous version never persisted generated tickets, so those cannot be recovered.

Malformed stored data blocks normal editing and leaves the data untouched. Save errors do not report success. No cloud account or sync is present; uninstalling or clearing app data removes local content.

The five-minute visual countdown resets whenever a ticket is opened through View Details on Home or My Bookings. Its new expiry is saved; activity recreation continues that countdown. It is separate from Valid Till. Upcoming means fewer than 12 hours have passed since Booked On; Completed starts at Booked On plus 12 hours, irrespective of Valid Till. Cancelled is an explicit local status, with no railway cancellation or refund. Times use the device timezone and clock.

Booking drafts, navigation selection and form-dialog inputs use saved instance state for activity recreation and OS-managed process restoration. Closing the app without restored instance state does not guarantee recovery of an unfinished draft. Persisted tickets and templates remain available.

## Reference screenshots

The new pages use measured reference colours (including navigation blue #0166FF, text #0C2065, profile blue #E2F9FF and peach #FFF1E4), screenshot-derived artwork, and spacing based on a 360dp / 921px design canvas. Home includes the actual planner illustrations, eight offering tiles, five swipeable railway fact cards and the social banner. My Bookings uses status-coloured ticket outlines with side cutouts and a bottom filter bar. You follows the reference avatar/profile/wallet/passenger/account arrangement; reusable journey templates are placed below the reference account grid. Menu is a right-side drawer over a darkened existing page, with the reference's icon artwork and entry order.

Names, dates, ticket references, balances and counts are rendered from app state rather than baked into screenshots. The wallet displays a clearly marked local balance. Disconnected service, biometric, account-authentication, transfer, refund and Aadhaar controls explain their status when tapped. Social accounts/links have not been supplied. The five facts use the captions and images from the supplied references; they are static reference content.

Screenshot-derived raster images retain the supplied resolution. Exact pixel equivalence is not verified: the original font files and original vector assets were not supplied, Android font rendering and device scaling may differ, and no Android renderer is available here. The supplied images have been visually inspected after extraction. `ReferencePreviews.kt` provides Android Studio preview entry points for the new screens.


## Build and checks

Open this folder in Android Studio with JDK 17 and Android SDK 35, sync Gradle, then build/run on API 24+.

The existing GitHub build workflow builds a debug APK and runs a launch check. A new `behavior-tests` job runs:

```bash
gradle connectedDebugAndroidTest
```

The launch check runs `.github/scripts/check-launch.sh` as one command so Bash conditionals remain intact. Its `crash-log` artifact includes logcat, launch output and an emulator screenshot. `.gitattributes` keeps shell scripts and workflow files at LF line endings when working on Windows.

There are fifteen storage/model instrumentation tests and twelve Compose navigation tests covering separate journeys, duplicate routes, snapshot isolation, repeat booking, reload countdowns, validity/cancellation, migration, corrupted data, validation, preferences, activity recreation, blank new forms drawer service visibility and swiping to a fourth Home ticket.

**Validation in the editing environment:** Kotlin syntax parsing and static consistency checks passed. Android compilation, emulator tests, and visual device verification were not run because the environment has no Android SDK or Gradle. The included workflow/tests must pass in an Android-equipped environment before treating the update as device-verified.

## Source map

- `MainActivity.kt`: existing booking editor and ticket components.
- `JourneyStore.kt`: models, validation, JSON persistence and migration.
- `AppShell.kt`: app state, navigation, draft handling and edit dialogs.
- `ReferencePages.kt`: reference artwork, Home, My Bookings, You and the right-side Menu drawer.
- `ReferencePreviews.kt`: Android Studio screen previews.
- `REFERENCE_ASSETS.md`: artwork provenance and crop notes.
- `app/src/androidTest/`: storage and navigation regression tests.

## Profile photo and local wallet

Under You, tap Upload Photo or the avatar to select an image, then Change Photo or Remove to update it. Images are resized to at most 1024 pixels and saved in app-private storage. The same photo appears in Menu. Editing name/mobile preserves the photo.

Add in You and Add Money in Menu open an editable rupee amount. Confirming adds that amount to the saved local balance in both places. Amounts must be positive with at most two decimals; the total balance limit is ₹10,00,000. Values use integer paise so additions do not introduce rounding errors. Cancel leaves the balance unchanged. This is local balance editing, with no payment collection or railway wallet connection. Older saved snapshots default to no photo and zero balance while retaining existing tickets.

## Opening and deleting tickets

View Details starts a fresh five-minute countdown for the selected ticket only. Journey details, booking/validity dates and other tickets remain unchanged. This does not extend Valid Till or change the Upcoming/Completed/Cancelled status.

In My Bookings, press and hold a ticket continuously for one second to open Delete ticket? Choose Delete to remove that ticket from local storage and both My Bookings and Home, or Keep to leave it unchanged. The hold gesture is only available in My Bookings. Releasing early or scrolling cancels the hold. The screen stays awake during the hold. Deletion is available for every booking filter and does not delete passengers or templates. Accessibility services can use the Delete ticket custom action to open the same confirmation.

The entry form and ticket details use navigation blue #0166FF, including Create Ticket and the form accents. Both Booking Details headers use a circular back button, a shared 14dp side inset and an 18dp text gap. Ticket Details places the mobile number under the title with a 4dp gap, following the supplied header reference.

## Reference passenger and account panels

You → Saved Passengers → Add/Edit opens a large bottom panel with the reference gender tiles and nested concession, berth, meal and optional ID menus. All options visible in the supplied references are included. A new passenger requires name, gender and DOB; mobile is optional and can be supplied later in the booking form. DOB determines the displayed age. Selecting an ID type requires its card number; No Preference clears it. Existing passengers keep their stored fields when upgrading. Vegetarian meals show a green circle in a green square; non-vegetarian meals show a red marker. Tea/Coffee and unspecified meals have no dietary marker.

Edit Details opens the larger Edit Your Details form. It saves personal details, username/email and address lines, PIN, district, state and country. District/state/country are editable text because this local app has no postcode lookup service. My Account is a separate panel showing the saved username, name, phone and email, a local Divyangjan preference and Delete Account confirmation. The green checks identify locally saved values, not verified online credentials. Delete Account clears only the local profile and photo after confirmation, keeping bookings, passengers, templates and wallet.

## Scrollable forms and Menu version label

Passenger, profile and preference panels size themselves within the dialog's safe drawing area and keyboard space. Their fixed heading sits above a bounded scrolling body. Extra space after the final control lets Add/Update buttons and the last preference option scroll fully above the navigation bar.

You → Edit Details → App version shown in Menu edits the right-side Menu footer (for example, V-2.5-101). Save with Update. The value persists with the profile; older data defaults to 1.0. This is a display setting. The installed APK's versionName/versionCode remain the build-time values 1.0 / 1.

## Your Details view

You → View Details opens the reference-style Your Details panel, with the saved name and an edit pencil, DOB in long-form English, gender, ID type and masked number, address lines, PIN, Post Office, City and Country. Rows are read-only and scroll within the safe area. The pencil opens Edit Your Details, where Post Office and City are now editable and saved. City falls back to the previously saved district if a separate City value is absent. The ID check indicates a locally saved value.

Tickets remain saved indefinitely, including completed and cancelled tickets. Delete them manually from My Bookings with a one-second hold and confirmation. No automatic age-based deletion runs.

Home hides the entire Upcoming Journey section (heading, View All and cards) when there are no upcoming tickets. The facts carousel reserves space for the longest caption at the current font scale, so scrolling to Hubballi does not move the social heading or banner.

The X, Facebook, Instagram and YouTube logos in the social banner each open the Ministry of Railways account in an installed app or browser. Tapping other artwork in the banner does nothing. Unimplemented services and menu actions do nothing when tapped; they no longer show placeholder information dialogs. Ticket creation, bookings, passengers, profile editing, wallet editing, sharing and Show/Hide Services retain their implemented actions.

## Launch and optional login

The blue train mark is the launcher icon and appears on a black native launch screen. A white Rail One brand screen follows, with the logo growing then shrinking. Login is off by default, so the animation normally opens Home.

In You, the App Login card sits directly above the Saved Journey Templates card. Open App Login, activate the login page, enter and confirm a six-digit mPIN, optionally enable enrolled device biometrics, then Save. Login settings require the current mPIN when changing or disabling an existing lock. The Biometric tile opens the same settings. A cold app launch then requires mPIN or the Android biometric prompt. Rotation preserves the current session; a fresh process requires authentication again. The lock gates the local app and does not sign into a remote railway account.

The mPIN is stored as a salted PBKDF2 verifier, never as plaintext, in separate preferences excluded from cloud/device-transfer backups. Five wrong attempts pause PIN verification for 30 seconds. Reset mPIN requires the device's screen-lock credential before choosing a new mPIN. A device without a screen lock cannot use that recovery route. No remote password or alternate-user account is configured, so Forgot Password and Different User are inactive, as requested for unimplemented controls.

## Bookings updates

Cards show Unreserved, UTS reference, ticket type, booking date, source, distance in km, destination and the Book Again/View Details actions. They expand to fit text instead of clipping the station row. Upcoming is orange, Completed green, Cancelled red and All blue; cards in All retain their individual status outline.

Tickets become Completed exactly 12 hours after Booked On regardless of Valid Till, and remain saved in Completed until manually deleted. Cancelled remains an explicit status. Empty filters show the grey ticket illustration and "No Tickets Found. Swipe down to refresh." Pulling down refreshes storage.

The header's reference sort icon opens Sort & Filters, with Sort By / Filter sections, Journey Date / Booking Date choices and Apply. The Filter section selects Upcoming, Completed, Cancelled or All. Sorting uses the entered timestamp rather than the time the record was saved. For these unreserved tickets the journey date is the booked date, so the two date choices currently produce the same chronological order. The original newest-first ordering is retained.

Saved Journey Templates is now a boxed option with an icon; tap it to expand or collapse the saved templates and their existing Add/Use/Edit/Delete controls.

## Latest booking and profile changes

- Home shows View All only with two or more upcoming tickets.
- The unreserved booking form has no Passenger Details section. Book Ticket uses the current profile name and mobile from You. Old tickets keep the contact captured when booked.
- Booking cards use compact text spacing and stronger coloured dashed separators. Long station names can wrap without clipping.
- Hold any My Bookings ticket for one second, then confirm deletion. Home has no delete gesture.
- The Biometric tile has an animated On/Off switch. With App Login enabled and a device biometric enrolled it toggles the saved biometric setting; otherwise it opens setup. The mPIN remains available.
- Saved passenger summaries use gender and berth abbreviations and a vertical bar before the full meal name. Selection menus retain full option names.
