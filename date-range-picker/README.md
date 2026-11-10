# Date Range Picker — use cases

A standalone Spring Boot demo of the prototype `<vaadin-date-range-picker>`
web component (part of `@vaadin/date-picker`) in a Vaadin Flow application.
Each view is one task that needs a start and an end date: a hotel stay, time
off, a car rental, a report filter.

There is no Flow component for the picker yet, so the module wraps the web
component itself in `com.example.daterangepicker.DateRangePicker`, a field
whose value is Flow's `DateRange`. It reuses the connector of Flow's
`DatePicker` for locale-based formatting and disabled dates. The web
components come from a pkg.pr.new preview of the web components branch that
adds the picker, through the matching Flow Components pull request snapshot
set in `pom.xml` (`flow-components.version`).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Book a stay | Check-in and check-out within a booking window, with fully booked nights disabled; the server rejects stays across a booked night or longer than 14 nights. |
| UC2 | Filter by period | An order list filtered to a period, with quick buttons for common periods and a period with only a start ("since"). |
| UC3 | Request time off | A `Binder` form: required, not in the past, no more working days than left; weekends and public holidays are not counted. |
| UC4 | Change the return date | A rental whose pick-up date is fixed: the start input is locked in the browser, so only the return date can be picked or typed. |
| UC5 | Dates in your format | The range in the formats of the US, the UK, Finland and Germany, with translated calendars that start the week on the right day. |
| UC6 | One field for the range | The range in one input in a phone-sized toolbar, typed or picked. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl date-range-picker spring-boot:run
```

Open <http://localhost:8080/>. The first start installs the preview packages
from pkg.pr.new, so it needs network access.

## API gaps

See [API-GAPS.md](API-GAPS.md): the missing Flow component, the internal
connector for locale and disabled dates, `DateRange` without open ends, ranges
across disabled dates, no start-only disabled dates, no length limits, no
presets, unparsable text that the server never hears about, no read-only end,
untranslated calendars, no browserless tester, typed text that clears a
date, and no placeholder of its own for the single input.
