# Walkthroughs & product tours — API gaps

Where Vaadin's API made the walkthroughs of this module awkward. Vaadin has
no tour component, so every use case builds on `Popover`.

## 1. No tour component

**Where it bites:** every use case / `tour/Tour.java`, `tour/TourPopover.java`, `tour/TourCard.java`
**Symptom:** a tour needs steps in order, "Step 2 of 5", Back, Next and Skip,
a highlight on the current target, and an event that says whether the user
finished or skipped it. `Popover` gives one floating panel next to one
target. It also opens on click, hover and focus of its target by default,
which a tour has to turn off.
**Workaround used:** a `Tour` class that moves one `TourPopover` from step to
step and keeps its state in a signal; a `TourCard` for the step's content.
**Suggested API:** a tour component, e.g.
`new Tour(TourStep.of(target, title, text), …)` with `start()`,
`startAt(int)`, `next()`, `back()`, `skip()`, a `Signal` of its state, and
finish and skip listeners. Steps that wait for an action instead of "Next"
(UC3) and an id with a hook to remember that a user has seen the tour (UC1)
belong with it.

## 2. No spotlight around the target

**Where it bites:** UC6 / `SpotlightView.java`, `tour/TourPopover.java`
**Symptom:** `Popover#setModal(true, true)` dims the whole page, the target
included, so it can not point at something by dimming the rest.
**Workaround used:** a modal popover without a backdrop, and a large
`box-shadow` on the target (`.tour-spotlight` in `styles.css`). The shadow is
cut off at the edge of the nearest scrolling container, so the top bar stays
undimmed, and in the drawer only the drawer is dimmed.
**Suggested API:** a backdrop that leaves the target uncovered, e.g.
`Popover#setBackdropCutout(boolean)` or a spotlight mode of the tour
component.

## 3. Hidden targets are not revealed

**Where it bites:** UC2 / `ComplexViewTourView.java`, UC6 / `SpotlightView.java`
**Symptom:** a step can point at a field in a closed `Details`, a button on a
`TabSheet` tab that is not selected, the side navigation in a closed
drawer, or a button below the fold. Nothing makes the target visible first:
a popover whose target has no size closes again (#6), and one whose target is
below the fold points off screen.
**Workaround used:** a `beforeShow` action per step that opens the panel,
selects the tab, or opens the drawer (and closes it again on the next step).
For every step, `tour-popover.ts` scrolls a target that is not fully on
screen to the middle of the screen before it opens the popover.
**Suggested API:** a tour step that brings its target on screen: scrolls it
into view and, for the standard containers (`Details`, `TabSheet`,
`AppLayout` drawer), opens what hides it.

## 4. A tour can not span routes

**Where it bites:** UC4 / `CrossViewTour.java`, `CrossViewPage.java`
**Symptom:** a popover's target is a component instance, but the components
of the next route do not exist until the user navigates there. The tour state
also has to outlive the view.
**Workaround used:** the tour's state is kept with the `UI`
(`ComponentUtil.setData`); each step names its route class and a key; each
page registers its targets by key and shows the current step when it is
attached.
**Suggested API:** tour steps that name a route and a target on it (for
example by id), with the tour scoped to the `UI` and navigating by itself.

## 5. Local storage can not be read in browserless tests

**Where it bites:** UC1 / `AppTourView.java`, `AppTourViewTest.java`
**Symptom:** the version whose tour this browser saw last lives in local
storage. `WebStorage#getItem(String, Callback)` answers after a round trip, so
the view can only decide in the callback, and the browserless tests never get
an answer: the per-browser path is checked only in a browser.
**Workaround used:** the view shows "Asking the browser…" until the callback
runs; the test only checks that the tour does not open before the answer.
**Suggested API:** a browserless simulator for `WebStorage`.

## 6. A popover opened while its view is attached closes again

**Where it bites:** every tour / `tour/TourPopover.java`, `src/main/frontend/tour-popover.ts`
**Symptom:** the popover overlay closes itself when it opens while its target
has no size. While Flow attaches a view, the view's container is hidden, so
`Popover#open()` in the same round trip — a tour that starts when the page
opens (UC1, UC2) — closes at once. The same happens for a target in
a closed `Details`. The server then gets a close from the browser that it can
not tell apart from the user pressing Escape, so the tour counted it as
skipped.
**Workaround used:** `TourPopover` asks the browser to open the popover once
the target has a size (`tour-popover.ts`), and keeps its own "showing" flag on
the server. The browserless tests check that flag, since they run no
browser.
**Suggested API:** a `Popover` that waits until its target is laid out, or
opens again when the target becomes visible, instead of closing; or a close
event that says why the popover closed.

## 7. A popover squeezes its content when its position has no room

**Where it bites:** UC1 / `AppTourView.java`, UC6 / `SpotlightView.java`
**Symptom:** on a phone, a popover at `PopoverPosition.BOTTOM_END` of a target
near the left edge, or at `END` of the full-width menu drawer, gets only the
space on that side. Its content then overflows, and the step's buttons are cut
off. The popover does not switch to a position that has room.
**Workaround used:** the steps open above or below their target (`TOP`,
`BOTTOM`), and the step card wraps its buttons and is never wider than the
screen.
**Suggested API:** a popover that moves to another position, or at least keeps
to the screen width, when the requested position has no room.
