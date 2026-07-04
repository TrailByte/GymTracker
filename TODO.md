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
- [x] **Rest-timer alert — DONE.** Persistent notification with a system chronometer
  countdown (shows only during rest), tap opens the workout. At zero: custom sound
  (res/raw/rest_done.mp3) + double vibration. Final architecture (after a real debugging
  session): a SINGLE AlarmManager exact alarm scheduled for restEndsAt is the sole
  alerter (fires reliably foregrounded, minimized, or backgrounded); RestTimerService
  owns only the foreground notification lifecycle (hardened: START_NOT_STICKY always,
  idempotent teardown, no zombie restarts); the in-app pill is purely visual, computed
  from restEndsAt, no timer of its own. Dropped the originally-planned 3-2-1 buzz
  buildup to keep the alarm surface simple (one alarm, one job). Root cause of the
  trickiest bug: WorkoutScreen had a leftover LaunchedEffect that called skipRest()
  when the pill's countdown hit zero, which canceled the alarm a moment before it
  fired — only reproduced with the workout screen open (not minimized/backgrounded).
  Removed; the alarm/receiver now owns clearing rest state entirely.

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
- [x] #1  Reorder exercises within the workout screen (reuse Reorderable lib;
  needs an orderIndex on the in-workout exercises — currently order is just
  insertion order of logs).
- [x] #3  Rest duration picker — replaced steppers with an iPhone-style scroll
  wheel (minutes 0-10, seconds in 10s steps). DIY snap-scroll, no dependency.
- [ ] #6  Remove and edit past workouts (from the Log/SessionDetail screen).
- [~] #12 (PARKED — see Scale/perf; do with gamification DB) PR pill/badge shown in the Log (workout) screen when a set hits a PR.
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
## Field-testing feedback — round 3
- [x] #15 **CRASH (priority): deleting an exercise that's used in a plan crashes.**
  Cause: template_exercises has a FK to exercises with no onDelete rule, so deleting
  the exercise violates the constraint. Fix: either block delete / archive when it's
  in a template (like we do for logged history), or add ON DELETE CASCADE/SET behavior
  to template_exercises. Decide which. Schema-touching → migration (we now do migrations).
- [ ] #16 Keep an exercise's volume PR and show it on the exercise card as a
  target to beat. (Ties into the stored-PR-per-exercise work parked with gamification.)
- [x] #17 Log screen: in-progress workout shows BOTH the mini-bar (bottom) AND the
  resume card (top). Redundant — pick one. Likely keep the top resume card on the Log
  screen and suppress the mini-bar while actually on the Log tab (or vice versa).
- [x] #18 Allow an explicitly typed 0 weight (bodyweight exercises). Currently the
  complete-guard (#8) requires weight > 0. Need to distinguish "empty/not entered"
  from "explicitly 0". Bodyweight sets should be completable with 0 weight.
- [x] #19 Move the per-exercise rest timer into the exercise title box; keep it
  visible when the workout is minimized too. (Overlaps with #4 per-exercise timer.)

## Session additions (not from the original numbered list)
- [x] Reorder exercises in the active workout — DONE. New `session_exercise_order`
  table (version 3 migration), drag handle on each exercise card, reuses the
  Reorderable lib pattern from templates. Old workouts with no saved order fall
  back to "first logged" order (unchanged behavior).
- [x] Exercise order now respected everywhere, not just the live workout —
  extended the same saved-order + fallback logic to SessionDetailScreen (the
  finished-workout view) and the Log screen's history-card exercise list
  (LogViewModel's per-session stats). Required a new `getAllExerciseOrder()`
  query since the Log screen computes stats for every session at once.
- [x] Swapped the Reps/kg column order (kg now before Reps) — in the live workout
  screen's header + input fields, AND in the finished-workout detail view.
- [x] Exercise Library: added a search field (filters by name) and muscle-group
  filter chips (All + one per group), both fixed above the scrolling list, and
  combine together. Search/filter currently only apply to the ACTIVE list, not
  the Archived section below — revisit if that's wanted too.

## Known workflow gotcha (noted so it doesn't happen again)
- When multiple new files are added together with edits to existing files,
  double-check ALL files (not just edited ones) actually get committed —
  Android Studio can leave a genuinely NEW file unversioned/unpushed while the
  edited files go through fine. Bit us once with SessionExerciseOrder.kt
  (DAO/ViewModel referenced it, but the class file itself was never pushed,
  so the project silently couldn't have compiled).