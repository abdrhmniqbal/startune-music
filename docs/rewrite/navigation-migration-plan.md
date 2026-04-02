# Startune Music Navigation Migration Plan

## Goal

Rewrite navigation to prefer native navigation behavior wherever it makes sense, while using `react-native-screen-transitions` to make detail transitions feel smoother and more intentional.

This migration should optimize for:

- native-feeling back behavior
- consistent route ownership
- smooth but restrained transitions
- less duplicated stack configuration
- fewer ad hoc navigation patterns inside components

## Current Shape

### Root

- `src/app/_layout.tsx`
  - root stack
  - `(main)` tabs
  - `settings` modal stack
  - persistent overlays:
    - `IndexingProgress`
    - `PlayerSheet`

### Main tabs

- `src/app/(main)/_layout.tsx`
  - Expo Router tabs
  - custom tab bar wrapper with mini player

### Per-tab stacks

- `src/app/(main)/(home)/_layout.tsx`
- `src/app/(main)/(search)/_layout.tsx`
- `src/app/(main)/(library)/_layout.tsx`

These currently repeat a lot of the same stack configuration and route wiring.

### Shared detail stacks

- `src/app/(main)/(home,search,library)/album/_layout.tsx`
- `src/app/(main)/(home,search,library)/artist/_layout.tsx`
- `src/app/(main)/(home,search,library)/playlist/_layout.tsx`

These are the right general route groups, but they should stay native-first until custom transitions can be added without breaking header visibility or back behavior.

## Migration Rules

### 1. Prefer native stack navigation for actual pages

Use native navigation for:

- album details
- artist details
- playlist details
- search pages
- genre drill-down pages
- settings and settings detail pages
- library detail drill-down pages

Do not keep custom in-component navigation state when route state can express the same flow.

### 2. Keep overlays as overlays when they are not true pages

Keep these out of route navigation unless there is a strong product reason to promote them into routes:

- full player
- action sheets
- sort sheets
- picker sheets

These are transient surfaces, not document-like destinations.

### 3. Transition choices should match context

Not every route should zoom.

Use transitions by route type:

- tabs:
  - no content animation
  - prioritize stability and responsiveness

- list -> detail:
  - prefer native stack transitions first
  - only reintroduce `react-native-screen-transitions` after the routed-header path is proven stable
  - prefer zoom for media-driven destinations where the source is visually obvious:
    - album
    - artist
    - playlist
  - only use zoom when we have a clear source element and native header behavior remains correct

- text/list drill-down pages:
  - use push-style transitions that feel native and low-friction:
    - search -> genre
    - genre -> top tracks
    - genre -> albums
    - home -> top tracks
    - home -> recently played

- modal flows:
  - use bottom-up presentation where the user is entering a temporary task:
    - settings root
    - playlist form if kept route-based

- settings detail pages:
  - use standard push transitions inside the settings stack

### 4. Avoid transition noise

Do not add animations just because we can.

Avoid:

- animating bottom-tab content switches
- mixing multiple unrelated transition styles in the same stack
- zoom transitions for routes with no stable visual source
- route transitions fighting with bottom-sheet animations

## Target Route Classes

### Class A: Main shell

- `(main)` tabs
- behavior:
  - stable
  - no visual flourish
  - should never blank, hitch, or detach unexpectedly

### Class B: Media detail routes

- album
- artist
- playlist

Target:

- native stack routes
- zoom transition where a source card/list row exists
- fallback to standard push if the route is opened from a context with no visual source

### Class C: Drill-down utility routes

- search
- genre detail
- top tracks
- recently played
- genre albums
- genre top tracks

Target:

- native push transition
- fast and understated

### Class D: Modal task routes

- settings root modal
- possibly playlist form if we keep it as a route

Target:

- vertical modal presentation
- clear exit affordance

### Class E: Overlay runtime surfaces

- player sheet
- action sheets
- picker sheets

Target:

- not part of route stack by default
- keep as runtime overlays

## Implementation Order

### Slice 1: Define shared stack presets

Create one small navigation module for shared stack option presets.

It should cover:

- root modal preset
- standard push preset
- media detail preset

This should replace repeated `headerStyle`, `contentStyle`, back-button, and animation config across layout files.

Status:

- completed with `src/modules/navigation/stack.tsx`
- home, search, library, and settings now share the same native stack preset helpers
- album, artist, and playlist detail groups also use the same shared native helpers after backing out the broken transition-stack integration

### Slice 2: Normalize route ownership

Audit all `router.push(...)` targets and classify them:

- actual route navigation
- overlay open/close
- temporary task flow

Then remove any remaining custom navigation behavior that duplicates route state.

Status:

- started
- shared helpers now exist for:
  - drill-down push routes
  - hidden nested routes
  - root modal route presentation
  - media detail and modal task routes that now stay on Expo Router native `Stack`

### Slice 3: Rework the main tab shell

Keep the current stability fix:

- `detachInactiveScreens={false}`
- `freezeOnBlur: false`
- `animation: "none"`

Treat this as a hard stability baseline unless profiling proves otherwise.

### Slice 4: Reintroduce media detail transitions carefully

Add `react-native-screen-transitions` only after routed headers remain stable, and only for:

- album detail entry
- artist detail entry
- playlist detail entry

Start with one route type first, verify behavior, then expand.

Status:

- paused after the first integration attempt caused:
  - unhandled `GO_BACK` actions
  - missing headers
  - broken title positioning
  - TypeScript integration issues in the shared transition stack
- current safe baseline:
  - album
  - artist
  - playlist
  use Expo Router native `Stack` with shared native screen options
- revisit only after we have a header-safe transition path

## Stability Rules

- routed screens must keep native back semantics
- header visibility regressions are release blockers
- title alignment regressions are release blockers
- if a transition package fights Expo Router stack behavior, keep the route native and defer the visual transition

### Slice 5: Normalize settings flow

Use a single modal entry at root:

- `/settings`

Then use standard native push transitions within the settings stack.

### Slice 6: Clean up route files and wrappers

Remove layout duplication and any wrapper routes/components that only exist to carry repeated stack config.

## Route-Specific Recommendations

### Home / Search / Library tab roots

- keep current shell stable
- no tab-content transition

### Album detail

- best candidate for zoom transition
- source surfaces:
  - album grid
  - album cards
  - search album row

### Artist detail

- good zoom candidate when opened from:
  - artist grid
  - artist search row
- fallback to standard push when entered from text links

### Playlist detail

- zoom candidate when entered from playlist artwork/list rows
- standard push fallback from places like notification or action menus

### Search page

- standard push
- should feel immediate, not theatrical

### Settings

- root settings should remain modal from the main shell
- settings subpages should push inside the settings stack

### Full player

- keep as bottom sheet for now
- do not move into route navigation during the navigation rewrite
- revisit only if there is a product reason, not just for symmetry

## Success Criteria

- tab switching remains stable under rapid interaction
- route transitions feel intentional, not random
- album/artist/playlist detail entry feels smoother
- back behavior is native and predictable
- repeated stack config is reduced
- route ownership is easier to trace from source component to destination
