# Startune Music Rewrite Inventory

## Purpose

This is the first execution document for the full rewrite.

It classifies the repo at a high level before we continue rewriting production code. The classification is intentionally practical:

- `keep`
- `improve`
- `split`
- `merge`
- `delete`

This inventory is not the final design. It is the working map for the rewrite.

## Classification Summary

### `src/app`

- `improve`

Reason:

- route ownership is mostly in the right place
- screens still need a consistency pass for loading, empty, error, and navigation behavior
- some screens still carry too much orchestration or UI-specific logic that should be normalized

### `src/components/blocks`

- `improve`
- `split`
- `delete`

Reason:

- many blocks are meaningful and should stay
- some still need compound-component cleanup
- some wrappers and aliases should be removed
- a few folder-level barrels are still present and should be deleted

### `src/components/patterns`

- `improve`

Reason:

- patterns are directionally correct
- they still need a consistency and prop-surface audit

### `src/components/ui`

- `improve`
- `merge`

Reason:

- some primitives are solid and should stay
- some UI building blocks overlap in purpose and should be normalized

### `src/components/providers`

- `improve`

Reason:

- provider boundaries are better than before
- bootstrap, database, and app-shell ownership still need simplification

### `src/modules`

- `improve`
- `split`
- `merge`
- `delete`

Reason:

- several modules already follow the new shape
- player, indexer, and settings still need deeper cleanup
- the next pass should be driven by the module boundary audit rather than ad hoc file selection

### Module boundary audit

- detailed audit document:
  - `docs/rewrite/module-boundary-audit.md`

- closest to target shape:
  - `favorites`
  - `genres`
  - `history`
  - `search`

- still need another cleanup pass:
  - `bootstrap`
  - `device`
  - `library`
  - `logging`
  - `lyrics`
  - `playlist`
  - `tracks`
  - `ui`

- still need the deepest rewrite work:
  - `player`
  - `indexer`
  - `settings`

### `src/core`

- `improve`

Reason:

- low-level integration boundaries exist
- runtime ownership and logging need a stricter pass

### `src/db`

- `improve`

Reason:

- the DB layer is centralized
- migrations and startup interactions still need a reliability and debuggability pass

### `src/utils`

- `improve`
- `merge`

Reason:

- several utilities are good candidates to keep
- duplicated intent still likely exists between `src/utils` and `src/modules`

### `android`

- `improve`

Reason:

- native setup is manageable
- custom modules, manifest settings, and Gradle setup need a consistency and diagnostics pass

## Immediate Structural Findings

### Removed barrel files

These were deleted as part of the first code cleanup slice:

- `src/components/blocks/player/index.tsx`
- `src/components/blocks/playlist-form/index.ts`

### Legacy-style file names already addressed

These were normalized in the rewrite:

- `src/modules/indexer/indexer.api.ts` -> removed
- `src/modules/indexer/metadata.api.ts` -> `src/modules/indexer/metadata.repository.ts`
- `src/modules/tracks/track-cleanup.api.ts` -> `src/modules/tracks/track-cleanup.repository.ts`

### Shared hook cleanup

- `src/hooks/use-theme-colors.ts`
  - moved to `src/modules/ui/theme.ts`
  - UI theme access now lives with the rest of the UI module boundary

- empty hook directories were removed:
  - `src/hooks`
  - `src/modules/bootstrap/hooks`
  - `src/modules/library/hooks`
  - `src/modules/playlist/hooks`

### Removed unused hooks

- `src/hooks/use-swipe-navigation.ts`

### Flattened hooks

- `src/modules/library/hooks/use-folder-browser.ts`
  - replaced by `src/modules/library/folder-browser.ts`
  - folder path state now lives directly in `src/app/(main)/(library)/index.tsx`
- `src/modules/playlist/hooks/use-playlist-form.ts`
  - replaced by screen-owned state in `src/app/(main)/(home,search,library)/playlist/form.tsx`
  - pure helper logic moved to `src/modules/playlist/playlist-form.ts`
- `src/modules/bootstrap/hooks/use-app-bootstrap.ts`
  - replaced by `src/modules/bootstrap/bootstrap.runtime.ts`
  - native lifecycle listeners now live in `src/components/providers/bootstrap-effects.tsx`

## Recommended Execution Order

### Slice 1

- [x] delete remaining barrel files
- [x] verify imports do not depend on them
- [x] keep behavior unchanged

### Slice 2

- [x] audit and rename or absorb remaining `*.api.ts` files
- [x] move metadata extraction ownership toward `repository.ts`
- [x] move track cleanup ownership toward `repository.ts`
- [x] remove the remaining compatibility re-export in `src/modules/indexer/indexer.api.ts`

### Slice 3

- [x] audit provider and bootstrap boundaries
- [x] reduce effect-driven bootstrap orchestration by moving it out of a screen-facing hook

### Slice 4

- continue player and indexer cleanup
- add missing structured logs in failure-prone paths

Progress:

- bootstrap, indexer, and player runtime boundaries now have first-pass structured logging
- favorites and playlist mutation hooks now log start, success, and failure paths
- library sort state, sort options, and pure sorting helpers are now split into separate files
- destructive track deletion and track file opening now log native and cleanup failures
- battery optimization helpers now log unsupported paths, requests, and failures
- track file opening now runs through `src/modules/device/file-viewer.ts` instead of living in `track-action-sheet.tsx`
- indexer config modules and `indexer.store.ts` no longer use `$...` compatibility helpers internally
- logging, UI, and library sort modules no longer use `$...` compatibility helpers internally
- player color state no longer uses `$...` compatibility helpers internally
- dead compatibility file `src/modules/player/player.computed.ts` was removed
- dead compatibility file `src/modules/player/player.queries.ts` was removed
- player state and queue helpers no longer expose `$...` compatibility wrappers internally
- `src/modules/player/queue.store.ts` no longer maintains the legacy `$queueInfo` export

### Slice 5

- do the shared component composition pass
- normalize loading, empty, and error states

## Rewrite Rule For Future Slices

Every slice should:

- improve architecture, not just local behavior
- reduce indirection where possible
- add logs where debugging is weak
- avoid adding new workaround code
- be small enough to commit clearly
