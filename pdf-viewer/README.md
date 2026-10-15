# PDF Viewer — use cases

A standalone Spring Boot demo of the prototype `<vaadin-pdf-viewer>` web
component in a Vaadin Flow application. Each view shows one thing a business
application does with PDF documents: invoices, terms, receipts, manuals,
contracts, labels, handbooks and forms, all generated on the fly with PDFBox.

There is no Flow component for the viewer yet, so the module wraps the web
component itself in `com.example.pdfviewer.PdfViewer`. The web components come
from a pkg.pr.new preview of the web components branch that adds the viewer,
through the matching Flow Components pull request snapshot set in `pom.xml`
(`flow-components.version`). The viewer is behind the `pdfViewerComponent`
feature flag, which `PdfViewerFeatureFlagProvider` registers and
`vaadin-featureflags.properties` turns on.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Check before sending | The generated invoice inside the app; "Send to customer" turns on only once `document-load` reports the PDF has rendered. |
| UC2 | Read before accepting | Terms of service; the accept checkbox turns on once `page-changed` has reached the last page. |
| UC3 | Document library | A file list with a preview; a cut-off upload, a deleted file and a password-protected file each get their own explanation from the `document-error` reason. |
| UC4 | Open the manual at a page | Help questions link into a user guide at `uc4?page=N`; scrolling writes the page back into the address, and a page past the end is corrected. |
| UC5 | Check against the receipt | An expense claim next to its receipt, fitted with `Zoom.PAGE_FIT` and a collapsed toolbar; stacks on a phone. |
| UC6 | Print a shipping label | "Print label" on an order row calls `print()`, waiting for `document-load` when the label is not shown yet. |
| UC7 | Jump to a clause | A review checklist opens a contract at the page with each answer, found with PDFBox text extraction on the server. |
| UC8 | Viewer in your language | The viewer's 38 texts in English, Finnish or German, starting from the browser's language. |
| UC9 | Approve in the toolbar | "Approve" and "Reject" buttons with tooltips in the viewer's toolbar; each decision opens the next waiting purchase order. |
| UC10 | Show a document once | A confidential briefing without download or print buttons, sent by the server only for the first request; a reload says it was opened already. |
| UC11 | Follow the contents page | A handbook whose first page links to its chapters; the view shows the current chapter and has its own "Back to contents". |
| UC12 | Fill in a PDF form | Answers from Vaadin fields are written into the PDF form's fields with PDFBox on the server, flattened, and the filled copy is shown. |
| UC13 | Turn a sideways scan | "Turn left" and "Turn right" in the toolbar turn the current page on the server with PDFBox, turning it in the file itself (kept while the view is open). |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl pdf-viewer spring-boot:run
```

Open <http://localhost:8080/>. The first start installs the preview packages
from pkg.pr.new, so it needs network access.

## API gaps

See [API-GAPS.md](API-GAPS.md): the missing Flow component and feature flag,
a preview package pinned by hand,
no way to search or highlight from code, `print()` without an outcome,
password-protected files, fixed toolbar buttons without a slot for the app's
own, no translations, no browserless tester, no page signal, an out-of-range
page that is not corrected, form fields that can not be filled in, no "view
only" mode, no way back after following a link, no signatures, no rotation,
and no header title apart from the file name.
