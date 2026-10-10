# Internationalization — use cases

A standalone Spring Boot demo of making a Vaadin Flow application work for users in another language: translated texts and a language picker, dates, numbers, money and time zones in the user's conventions, right-to-left layouts for Arabic and Hebrew, and data that administrators translate themselves. The application is translated to English, German, Finnish, Arabic and Hebrew; the language picker in the top bar switches every use case. Each view exercises a single realistic scenario; `API-GAPS.md` records what Flow does not cover yet.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Translated texts | An order confirmation from `translations*.properties` through `getTranslation` and `LocaleChangeObserver`: parameters, a plural that follows a number, a date inside a sentence, a key only the English fallback file has, and a key no file has. |
| UC2 | Language switcher | The first language comes from the browser's `Accept-Language`; a picked language applies to every tab of the session and is remembered in a cookie, and the view shows where the current language came from. |
| UC3 | Dates and times | Date, time and date-time pickers that follow a language switch (format, month and weekday names, first day of the week, 12/24-hour clock), next to dates formatted on the server. |
| UC4 | Numbers and currency | Prices, VAT percentage and big numbers per locale, with the currency kept as part of the order; a `BigDecimalField` that reads the local decimal separator next to a `NumberField` that does not. |
| UC5 | Time zones | Events stored as `Instant`s, shown in the browser's time zone or a picked one, with UTC and office columns, daylight saving time, and a call scheduled in the chosen zone. |
| UC6 | Right-to-left layout | The whole application mirrors for Arabic and Hebrew; physical versus logical CSS side by side, and which icons mirror and which must not. |
| UC7 | Mixed-direction text | A Latin company name and ticket number inside a Hebrew sentence, inserted naively and isolated with Unicode isolates, plus `<bdi>` and `dir="auto"` for values and free text. |
| UC8 | Sorting and searching names | A list sorted with the language's `Collator` (Å, Ä, Ö after Z in Finnish) or plain `String.compareTo`, and a search that ignores accents and case. |
| UC9 | Translations as signals | Texts bound once to translation signals that follow both the language and a count, without `LocaleChangeObserver` code. |
| UC10 | Translated data | Categories an administrator names in every language, stored as a `LocalizedText` value of each category; users see their language or the English name marked as untranslated, and edits reach every session at once. |

Translations live in `src/main/resources/vaadin-i18n/`. `translations.properties` holds the English texts and is the fallback for every other language. Only the sample part of each view (the bordered box) is translated; the explanations stay in English.

The JDK's `Collator` used in UC8 predates CLDR, so it puts Ø and Ł after Z in every language. ICU4J's `Collator` follows CLDR, if an application needs that.

## Run

```
cd i18n
mvn spring-boot:run
```

Open <http://localhost:8080/>.
