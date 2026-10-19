# Clipboard — use cases

A standalone Spring Boot demo of the Clipboard API in Vaadin Flow,
`com.vaadin.flow.component.clipboard.Clipboard` (resolved from the parent
pom's `vaadin.version` property). Each view copies to or pastes from the
clipboard in one realistic scenario. Where the API falls short is recorded in
[API-GAPS.md](API-GAPS.md).

## API at a glance

```java
// Copy: the write runs in the browser's click handler, while the user
// gesture is still valid. The source can be any ClickNotifier component.
Clipboard.onClick(button).writeText("hello");
Clipboard.onClick(button).writeText(textField); // value read at click time
Clipboard.onClick(button).writeHtml("<p>hello</p>");
Clipboard.onClick(button).writeImage(image);    // the <img>, copied as PNG
Clipboard.onClick(button).write(
        ClipboardContent.create().text("hello").html("<p>hello</p>"));

// Each write has an overload that reports the browser's answer.
Clipboard.onClick(button).writeText("hello",
        copied -> Notification.show("Copied " + copied),
        error -> Notification.show("Copy failed: " + error.message()));

// Copy from a client-rendered template, e.g. a grid row: the action is bound
// once and copies the first argument the template calls it with. The
// callbacks are shared too; onCopied tells the rows apart by the copied text.
// Not released yet (vaadin/flow#25366, vaadin/flow-components#10470).
LitRenderer.<Customer> of(
        "<button @click=${() => copy(item.email)}>Copy</button>")
        .withProperty("email", Customer::email)
        .withClientAction("copy", Clipboard.writeText(
                copied -> Notification.show("Copied " + copied),
                error -> Notification.show("Copy failed: " + error.message())));

// Paste: text/plain and text/html of a paste on a focused element.
Clipboard.onPaste(div, event -> {
    if (event.hasHtml()) {
        handleHtml(event.getHtml());
    } else if (event.hasText()) {
        handleText(event.getText());
    }
});

// File paste: each pasted file is uploaded to the server.
Clipboard.onFilePaste(div, PasteFileHandler.batch()
        .onStart(start -> showProgress(start.totalFiles()))
        .onFile(file -> render(file))
        .onComplete(complete -> hideProgress())
        .build());
```

The `onCopied` callback receives the copied text (`writeImage` takes a
`SerializableRunnable` instead), and `onError` receives the browser's error
with its `name()` and `message()`.

## Use cases

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Copy static text | `Clipboard.onClick(button).writeText(link, onCopied, onError)` copies a fixed link. `onCopied` shows "Copied" with a check icon on the button, and `UI#triggerAfter` sets the label back after one second, without a server-side scheduler. |
| UC2 | Copy a field's value | `Clipboard.onClick(button).writeText(textField, …)` copies what the field holds when the user clicks. The browser reads the value, so an edit the server has not seen yet is copied too. |
| UC3 | Copy rich content | `Clipboard.onClick(button).write(ClipboardContent.create().text(…).html(…))` writes HTML and a plain-text fallback in one operation. Rich editors get the HTML, plain-text destinations get the text. |
| UC4 | Copy image | `Clipboard.onClick(button).writeImage(image, onCopied, onError)` draws the `<img>` onto a canvas in the browser and copies it as PNG. |
| UC5 | Paste a table | `Clipboard.onPaste(div, …)` reads cells copied from the embedded Spreadsheet, Excel or Google Sheets into a grid. It uses the HTML `<table>` when there is one and falls back to tab-separated text. |
| UC6 | Copy from a context menu | `Clipboard.onClick(menuItem).writeText(…)` on a `ContextMenu` item: a `MenuItem` is a `ClickNotifier`, so it works as a button does. |
| UC7 | Paste files | `Clipboard.onFilePaste(div, PasteFileHandler.batch()…)` uploads each pasted file. `onStart`, `onFile` and `onComplete` drive a progress bar, a file log and image previews. |
| UC8 | Copy from a grid | The use case from the [forum thread](https://vaadin.com/forum/t/clipboard-copy/164697/11): a copy button next to the email in every row of a 500-row `Grid`. A `LitRenderer` renders the cell in the browser, and its template calls `copy(item.email)`, a client action bound once for the whole column with `withClientAction("copy", Clipboard.writeText(onCopied, onError))`, which also shows a "Copied …" notification. No row has a server-side component or binding of its own. The API is not released yet (vaadin/flow#25366, vaadin/flow-components#10470). |

A use case that checks whether the clipboard can be used at all is missing,
because the API does not expose that yet ([API-GAPS.md #5](API-GAPS.md)).

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl clipboard spring-boot:run
```

Open <http://localhost:8080/>.

## Browser notes

- The Clipboard API only works in secure contexts (HTTPS or `localhost`).
  Over plain HTTP a copy fails and the `onError` callback runs.
- The write runs in the click handler of the source component, so the user
  gesture is always still valid when the browser writes to the clipboard.
- A paste only reaches an element that has focus. The drop zones of UC5 and
  UC7 are focusable `Div`s: click them before you paste.
