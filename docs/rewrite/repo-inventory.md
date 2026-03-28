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
- player, indexer, settings, and some compatibility layers still need deeper cleanup
- a few legacy naming choices such as `*.api.ts` still exist and should be removed or renamed

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

### Remaining barrel files

These should be deleted in the next rewrite slice:

- `src/components/blocks/player/index.tsx`
- `src/components/blocks/playlist-form/index.ts`

### Remaining legacy-style file names

These should be reviewed and likely renamed or absorbed:

- `src/modules/indexer/indexer.api.ts`
- `src/modules/indexer/metadata.api.ts`
- `src/modules/tracks/track-cleanup.api.ts`

### Remaining reusable hooks that need review

These are not screen hooks anymore, but still need to be justified:

- `src/modules/bootstrap/hooks/use-app-bootstrap.ts`
- `src/modules/library/hooks/use-folder-browser.ts`
- `src/modules/playlist/hooks/use-playlist-form.ts`
- `src/hooks/use-swipe-navigation.ts`
- `src/hooks/use-theme-colors.ts`

## Recommended Execution Order

### Slice 1

- delete remaining barrel files
- fix imports
- keep behavior unchanged

### Slice 2

- audit and rename or absorb remaining `*.api.ts` files
- move them toward `repository.ts`, `service.ts`, or `utils.ts`

### Slice 3

- audit provider and bootstrap boundaries
- reduce effect-driven orchestration further

### Slice 4

- continue player and indexer cleanup
- add missing structured logs in failure-prone paths

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
