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
- [x] **Bug fixes** — template reps pass into workout; duplicate-exercise guard
  in templates; duration timer computed from session start time (no longer stops
  when leaving Log screen, survives app restart)
- [x] **Theme: foundation** — Iron & Chalk palette (Color.kt), light + dark schemes
  following system setting (Theme.kt), type scale (Type.kt). MainActivity uses
  GymTrackerTheme. Every screen recolors via Material color roles.
- [x] **Theme: signature** — uppercase tracked screen titles (ScreenTitle component),
  monospace on timers/data, PlateBadge component built (Components.kt / PlateBadge.kt).
- [x] **Push E4** — View past workouts read-only (SessionDetailScreen) + Repeat
   (create fresh active session copying a past one's exercises).
- [x] **Home stats** — "Ready to train?" header, This Week / Week Streak / Total cards,
  Recent PRs with plate badges (PlateBadge component is ready, not yet used anywhere).
  This is the landing screen + where the signature finally appears in context.
  (Week streak needs a weekly-goal setting wired; can stub it first.)

## Outstanding

- [ ] **Progress / Stats charts** — per-exercise weight-over-time line chart.
  Stats tab is still a placeholder.
  TRAP: filtering logs by exercise id must use the stable flatMapLatest +
  setCurrentX() pattern, NOT a parameterized .stateIn() function (flicker bug).

  NOTE: Home history rows (onOpenSession) currently no-op — wire them here.
- [ ] **Exercise Library** — add / edit / archive custom exercises.
  Decisions made: soft-delete (archive, keep history) when logs exist; own tab.
  NOTE: revisit tab count — would make 6 tabs; maybe fold into Setup.
  Needs schema change (`archived` flag on Exercise) → one uninstall.
- [ ] **UI friction fixes** — TO BE ENUMERATED. Known so far:
    - "Stuttery sometimes" — undiagnosed. Need to pin down WHEN (typing? scrolling?
      rest timer running?) before fixing.
    - Start-Workout choice (Empty/From Template) is a cramped AlertDialog;
      would be better as a bottom sheet.
- [ ] **Light/dark toggle in Settings** — currently follows system only. The manual
  toggle was deferred until the theme existed; theme now exists, so this can be wired
  (needs a theme-mode preference + plumbing GymTrackerTheme's darkTheme param).

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
- Theme color roles cascade automatically; new screens that use MaterialTheme.colorScheme
  roles get Iron & Chalk for free. Use ScreenTitle for headers, PlateBadge for weights/PRs.