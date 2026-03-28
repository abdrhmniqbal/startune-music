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
- [ ] Prefer `uniwind` first, then inline styles only when needed
- [ ] Prefer native navigation state over custom navigation-history workarounds
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
- [~] `favorites`, `genres`, `history`, and `search` are closest to the target module shape
- [~] `library`, `playlist`, `tracks`, `device`, `logging`, `bootstrap`, `lyrics`, and `ui` need another cleanup pass
- [ ] `player`, `indexer`, and `settings` still need the deepest boundary rewrite work

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
- keep styling consistent with the rest of the app
- do not split components unless the split creates a stable boundary
- do not keep giant files just because they are already working

### Navigation and Screen Flow

- keep Expo Router
- reduce custom navigation workarounds
- make route transitions consistent
- keep sheets and overlays out of route semantics unless they are truly screens
- use `react-native-screen-transitions` only when the dependency situation is stable enough to adopt it safely

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

- [ ] create a full inventory of:
  - providers
  - stores
  - services
  - repositories
  - remaining compatibility layers
  - `useEffect` usage
  - logging coverage
  - performance hotspots
- [ ] classify every file as:
  - keep
  - improve
  - split
  - merge
  - delete
- [ ] define the final store boundaries
- [ ] define logging coverage expectations before implementation starts

### Phase 1: Foundation and Providers

- [ ] flatten provider composition
- [ ] ensure bootstrap, database, and app-shell ownership are explicit
- [ ] reduce provider-level effects and derived state
- [ ] ensure global error and logging initialization are reliable

### Phase 2: Core Runtime Domains

- [ ] finish player architecture cleanup
- [ ] finish indexer architecture cleanup
- [ ] finalize settings persistence ownership
- [ ] remove compatibility APIs that still imitate older patterns

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

## Immediate Next Step

- [ ] perform the Phase 0 inventory and classify the remaining files before writing more production code
