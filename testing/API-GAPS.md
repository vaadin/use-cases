# Testing — API gaps discovered while building the demos

Places where testing a Vaadin Flow application is harder than it should be, or needs knowledge of internals. Each entry names the use case and the test that surfaced it.

The short version: **the testing tools are good one by one, but each stops at a different edge.** Browserless tests cover components and navigation well, but not background work or most browser APIs. TestBench and Playwright both drive a real browser, but neither knows enough about Vaadin's web components to read a grid row or mask a screenshot without help. The load testing plugin turns an end-to-end test into a k6 script, but it asks the application to give up WebSocket push.

---

## Browserless: no public way to run queued `UI.access` tasks

**Where it bit us:** uc6 / ReportViewTest.java
**Symptom:** a background job's result reaches the UI through `UI.access`. In a browserless test, that command is queued until the queue runs, but the public `roundTrip()` does not run it. A test of any background work or push therefore sees the old state.
**Workaround used:** `com.vaadin.browserless.internal.MockVaadin.runUIQueue()`, an internal class.
**Suggested API:** a public `runPendingAccessTasks()` on `BaseBrowserlessTest` (or have `roundTrip()` run the access queue, as a real round trip would).

## Browserless: most browser APIs have no simulator

**Where it bit us:** uc7 / BrowserApisViewTest.java (and the i18n, theming, page-visibility and wake-lock modules)
**Symptom:** geolocation has `GeolocationSimulator`, which makes its tests easy. The others don't. Page visibility can only be faked by firing the `vaadin-page-visibility-change` DOM event on the UI, and because its listener is debounced, the event data must also carry the internal phase key (`JsonConstants.EVENT_DATA_PHASE` = trailing), or Flow ignores it. The browser's time zone (`ExtendedClientDetails`) cannot be set at all, so a browserless UI is always in UTC. There is no way to send request locales or cookies before the session starts, or to answer a media query.
**Workaround used:** a hand-built DOM event with the phase key for page visibility; time zone tests limited to UTC; the locale set with `UI.setLocale` after navigating.
**Suggested API:** one simulator per browser-backed API, following `GeolocationSimulator`: `PageVisibilitySimulator.current().hide()`, `ClientDetailsSimulator.current().setTimeZone(ZoneId)`, request locales and cookies for the next session, and media-query answers.

## Browserless: the Grid tester cannot sort by header

**Where it bit us:** uc4 / OrdersViewTest.java
**Symptom:** `GridTester` reads cells, selects rows and finds cell components, but has no way to click a column header to sort, which is how users sort.
**Workaround used:** `grid.sort(GridSortOrder.desc(column).build())` directly on the component.
**Suggested API:** `GridTester#sortBy(String columnKey, SortDirection)` that goes through the same path as a header click.

## Browserless: a link to a view outside `@ViewPackages` breaks the navigation

**Where it bit us:** uc8 / EndToEndViewTest.java
**Symptom:** a view that contains a `RouterLink` to another view fails to open with `MockRouteNotFoundError` unless the link's target is also listed in `@ViewPackages`. The error names the view being opened, not the link that caused it.
**Workaround used:** list the link targets in `@ViewPackages` too.
**Suggested API:** register link targets lazily, or report "RouterLink to X: X is not in @ViewPackages".

## Security: every layout needs its own access annotation

**Where it bit us:** all view tests, `MainLayout.java`
**Symptom:** with `VaadinSecurityConfigurer`, an `@AnonymousAllowed` view inside a layout without an access annotation is denied, and the test fails with a redirect to a login view it cannot resolve. Nothing points at the layout.
**Workaround used:** `@AnonymousAllowed` on `MainLayout`, and `@WithAnonymousUser` on the view tests.
**Suggested API:** a clear error naming the layout that denied access, or layouts inheriting the access of the view they wrap.

## `EmailField` shows an empty error message for its own format check

**Where it bit us:** uc3 / RegistrationViewTest.java
**Symptom:** a Binder `EmailValidator` with a message never shows it: `EmailField` checks the format itself first and shows its own message, which is empty unless set. The field turns red with no text, which only a test (or a careful look) catches.
**Workaround used:** set the field's own message with `EmailFieldI18n#setPatternErrorMessage`.
**Suggested API:** a non-empty default message, or the Binder validator's message taking precedence.

## A click shortcut fires twice on its own focused button

**Where it bit us:** uc10 / AccessibilityPlaywrightIT.java
**Symptom:** `send.addClickShortcut(Key.ENTER)` listens on the whole page. With focus on the Send button, Enter both activates the button and fires the shortcut, so the form is sent twice. In a text area, Enter submits instead of starting a new line. Only the real-browser keyboard test caught it.
**Workaround used:** `addClickShortcut(Key.ENTER).listenOn(name, email)`, limited to the single-line fields.
**Suggested API:** a click shortcut that does not fire when the event's target is the button itself or a multi-line text input.

## TestBench: screenshot references depend on the browser version, and cannot mask

**Where it bit us:** uc9 / InvoiceScreenshotIT.java, uc8 / CheckoutIT.java
**Symptom:** `compareScreen("invoice")` looks for `invoice_linux_chrome_154.png`, so every Chrome update makes every reference "missing". There is no way to mask a region that changes on every load. Inside a `BrowserTestBase`, the inherited `assertEquals` overloads for elements shadow JUnit's static import, so `assertEquals("text", cell.getText())` does not compile.
**Workaround used:** references regenerated per browser version; the volatile element hidden with JavaScript before the screenshot; `Assertions.assertEquals` written out.
**Suggested API:** a reference naming option without the browser version, `compareScreen(id, mask...)`, and no assertion methods on the test base class.

## Playwright: no Vaadin-aware locators, and grid rows have no text

**Where it bit us:** uc8 / CheckoutPlaywrightIT.java, uc10 / AccessibilityPlaywrightIT.java
**Symptom:** Playwright's role and label locators mostly work on Vaadin components, with three exceptions. A grid row's text is empty, because cells show their content through slots, so `containsText` on a row fails. `getByLabel("Message")` matches both the text area and its host and trips strict mode. Picking from a `Select` means clicking the field and then the overlay's option. Playwright for Java also has no screenshot assertion (Playwright for JavaScript does).
**Workaround used:** find the cell by its accessible name (which does include slotted content) and read its row index; `getByRole(TEXTBOX, name)` for text areas; a small pixel comparator (`ScreenshotComparison`).
**Suggested API:** a small Vaadin locator library for Playwright (grid cells by row and column, select options, field values), shipped next to TestBench's element classes.

## Load testing: recording needs long polling, and the helpers guess the host

**Where it bit us:** uc11 / LoadTestView.java, `Application.java`, `pom.xml`
**Symptom:** the recorder captures HTTP traffic only, so an application with WebSocket push must switch to `Transport.LONG_POLLING` to be recorded, which changes what is load tested. The test helpers (`LoadTestItHelper`, `PlaywrightHelper`) take the host from the `HOSTNAME` environment variable when it is set, which in a container or on a CI runner is the machine's name, not the server's. The port is the `server.port` system property, the same name Spring uses. The server metrics table stays empty unless the application exposes the actuator `metrics` endpoint. Every virtual-user iteration leaves a new session in memory until it times out.
**Workaround used:** long polling for the whole application; tests run where `HOSTNAME` resolves to the server; `--management.endpoints.web.exposure.include=health,metrics` only for the load test run, on the management port.
**Suggested API:** WebSocket recording (or a per-recording transport switch), explicit `loadtest.host` / `loadtest.port` properties, and a note in the report when metrics are unavailable or sessions pile up.
