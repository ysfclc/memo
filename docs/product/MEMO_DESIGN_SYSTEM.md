# Memo Design System — Signal & Graphite

**Status:** foundation v0.1 implemented in source; component migration and visual acceptance remain open  
**Date:** 9 November 2026  
**Android source:** `app/src/main/java/app/memo/android/ui/MemoTheme.kt`

## 1. Design intent

Memo should feel like one intelligent working environment: calm enough for long sessions, precise enough for consequential actions, tactile enough to feel native, and distinctive enough not to resemble a generic Material dashboard.

The design language is **Signal & Graphite**:
- **Graphite:** deep green-black surfaces in dark mode; warm mineral paper in light mode.
- **Signal Mint:** active accent for intent, focus, selected navigation and meaningful action—not decoration.
- **Violet:** reserved for AI/Council context; not a second general-purpose accent.
- Amber, red and blue are semantic signals only. Never use color alone to communicate state.

This is a custom product language implemented with Compose/Material 3 primitives, not a fork of Material components.

## 2. Visual principles

1. Quiet hierarchy: typography, spacing and alignment do most of the work.
2. One focal action per screen; secondary actions are contextual.
3. Real information density: useful content and evidence, never decorative metrics or filler.
4. Surface restraint: flat canvas, subtle separators and occasional raised surfaces. Do not wrap every row in a rounded card.
5. Tactile, not toy-like: generous touch targets and immediate, purposeful feedback.
6. Honest state: status, progress and completion reflect real evidence.
7. Adaptive by default: keyboard-safe, accessible, responsive, localized and respectful of reduced motion.

## 3. Foundation tokens

### Color

| Role | Light | Dark | Use |
|---|---|---|---|
| Canvas | `#F4F6F2` | `#0B1210` | App background |
| Surface | `#FCFDFC` | `#121B18` | Primary content surface |
| Surface variant | `#E8EDE8` | `#202C27` | Grouped/secondary surface |
| Primary / Signal | `#087A63` | `#72E2BE` | Active intent, selected controls |
| Primary container | `#D4F1E6` | `#075B49` | Selected/contained emphasis |
| Text | `#17201D` | `#E5EEE9` | Primary reading |
| Secondary text | `#46534D` | `#B6C5BD` | Supporting information |
| Violet | `#6262A7` | `#BDBBFF` | AI/Council context only |

Semantic success/warning/info/error roles use dedicated foreground/container pairs. Color never carries meaning without a label, icon or explicit state.

### Typography

Use the system sans family initially to avoid font-download, privacy and performance dependencies.
- Display: 30–34sp semibold, tight tracking.
- Screen headline: 28sp semibold.
- Section headline: 20–24sp semibold.
- Titles: 14–18sp medium/semibold.
- Body: 14–16sp regular with 21–24sp line height.
- Metadata: 12sp; 11sp labels only for compact controls, never essential reading.

Sentence case is the default. Avoid long all-caps headings.

### Spacing and geometry

- 4dp base rhythm; common scale 8/12/16/20/24/32/48dp.
- 20dp page margin on compact phones, responsive adjustment on wider windows.
- Corner radii: 6 / 10 / 14 / 20 / 26dp. Pills are reserved for compact filters and status chips.
- Minimum interactive target: 48dp.
- Elevation is mostly flat; 2dp raised, 6dp floating, 12dp modal. Prefer tonal/border separation over shadows.

### Motion

- 90ms instant, 150ms quick, 220ms standard, 320ms expressive.
- Motion communicates cause/effect and continuity; never delays an action.
- Respect Android animator scale/reduced-motion preference.
- Avoid perpetual pulsing, gratuitous parallax and ambient glow loops.

## 4. Component system

Build and migrate reusable components in this order:

1. **App shell:** adaptive navigation, contextual header, system status and stop affordance.
2. **Typography/layout:** screen header, section header, content column, divider, empty/loading/error/offline/recovery states.
3. **Actions:** primary, secondary, tertiary/text, destructive, icon-only with descriptions, split/overflow menu.
4. **Inputs:** composer, search, text field, attachment strip, voice capture, filter chips and selection controls.
5. **Data display:** entity row, conversation row, priority row, feed item, inbox thread, execution event, evidence/source citation, artifact preview.
6. **State communication:** status label, progress, approval checkpoint, permission explanation, uncertain outcome, connection health.
7. **Overlays:** bottom sheet, dialog, contextual menu, confirmation and undo.

Components consume theme tokens, expose semantics, support loading/disabled/error states and have light/dark previews. Avoid abstractions that do not enforce a real interaction or accessibility contract.

## 5. Navigation expression

The proposed primary destinations remain **Talk / Social / Live / Work / Studio**. Until Social has real connected-account/feed/inbox functionality, do not expose a dead tab.

On phones, the four currently implemented primary destinations use a persistent bottom bar: Talk, Live, Work and Studio. The selected destination uses a restrained Signal Mint capsule and stronger label weight; a subtle divider separates navigation from content. Supporting destinations remain secondary. Social is a fifth product destination in the target architecture, but it must not appear until connected-account, feed and inbox behavior is real. On expanded screens, evaluate a navigation rail as a separate adaptive-shell step rather than forcing a phone bar onto tablets.

The header becomes context-aware instead of repeating a generic Memo bar. Lock/privacy state and stop-active-work remain globally reachable without competing with the screen title.

## 6. Domain expression

- **Talk:** conversation-first, quiet canvas, composer anchored to the task, voice/capture integrated into one intent surface. No dashboard metrics.
- **Social:** editorial feed and inbox, source/account identity always visible, clear unread and relationship context, media gets room. No fabricated analytics.
- **Live:** chronological execution timeline, strong state labels, stop/pause/takeover affordances and evidence at the point of action.
- **Work:** Today priorities and linked records, explicit MUST / SHOULD / COULD / NOT TODAY hierarchy.
- **Studio:** focus mode for creation, persistent draft/revision state, preview and export as first-class actions.

## 7. Interaction and accessibility contracts

- Meaningful labels/content descriptions, 48dp targets, keyboard/focus behavior, visible disabled/loading states.
- Destructive/external actions require contextual confirmation and exact-scope approval where applicable.
- Drafts autosave or explicitly report failure; never clear optimistically before durable persistence.
- Empty states explain why content is absent and provide one relevant next action.
- Error states explain whether data was saved, whether an external action may have occurred, and how to recover.
- Contrast is tested in both themes; status is communicated by text/icon as well as color.
- English, Polish and Turkish strings must fit without clipping. Do not hardcode widths around English copy.

## 8. Implementation and acceptance gates

### Foundation (implemented in source; build not yet verified)
- Signal & Graphite light/dark palette.
- Semantic success/warning/info roles.
- Spacing, radius, elevation and motion token groups.
- Typography and shape scale.
- Primary navbar selection colors use the semantic accent.
- Reusable `MemoPageHeader`, `MemoSectionHeading`, `MemoPanel` and `MemoStatusTag` primitives are in `MemoPrimitives.kt`.
- Talk and Work use the shared page-header primitive. Talk's intent composer, recent-conversation section and conversation rows now use shared section/panel primitives.

### Next
- [~] Build shared Compose primitives: page/section headers, panels and semantic status tags exist; action, input, data-display, overlay and adaptive-shell components remain.
- [~] Migrate Talk first; the page header, intent composer, recent-conversation heading, conversation rows, active-run panel and message bubbles use shared primitives. `MemoMessageBubble` provides role-aware alignment, restrained asymmetric geometry, selectable content and an action slot; existing edit/delete behavior is preserved. Conversation controls, search, model/privacy controls and composer behavior still need component-level design and accessibility review.
- [ ] Apply the system to Live, Work and Studio, then Social when real behavior is ready.
- [ ] Replace repeated one-off colors/radii/spacing where safe; retain semantic exceptions.
- [ ] Add previews for light/dark, small phone, large phone and tablet/window.
- [ ] Add screenshot/golden tests for key journeys and accessibility checks.
- [ ] Verify Polish/Turkish strings, large font scale, keyboard insets, edge-to-edge and reduced motion on a physical Android device.

**Do not claim the design system is complete because the token file exists.** Completion requires reusable components, screen migration, visual review and device verification. Preserve existing navigation, encrypted storage, app identity, signing and migrations throughout.
