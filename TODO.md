# GymTracker — TODO

A running checklist of what's done and what's outstanding. Edit freely.

## Done
- [x] Project setup, data layer (entities, DAOs, seeded exercises)
- [x] Workout logging: inline set editing, per-exercise Add Set, completed
  checkmark, Add Exercise / Finish / Cancel, picker search (sectioned),
  rest timer under the right exercise (per-session + global default)
- [x] Weight units: stored in kg, display/input converts (lbs/kg toggle)
- [x] Templates: create, drag-reorder, target sets × reps, duplicate guard
- [x] Active-workout model: persisted active session id; start opens the live
  workout as a pushed route
- [x] Start from template (Empty vs From Template), pre-fills sets/reps
- [x] Theme: Iron & Chalk palette, light/dark + System toggle, uppercase
  titles, monospace data, PlateBadge
- [x] Home: stats (This Week, Week Streak, Total Volume) + PRs (weight + volume,
  plate badges) + START WORKOUT. Weekly-goal setting.
- [x] Profile hub (5th tab): Settings + Exercise Library sub-screens
- [x] Exercise Library: add / edit / archive (soft-delete) / restore
- [x] Log tab = workout history; active workout as a resume card; Home no longer
  owns history
- [x] Minimized-workout mini-bar above the tabs (name + live timer), down-arrow
  to minimize; name + timer also in the workout top bar
- [x] Timer is timestamp-based — correct after minimize / background / app kill

## Outstanding
- [ ] **Rest-timer alert (NEEDED)** — when the rest countdown hits zero, the user
  is usually NOT looking at the screen. Needs at minimum a sound + vibration when
  rest ends. Right now nothing fires if you've navigated away or backgrounded.
  - Simplest: play a sound + vibrate when the in-app countdown reaches 0
    (works while app is foregrounded).
  - Fuller: a notification that fires even if the app is backgrounded/closed —
    requires scheduling (WorkManager/AlarmManager or a foreground Service) +
    POST_NOTIFICATIONS permission (Android 13+). Bigger chunk.
  - Decision needed: in-app-only sound first, or go straight to notifications?
- [ ] **Background timer / notification (future)** — a live workout/rest timer in
  the notification shade while the app is backgrounded, like dedicated timer apps.
  Needs a foreground Service + notifications. Larger feature; only if wanted.
- [ ] **Progress / Stats charts** — per-exercise weight-over-time line chart.
  Stats tab is still a placeholder.
  TRAP: filter logs by exercise id with stable flatMapLatest + setCurrentX().
- [ ] **Levels & Achievements (gamification)** — slots into the Profile hub header.
  NEEDS A SCOPING PASS — what grants XP, level curve, achievement set, schema,
  visuals (ties into plate-badge motif).
- [ ] **UI friction**:
  - "Stuttery sometimes" — undiagnosed; pin down WHEN before fixing.
  - Start-Workout choice (Empty/From Template) cramped AlertDialog → bottom sheet.
  - Settings & Library "Back" text button → proper back arrow icon.

## Known traps / notes
- Flow filtered by an id → MutableStateFlow id + flatMapLatest, set via
  setCurrentX() from a LaunchedEffect. NOT a parameterized .stateIn() (flicker).
- Schema changes have no migrations → uninstall/reinstall after any entity change.
- Weight always stored in kg. Convert via formatWeight/displayWeight/toKg.
- AGP 9 bundles Kotlin; do NOT add the separate kotlin-android plugin.
- Elapsed time computed from WorkoutSession.date (start), not counted.
- Use ScreenTitle for headers, PlateBadge for weights/PRs.
- Big pasted files can truncate or mis-merge (duplicate/clobbered function
  headers → cascading "unresolved reference"). For big files, replace the whole
  file from a download rather than pasting into the middle.

## Branding / polish (added)
- [ ] **App name → "Forj"** — change app_name in strings.xml (done / in progress).
- [ ] **Custom app icon** — replace the default Android robot launcher icon.
  Use Android Studio's Image Asset Studio (right-click res → New → Image Asset)
  to generate all densities + adaptive icon from a single source image.
- [ ] **Wordmark inside the app** — show "Forj" somewhere in-app (Profile hub header
  or a small Home wordmark). Optional polish.