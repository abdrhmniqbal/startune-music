# Startune Music Module Boundary Audit

## Purpose

This audit checks the module boundaries we already refactored against the updated rewrite requirements.

It is intentionally strict. A module can be:

- `aligned`: matches the new boundary rules closely enough to keep moving
- `partial`: direction is correct, but important cleanup is still needed
- `rewrite`: current boundary still conflicts with the new architecture

## Audit Rules

A module is considered `aligned` only if it mostly satisfies these:

- responsibilities are clearly separated
- React Query hooks stay in `queries.ts` and `mutations.ts`
- database or native IO stays in `repository.ts` or `service.ts`
- no compatibility-only files remain
- no screen-wrapper hooks remain
- logging exists where failures would otherwise be hard to trace
- state ownership matches the rewrite plan

## Current Status

### `albums`

- status: `aligned`
- notes:
  - kept intentionally small
  - only owns album-specific pure helpers
  - query ownership is correctly handled elsewhere

### `artists`

- status: `aligned`
- notes:
  - only owns artist-specific pure helpers
  - no fake query wrapper remains

### `bootstrap`

- status: `partial`
- notes:
  - runtime ownership is clearer after moving orchestration out of screen hooks
  - still depends on provider-side `useEffect` boundaries
  - local settings config is now preloaded during bootstrap, which reduces mount-time effects in settings screens
  - listener orchestration now lives in `bootstrap-listeners.service.ts` instead of being duplicated inside the provider effect component
  - should be revisited together with provider simplification

### `providers`

- status: `partial`
- notes:
  - `DatabaseProvider` is leaner now and reports ready/error from the actual async startup path
  - database startup loading now lives in `database-startup.service.ts` instead of an inline provider async block
  - provider/runtime effects still deserve another simplification pass later

### `device`

- status: `partial`
- notes:
  - native bridges are now correctly moving under `src/modules/device`
  - battery and file-viewer flows now log failures
  - more device and intent-style actions should follow this pattern

### `favorites`

- status: `aligned`
- notes:
  - repository, queries, mutations, keys, and types are clearly separated
  - mutation logging is now present

### `genres`

- status: `aligned`
- notes:
  - repository/query/type split is good
  - no obvious compatibility leftovers remain

### `history`

- status: `aligned`
- notes:
  - history ownership now sits in its own module instead of player compatibility layers
  - playback-driven history cache updates now live in `history-cache.service.ts` instead of the player module

### `indexer`

- status: `rewrite`
- notes:
  - store is thinner now, but the module still carries heavy runtime scheduling and refresh responsibilities overall
  - auto-scan, track-duration, and folder-filter config ownership now live under `src/modules/settings`, which removes the settings-like preference files from the indexer module
  - more repository/service separation is still needed
  - logging is better, but the boundary is still too heavy
  - runtime controls are now separated into `indexer.service.ts`, which is a step toward a thinner state-only store
  - post-scan media reload and query invalidation now live in `indexer-refresh.service.ts`
  - abort token, queued-run, and completion-timeout coordination now live in `indexer-runtime.ts`
  - progress mapping and terminal state transitions now live in `indexer-progress.service.ts`

### `library`

- status: `partial`
- notes:
  - query and repository split is good
  - sorting is now split into state, constants, types, and pure utilities
  - folder browser is reasonable, but library state is still spread across screen + sort store

### `logging`

- status: `partial`
- notes:
  - config state now lives in `logging.store.ts`
  - runtime logging, file persistence, and crash-log sharing now live in `logging.service.ts`
  - this is materially better, but global error and console interception still make the module runtime-heavy

### `lyrics`

- status: `partial`
- notes:
  - file and parsing ownership is local and understandable
  - still needs a follow-up performance and API-boundary pass because it is tied closely to player timing behavior

### `player`

- status: `rewrite`
- notes:
  - much better than before, but still the heaviest module
  - state is split across `player.store.ts`, `queue.store.ts`, and `player-colors.store.ts`
  - runtime orchestration is improved, but the module still owns too many responsibilities
  - one compatibility file was removed in this audit pass: `src/modules/player/player.queries.ts`
  - playback commands are no longer re-exported through `player.store.ts`
  - queue runtime commands are now separated into `queue.service.ts` instead of living beside derived queue state
  - artwork color extraction and cache logic are now separated into `player-colors.service.ts`
  - player session persistence now lives in `player-session.repository.ts`
  - TrackPlayer mapping and repeat-mode adapter helpers now live in `player-adapter.ts`
  - session persistence/restoration now live in `player-session.service.ts`
  - active-track and playback-progress sync helpers now live in `player-runtime-state.ts`
  - TrackPlayer event wiring now lives in `player-events.service.ts`
  - track-activation side effects now live in `player-activity.service.ts`
  - playback transport controls now live in `player-controls.service.ts`
  - player library loading now lives in `player-library.service.ts`
  - local favorite-toggle helper now lives in `player-favorites.service.ts`
  - playback-driven history cache invalidation no longer lives in the player module

### `playlist`

- status: `partial`
- notes:
  - repository/query/mutation split is good
  - `playlist-form.ts` is acceptable as a screen helper, but still worth checking against final screen-ownership rules later

### `search`

- status: `aligned`
- notes:
  - query ownership is now in the module rather than spread across screens

### `tracks`

- status: `partial`
- notes:
  - repository/query/mutation split is good
  - cleanup and metadata responsibilities still deserve a second pass to make sure intent is not overlapping

### `ui`

- status: `partial`
- notes:
  - UI store and theme helper are in the right module now
  - scroll-driven tab/player visibility behavior still needs a design-level check against native UX and performance requirements

### `settings`

- status: `partial`
- notes:
  - `useSettingsStore` now exists for local preference state
  - settings ownership is much more coherent now, even though some actions still execute through feature modules
  - local settings persistence now lives in `settings.repository.ts`
  - auto-scan and track-duration preference ownership now live under `src/modules/settings`
  - folder-filter config helpers now live under `src/modules/settings/folder-filters.ts`
  - route metadata now lives in `src/modules/settings/settings.routes.ts`
  - shared settings row UI now lives in `src/components/patterns/settings-row.tsx`
  - settings screens now read local preferences directly from `useSettingsStore`
  - folder filter state now lives in `useSettingsStore` with the other local preferences
  - library, logging, and folder-filter settings screens no longer need mount-time config-loading effects

## Immediate Conclusions

### Boundaries that are in good shape

- `albums`
- `artists`
- `favorites`
- `genres`
- `history`
- `search`

### Boundaries that are directionally correct but still need another pass

- `bootstrap`
- `device`
- `library`
- `logging`
- `lyrics`
- `playlist`
- `settings`
- `tracks`
- `ui`

### Boundaries that still need deeper rewrite work

- `indexer`
- `player`

## Next Priority After This Audit

1. Revisit `player`
2. Revisit `indexer`
3. Simplify provider/runtime effects after the heavy modules are cleaner
4. Revisit `settings` for final cleanup once player/indexer settle down
