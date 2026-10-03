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
