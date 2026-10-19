# API gaps — scopes

Places where the APIs used by this module were missing or awkward.

## 1. No Spring scope for `BrowserTab`

**Where it bites:** UC6 / BookingDraft.java, UC7 / ScopeCounters.java
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
Upstream: https://github.com/vaadin/flow/issues/13468

## 2. Some browser tab behaviour cannot be simulated in browserless tests

**Where it bites:** UC6 and UC7 tests
**Symptom:** `BaseBrowserlessTest#reload()` keeps the window name, so a reload
now stays in the same `BrowserTab`. A second tab in the same session needs
`BrowserlessUserContext#newWindow()`, which `SpringBrowserlessTest` does not
offer. Two things the scope exists for can not be simulated at all: a
duplicated tab (a second UI with the same window name), and a closed tab
expiring after the heartbeat timeout (`BrowserTab.destroyInactiveTabs` is
package-private). The tests cover rendering, the non-tab state, and tab
destruction at session end.
**Workaround used:** None; tab behaviour was checked manually in a browser.
**Suggested API:** browserless helpers such as `duplicateTab()`, a way to expire
inactive tabs or advance the heartbeat clock, and `newWindow()` in
`SpringBrowserlessTest`.

## 3. A closed tab keeps its state until the heartbeat timeout

**Where it bites:** UC6 / BookingDraft.java
**Symptom:** A browser tab is destroyed only when none of its UIs has sent a
heartbeat for the heartbeat timeout (three heartbeat intervals, 15 minutes by
default). A seat held by an abandoned booking stays blocked for that long. The module
lowers `vaadin.heartbeatInterval` to 20 seconds for the demo, which also
affects every other UI in the application.
**Workaround used:** A shorter global heartbeat interval.
**Suggested API:** A separate, configurable grace period for tabs whose last
UI was closed, short enough to cover a reload but independent of the heartbeat
interval.

## 4. Duplicated tabs cannot be detected

**Where it bites:** UC6 / BookingDraft.java, UC7 / ScopePlaygroundView.java
**Symptom:** The tab id is `window.name`, which the browser copies when a tab
is duplicated. The duplicate shares the booking with the original, which is
exactly the mix-up the scope is meant to prevent, and the application has no
way to notice.
**Workaround used:** None; documented in the `@BrowserTabScope` Javadoc and in UC7.
**Suggested API:** Detect a second live UI claiming the same tab id (for
example with a per-page `sessionStorage` marker) and either give it a new id or
expose `BrowserTab#isDuplicate()` so that the application can decide.

## 6. Spring `@SessionScope` is not tied to the browserless Vaadin session

**Where it bites:** UC3 / OrderExportViewTest.java, UC7 / ScopePlaygroundViewTest.java
**Symptom:** In `SpringBrowserlessTest`, Spring's `RequestContextHolder` stays
bound to Spring Test's own `MockHttpServletRequest` for the whole test method.
`cleanVaadinEnvironment()` + `initVaadinEnvironment()` create a new Vaadin
session and HTTP session, but `@SessionScope` beans keep resolving to the old
Spring mock session. UC7's test therefore has to expect the HTTP session
counter to keep counting in the "new session" case.
`SpringBrowserlessApplicationContext` already binds each user's request to
`RequestContextHolder` (vaadin/browserless-test#110), but
`SpringBrowserlessTest#initVaadinEnvironment()` does not.
Related, and also true in production: `@SessionScope` beans only resolve on
threads that are handling an HTTP request, so they cannot be used from
`UI.access` in background threads or from server push.
**Workaround used:** The test expectation documents the behaviour.
**Suggested API:** `SpringBrowserlessTest` could bind `RequestContextHolder`
to the mock Vaadin request in `initVaadinEnvironment()`, as the multi-user
context does, so that Spring web scopes follow the simulated session.
