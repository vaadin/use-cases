# Internationalization — API gaps discovered while building the demos

Places where Flow has no API for a genuine internationalization use case, or makes one awkward enough to need a workaround. Each entry names the use case that surfaced it.

The short version: **Flow translates texts, but the rest of the UI does not follow the language.** `I18NProvider`, `getTranslation`, `LocaleChangeObserver` and `UI.localeSignal()` cover the texts. But the text direction, the date pickers, the number fields, Grid cells, ComboBox labels, field labels and menu titles all have to be updated by hand on every locale change. The shims live in `src/main/java/com/example/MissingAPI.java` and `I18nConfig.java`, so the views don't each repeat them.

---

## The text direction does not follow the locale

**Where it bit us:** uc6 / RightToLeftView.java, and every view through `I18nConfig.java`
**Symptom:** switching to Arabic or Hebrew changes the texts but leaves the page left to right. `UI.setDirection` exists, but it is not tied to `UI.setLocale`, and neither Flow nor the JDK (outside AWT's `ComponentOrientation`) can say which direction a locale is written in.
**Workaround used:** `MissingAPI.directionOf(Locale)` with a hand-maintained list of right-to-left languages, plus a UI init listener in `I18nConfig` that runs `Signal.effect(ui, () -> ui.setDirection(directionOf(ui.localeSignal().get())))`.
**Suggested API:** `Direction.of(Locale)`, and `UI.setLocale` setting the direction too (on by default, or behind a `vaadin.i18n.follow-direction` property).

## No translation signal, and no `bindLabel`

**Where it bit us:** uc9 / TranslationSignalView.java; uc3, uc4, uc5, uc7, uc8 and uc10 for labels and column headers
**Symptom:** `UI.localeSignal()` exists, but there is no signal counterpart of `getTranslation`, so binding a text to a translation means writing `Signal.computed(() -> getTranslation(ui.localeSignal().get(), key))` each time. Field labels, Grid column headers and `ComboBox` labels have no `bind…` method at all, so every view that translates them still needs an effect that sets them one by one.
**Workaround used:** `MissingAPI.translate(key, params...)`, a computed signal over `UI.localeSignal()` that also reads any parameter that is itself a signal. Labels and headers are set from one `Signal.effect` per view.
**Suggested API:** `Signal<String> Component#translationSignal(String key, Object... params)` (or `I18NProvider.translationSignal`), and `HasLabel#bindLabel(Signal<String>)` / `Column#bindHeader(Signal<String>)`.

## Components do not follow a locale change

**Where it bit us:** uc3 / DatesAndTimesView.java, uc4 / NumbersAndCurrencyView.java, uc5 / TimeZonesView.java, uc8 / LocaleAwareSortingView.java, uc10 / TranslatedDataView.java
**Symptom:** `DatePicker`, `TimePicker` and `DateTimePicker` read the UI's locale only when they are attached, and `BigDecimalField` reads it only in its constructor. After `UI.setLocale` they keep the old format until they are re-created. Grid cells, `ComboBox` and `ListBox` labels that format with the locale stay as they were rendered, and an in-memory sort comparator built from a `Collator` keeps sorting in the old language.
**Workaround used:** an effect on `UI.localeSignal()` in each view that calls `setLocale` on every picker and field, `refreshAll()` on every Grid, sets the items of every `ComboBox` / `ListBox` again, and replaces the sort comparator.
**Suggested API:** components without an explicitly set locale follow `UI.localeSignal()`, and data components re-render their items when it changes.

## Picker texts are English unless the application translates them

**Where it bit us:** uc3 / DatesAndTimesView.java
**Symptom:** with a German locale, the date picker formats dates the German way but its calendar still shows "March", "Sunday" and "Today", and starts the week on Sunday. Month names, weekday names and the first day of the week are all in the JDK's locale data, but the component only uses a `DatePickerI18n` the application fills in.
**Workaround used:** `MissingAPI.datePickerI18n(owner, locale)`, which builds the names from `Month` / `DayOfWeek#getDisplayName`, takes the first day of the week from `WeekFields.of(locale)` and the button texts from the application's translations.
**Suggested API:** a locale-derived default `DatePickerI18n` (and the same for `DateTimePicker`), with button and error texts looked up through the `I18NProvider` under well-known keys such as `vaadin.date-picker.today`.

## `NumberField` ignores the locale

**Where it bit us:** uc4 / NumbersAndCurrencyView.java
**Symptom:** a German user types `1234,5` and `NumberField` does not understand it, because it always uses a dot as the decimal separator. `BigDecimalField` follows the locale but returns a `BigDecimal`.
**Workaround used:** none. The view shows both fields side by side and uses `BigDecimalField` for the price.
**Suggested API:** locale-aware parsing and formatting in `NumberField` / `IntegerField`, or one numeric field with a locale and a value type.

## No way to remember the chosen language

**Where it bit us:** uc2 / LanguageSwitcherView.java, `LanguagePreference.java`
**Symptom:** Flow matches the browser's `Accept-Language` when the session starts, which is right for a first visit. But a language the user picks is lost when the session ends, and there is no hook to take it into account at session start. Writing a cookie from the server does not work either: with WebSocket push there is no HTTP response to put the `Set-Cookie` header in.
**Workaround used:** a session init listener reads a `locale` cookie and overrides the matched locale. Picking a language writes the cookie through `executeJs("document.cookie = …")`.
**Suggested API:** a `LocaleResolver`-style SPI that Flow asks when a session starts and tells when the locale changes, with a cookie-based implementation built in.

## The default locale of the automatic provider is arbitrary

**Where it bit us:** `I18nConfig.java`
**Symptom:** without an `I18NProvider` bean, Flow creates a `DefaultI18NProvider` from the `translations_*.properties` files it finds, and its default locale is the first one in that list. Here that would be Arabic, so a browser asking for Japanese would get a right-to-left Arabic UI.
**Workaround used:** an explicit `@Bean I18NProvider` that passes the supported locales in order, English first.
**Suggested API:** a `vaadin.i18n.default-locale` property, or treating the locale of the fallback `translations.properties` as the default.

## The server's default locale leaks into translations

**Where it bit us:** `src/main/resources/vaadin-i18n/translations_en_US.properties`
**Symptom:** `DefaultI18NProvider` uses `ResourceBundle.getBundle` with the default control. When there is no file for the requested locale, that control looks for the JVM's default locale before it falls back to `translations.properties`. On a server running with a German default locale, English users would get German texts.
**Workaround used:** an empty `translations_en_US.properties`, so that every supported locale has a file of its own.
**Suggested API:** load bundles with `ResourceBundle.Control.getNoFallbackControl(FORMAT_PROPERTIES)`, so a missing locale goes straight to the fallback file.

## `MessageFormat` is not enough for real-world messages

**Where it bit us:** uc1 / TranslatedTextView.java, uc9 / TranslationSignalView.java
**Symptom:** the translations are formatted with `java.text.MessageFormat`, which has three problems. First, plurals are `ChoiceFormat` ranges, which cannot express CLDR plural rules: Arabic picks between six forms by the number modulo 100, so the Arabic plurals here are right for 0–102 only. Second, date parameters must be `java.util.Date`, not `java.time`, and are formatted in the JVM's default time zone, not the user's. Third, apostrophes must be doubled in messages that get parameters but not in those that don't, because `DefaultI18NProvider` only runs `MessageFormat` when there are parameters.
**Workaround used:** ranges in the Arabic files (with a comment about the limit), and the delivery date converted to a `Date` at midnight in the JVM's zone so the day does not shift.
**Suggested API:** ICU-style message formatting (CLDR plurals and `select`), `java.time` parameters formatted in the UI's time zone, and the same escaping rules with and without parameters.

## No component API for the direction of a single element

**Where it bit us:** uc7 / MixedDirectionView.java, uc6 / RightToLeftView.java, uc10 / TranslatedDataView.java
**Symptom:** a Latin company name inside a Hebrew sentence borrows the sentence's direction, so "Acme Ltd." shows up as ".Acme Ltd". The fixes are Unicode isolates around parameters, a `<bdi>` element, or `dir` on the element. Flow has no `Bdi` component and no setter for an element's own `dir`, and translation parameters are inserted as they are. The same is true of an Arabic name field in an English admin UI, which should be right to left.
**Workaround used:** `MissingAPI.isolate(text)` (FSI … PDI) for translation parameters, `new Element("bdi")`, and `getElement().setAttribute("dir", …)` for free text, code labels and per-language fields.
**Suggested API:** `HasDirection#setDirection(Direction | AUTO)` on components, a `Bdi` HTML component, and an option for `I18NProvider` to isolate parameters.

## `@Menu` and `@PageTitle` cannot be translated

**Where it bit us:** every view (`MainLayout` and the side navigation)
**Symptom:** the side navigation is built from `@Menu(title = …)` and the browser tab from `@PageTitle`. Both take a literal string, so the navigation stays English while the rest of the page is in Arabic. `HasDynamicTitle` covers the tab title, but there is nothing for menu entries.
**Workaround used:** none. The demo's navigation stays English, and only the sample part of each view is translated.
**Suggested API:** a translation key on both, e.g. `@Menu(titleKey = "menu.orders")` and `@PageTitle(key = "title.orders")`, resolved through the `I18NProvider` on every locale change.

## No user time zone next to the user locale

**Where it bit us:** uc5 / TimeZonesView.java
**Symptom:** the browser's time zone is available as soon as the UI starts (`ExtendedClientDetails#getZoneId`), which is great. But it is a one-off value: there is no session or UI time zone that an application can override and that formatting code, `MessageFormat` dates or Grid renderers would use. Every view has to pass the zone around itself.
**Workaround used:** a view-local `ValueSignal<ZoneId>` seeded from the client details and bound to a zone picker.
**Suggested API:** `UI#getZoneId()` / `UI#zoneSignal()` (seeded from the browser, settable like the locale), used by date formatting in translations and renderers.

## Translated data has no building blocks

**Where it bit us:** uc10 / TranslatedDataView.java, `LocalizedText.java`
**Symptom:** texts that administrators enter in several languages (category names, statuses) are common. But there is no field for "one value, several languages", no Binder support for a `Map<Locale, String>` property, and no public helper for the fallback order `de-CH` → `de` → default that `ResourceBundle` uses internally.
**Workaround used:** a `LocalizedText` record with its own fallback lookup, one `TextField` per language, and a hand-written save that builds the value from the fields.
**Suggested API:** a `LocalizedTextField` (tabs or a language dropdown) bound to a localized value type, and `I18NUtil.candidateLocales(Locale, Locale defaultLocale)`.

## No test support for the browser's language and time zone

**Where it bit us:** all tests, in particular uc2 / LanguageSwitcherViewTest.java and uc5 / TimeZonesViewTest.java
**Symptom:** a browserless test always starts with a request asking for `en-US` and with placeholder client details whose time zone reads as UTC. The session's locale is not matched against the `I18NProvider` as in a real request either: it is the JVM's default locale, so a test that passes on a machine running in `en_US` fails on a CI runner whose default is plain `en`. A test cannot simulate a browser that asks for Finnish, sends a remembered-language cookie, or runs in Tokyo, so the session-start path of UC2 and the detected zone of UC5 are only tested with those defaults.
**Workaround used:** the module's `pom.xml` pins the test JVM's default locale to `en_US`; tests switch the locale with `UI.setLocale` after navigating, pick the time zone through the view's own zone picker, and the cookie parsing is unit-tested separately (`LanguagePreferenceTest`).
**Suggested API:** browserless helpers to set the request's locales and cookies before the session starts, and the time zone in `ExtendedClientDetails`.
