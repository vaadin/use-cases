# Date Range Picker — API gaps discovered while building the demos

Places where the prototype `<vaadin-date-range-picker>` web component (part of
`@vaadin/date-picker`), or the way it reaches a Flow application, does not
cover a use case or makes one awkward. Each entry names the use case that
surfaced it.

The web component's API: `startValue` and `endValue` (ISO dates), `min`,
`max`, `isDateDisabled`, `startPlaceholder`, `endPlaceholder`,
`clearButtonVisible`, `readonly`, `required`, `invalid`, `errorMessage`,
`showWeekNumbers`, `separateDatePicking`, `singleInput`, `opened`, `i18n` (the
date picker's texts plus the accessible names of the range), and the `change`
event when the user commits a range. The Flow side is
`com.example.daterangepicker.DateRangePicker`, a wrapper this module builds
itself (#1).

## 1. No Flow component for the range picker

**Where it bites:** every use case / `com.example.daterangepicker.DateRangePicker`
**Symptom:** Flow Components has no date range picker, so a Flow application
can not use the web component without writing the Java side: the value type,
the synchronization of `startValue` and `endValue` on `change`, server-side
validation, the texts, the locale and disabled dates.
**Workaround used:** `DateRangePicker` extends `AbstractField<DateRangePicker,
DateRange>`, with Flow's existing `DateRange` as the value. It works with
`Binder` and `bindValue` like other fields. It turns on `manualValidation`, so
`Binder` decides what is invalid.
**Suggested API:** a `DateRangePicker` in the date picker module with the
`DatePicker` API applied to a range: `setMin`, `setMax`, `setDisabledDates`,
`setDisabledWeekdays`, `setLocale`, `setI18n`, the placeholders,
`setSeparateDatePicking`, `setSingleInput`, plus `getStart()` / `getEnd()` for
a range with one end (#3).

## 2. Locale and disabled dates need the date picker's internal connector

**Where it bites:** UC1 / `BookAStayView.java`; UC5 /
`DatesInYourFormatView.java`; `DateRangePicker.java`
**Symptom:** formatting and parsing dates for a locale, and disabling dates,
both need JavaScript functions on the element (`i18n.formatDate`,
`i18n.parseDate`, `isDateDisabled`), which the server can not send.
**Workaround used:** the wrapper reuses `datepickerConnector.ts` from Flow's
`DatePicker`: it calls `window.Vaadin.Flow.datepickerConnector.initLazy` on
the range picker, then `$connector.updateI18n(languageTag, i18n)` and
`$connector.setDateMetadataConfig(...)`, the same calls `DatePicker` makes.
This works because the range picker shares the date picker's `i18n` and
`isDateDisabled` API, but the connector is not public API and can change
without notice. The range picker also has no `dateMetadataProvider`, so the
per-date metadata `DatePicker` offers (`DateMetadataProvider`) is not
available.
**Suggested API:** the Flow component of #1 brings the connector along, and
the web component supports `dateMetadataProvider` like the date picker.

## 3. `DateRange` can not hold a range with one end

**Where it bites:** UC2 / `FilterOrdersView.java`
**Symptom:** the picker allows a range with only a start ("since 1
September") or only an end, which filters need often. Flow's `DateRange`
record requires both ends, so such a range has no value: the wrapper reports
`null`. Picking only a start on an empty field then fires no value change at
all, because the value stays `null`.
**Workaround used:** the wrapper offers `getStart()` / `getEnd()` and an
`addChangeListener` for the `change` event; UC2 listens to both the value and
the change event and reads the two ends.
**Suggested API:** `DateRange` with nullable ends (or an `isOpen` variant), so
the value can say "from this date" and value changes fire for it.

## 4. A range may span disabled dates

**Where it bites:** UC1 / `BookAStayView.java`
**Symptom:** `isDateDisabled` only stops the user from picking a disabled date
as a start or an end. A range from before to after a fully booked night is
accepted and drawn across the disabled dates.
**Workaround used:** the server checks every night of the stay and shows an
error message that names the booked night.
**Suggested API:** an option that stops a range from spanning disabled dates,
e.g. `allowDisabledDatesInRange = false`, with the calendar not offering end
dates past the next disabled date.

## 5. A date can not be disabled as a start only

**Where it bites:** UC1 / `BookAStayView.java`
**Symptom:** a fully booked night means no check-in on that day, but
checking out on its morning is fine. `isDateDisabled` disables a date for
both ends, so the guest can not end a stay on the morning of a booked night by
picking it in the calendar.
**Workaround used:** none; the view says so.
**Suggested API:** the date passed to `isDateDisabled` could say which end is
being picked, or separate `isStartDateDisabled` / `isEndDateDisabled`
functions.

## 6. No minimum or maximum length

**Where it bites:** UC1 / `BookAStayView.java`; UC3 / `RequestTimeOffView.java`
**Symptom:** stays have a maximum number of nights and often a minimum; time
off is limited by the days left. The picker can not limit the length, so the
calendar offers end dates the application will reject.
**Workaround used:** server-side validation with an error message.
**Suggested API:** `minLength` / `maxLength` in days, with the calendar
disabling end dates outside them once a start is picked.

## 7. No presets

**Where it bites:** UC2 / `FilterOrdersView.java`
**Symptom:** report filters almost always offer "Today", "Last 7 days", "This
month". The picker and its overlay have no place for them.
**Workaround used:** tertiary buttons next to the picker that set the value.
**Suggested API:** a slot in the overlay for preset buttons, or a `presets`
property with labels and ranges.

## 8. Unparsable text is not reported to the server

**Where it bites:** UC3 / `RequestTimeOffView.java`
**Symptom:** with `manualValidation`, as Flow fields use it, the server has to
validate. Text the picker can not parse leaves that end empty, and nothing
tells the server that the user typed something: there is no
`unparsable-change` event and no bad-input state, as the date picker has. The
form then says "Pick your first and last day off" instead of "This date is
not valid".
**Workaround used:** none.
**Suggested API:** an `unparsable-change` event and a way to read whether
either input holds unparsable text, like `DatePicker` uses for its
bad-input check.

## 9. One end can not be made read-only

**Where it bites:** UC4 / `ChangeReturnDateView.java`, `lock-range-start.ts`
**Symptom:** once a rental has started, its pick-up date is fixed, but the
return date can still move. `readonly` applies to the whole field, and
`separateDatePicking` only changes what a click picks; the start input stays
editable, and the calendar button always picks the whole range from the
start. Correcting a changed start afterwards is easy to miss, so the change
has to be prevented in the browser.
**Workaround used:** `lock-range-start.ts` blocks typing, pasting and dropping
into the start input, marks it `aria-readonly`, takes it out of the tab order,
moves focus from it to the end input, and makes the calendar button open the
calendar for the end only. In the calendar, pressing the start date and
dragging moves the start, and dragging across other dates "paints" a new
range, so the script keeps mouse moves from the calendar unless the press was
on the end date. It depends on the picker's current light DOM inputs, toggle
button and the `range-end` part of the calendar dates. The server still puts the start back if a changed
one arrives.
**Suggested API:** `startReadonly` / `endReadonly`, with the calendar button,
clicks on the field and drags in the calendar then changing only the editable
end.

## 10. Month names, day names and the first day of the week are not taken from the locale

**Where it bites:** UC5 / `DatesInYourFormatView.java`, `RangeTexts.java`
**Symptom:** the locale decides how dates are formatted and parsed, but the
calendar stays in English and starts the week on Sunday unless the
application supplies the names and the first day of the week in `i18n` for
every language. `java.time` knows all of them for any locale.
**Workaround used:** `RangeTexts` builds them from `java.time` and translates
the picker's own eight words.
**Suggested API:** the Flow component fills in month names, day names and the
first day of the week from `setLocale`, leaving `setI18n` for overrides; the
same would help `DatePicker`.

## 11. No browserless tester for the range picker

**Where it bites:** every view test / `src/test/.../DateRangePickerTester.java`
**Symptom:** browserless tests can not pick or type a range.
**Workaround used:** `DateRangePickerTester` (test sources) sets
`startValue` and `endValue` as the client would and fires `change`. Checks of
the locale and disabled dates read the pending JavaScript calls through
`UIInternals`.
**Suggested API:** a `DateRangePickerTester` in browserless-test with
`pick(start, end)`, `typeStart(text)` / `typeEnd(text)` and access to the
disabled dates.

## 12. Replacing a date with unparsable text clears the input

**Where it bites:** UC3 / `RequestTimeOffView.java`
**Symptom:** the web component says that unparsable text is kept in the input
and makes the field invalid. That holds when the input was empty. When it held
a date, typing text that does not parse (e.g. "abc") and pressing Enter clears
the input and the date, and the field is not invalid: the typed text is gone
without a message.
**Workaround used:** none.
**Suggested API:** keep the text and mark the field invalid in both cases,
as the documentation says.

## 13. The single input has no placeholder of its own

**Where it bites:** UC6 / `OneFieldView.java`
**Symptom:** with `singleInput`, the placeholder is always the start and end
placeholders joined with a dash. A filter that wants "Any time" shows "Any
time –" instead.
**Workaround used:** placeholders that read as a pair ("Start date – end
date").
**Suggested API:** a `placeholder` property used by the single input.
