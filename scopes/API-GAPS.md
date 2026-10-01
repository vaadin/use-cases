# API gaps — scopes

Places where the APIs used by this module were missing or awkward.

## No Spring scope for `BrowserTab`

**Where it bit us:** uc6 / `BookingDraft.java`, uc7 / `TicketWorkspace.java`, uc8 / `ScopeCounters.java`
**Symptom:** vaadin/flow#25901 adds `BrowserTab` with a programmatic attribute
map (`getAttribute` / `setAttribute` / `addDestroyListener`), but there is no
matching bean scope. Every other Vaadin lifetime (`@VaadinSessionScope`,
`@UIScope`, `@RouteScope`) can be used with plain injection, so tab-scoped state
would otherwise be the only kind that has to be looked up and created by hand.
**Workaround used:** `MissingAPI.BrowserTabScope`, `MissingAPI.VaadinBrowserTabScope`
and `MissingAPI.BrowserTabScopeConfig`: a Spring `Scope` that keeps the beans in
a `BrowserTab` attribute and runs their destruction callbacks (`@PreDestroy`)
from a tab destroy listener. It has to take the session lock itself, because
`BrowserTab.get(UI)` and the attribute methods require it.
**Suggested API:** `@BrowserTabScope` in `com.vaadin.flow.spring.annotation`,
backed by a `VaadinBrowserTabScope` registered in `VaadinScopesConfig` next to
the existing scopes, and the CDI equivalent in vaadin-cdi.

## Browser tabs cannot be simulated in browserless tests

**Where it bit us:** uc6, uc7, uc8 tests
**Symptom:** The mock UI has no `window.name`, so every UI becomes its own
browser tab. There is no way to simulate the things the scope exists for: a
reload (a new UI in the same tab), a second tab in the same session, a
duplicated tab, or a closed tab expiring after the heartbeat timeout
(`BrowserTab.destroyInactiveTabs` is package-private). The tests only cover
rendering, the non-tab state, and tab destruction at session end.
**Workaround used:** None; tab behaviour was checked manually in a browser.
**Suggested API:** browserless helpers such as `reload()` (new UI, same window
name), `openTab()` / `duplicateTab()`, and a way to expire inactive tabs or
advance the heartbeat clock.

## A closed tab keeps its state until the heartbeat timeout

**Where it bit us:** uc6 / `BookingDraft.java`
**Symptom:** A browser tab is destroyed only when none of its UIs has sent a
heartbeat for the heartbeat timeout (three heartbeat intervals, 15 minutes by
default). A seat held by an abandoned booking stays blocked for that long. The module
lowers `vaadin.heartbeatInterval` to 20 seconds for the demo, which also
affects every other UI in the application.
**Workaround used:** A shorter global heartbeat interval.
**Suggested API:** A separate, configurable grace period for tabs whose last
UI was closed, short enough to cover a reload but independent of the heartbeat
interval.

## Duplicated tabs cannot be detected

**Where it bit us:** uc6 / `BookingDraft.java`, uc7 / `TicketWorkspace.java`
**Symptom:** The tab id is `window.name`, which the browser copies when a tab
is duplicated. The duplicate shares the booking or the open ticket with the
original, which is exactly the mix-up the scope is meant to prevent, and the
application has no way to notice.
**Workaround used:** None; documented in the `@BrowserTabScope` Javadoc and in UC8.
**Suggested API:** Detect a second live UI claiming the same tab id (for
example with a per-page `sessionStorage` marker) and either give it a new id or
expose `BrowserTab#isDuplicate()` so that the application can decide.

## `BrowserTab` state is not reactive

**Where it bit us:** uc7 / `TicketPerTabView.java`
**Symptom:** `BrowserTab` attributes are a plain map. If one component changes
tab state, other components in the same tab (for example a header showing the
open ticket) are not notified, unlike state held in a signal.
**Workaround used:** The beans hold plain fields, and the view refreshes the
components it owns by hand.
**Suggested API:** Recommend (and document) keeping signals inside tab-scoped
beans, or offer a signal-valued attribute accessor on `BrowserTab`.

## Spring `@SessionScope` is not tied to the browserless Vaadin session

**Where it bit us:** uc3 / `OrderExportViewTest.java`, uc8 / `ScopePlaygroundViewTest.java`
**Symptom:** In `SpringBrowserlessTest`, Spring's `RequestContextHolder` stays
bound to Spring Test's own `MockHttpServletRequest` for the whole test method.
`cleanVaadinEnvironment()` + `initVaadinEnvironment()` create a new Vaadin
session and HTTP session, but `@SessionScope` beans keep resolving to the old
Spring mock session. UC8's test therefore has to expect the HTTP session
counter to keep counting in the "new session" case.
Related, and also true in production: `@SessionScope` beans only resolve on
threads that are handling an HTTP request, so they cannot be used from
`UI.access` in background threads or from server push.
**Workaround used:** The test expectation documents the behaviour.
**Suggested API:** `browserless-test-spring` could bind `RequestContextHolder`
to the mock Vaadin request (and rebind it in `initVaadinEnvironment()`), so
that Spring web scopes follow the simulated session.
