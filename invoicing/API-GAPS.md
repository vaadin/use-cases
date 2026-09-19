# Invoicing — API gaps discovered while building the demos

Places where Vaadin has no API for a genuine invoicing use case, or makes one
awkward enough to need a workaround. Each entry names the use case that
surfaced it.

The short version: Flow 25's `DownloadHandler`
([vaadin/platform#7469](https://github.com/vaadin/platform/issues/7469)) made
*sending* a generated file easy, and this module is the better for it — UC1 is
six lines. What is still missing is everything on either side of the
transfer: the document itself, the URL the handler ended up on, a way to show
a PDF inside the application, and a completion callback that an application
can safely act on.

---

## The document is entirely the application's problem

**Where it bit us:** uc1 / DownloadInvoiceView.java, and every other use case
**Symptom:** an invoice is the most ordinary business document there is —
letterhead, addressee, lines, VAT, total, page numbers — and Vaadin has
nothing for producing one. `com.example.pdf.InvoicePdf` is 200 lines of
PDFBox that positions text at hard-coded millimetre offsets, and every
application that needs an invoice writes it again.
**Workaround used:** PDFBox, plus a column layout by hand.
**Suggested API:** not necessarily a reporting engine, but the pieces the
platform already half-owns: a documented recipe, and — where it overlaps with
[vaadin/platform#7196](https://github.com/vaadin/platform/issues/7196) — one
shared way of turning a Grid or a data set into rows a document writer can
consume. Today the export API, a print rendering and a PDF writer would each
extract the same values three different ways.

## A `DownloadHandler` has no URL the server can use

**Where it bit us:** uc2 / OpenInNewTabView.java
**Symptom:** "open the invoice in a new tab" is a server-side decision — it
follows saving a draft, confirming a dialog, or a context menu item. But
`Page#open` takes a URL string, and a `DownloadHandler` has no URL until it
has been bound to an element; nothing gives it back afterwards except
`Anchor#getHref()`. The same gap blocks handing the document to anything that
needs a URL: a mail body, a QR code, a `window.open`.
**Workaround used:** keep a hidden `Anchor` with `target="_blank"` in the view
and click it from JavaScript (`MissingAPI#openInNewTab`).
**Suggested API:** `Page#open(DownloadHandler)`, or a way to register a
handler against the UI and read back its URL, as
[vaadin/flow#21929](https://github.com/vaadin/flow/issues/21929) asks for. The
issue notes this was a one-liner with `StreamResource` before 24.8; the new
API is better in every other respect, which makes this the one thing worth
adding back.

## There is no way to show a PDF inside the application

**Where it bit us:** uc3 / PreviewInvoiceView.java
**Symptom:** approving a document before it goes out means looking at the
document. `IFrame#setSrc(DownloadHandler)` gets the generated file on screen,
which is genuinely useful — but the viewer is the browser's. The application
cannot open it at a page, scroll to a line, react to the user paging through
it, or render anything at all on the mobile browsers that download PDFs
instead of displaying them.
**Workaround used:** an `IFrame` pointing at the inline handler, plus the
application's own HTML rendering of the same invoice beside it, which is what
a reviewer actually reads.
**Suggested API:** the PDF viewer component asked for in
[vaadin/web-components#7669](https://github.com/vaadin/web-components/issues/7669),
with a page API and an event when the user changes page.

## A PDF cannot be streamed, and nothing warns you

**Where it bit us:** uc4 / BillingRunView.java
**Symptom:** `DownloadHandler.fromInputStream` invites the belief that the
response is streamed, and for a CSV it is. A PDF's cross-reference table is
written last, so the whole document must exist before its first byte can go
out: a month of invoices is a month of invoices in memory, twice over if the
bytes are also copied into a `ByteArrayInputStream`. The transfer progress the
browser sees is therefore the *copy*, not the generation — the user waits
through the slow part with no progress at all.
**Workaround used:** render the batch eagerly and accept the memory; the
progress bar reflects the transfer only.
**Suggested API:** nothing Vaadin can fix in PDF, but the download API could
carry the distinction: a `DownloadHandler` that reports "preparing" as well as
"transferring", so an application can show the user the part that actually
takes the time.

## The completion callback is outside the UI

**Where it bit us:** uc4 / BillingRunView.java, uc5 / DeliveryReceiptView.java
**Symptom:** "mark the invoice as sent" must happen when the transfer
completed, not when the link was clicked, and `whenComplete` is the only place
that knows. It runs on the request thread that wrote the response, without the
UI lock, so every line of application code in it has to remember `UI#access`
— and a listener that forgets does nothing, silently, until the next
interaction. `onProgress` has the same shape.
[vaadin/flow-components#10075](https://github.com/vaadin/flow-components/issues/10075)
reports the same class of problem for `withResponseListener`.
**Workaround used:** both callbacks do their state change on the shared
service and then hop through `UI#access` for the repaint.
**Suggested API:** document the threading on `TransferProgressAwareHandler`,
and offer a variant that is delivered with the session locked — or at least
fail loudly when a callback touches a component without the lock.

## Nothing simulates a download in tests

**Where it bit us:** every view test in this module
**Symptom:** a `DownloadHandler` can only be exercised through a real HTTP
request: building a `DownloadEvent` by hand needs a `VaadinRequest`, a
`VaadinResponse` and a session. So a browserless test cannot assert that
clicking a link delivers the right bytes with the right file name, which is
the whole behaviour of this module.
**Workaround used:** keep the document generation in a plain class and test it
directly (`InvoicePdfTest` reads the generated PDFs back with PDFBox), and
test the views for the parts that stay on the server: the anchor resolves to a
URL, the preview regenerates, the delivery book is updated.
**Suggested API:** a download simulator in the browserless test kit —
`test(anchor).download()` returning the bytes, the file name and the content
type — matching the upload side, which can already be driven from a test.
