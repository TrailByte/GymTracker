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
## Field-testing feedback
Grouped by size. Order TBD.

### Quick fixes
- [x] #2  "Remove" text breaks layout when exercise name is long (Row weighting).
- [x] #14 Viewing a past workout shows "Workout" instead of the real name
  (SessionDetailScreen still passes a hardcoded title — load real name).
- [x] #7  Completed sets: highlight the whole row, not just the check.
- [x] #8  Block marking a set complete if reps OR weight is empty/zero.
- [x] #11 Confirmation dialog before removing an exercise or a workout plan.
- [x] #10 Keyboard / FAB hides the input field at bottom of screen
  (imePadding + ensure focused field scrolls into view).

### Medium
- [ ] #1  Reorder exercises within the workout screen (reuse Reorderable lib;
  needs an orderIndex on the in-workout exercises — currently order is just
  insertion order of logs).
- [ ] #3  Rest timer increments: add +30, and allow typing the value manually.
- [ ] #6  Remove and edit past workouts (from the Log/SessionDetail screen).
- [ ] #12 PR pill/badge shown in the Log (workout) screen when a set hits a PR.
- [x] #13 Workout history card stats: per-exercise sets, total volume, duration.
- [ ] #9  Scroll lag in the workout screen (THE "stuttery" item — needs profiling;
  likely the inline OutlinedTextFields recomposing, or unstable list keys).

### Bigger / needs scoping
- [ ] #4  Different rest timer PER EXERCISE (decision: per-exercise override stored
  where — on the template exercise? on the in-workout exercise? both?).
- [ ] #5  Exercise TYPES — some exercises track reps + duration (planks, holds) or
  duration only, not reps + weight. BIG: needs an exercise "type" field, different
  input UI per type, and type-aware display/PRs/volume. Almost its own mini-project.

## Smaller follow-ups (noted)
- [ ] Delete-set discoverability: long-press to delete works but isn't self-
  explanatory. Consider a hint (drag handle / one-time tip) or a visible delete
  affordance.
- [ ] Set renumbering: deleting a middle set leaves stored setNumbers with gaps
  (e.g. 1, 3) until next add. Renumber on delete if it bothers.
- [ ] Log card: in-card scroll for long exercise lists is a nested-scroll combo;
  revisit vs "+N more" if it feels fiddly in use.
## Scale / performance
- [ ] **Log screen shouldn't load ALL logs via getAllLogs().** At years of data this
  pulls everything into memory. Infinite scroll itself is FINE (LazyColumn already
  renders only visible cards) — the issue is querying everything at once. Fix:
  paginate the history query (load a page, fetch more on scroll) and/or precompute
  per-session stats. Keep the infinite-scroll feel; just don't load the whole table.
- [ ] **PR detection should be a stored per-exercise best, not a live backlog scan.**
  Computing PRs against an unbounded history doesn't scale. Add a stored "personal
  best" per exercise (best weight, best volume — likely a new column/table), updated
  when a set is completed. PR check becomes O(1): is this set > stored best.
  - Schema addition justified here (caching a derived fact to avoid unbounded compute).
  - Caveat: stored bests can drift if past sets are edited/deleted → need to handle
    recompute-on-edit, or accept minor drift.
  - The current #12 live computation is fine for now at small data sizes; this is
    the "before it's a real backlog" upgrade.