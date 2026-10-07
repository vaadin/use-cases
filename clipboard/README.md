# Vaadin Clipboard API Use Cases

This module contains a collection of Vaadin Flow views demonstrating the
browser Clipboard API exposed via `com.vaadin.flow.component.clipboard.Clipboard`.

Rough edges found while building the views — including the ones that
shaped UC8 — are collected in [API-GAPS.md](API-GAPS.md).

The original tracking issue is
[vaadin/platform#8759](https://github.com/vaadin/platform/issues/8759);
the initial write API landed via
[vaadin/flow#23615](https://github.com/vaadin/flow/pull/23615) and
file-paste support is on
[vaadin/flow#24485](https://github.com/vaadin/flow/pull/24485).

## API at a glance

```java
// Write — fires on a user click on the source component.
Clipboard.onClick(button).writeText("hello");
Clipboard.onClick(button).writeText(textField);
Clipboard.onClick(button).writeHtml("<p>hello</p>");
Clipboard.onClick(button).writeImage(image);   // 25.3 paste-events branch
Clipboard.onClick(button).write(
        ClipboardContent.create().text("hello").html("<p>hello</p>"));

// Read — listens for browser paste events on a focused element.
// 25.3 paste-file branch.
Clipboard.onPaste(div, event -> {
    if (event.hasHtml()) {
        handleHtml(event.getHtml());
    } else if (event.hasText()) {
        handleText(event.getText());
    }
});

// File paste — each pasted file is uploaded to the URL Flow generates
// for the handler. 25.3 paste-file branch.
Clipboard.onFilePaste(div, PasteFileHandler.session()
        .onStart(start -> showProgress(start.totalFiles()))
        .onFile(file -> render(file))
        .onComplete(end -> hideProgress())
        .build());
```

Every variant has an overload that takes `onSuccess` and `onError`
callbacks (`SerializableConsumer<String>` and
`SerializableConsumer<PromiseAction.Error>`). The handler is registered
once at view-construction time and re-fires on every click of the source
component, which can be any `Component` that implements `ClickNotifier`
— a `Button`, a context-menu `MenuItem`, and so on.

## Use Cases

1. **UC1 — Copy static text on click, with in-button feedback** —
   `Clipboard.onClick(button).writeText(link, onCopied, onError)`. The
   server-side `onCopied` callback sets the button label to "Copied"
   and adds a check icon; a one-second `setTimeout` on the client
   dispatches a custom DOM event that a server-side listener uses to
   revert the button. Demonstrates pairing the clipboard outcome
   callback with a server-driven UI flash, without `@Push`.
2. **UC2 — Copy current value of a component** —
   `Clipboard.onClick(button).writeText(textField, …)`. The source
   component's value is read client-side at click time, so editing the
   field after render still works.
3. **UC3 — Copy rich content (HTML + plain text)** —
   `Clipboard.onClick(button).write(ClipboardContent.create().text(…).html(…))`.
   Rich destinations get the HTML; plain destinations get the
   plain-text fallback.
4. **UC4 — Copy image** —
   `Clipboard.onClick(button).writeImage(image, onCopied, onError)`.
   The source component's root `<img>` is rasterised to PNG on the
   client and written to the clipboard inside the click handler.
5. **UC5 — Paste a table from a spreadsheet** —
   `Clipboard.onPaste(div, event -> …)`. Reads the HTML branch of the
   paste (a `<table>`) when available; falls back to TSV from the plain
   text branch.
6. **UC6 — Copy via context menu** —
   `Clipboard.onClick(menuItem).writeText(…)` on a `ContextMenu` item,
   demonstrating that the same path works for any `ClickNotifier`.
7. **UC7 — Paste images and files** —
   `Clipboard.onFilePaste(div, PasteFileHandler.session()…)`. Each
   pasted file is uploaded via Flow's standard upload mechanism;
   `onStart` / `onFile` / `onComplete` give the application
   paste-aware lifecycle hooks for progress reporting.
8. **UC8 — A copy button in every grid row** — the use case from the
   [forum thread](https://vaadin.com/forum/t/clipboard-copy/164697/11):
   a copy button next to the value it copies, in every row of a 500-row
   `Grid`. A `ComponentRenderer` builds the cell and each row's button
   binds its own value as a literal —
   `Clipboard.onClick(button).writeText(customer.email())`. The grid
   only materialises the rows it renders, so the buttons follow the
   viewport rather than the row count; what does scale with scrolling is
   one clipboard binding per rendered row, which is the cost written up
   in [API-GAPS.md](API-GAPS.md).

The remaining PRD item (availability signal) is omitted because the
underlying API is still not part of this Flow branch — see
[API-GAPS.md](API-GAPS.md).

## Running the Application

1. **Prerequisites**: Java 25+, Maven 3.9+
2. **Run**: `./mvnw` (defaults to `spring-boot:run`)
3. **Access**: <http://localhost:8080>

For production builds, use `./mvnw package`.

## Technical Stack

- **Vaadin 25.2-SNAPSHOT**
- **Spring Boot 4.0.5**
- **Java 25**
- **Maven**

## Browser Notes

- The Clipboard API only works in **secure contexts** (HTTPS or
  `localhost`); over plain HTTP the operations fail and the `onError`
  callback is invoked.
- Because the write is performed by the click handler installed on the
  source component, the user gesture is always preserved — no
  Firefox/Safari gesture-timeout issues.
