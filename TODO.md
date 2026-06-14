# GymTracker — TODO

A running checklist of what's done and what's outstanding. Edit freely.

## Done
- [x] **Push 0** — Project setup (Compose, Room, KSP, AGP 9 built-in Kotlin)
- [x] **Data layer** — 5 entities, 3 DAOs, AppDatabase with seeded exercises
- [x] **ViewModels** — Home, Workout, Templates
- [x] **Screens + nav** — Home, Workout, Templates, Progress (placeholder)
- [x] **Push A** — `completed` flag on sets; edit/toggle/delete/finish plumbing
- [x] **Push B** — Inline set editing, per-exercise Add Set, completed checkmark,
  Add Exercise / Finish / Cancel buttons, exercise picker search,
  per-session rest stepper, lbs/kg (store kg, display converts)
- [x] **Push C** — Settings screen (kg/lbs toggle, global default rest), Setup tab
- [x] **Push D** — Template editing: open template, add/remove/drag-reorder exercises
- [x] **Push E1** — Template exercises carry target sets × reps
- [x] **Push E2** — Active-workout concept (persisted active session id);
  Home/Log restructure; 5 tabs (Home, Log, Plans, Stats, Setup);
  starting a workout switches to Log tab; Home history hides active session
- [x] **Push E3** — Start from template: Empty vs From Template choice on Home;
  template's exercises + target sets/reps pre-fill the new workout
- [x] **Rest timer placement** — renders under the exercise whose set was completed
- [x] **Bug fixes** — template reps now pass into workout; duplicate-exercise guard
  in templates; duration timer computed from session start time (no longer stops
  when leaving the Log screen, survives app restart)

## Outstanding
- [ ] **Push E4** — View past workouts read-only (SessionDetailScreen) + Repeat
  (create fresh active session copying a past one's exercises).
  NOTE: Home history rows (onOpenSession) currently no-op — wire them here.
- [ ] **Exercise Library** — add / edit / archive custom exercises.
  Decisions made: soft-delete (archive, keep history) when logs exist; own tab.
  NOTE: revisit tab count — would make 6 tabs; maybe fold into Setup.
  Needs schema change (`archived` flag on Exercise) → one uninstall.
- [ ] **Iron & Chalk theme** — real colors + typography from the mockup
  (charcoal bg, chalk text, iron-red accent, plate-badge PRs, uppercase headings).
  Also enables the light/dark toggle in Settings (deferred until theme exists).
  Also: Start-Workout choice would be better as a bottom sheet than an AlertDialog.
- [ ] **Home stats** — "Ready to train?" header, This Week / Week Streak / Total,
  Recent PRs with plate badges. (Week streak needs the weekly-goal setting wired.)
- [ ] **Progress / Stats charts** — per-exercise weight-over-time line chart.
  TRAP: filtering logs by exercise id must use the stable flatMapLatest +
  setCurrentX() pattern, NOT a parameterized .stateIn() function (flicker bug).
- [ ] **UI friction fixes** — TO BE ENUMERATED. Known so far:
    - "Stuttery sometimes" — undiagnosed. Need to pin down WHEN (typing? scrolling?
      rest timer running?) before fixing.

## Known traps / notes
- Flow recreation flicker: any "flow filtered by an id" must use a MutableStateFlow
  id + flatMapLatest in the ViewModel, set via a `setCurrentX()` called from a
  LaunchedEffect. Calling `.stateIn()` inside a function the composable invokes
  recreates the flow every recomposition → flicker. Bit us 3× (workout logs,
  template exercises, and would hit Progress charts).
- Schema changes have no migrations yet → uninstall/reinstall the app after any
  entity change. Fine while there's no real data to lose.
- Weight: always stored in kg. Display/input converts via formatWeight/displayWeight/toKg.
- AGP 9 bundles Kotlin; do NOT add the separate kotlin-android plugin.
- Elapsed workout time is computed from WorkoutSession.date (start), not counted.