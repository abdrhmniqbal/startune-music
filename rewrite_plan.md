# Startune Music Full Rewrite Plan

## Status Legend

- `[x]` Completed and verified in the repo
- `[~]` Partially completed or started
- `[ ]` Not started yet

## Purpose

This is now treated as a full rewrite, not a local refactor.

The goal is to make the app:

- faster and more native-feeling
- easier to reason about
- easier to debug
- more consistent in UI, state ownership, and module boundaries
- closer to current React Native, Expo, React Query, Zustand, and HeroUI Native best practices

This plan applies to the whole repo. Every file is in scope.

## Merged Baseline (Single Source of Truth)

This document now merges and supersedes planning status previously tracked across:

- `docs/rewrite/repo-inventory.md`
- `docs/rewrite/module-boundary-audit.md`
- `docs/rewrite/navigation-migration-plan.md`

Those documents remain useful as detailed history, but execution should now follow this file as the primary rewrite plan.

## Core Rule For Every File

For every file we touch, we will ask:

1. Does this file still deserve to exist?
2. Does it have a clear single responsibility?
3. Is its state ownership correct?
4. Is it using the simplest correct pattern?
5. Is it causing unnecessary rerenders, duplicated work, or hidden side effects?
6. Is it easy to debug when something fails?
7. Does it match the visual and architectural direction of the rest of the app?

If the answer is yes, we keep it.

If the answer is no, we do one of these:

- rewrite it
- split it
- merge it
- move it
- delete it

We do not keep code just because it already works.

## Rewrite Principles

- [x] Keep Expo Router as the routing foundation
- [x] Replace Nanostores with Zustand
- [x] Remove barrel exports and use direct alias imports
- [x] Remove screen-specific orchestration hooks and move logic into screens or reusable module helpers
- [ ] Use React Query as the only UI-facing fetched and mutated data layer
- [ ] Converge on a small set of clear Zustand stores only
- [ ] Use HeroUI Native consistently as the base UI layer
- [ ] Prefer compound components over prop-heavy escape-hatch APIs
- [ ] Prefer compound composition over complicated monolithic components
- [ ] Prefer `uniwind` first, then inline styles only when needed
- [ ] Prefer native navigation state over custom navigation-history workarounds
- [ ] Rewrite navigation to use native navigation wherever it is the better fit
- [~] Use `react-native-screen-transitions` selectively for detail routes that benefit from visual continuity, but only after native headers and back behavior remain stable
- [ ] Avoid `useEffect` unless the behavior is materially worse or impossible without it
- [ ] Add structured logging anywhere failure, async work, native bridging, or state transitions can become hard to debug
- [ ] Treat performance as a product requirement, not a cleanup task

## Rewrite Constraints

- [ ] No new barrel exports
- [ ] No new Nanostore-like compatibility APIs
- [ ] No direct database access from screens or presentational components
- [ ] No query result mirroring into global stores unless it is required for native integration or local-first UX
- [ ] No new screen wrapper hooks
- [ ] No new `useEffect` without a clear written reason
- [ ] No new “god components” with large prop matrices and many `className` overrides
- [ ] No hidden cache invalidation side effects inside read helpers

## Boundary Audit

- [x] Initial module boundary audit completed
- [x] `favorites`, `genres`, `history`, and `search` are aligned with the target module shape
- [~] `library`, `playlist`, `tracks`, `device`, `logging`, `bootstrap`, `lyrics`, `ui`, and `settings` need another cleanup pass
- [x] `player` and `indexer` no longer need a shrinking-first rewrite pass
- [~] `player` is improving, and queue runtime commands are now split from queue state surfaces
- [~] `settings` is improving, and folder filter state now lives in `useSettingsStore` with the other local preferences

Module status reference:

- `aligned`: `albums`, `artists`, `favorites`, `genres`, `history`, `search`
- `partial`: `bootstrap`, `device`, `library`, `logging`, `lyrics`, `player`, `playlist`, `providers`, `settings`, `tracks`, `ui`, `navigation`, `indexer`
- `rewrite`: none currently

## Keep / Rewrite Decision Matrix

### Keep as-is

Keep the file if all of these are true:

- responsibility is clear
- side effects are minimal and expected
- performance is acceptable
- naming and location make sense
- logging is either not needed or already adequate
- the code matches the new architecture

### Improve in place

Improve the file in place if:

- the boundary is correct
- the implementation is noisy, repetitive, or slightly inefficient
- the fix is local and does not justify moving ownership

### Split

Split the file if:

- it mixes UI, data access, mutations, and native side effects
- it owns multiple unrelated responsibilities
- it is too long because it hides multiple stable subparts

### Merge

Merge files if:

- they are split without real ownership boundaries
- the indirection makes the feature harder to trace
- the files mostly pass data through without adding value

### Delete

Delete the file if:

- it exists only for compatibility with a removed pattern
- it is a wrapper with no stable architectural purpose
- it duplicates an intent already handled elsewhere

## Target Architecture

### State Ownership

We should converge on only these store classes:

- [~] `usePlayerStore`
- [~] `useUIStore`
- [ ] `useSettingsStore`
- [~] `useIndexerStore`

Rules:

- React Query owns fetched and mutated domain data
- Zustand owns local session state, UI state, and native-runtime state
- repositories own database and native IO
- services own imperative runtime integration
- presentational components should not know how persistence works
- compatibility-style `$...` exports should keep shrinking until they are gone
- player state helpers should use explicit getters and setters instead of compatibility-shaped wrapper objects
- shared UI theme helpers should live under `src/modules/ui`, not `src/hooks`
- UI stores should avoid internal pass-through setter wrappers when a direct state write is clearer
- player runtime commands should be imported from `player.service.ts`, not re-exported through `player.store.ts`
- queue mutations should be imported from `queue.service.ts`, while queue-derived view state should stay close to the UI that uses it
- after shuffle, skip, and other native queue mutations, the native TrackPlayer queue should be treated as the source of truth for queue order and active-track sync
- player artwork color extraction and caching should live in `player-colors.service.ts`, while `player-colors.store.ts` stays state-only
- player session persistence should live in `player-session.repository.ts`
- TrackPlayer mapping and repeat-mode adapter helpers should live outside `player.service.ts`
- queue runtime should reuse shared TrackPlayer mapping helpers instead of duplicating them locally
- player session lifecycle should live in `player-session.service.ts`, and runtime state sync helpers should live outside `player.service.ts`
- TrackPlayer event wiring should live outside `player.service.ts`
- playback transport controls should live outside `player.service.ts`
- player library loading and favorite-toggle helpers should live outside `player.service.ts`
- playback-driven history cache updates should live under `src/modules/history`, not `src/modules/player`
- indexer runtime controls should live in `indexer.service.ts`, while `indexer.store.ts` stays focused on indexer state
- post-scan media reload and query invalidation should live outside `indexer.service.ts`
- one-off indexer query invalidation helpers should live with `indexer-refresh.service.ts`, not in a separate compatibility file
- indexer run coordination state should live outside `indexer.service.ts`
- indexer progress mapping and terminal state transitions should live outside `indexer.service.ts`
- logging config state should live in `logging.store.ts`, while runtime logging, file persistence, and crash sharing should live in `logging.service.ts`
- settings route metadata and reusable settings UI patterns should live under `src/modules/settings` and shared components, instead of being duplicated across screens
- repeated settings header config should be rendered from one shared list instead of repeated `Stack.Screen` blocks
- small screen-specific sort-field mapping can stay local when that is clearer than adding another shared helper file
- library tab query field mapping should stay explicit and typed instead of relying on `as any`
- player-facing UI should avoid subscribing to playback progress unless the current mode actually needs it
- player-facing list rows should prefer subscribing to `currentTrack?.id` instead of the full track object when they only need active-state styling
- screen-level list wiring should use shared local props and narrow store selectors when that reduces repeated render setup without hiding ownership
- virtualized list tuning should use shared defaults for pool ratio and item estimates instead of drifting list by list without review
- local settings config should be preloaded during bootstrap so settings screens do not each need mount-time config effects
- provider startup flows should notify readiness and failure from the actual async path, not from extra watcher effects
- bootstrap listener registration should live in the root provider layer without single-use wrapper components
- provider startup loading should live in runtime services instead of inline async blocks inside provider components
- bootstrap listener registration should live outside `bootstrap.runtime.ts`
- local preference state should converge into `useSettingsStore` instead of being spread across separate per-setting Zustand stores
- settings screens should read local preferences from `useSettingsStore` directly instead of going through per-setting selector wrappers
- settings modules should prefer shared settings state accessors over per-field getter/setter wrappers
- local settings persistence should converge into `settings.repository.ts` instead of being split across feature modules
- folder filter state and folder-filter config helpers should live under `src/modules/settings`
- auto scan and track-duration preference ownership should live under `src/modules/settings`, even when indexer consumes the resulting config

### Module Shape

Target shape for each feature where applicable:

- `repository.ts`
- `queries.ts`
- `mutations.ts`
- `types.ts`
- `store.ts`
- `service.ts`
- `utils.ts`

Rules:

- `repository.ts` is for database or native IO only
- `queries.ts` is for React Query hooks only
- `mutations.ts` is for React Query mutation hooks only
- `store.ts` exists only if the feature truly needs local runtime state
- `service.ts` exists only for imperative native/runtime orchestration
- `utils.ts` is pure and side-effect free
- library sorting should follow the same split: store state in `library-sort.store.ts`, static options in `library-sort.constants.ts`, and pure comparators in `library-sort.utils.ts`
- native file viewing and other bridge-heavy actions should live under `src/modules/device`, not inside UI components

### Components

Target component rules:

- use HeroUI Native primitives first
- build local compounds on top when needed
- avoid boolean-heavy reusable APIs
- prefer compound composition instead of complicated all-in-one components
- keep styling consistent with the rest of the app
- do not split components unless the split creates a stable boundary
- do not keep giant files just because they are already working
- prefer one render path with small conditional sections over duplicating the same large list/tree structure for empty and non-empty states
- extract a tiny shared hook when the same UI-side behavior is duplicated across several components and the hook is simpler than the duplication
- prefer small local helper functions when they remove repetitive section-building logic inside one shared block
- shared presentational helpers can expose tiny related utilities when multiple callers are repeating the same prop-shaping logic
- delete wrapper components when they only forward props to a single shared block without adding stable behavior

### Navigation and Screen Flow

- keep Expo Router
- reduce custom navigation workarounds
- make route transitions consistent
- keep sheets and overlays out of route semantics unless they are truly screens
- use `react-native-screen-transitions` only when the dependency situation is stable enough to adopt it safely
- keep routed layouts on Expo Router native `Stack` until any custom transition layer can preserve header visibility, title alignment, and back semantics
- apply zoom transitions only to media-detail routes with a clear visual source
- use simpler native slide transitions for utility drill-down screens
- keep tab switching animation-free unless profiling shows a safe native alternative
- tab-root navigation can use the built-in bottom-tab `shift` animation when it improves feel without reintroducing blank-scene issues
- keep detail-route shared zoom transitions deferred until they can coexist with native headers and back behavior
- centralize repeated stack configuration in shared navigation presets before changing individual route transitions
- encode route classes as shared screen-option helpers instead of re-declaring animation and visibility rules in each layout
- shared route chrome, such as repeated stack header action buttons, should live in small reusable components instead of being hand-built in each layout

See also:

- `docs/rewrite/navigation-migration-plan.md`

## Logging Plan

Logging is part of the rewrite, not an optional add-on.

### Logging goals

- [ ] make user issues easier to reproduce
- [ ] make native/runtime failures easier to diagnose
- [ ] make async workflow timing easier to inspect
- [ ] avoid logging noise that hides real issues

### Logging levels

- `minimal`
  - critical failures
  - caught exceptions
  - database migration failures
  - indexing failures
  - playback failures
  - native bridge failures
- `extra`
  - screen entry points
  - important mutations
  - query failures and retries
  - indexing lifecycle
  - playback lifecycle
  - background resume and restoration

### Where logging must exist

- [ ] app bootstrap
- [ ] database open and migration flow
- [ ] media-library permission and change listeners
- [ ] indexer start, skip, reindex, delete cleanup, completion, and cancellation
- [ ] metadata extraction failures and malformed-file handling
- [ ] file-system reads for lyrics, settings, and session persistence
- [~] file opening and file-path resolution now log native failures
- [ ] playback lifecycle:
  - setup
  - queue replacement
  - track activation
  - seek
  - next and previous
  - resume restoration
  - notification and remote-control entry points
- [~] favorites and playlist mutations
- [ ] destructive actions such as track deletion
- [~] destructive track deletion now logs permission, native deletion, and cleanup failures
- [ ] route-level failures or invalid params where the user can get stuck

### Logging rules

- [ ] no sensitive user data in logs
- [ ] no spam logging on every render
- [ ] no logging inside hot render loops unless behind development-only guards
- [ ] log transitions and failures, not noise
- [ ] each important async workflow should have a clear start, success, and failure log path

## Performance Plan

Performance is a first-class rewrite track.

### Performance rules

- [ ] treat each global subscription as a cost
- [ ] use primitive Zustand selectors where possible
- [ ] prefer render-time derivation over mirrored state
- [ ] keep high-frequency playback updates isolated from large UI trees
- [ ] keep list rows cheap and stable
- [ ] avoid unnecessary React state when data can be derived
- [ ] audit expensive screens with many nested closures and inline objects
- [ ] remove workaround layers that hide repeated work

### Performance hotspots to audit

- [ ] app bootstrap and first render
- [ ] library tabs and pull-to-refresh
- [ ] search typing and result switching
- [ ] album, artist, genre, and playlist detail screens
- [ ] full player, queue, lyrics, and action sheets
- [ ] indexing and metadata extraction
- [ ] notification tap entry points

## Repo-Wide Execution Order

### Phase 0: Inventory and Rules

- [x] create a full inventory of:
  - providers
  - stores
  - services
  - repositories
  - remaining compatibility layers
  - `useEffect` usage
  - logging coverage
  - performance hotspots
- [x] classify every file as:
  - keep
  - improve
  - split
  - merge
  - delete
- [~] define the final store boundaries
- [~] define logging coverage expectations before implementation starts

### Phase 1: Foundation and Providers

- [~] flatten provider composition
- [~] ensure bootstrap, database, and app-shell ownership are explicit
- [~] reduce provider-level effects and derived state
- [ ] ensure global error and logging initialization are reliable

### Phase 2: Core Runtime Domains

- [~] finish player architecture cleanup
- [~] finish indexer architecture cleanup
- [~] finalize settings persistence ownership
- [~] remove compatibility APIs that still imitate older patterns

### Phase 3: Feature Modules

- [ ] audit every module under `src/modules`
- [ ] finish missing `repository` / `queries` / `mutations` / `service` boundaries
- [ ] remove duplicated intent between modules and `src/utils`
- [ ] standardize query keys and invalidation ownership

### Phase 4: Screens and Components

- [ ] audit every screen under `src/app`
- [ ] audit every shared component under `src/components`
- [ ] rewrite prop-heavy reusable components into better compounds where justified
- [ ] remove dead wrappers and pass-through layers
- [ ] unify loading, empty, error, and destructive-action patterns

### Phase 5: Logging and Diagnostics

- [ ] add missing logs in all important workflows
- [ ] standardize log messages and scopes
- [ ] ensure the app can export useful logs for debugging
- [ ] verify extra logging does not create hot-path performance regressions

### Phase 6: Performance Pass

- [ ] measure and reduce rerenders on hot screens
- [ ] reduce expensive derived work in render
- [ ] isolate high-frequency state updates from large trees
- [ ] simplify async chains that make the app feel network-bound even though it is local-first

### Phase 7: Final Consistency Pass

- [ ] unify naming
- [ ] unify folder structure
- [ ] unify component conventions
- [ ] unify navigation and transition behavior
- [ ] unify logging style
- [ ] remove remaining dead code and compatibility shims

## Area Checklists

### `src/app`

- [ ] every screen owns only screen concerns
- [ ] route params are validated
- [ ] navigation is direct and unsurprising
- [ ] loading, empty, and error states are consistent
- [ ] no screen contains hidden domain orchestration that belongs in modules

### `src/components`

- [ ] reusable components have clear boundaries
- [ ] no large prop soup APIs without strong justification
- [ ] compounds are used when they improve readability and flexibility
- [ ] hot-path components are cheap to render
- [ ] presentational components do not directly touch repositories

### `src/modules`

- [ ] each module has a clear ownership boundary
- [ ] no mixed read + mutation + invalidation helpers unless truly justified
- [ ] no duplicated types or utils across modules
- [ ] services own imperative runtime behavior
- [ ] stores own only local runtime state

### `src/core`

- [ ] low-level native integrations are thin and predictable
- [ ] side effects are isolated
- [ ] logging exists around failure-prone native bridges

### `src/db`

- [ ] migrations are deterministic
- [ ] schema ownership is clear
- [ ] client setup is reliable and debuggable
- [ ] database errors are logged clearly

### `src/utils`

- [ ] utilities are pure unless there is a strong reason otherwise
- [ ] no domain ownership leaks from modules into utils
- [ ] duplicate helpers are merged or deleted

### `android`

- [ ] native config is modern and intentional
- [ ] custom modules and manifest entries are documented by use
- [ ] logging exists where native bridge behavior is hard to inspect from JS

## Already Achieved

- [x] Nanostores were removed from `src/`
- [x] barrel exports were removed from `src/modules`, `src/components`, and `src/hooks`
- [x] leftover component barrel files were removed from:
  - `src/components/blocks/player/index.tsx`
  - `src/components/blocks/playlist-form/index.ts`
- [x] screen-specific orchestration hooks were removed
- [x] several modules were already split into clearer domain layers
- [x] player runtime has already started moving out of the store and into service boundaries
- [x] global UI state now lives in `src/modules/ui/ui.store.ts`
- [x] legacy repository-style `*.api.ts` files started being normalized:
  - `src/modules/indexer/metadata.api.ts` -> `src/modules/indexer/metadata.repository.ts`
  - `src/modules/tracks/track-cleanup.api.ts` -> `src/modules/tracks/track-cleanup.repository.ts`
- [x] unused legacy hook removed:
  - `src/hooks/use-swipe-navigation.ts`
- [x] reusable folder-browser hook flattened:
  - `src/modules/library/hooks/use-folder-browser.ts` was replaced by `src/modules/library/folder-browser.ts`
  - folder navigation state now lives directly in `src/app/(main)/(library)/index.tsx`
- [x] playlist form screen hook flattened:
  - `src/modules/playlist/hooks/use-playlist-form.ts` was removed
  - playlist form screen state now lives directly in `src/app/(main)/(home,search,library)/playlist/form.tsx`
  - pure helper logic now lives in `src/modules/playlist/playlist-form.ts`
- [x] bootstrap hook flattened into runtime and provider boundaries:
  - `src/modules/bootstrap/hooks/use-app-bootstrap.ts` was removed
  - bootstrap orchestration now lives in `src/modules/bootstrap/bootstrap.runtime.ts`
  - AppState and MediaLibrary listeners now live in `src/components/providers/bootstrap-effects.tsx`
  - `src/components/providers/database-provider.tsx` now logs migration completion, cached track loading, and provider failures
- [~] structured logging expanded in core runtime flows:
  - bootstrap now logs startup phases, permission resolution, and scheduled auto-index runs
  - indexer now logs run start, queueing, completion, stop, resume, and fatal failures
  - player runtime now logs setup, session restore, transport commands, and failure paths
  - indexer config and state modules no longer use `$...` compatibility exports internally
  - logging config no longer uses `$...` compatibility exports internally
- [~] compatibility-style store shims continue shrinking:
  - indexer config and state modules no longer expose `$...` wrappers
  - UI store no longer exposes `$...` wrappers
  - library sort store no longer exposes `$...` wrappers
  - player color state no longer exposes `$...` wrappers
  - dead computed compatibility file `src/modules/player/player.computed.ts` was removed
- [~] shared library tab state normalization started:
  - `src/components/blocks/library-tab-state.tsx` now centralizes loading/empty/content gating for library tabs
  - `src/components/blocks/albums-tab.tsx`, `artists-tab.tsx`, and `tracks-tab.tsx` now use one shared render-path boundary for loading and empty states
- [~] search and playlist high-traffic list surfaces now use narrower render-time work:
  - `src/components/blocks/search-results.tsx` now memoizes section list data and stabilizes key callbacks for list rendering
  - `src/components/blocks/search-results.tsx` now composes result-row rendering through a dedicated subcomponent instead of one monolithic switch-heavy render block
  - `src/components/blocks/recent-searches.tsx` now composes row rendering through a dedicated row subcomponent instead of inline wrapper-heavy row markup
  - `src/components/blocks/playlist-list.tsx` now memoizes row data, empty footer state, and row render callbacks
- [~] route-level invalid-param diagnostics expanded:
  - `src/app/(main)/(home,search,library)/album/[name].tsx`, `artist/[name].tsx`, `playlist/[id].tsx`, and `src/app/(main)/(search)/genre/[name].tsx` now log missing or decode-failed route params
- [~] player and indexer lifecycle logging expanded:
  - `src/modules/player/player-controls.service.ts` now logs start/success/failure paths for transport and repeat-mode workflows
  - `src/modules/player/queue.service.ts` now logs queue mutation lifecycle paths (add/queue-next/remove/clear) with rollback-context on failures
  - `src/modules/indexer/indexer-progress.service.ts` now logs begin/complete/fail/hide transition points outside the hot progress-update path
- [~] logging output signal quality improved:
  - `src/modules/logging/logging.service.ts` now avoids forwarding undefined context/error arguments to console output, removing noisy `... undefined` suffixes in runtime logs
- [~] bootstrap/database/media-listener logging closure advanced:
  - `src/modules/bootstrap/bootstrap-listeners.service.ts` now logs listener registration/unregistration and auto-scan trigger entry points
  - `src/components/providers/database-provider.tsx` now logs migration wait, startup loading, ready, and cancellation states
  - `src/modules/bootstrap/database-startup.service.ts` now logs startup load failure paths and rethrows with context
  - `src/core/storage/media-library.service.ts` now logs permission read/request lifecycle and failure paths
- [~] hot-screen store subscription narrowing pass completed:
  - home/search/genre and settings list screens now subscribe to primitive `isIndexing` selectors instead of full `indexerState` objects
  - `src/app/(main)/(home)/index.tsx` now subscribes to `currentTrack?.id` for active-row styling instead of the full track object
- [~] playback deletion edge-case handling improved:
  - `src/modules/player/queue.service.ts` now handles removing the currently active track by rebuilding native queue state safely, selecting a fallback active track, and keeping playback/session state in sync
- [~] library tracks refresh UX bug fixed:
  - `src/components/blocks/track-list.tsx` now renders the empty state through `LegendList` `ListEmptyComponent` instead of short-circuiting the list render, so pull-to-refresh remains available even when the track list is empty
  - `src/components/blocks/track-list.tsx` now uses a dedicated `TrackListItem` composed row and stabilized callbacks instead of a monolithic inline render block
- [~] favorites list render-path normalization advanced:
  - `src/components/blocks/favorites-list.tsx` now keeps one `LegendList` render path for empty and non-empty states via `ListEmptyComponent`, so list behaviors (including refresh wiring) stay consistent
  - favorites row handlers and item rendering are now callback-stabilized to reduce avoidable list rerender churn on high-traffic library surfaces
- [~] artist artwork source consistency improved:
  - `src/components/blocks/artists-tab.tsx` now prioritizes `trackArtwork` before `artist.artwork`, aligning artist-list artwork selection with artist detail screen behavior
  - `src/modules/library/library.repository.ts` search artist mapping and `src/modules/favorites/favorites.repository.ts` favorite artist mapping now use the same track-first artwork fallback order
  - shared fallback logic now lives in `src/modules/artists/artist-artwork.ts` and is reused by artist tab, search, and favorites mappings
- [~] player mini-surface composition improved:
  - `src/components/blocks/mini-player.tsx` now composes artwork, metadata, and controls through dedicated subcomponents instead of one monolithic component body

These are now treated as groundwork, not the finish line.

## Acceptance Criteria For The Rewrite

- [ ] every file has been consciously kept, improved, split, merged, moved, or deleted
- [ ] no major workflow depends on unclear ownership
- [ ] logging is sufficient to diagnose common user-facing issues
- [ ] performance is materially better on library, search, player, and detail flows
- [ ] architecture is consistent enough that new features do not need workaround patterns
- [ ] the app feels cleaner, more native, and easier to maintain

## Execution Rule

We do not execute this as a big-bang rewrite.

We execute it in slices, but every slice must satisfy these rules:

- leave the touched area cleaner than before
- improve consistency, not just local behavior
- add logging where missing
- reduce side effects where possible
- reduce performance cost where visible
- avoid introducing new workaround code

## Active Next Steps (Priority Order)

The rewrite is in progress and not yet complete. Execute the next slices in this order:

### Next Slice A: Shared component composition + state normalization

- [x] normalize loading, empty, and error states for high-traffic blocks (`library`, `search`, `playlist`, `player` surfaces)
- [x] remove remaining pass-through wrappers and prop-heavy escape hatches in `src/components/blocks` and `src/components/patterns`
- [x] enforce one render path per list block (conditional sections, no duplicated full trees)

### Next Slice B: Logging closure for failure-prone workflows

- [ ] complete start/success/failure logs for bootstrap, DB startup, media permission/listeners, player transport, and indexer lifecycle
- [ ] add route-level invalid-param and dead-end logging where navigation can trap the user
- [ ] verify logging level gating (`minimal` vs `extra`) in hot paths

### Next Slice C: Performance closure on hot screens

- [ ] audit and reduce broad Zustand subscriptions on library/search/player surfaces
- [ ] verify list row memoization and stable callbacks across detail and results screens
- [ ] isolate playback-progress subscriptions to only views that truly need high-frequency updates

### Next Slice D: Provider/runtime simplification final pass

- [ ] collapse remaining provider effects that mirror runtime state
- [ ] ensure provider startup and failure signaling only comes from real async boundaries
- [ ] finalize global error pipeline initialization and crash-log export reliability

## Definition of Done For The Next 3 Slices

- [ ] each touched area has explicit ownership (`repository`/`queries`/`mutations`/`service`/`store`) with no compatibility shim reintroduced
- [ ] each changed async workflow has structured start/success/failure logs
- [ ] each changed list or hot-path view shows reduced unnecessary rerenders versus baseline
- [ ] no added `useEffect` without a short in-file reason and no new screen wrapper hooks

## Immediate Next Step

- [x] execute Next Slice A on `src/components/blocks` and `src/components/patterns`, then update this file with module-by-module completion notes
