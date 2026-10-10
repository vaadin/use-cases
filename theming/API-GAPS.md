# Theming — API gaps discovered while building the demos

Places where Flow has no API for a genuine theming use case, or makes one awkward enough to need a workaround. Each entry names the use case that surfaced it.

The short version: **styling an application is well covered, but changing its look at runtime is not.** Stylesheets, theme tokens, variants, part names and `Style.bind` cover the look an application ships with. But switching the theme, following the user's system settings, remembering the user's choices, serving a customer's stylesheet and overriding tokens for the whole page all need code of their own. That code lives in `src/main/java/com/example/MissingAPI.java` and `AppearanceSetup.java` instead of being repeated in every view.

---

## The theme cannot be switched at runtime

**Where it bit us:** uc9 / ThemeSwitchView.java, `Application.java`, `AppearanceSetup.java`
**Symptom:** the theme is normally loaded with `@StyleSheet(Aura.STYLESHEET)` on the app shell. Such a stylesheet is part of the page Flow renders and cannot be removed again, so switching to Lumo, or loading a brand only for some users, is impossible that way.
**Workaround used:** no theme on the app shell. A UI init listener adds the theme with `Page.addStyleSheet` and keeps the `Registration` to remove it later. The price is that the first paint of every page happens before the theme has loaded.
**Suggested API:** a per-session or per-UI theme API, e.g. `UI#setTheme(String styleSheet)`, whose stylesheet also goes into the page Flow renders, so a switched theme is there from the first paint.

## The server cannot see the user's display preferences

**Where it bit us:** uc2 / DarkModeView.java, uc8 / AccessibleThemingView.java
**Symptom:** with `ColorScheme.Value.SYSTEM`, the browser decides between light and dark, but the server never learns which one it picked. `ExtendedClientDetails#getColorScheme` only returns what the application itself set. The same is true of `prefers-contrast`, `prefers-reduced-motion` and `forced-colors`, so the server cannot adapt charts, images or its own defaults to them.
**Workaround used:** `MissingAPI.mediaQuery(owner, query)`, which runs `matchMedia` in the browser and reports the result and later changes as a DOM event into a signal.
**Suggested API:** signals for the common preferences next to `Page#pageVisibilitySignal()`, e.g. `Page#colorSchemeSignal()` (the effective scheme), `Page#prefersReducedMotionSignal()`, `Page#prefersContrastSignal()`, or a generic `Page#mediaQuerySignal(String)`.

## No way to remember appearance choices

**Where it bit us:** uc2 / DarkModeView.java, uc4 / UserPreferencesView.java, uc8 / AccessibleThemingView.java, uc9 / ThemeSwitchView.java
**Symptom:** a user's color scheme, accent color, text size or theme should still be there on the next visit. Flow has no place for such preferences. Writing a cookie from the server does not work either: with WebSocket push there is no HTTP response to put the `Set-Cookie` header in.
**Workaround used:** `MissingAPI.writeCookie` through `executeJs("document.cookie = …")`, read back by a session init listener in `Appearance`.
**Suggested API:** a small client-side preference store reachable from the server (cookie- or `localStorage`-backed, readable at session start), or appearance persistence built into the theme API above.

## Flow cannot serve a stylesheet it generates

**Where it bit us:** uc3 / TenantThemeView.java, `TenantStyleSheetController.java`
**Symptom:** a customer's styling comes from the database, so its stylesheet is generated, not a file. `Page#addStyleSheet` only takes a URL, and Flow has no stylesheet counterpart of a `DownloadHandler` that would serve generated CSS at a URL.
**Workaround used:** a Spring `@RestController` at `/tenant-theme/<id>.css`, with the customer's values validated before they go into CSS.
**Suggested API:** `Page#addStyleSheet(DownloadHandler)` (or a `StyleSheet.fromString(String css)`), returning the same `Registration`.

## Token overrides for the whole page need JavaScript

**Where it bit us:** uc4 / UserPreferencesView.java, uc8 / AccessibleThemingView.java
**Symptom:** the themes define their tokens on `:root` and compute some from others there (Aura's accent color from its light and dark variants). An override has to go on the same element to take part in that computation, but `UI#getElement()` is the `<body>`, and the server has no handle for `<html>`.
**Workaround used:** `MissingAPI.setRootProperties(ui, properties)`, which sets and removes inline custom properties on `document.documentElement` through `executeJs`.
**Suggested API:** `Page#getRootStyle()` returning a `Style` for `<html>` (with `bind` like the element `Style`), or `UI#setCssProperty(String, String)` that targets the root.

## No color picker component

**Where it bit us:** uc4 / UserPreferencesView.java
**Symptom:** letting a user pick a color is a common settings need, but Vaadin has no color picker component.
**Workaround used:** the native `<input type="color">` through the `Input` HTML component, next to preset swatch buttons.
**Suggested API:** a `ColorPicker` field with a `String` (hex) or color value, swatches and an optional custom color.

## Density is not a component API, and Lumo's is page-wide

**Where it bit us:** uc5 / DensityView.java
**Symptom:** Aura scales a part of the page through a `small` / `large` theme name on any element, but that is only a raw attribute on the Java side (`Div` has no `HasTheme`), and it clashes with the theme names layouts use for spacing. Lumo's compact preset redefines its sizes on `:root`, so it can only shrink the whole page, and there is no larger preset.
**Workaround used:** `getElement().bindAttribute("theme", …)` on a plain `Div` for Aura. For Lumo, the compact stylesheet is added while the view is open and removed when it closes.
**Suggested API:** a theme-independent density API, e.g. `HasDensity#setDensity(Density.COMPACT)` on layouts and containers, implemented by both themes for a subtree.

## Tokens and variants are theme-specific

**Where it bit us:** uc1 / BrandTokensView.java, uc3 / Tenant.java, uc6 / VariantsView.java, uc9 / ThemeSwitchView.java
**Symptom:** the same design decision has different token names in each theme (`--aura-accent-color-light` versus `--lumo-primary-color`, `--aura-base-radius` as a unitless number versus `--lumo-border-radius-m` as a length). A brand or customer stylesheet that must survive a theme switch has to set both. Theme-specific variants (`ButtonVariant.AURA_DANGER`, `LUMO_CONTRAST`) silently fall back to the default look in the other theme.
**Workaround used:** the brand and the customer stylesheets set the tokens of both themes, and the views use the generic variants.
**Suggested API:** shared brand tokens in the `--vaadin-*` set that both themes derive from (at least accent color, font family and radius), so one stylesheet brands either theme.

## No test support for display preferences and loaded stylesheets

**Where it bit us:** all tests, in particular uc2 / DarkModeViewTest.java and uc8 / AccessibleThemingViewTest.java
**Symptom:** a browserless test cannot simulate a browser in dark mode or with reduced motion, and cannot list the stylesheets a UI has added at runtime through `Page#addStyleSheet`.
**Workaround used:** a test helper (`MediaQueries`) fires the DOM event `MissingAPI.mediaQuery` listens to, and `AppearanceSetup.styleSheets(ui)` / `MissingAPI.rootProperties(ui)` record what was applied.
**Suggested API:** browserless helpers to set the media features a test browser reports, and a way to read a UI's dynamically added stylesheets.
