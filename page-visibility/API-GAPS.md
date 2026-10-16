# Page Visibility API — API gaps

Where `Page#pageVisibilitySignal()` made a use case in this module awkward.

## 1. No browserless simulator for the page visibility

**Where it bites:** every test of the module / `PageVisibilityTestSupport.java`
**Symptom:** the browserless tests can not move a tab between `VISIBLE`,
`VISIBLE_NOT_FOCUSED` and `HIDDEN`. `Page#setPageVisibility(String)`, which
updates the signal, is package-private because only the client bridge calls
it, and browserless-test has no tester for it.
**Workaround used:** `PageVisibilityTestSupport.setPageVisibility` calls the
package-private setter through reflection.
**Suggested API:** a browserless helper such as
`setPageVisibility(PageVisibility)` in `BrowserlessTest`.

## 2. No time of the last visibility change

**Where it bites:** UC4 / `RefreshStaleDataView.java`
**Symptom:** the signal holds only the current state. To refresh data after
the tab was hidden for 5 seconds or more, the view has to remember when the
tab became hidden and handle every way back itself — including a return to a
window that is visible but not focused.
**Workaround used:** a `hiddenAt` field that a `Signal.effect` sets on
`HIDDEN` and clears on the next visible state.
**Suggested API:** the time of the last change next to the state, e.g. a
signal of `record PageVisibilityState(PageVisibility visibility, Instant since)`.
