# Validation record

## Completed in this workspace

- Parsed all Kotlin/Kotlin Gradle files, with the tree-sitter Kotlin grammar: no syntax-error or missing-token nodes.
- Checked that all 18 TicketData fields have matching JSON writer and reader keys.
- Inspected ticket generation for list append rather than route/name replacement.
- Checked persisted countdown timestamps, draft discard protection, explicit UUID template identity, and connecting-journey Back routing.
- Parsed the Android XML resources and manifest.
- Parsed GitHub Actions YAML and checked that the behavior-tests job invokes connectedDebugAndroidTest.
- Checked that no empty click handler remains in the application screens.
- Added 15 storage/model tests and 12 Compose UI/navigation tests.

## Not performed in this workspace

- Gradle compilation or APK generation.
- Running the Android instrumentation tests.
- Emulator or physical-device visual and accessibility review.

There is no Android SDK or Gradle installation in this environment. Syntax/static checks do not establish type correctness, a passing Android build, or device layout correctness. Use Android Studio or the included GitHub Actions jobs to finish these checks.

## Uploaded GitHub run evidence and launch-check correction

The uploaded run log shows successful APK installation and `Status: ok` for the initial launch of `com.example.railone/.MainActivity`. The subsequent failure was a shell syntax error in the workflow's multi-line `if`, not evidence of an app crash. The emulator action executed each script line separately.

The launch check now runs one Bash script, with source checkout in the crash-check job. It checks launch status, process survival and crash logs, and saves `logcat.txt`, `launch-output.txt` and `launch.png` in the `crash-log` artifact. LF endings are enforced for shell scripts and workflow files. Bash syntax and mocked success/failure cases were checked locally; the corrected emulator job still needs a GitHub run. The supplied log does not establish that instrumentation tests passed or that rendered layouts match the references.

## Manual device checklist

1. Update-install using the same application ID and signing key; check that old saved journeys appear as templates and saved contacts under You.
2. Create tickets for two different routes; verify both in My Bookings and open them independently.
3. Repeat the same route; verify that it creates an additional record.
4. Force-stop and reopen; verify tickets, references, colours and dates remain unchanged and countdowns restart at five minutes only when View Details is tapped.
5. Edit/delete a saved passenger and a template; confirm generated tickets remain unchanged.
6. Test Book Again and Connecting Journey; check fresh dates and correct Back destination.
7. Change a booking field and press Back; check Keep Editing / Discard.
8. Validate invalid dates, zero travellers, blank contacts, negative/invalid distance and fare.
9. Cancel one ticket; inspect Cancelled and All filters and confirm other tickets are unchanged.
10. Check small screens, landscape, enlarged font, keyboard scrolling and Android system Back.

## Reference visual update

Screenshot-derived raster assets were extracted and visually inspected. Colours and key positions were measured against the references. Resource IDs and package consistency were checked. This is source/asset verification, not an Android screenshot comparison; exact rendered font and pixel alignment remain unverified.

All 45 PNG resources decode and every drawable reference resolves. The 24 initial artwork crops were compared with their screenshot source pixels and matched exactly. Additional logo/menu/filter crops and transparent navigation masks were also inspected.

## Ticket wording and Home carousel update

Removed the ribbon above Thank You and the previous preview wording from app source. Create Ticket, booking statuses, cancellation messages and accessibility labels use the updated wording. Home shows all upcoming journeys in a horizontal pager with stable ticket IDs and a page count. My Bookings remains a vertical list. Added a device regression test that swipes to the fourth Home ticket and opens its details; Android tests still need to run in GitHub.

## Facts, profile photo and wallet update

Added Noney, Hubballi and electrification screenshot crops to the existing horizontally scrollable facts list. Checked that all 42 drawable images decode and referenced resource names exist. Added backwards-compatible photo-file and integer-paise wallet fields. New Android tests cover amount validation, exact arithmetic, old snapshot defaults, photo import/resizing, and wallet editing/recreation. These device tests have not run locally; GitHub is configured to run them.

## Countdown reset and ticket deletion

View Details updates and persists only the selected expiry before navigation, and sets the display clock to the opening time. A two-second pointer hold on a My Bookings card opens a confirmation; release/scroll cancels it. Updated callbacks avoid restarting the gesture during once-per-second clock updates. Added device tests for resets through both entry points and Keep/Delete isolation through the accessibility confirmation action. Added a device gesture test for a short release and a full two-second hold, plus a check that deletion also removes the Home card. Android tests have not run locally.

## Two-second hold and header correction

The hold threshold is now 2000ms. Delete remains available only on My Bookings cards; it removes the shared stored ticket, so Home updates too. Both Booking Details screens use a shared header with navigation blue #0166FF, a circular back button and aligned title/mobile text. Create Ticket and booking form controls use the same blue. Static parsing passed; final device alignment remains unverified.

## Latest test compilation fix and reference forms

The supplied run compiled the application but failed compiling NavigationTest: CustomActions was incorrectly addressed through SemanticsProperties. It now uses SemanticsActions.CustomActions and an explicit List<CustomAccessibilityAction>. The behavior-tests job compiles both APKs before starting the emulator. The daemon-startup message recovered in that run; deprecation warnings were not the failing task.

Added reference passenger, preference, profile-edit and account bottom panels. Persistence includes backward-compatible defaults for all new fields. Added model tests for field reload/defaults, legacy meal markers and DOB validation, and UI tests for passenger creation and separate profile/account flows. Kotlin syntax, XML/YAML, resource references and image decoding were checked locally. Android compilation, instrumentation execution and rendered layout remain unverified here.

## Uploaded behavior-test run and carousel test correction

The latest supplied run compiled both application and test code, executed all 26 tests, and reported one failure in homeCarouselReachesFourthTicketAndOpensItsDetails. The other 25 tests passed. The abbreviated log identifies a visibility assertion but omits the test source line; it does not prove which assertion failed.

The carousel test previously scrolled the pager into view, then assumed the separate page counter immediately below it was visible. It now scrolls each counter into view before asserting visibility, checks the expected counter after every real swipe, and scrolls the opened ticket reference into view before its final assertion. Application behavior and test expectations are unchanged. Local syntax/static checks passed; the corrected test still requires an emulator rerun.

## Follow-up carousel gesture correction

The next supplied run again passed 25/26 tests and now identifies the first post-swipe check: the expected 2 / 4 counter was absent. It does not report the actual page. The prior viewport fix did not resolve that gesture assertion.

The test fixture now hides service tiles so the full carousel is visible. Instead of the generic full-width swipeLeft gesture, each swipe uses a slow 600ms drag from 75% to 25% of the viewport width at 25% of card height. This avoids the card action buttons and keeps travel shorter than one card while crossing its halfway point. All three expected page transitions and the fourth ticket's details remain required. Failure messages include the UI semantics tree to expose the actual counter/page. No application code changed. Static checks passed; emulator execution is still required.

## Safe-area scrolling and editable Menu version

User screenshots showed final controls underneath the gesture navigation area. ReferenceSheet now requests an edge-to-edge dialog, applies safe drawing and IME insets, sizes from the remaining BoxWithConstraints height, and gives the scroll body the remaining space with a 24dp trailing pad. The shared fix covers profile, passenger, account and preference sheets.

Added a backward-compatible menuVersion profile field, version input validation and the dynamic Menu footer. Extended existing tests to check full Add/Update button height after scrolling, saving/displaying the Menu version, and legacy defaults. Local source checks pass. Actual keyboard/system-bar rendering and these updated Android tests still need a device/GitHub run.

## Reference Your Details panel

Replaced the name/mobile message dialog with a dedicated read-only reference sheet and edit shortcut. Added backward-compatible Post Office and City fields to profile storage and editing. Added a UI regression for viewing DOB, masked ID and postal/city values, using the edit shortcut and preserving tickets. Extended storage reload/default tests. Source syntax and static checks passed; device execution and visual matching remain unverified locally.

## Empty Home, ticket retention and facts layout update

- Home only emits the Upcoming Journey heading, View All and pager when upcoming tickets exist.
- Retention uses Booked On plus exactly 24 hours, independently of validity, cancellation, creation time and the details countdown. Expired records are removed from the shared snapshot on load and during the foreground clock updates. Closed-app cleanup occurs on the next load.
- All fact captions are measured before layout; the row reserves the longest caption height, including off-screen Hubballi, and adapts to font scale without truncating captions.
- Added regression coverage for the deadline boundary, future and legacy records, persisted cleanup, unchanged profile/passengers/templates/wallet, empty/completed-only Home and stable facts-row/social position. Storage tests use an injected clock so their dated fixtures remain reproducible.
- Static Kotlin, serialization, XML and workflow checks passed. There are 30 instrumentation tests included; they have not been executed locally because this environment has no Android SDK/emulator. GitHub must run the device checks.

## Social links and inactive buttons

- Added four separate accessible click regions aligned with the existing social logos, scaling with the banner. HTTPS ACTION_VIEW links: x.com/RailMinIndia, facebook.com/RailMinIndia, instagram.com/railminindia and youtube.com/user/RailMinIndia.
- Removed placeholder Home/You service messages and Menu information popups. Unimplemented menu actions leave the drawer open and do nothing. Storage errors and confirmations for actual edits remain.
- Social destinations checked against published Ministry account references (PIB release 106141 for X/Facebook/YouTube and the Ministry profile listing for Instagram). Direct Instagram fetching was unavailable.
- Static syntax, serialization, XML and CI checks passed. Browser/app launching and visual touch alignment have not been tested on a device locally.

## Facts layout test scroll fix

The supplied GitHub run compiled both APKs and passed 29 of 30 tests. The sole failure was the new social layout test trying to call performScrollTo on railway-facts before the outer LazyColumn had composed that off-screen item. Home now exposes a parent home-content test tag; the test scrolls that existing LazyColumn with performScrollToNode, then makes the social heading visible before comparing its position across horizontal facts scrolling. Both the row-height and heading-position assertions remain. Static checks passed; the corrected device test has not run locally.

## Latest: launch, optional login and bookings

- Added native splash theme, launcher icons, grow/shrink brand animation and an optional login gate. Added AndroidX BiometricPrompt hosted by FragmentActivity, a six-digit mPIN verifier, persisted failed-attempt delay and device-credential recovery. Authentication preferences are separate from journey data and excluded from backups. No plaintext mPIN is persisted or logged; hashing runs off the main thread.
- Login starts disabled. The setting card sits above the expandable Saved Journey Templates card. Existing profile edits do not change login configuration. Unimplemented remote password/different-user controls remain inactive.
- Card body height is content-driven so station/distance rows fit. Header/outline/selected icons use status colours. Empty filters show the supplied illustration and refresh message, with pull-to-refresh. The sort panel stages choices until Apply; sorting uses the entered Booked On timestamp, shared with Journey Date for unreserved tickets.
- Completion now uses Booked On +12h and retains cancellation priority. Auto-deletion remains +24h. Updated the existing boundary test and completed-only Home fixture accordingly.
- Added salted-verifier reload, retry-delay persistence, invalid-PIN rejection, unaffected journey storage, optional-login setup/wrong/correct-PIN flow and sort/filter regression tests. Navigation tests explicitly wait for the new launch animation. There are 37 instrumentation tests included, including full station-row visibility on booking cards.
- Local Kotlin syntax, ticket JSON symmetry, XML/YAML and resource checks passed. The Android SDK/Gradle/emulator are absent locally; compilation, device biometrics/credential recovery, touch/layout screenshots and instrumentation results must be verified by GitHub and an actual enrolled device. The last supplied GitHub run predates these changes and reported 29/30 passing tests; it is not evidence that this revision passes.

## Latest revision supersedes earlier retention and hold notes

Automatic ticket removal has been removed from both load and foreground saves. Tickets become Completed at Booked On +12h and remain saved; manual deletion requires a one-second hold and confirmation. Added retention-over-one-year and passenger abbreviation regression assertions plus a single-ticket Home test. Booking form contacts come from the current profile. Reference card text spacing and separators were tightened and the biometric tile now uses an animated persistent toggle.

39 instrumentation tests are included. Kotlin syntax, XML/YAML and archive checks were run locally; Android compilation and device tests cannot run here because Gradle/Android SDK/emulator are unavailable. GitHub must run the full build and test suite.

## About and contact update

Added About, scrollable local Terms Of Use and Privacy Policy screens, a verified official privacy-policy link, and external mail/dialler/social intents. Added two intent-contract tests and one full About → Terms/Privacy → About → Home navigation test. 42 Android tests are included. Static Kotlin parsing and XML/YAML/archive validation are available locally; Android compilation and actual handoff into installed mail, phone and social apps require GitHub/device validation.

The full supplied official legal text was not reproduced. Original project-specific notices and an external official privacy-policy link are provided instead. CRIS attribution explicitly refers to the official RailOne service.

## Independent routes and stable update identity

Added five instrumentation regressions covering direct/alternative routes, reversed Via order, paired edits, unchanged passenger/contact and booking fields, old snapshot migration, and route persistence. Total: 47 Android regression tests included, not executed locally. The preferences name and snapshot key remain unchanged. A fixed development keystore is bundled and version codes increase on GitHub builds. The backup/migration PowerShell tool uses binary streams and checksum verification; it was reviewed statically but cannot be device-tested here. Kotlin syntax, XML and workflow checks were rerun. Android build, instrumentation tests and Windows/device migration still require CI or a physical device.

## Completed details, PDF sharing and automatic biometrics

Parsed the uploaded one-page A4 invoice and inspected its rendered layout. Added native completed-ticket details and A4 PDF generation with invoice-specific FileProvider read grants and the system share chooser. Added five Android regressions for completed UI, saved-field formatting/filename safety, renderable A4 PDF and red travel-invalid notice, share URI/read permissions and provider path confinement. Total: 52 instrumentation tests included, not executed locally. Auto biometric login is launched once after the splash stages and retains its attempted state across rotation; cancelled or unavailable authentication falls back to mPIN. Device biometric success/cancellation, Android share UI and generated-PDF rendering require device/CI validation. No Gradle/Android SDK is available here; static parsing cannot establish a passing build.

## Route distance and station autocomplete

Added route distance to persistence, both directions, labels and route editing. Old route records recover distance from matching stored templates/tickets in either direction, or remain present with a missing-distance prompt. Uppercase normalization applies when saving and typing station/Via fields. Autocomplete uses names from saved routes only; exact station pairs autofill when unique and expose an explicit path choice when ambiguous. Added four model/storage tests and one Compose UI test for these behaviors and manual edits. Total: 57 instrumentation tests included, not run locally. Kotlin syntax, JSON ticket symmetry, Android XML, workflow configuration and ZIP checks pass. Gradle compilation, instrumentation execution and keyboard/dropdown behavior on a device are still unverified in this environment.

## Multi-user setup and booking gates

Added per-user snapshots with original snapshot-key compatibility, a local user registry, unique usernames, per-user mPIN/biometric preference keys and user selection from You and login. Authentication callbacks and asynchronous mPIN results check the selected user before unlocking. Tickets store owner ID and username while existing name/mobile snapshots stay unchanged. Profile completeness is enforced before opening booking and immediately before ticket save. Added five model/storage/authentication tests and two UI tests for profile gating, user creation/switching and authentication on switching back. Total: 64 Android tests included, not executed here. Static Kotlin, XML and workflow checks pass. Device/CI testing of login switching and complete booking flow remains required.

## Independent Via edits and removal

Added a Remove Via action and an explicit choice between editing one direction and both directions. Added three route regressions for paired Via removal with distance retained, independent reverse edits and later synchronization, persistence and collision protection. Total: 67 instrumentation tests included, not run locally. Kotlin/XML/workflow static checks pass; Android build and device UI remain unverified.

## CI profile-edit test correction

The user's uploaded CI run compiled the app and test APKs and completed 67 instrumentation tests: 66 passed, one failed. `extendedProfileSaveOpensSeparateAccountSheet` appended `traveller` to the fixture's prefilled username, producing `travellertraveller`. Changed this test to replace name, mobile, username and email rather than append, and added exact saved-value assertions for all four fields. Production code is unchanged. Static checks pass; the corrected Android test suite has not been rerun in this workspace.

## Saved fares by train type

Added optional fares for the four supported train types, route/train-type fare lookup, editable booking overrides, reverse-direction fare copying and independent fare edits. Added four model regressions covering persistence, reverse copying, missing fares, independent edits, booking saves and pre-fare migration; added one navigation regression for train-type lookup and manual overrides. Total: 72 instrumentation tests included, not executed locally. Kotlin syntax parsing, ticket JSON symmetry, XML and workflow checks passed. No Android SDK/Gradle/emulator is available here, so compilation and device behavior remain unverified.

## Adult fares and Others navigation

Added exact decimal adult multipliers (1–4), ordinary-only return doubling and base fare recovery when saving a total. New bookings reject adult counts outside 1–4. Added an Others tile and separate searchable route/template lists with direct/Via filters; adjusted existing login/user navigation tests for the new entry point. Added three model tests and one navigation regression, bringing the suite to 76 tests. Home action buttons now use a purple tint. Kotlin parser, XML, ticket serialization and workflow checks pass. Android compilation/instrumentation remain unverified locally.

## October 10 controls and station catalog

Added six regressions for station persistence/unique extraction, name/code lookup and code exclusion from ticket JSON, combined passenger limits, Ordinary-only Return and repeated/manual fare calculation, reverse Via matching across multiple routes, and booking code/counter interaction. Existing login tests now verify automatic submission without clicking Login. Fare UI tests target the new Mail/Express pill, and template navigation tests scroll within Others. Total 82 instrumentation tests, not executed locally. Static Kotlin syntax (24 files), XML, workflow, ticket field symmetry and explicit Material icon imports pass. Android SDK/Gradle are unavailable locally. No on-device speed or pixel-equivalence claim is made.

## Header and control alignment

Reserved full 48 dp touch targets for the route swap and library back buttons, centered the swap against the station outlines, and added explicit back-button/title spacing with wrapping for long titles. Lowered the booking filter contents by 6 dp while retaining navigation-bar insets and the original bar height. Removed filter ripples and ignored taps on the active filter. Static Kotlin, XML, serialization and workflow checks pass; the existing 82 Android tests are included but were not executed locally. Android compilation and on-device visual verification remain unverified because the Android SDK/Gradle/emulator are unavailable here.

## Stacked stations, capsule controls and grouped bookings

Stacked From/To fields with opposite-facing train icons and a vertical swap control. Class uses Second/First capsules; non-Ordinary types hide Return, with existing Journey/fare reset retained. Passenger badges are wide capsules and show the requested child-age text. Others uses five equal square tiles in a three-column arrangement; the booking train-type Others pill uses a 24 dp chevron. All bookings groups Upcoming then Completed and excludes Cancelled (which remains in its own tab). Reduced completed-ticket summary padding by 8 dp per edge and its first row gap by 4 dp. Added a grouping/cancellation navigation regression and updated existing Return/scroll assertions. Total: 83 Android tests included, not executed locally. Static parsing/XML/serialization checks pass. Android compilation and on-device visual behavior remain unverified here. Signing and stored-data formats are unchanged.
