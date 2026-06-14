# GymTracker — TODO

A running checklist of what's done and what's outstanding. Edit freely.

## Done
- [x] **Push 0** — Project setup (Compose, Room, KSP, AGP 9 built-in Kotlin)
- [x] **Data layer** — 5 entities, 3 DAOs, AppDatabase with seeded exercises
- [x] **ViewModels** — Home, Workout, Templates
- [x] **Screens + nav** — Home, Log, Templates, Progress, Settings (5 tabs)
- [x] **Push A** — `completed` flag on sets; edit/toggle/delete/finish plumbing
- [x] **Push B** — Inline set editing, per-exercise Add Set, completed checkmark,
  Add Exercise / Finish / Cancel, picker search, per-session rest stepper,
  lbs/kg (store kg, display converts)
- [x] **Push C** — Settings screen (kg/lbs, global default rest), Setup tab
- [x] **Push D** — Template editing: add/remove/drag-reorder exercises
- [x] **Push E1** — Template exercises carry target sets × reps
- [x] **Push E2** — Active-workout concept (persisted active session id);
  Home/Log restructure; starting a workout switches to Log; Home hides active session
- [x] **Push E3** — Start from template (Empty vs From Template); pre-fills sets/reps
- [x] **Push E4** — View past workouts read-only (SessionDetailScreen) + Repeat;
  real session names wired through (Log + detail)
- [x] **Rest timer placement** — renders under the exercise whose set was completed
- [x] **Bug fixes** — template reps pass through; duplicate-exercise guard;
  duration timer computed from session start (survives leaving screen / restart)
- [x] **Theme: foundation** — Iron & Chalk palette, light + dark schemes, type scale
- [x] **Theme: signature** — uppercase tracked titles (ScreenTitle), monospace data,
  PlateBadge component
- [x] **Home stats** — date + "Ready to train?", stat cards w/ icons (This Week,
  Week Streak, Total Volume), START WORKOUT, Recent PRs (weight PR + volume PR,
  plate badges), recent workouts. Weekly-goal setting added to Settings.
- [x] **Templates polish** — "NEW" pill button at top (was a FAB)
- [x] **Exercise picker polish** — sectioned muscle-group bands, cleaner rows
- [x] **Light/Dark toggle** — System / Light / Dark in Settings, persisted

## Outstanding
- [ ] **Exercise Library** — add / edit / archive custom exercises.
  Decisions made: soft-delete (archive, keep history) when logs exist; own tab.
  NOTE: revisit tab count — would make 6 tabs; maybe fold into Setup.
  Needs schema change (`archived` flag on Exercise) → one uninstall.
  *** This is the last real functional gap — currently locked to 21 seed exercises. ***
- [ ] **Progress / Stats charts** — per-exercise weight-over-time line chart.
  Stats tab is still a placeholder.
  TRAP: filter logs by exercise id with stable flatMapLatest + setCurrentX(),
  NOT a parameterized .stateIn() function (flicker bug).
- [ ] **Levels & Achievements (gamification)** — user profile with XP/levels and
  unlockable achievements. NEEDS A SCOPING PASS before building — open questions:
  - What grants XP? (workouts completed, volume lifted, streak weeks, PRs hit?)
  - Level curve (linear? escalating thresholds?)
  - Achievement set (first workout, 7-day streak, 100 workouts, bodyweight bench, etc.)
  - Where does the profile live? (new tab? inside Setup? top of Home?)
  - Schema: likely a new table for unlocked achievements + XP stored in prefs or DB.
  - Visual: badges, progress bar to next level — ties into the plate-badge motif.
- [ ] **UI friction fixes** — known so far:
  - "Stuttery sometimes" — undiagnosed. Need to pin down WHEN (typing? scrolling?
    rest timer running?) before fixing.
  - Start-Workout choice (Empty/From Template) is a cramped AlertDialog;
    would be better as a bottom sheet.

## Known traps / notes
- Flow recreation flicker: any "flow filtered by an id" must use a MutableStateFlow
  id + flatMapLatest in the ViewModel, set via setCurrentX() from a LaunchedEffect.
  Calling .stateIn() inside a function the composable invokes recreates the flow
  every recomposition → flicker. Bit us 3× (workout logs, template exercises).
- Schema changes have no migrations yet → uninstall/reinstall after any entity change.
- Weight: always stored in kg. Convert via formatWeight/displayWeight/toKg.
- AGP 9 bundles Kotlin; do NOT add the separate kotlin-android plugin.
- Elapsed workout time computed from WorkoutSession.date (start), not counted.
- Theme color roles cascade automatically; use ScreenTitle for headers,
  PlateBadge for weights/PRs.