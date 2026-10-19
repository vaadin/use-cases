# Reporting — API gaps

Where Vaadin has no API for a reporting use case, or makes one awkward
enough to need a workaround.

Flow 25's `DownloadHandler` makes *sending* a generated file easy: UC1 is six
lines. What is still missing is around the transfer: the document itself, a
URL the server can use, a PDF viewer, and a completion callback that reaches
the browser.

## 1. No help for producing the document

**Where it bites:** UC1 / `DownloadInvoiceView.java`, and every other use case
**Symptom:** an invoice is the most ordinary business document: letterhead,
addressee, lines, VAT, total, page numbers. Vaadin has nothing for producing
one. `com.example.pdf.InvoicePdf` is 200 lines of PDFBox that place text at
fixed millimetre offsets. Every application that needs an invoice writes this
again.
**Workaround used:** Apache PDFBox, with the column layout done by hand.
**Suggested API:** not necessarily a reporting engine, but a documented
recipe, and one shared way to turn a Grid or a data set into rows that a
document writer can read. This overlaps with the Grid export API
([vaadin/platform#7196](https://github.com/vaadin/platform/issues/7196)).
Today an export, a print rendering and a PDF writer each read the same values
in their own way.

## 2. A `DownloadHandler` has no URL the server can use

**Where it bites:** opening a generated document from server code, next to
UC2 / `OpenInNewTabView.java`
**Symptom:** UC2's button opens the document with
`OpenInNewTab.onClick(component, DownloadHandler)`, but that only works for a
click. `Page#open(String)` needs a URL, and a `DownloadHandler` has no URL
until it is bound to an element. Only `Anchor#getHref()` gives it back. So
nothing helps when the server decides later to open the document: after
saving a draft, after a confirm dialog, or from a context menu item. Nor
anything else that needs the URL: a mail body, a QR code.
**Workaround used:** none in this module. An application would keep a hidden
`Anchor` with `target="_blank"` and click it through JavaScript.
**Suggested API:** `Page#open(DownloadHandler)`, or a way to register a
handler on the UI and read back its URL.
Upstream: https://github.com/vaadin/flow/issues/21929

## 3. No way to show a PDF inside the application

**Where it bites:** UC3 / `PreviewInvoiceView.java`
**Symptom:** `IFrame#setSrc(DownloadHandler)` shows the generated file, but
in the browser's own viewer. The application cannot open it at a page,
scroll to a line, or react when the user turns a page. Mobile browsers that
download PDFs instead of showing them show nothing at all.
**Workaround used:** an `IFrame` that points at the inline handler, and next
to it the application's own HTML rendering of the same invoice.
**Suggested API:** a PDF viewer component with a page API and an event when
the user changes the page.
Upstream: https://github.com/vaadin/web-components/issues/7669

## 4. Progress covers only the transfer, not the generation

**Where it bites:** UC4 / `BillingRunView.java`
**Symptom:** `DownloadHandler.fromInputStream` suggests the response is
streamed, and for a CSV it is. A PDF writes its cross-reference table last,
so the whole document must exist before the first byte goes out. A month of
invoices sits in memory, twice if the bytes are copied into a
`ByteArrayInputStream`. `TransferProgressListener#onProgress` reports only
the copy, not the generation, so the user waits through the slow part with no
progress at all.
**Workaround used:** render the batch eagerly and accept the memory. The
progress bar shows the transfer only.
**Suggested API:** Vaadin can't make a PDF streamable, but the download API
could report a "preparing" phase as well as "transferring", for example an
`onPreparing` callback on `TransferProgressListener`.

## 5. Transfer callbacks reach the browser only with `@Push`, silently

**Where it bites:** UC4 / `BillingRunView.java`, UC5 / `DeliveryReceiptView.java`
**Symptom:** "mark the invoice as sent" must happen when the transfer has
completed, not when the link was clicked, and `whenComplete` is the only
place that knows. `TransferProgressAwareHandler` already runs `whenComplete`
and `onProgress` through `UI#access`, so they may change components. But the
callback runs while the response to a *file* request is being written, so
there is no client round trip to carry the changes. Without `@Push` the
progress bar stays at zero and the badge stays "Not sent" until some other
interaction happens. Nothing fails and nothing is logged. The need for push is
only in the Javadoc of `TransferProgressAwareHandler`.
**Workaround used:** `@Push` on `Application`.
**Suggested API:** log a warning when a transfer callback changes a UI that
has no push enabled, or say it where an application meets it, on
`DownloadHandler` itself.
Upstream: https://github.com/vaadin/flow/issues/13283 (the general case: a
`UI#access` with neither push nor polling enabled fails silently)

## 6. Browserless tests can not check the file name or content type

**Where it bites:** every view test of the module
**Symptom:** `AnchorTester#download(OutputStream)` now runs the handler
behind an `Anchor` and returns the bytes. It does not give back the file
name, the content type or the `Content-Disposition` (inline or attachment)
that the handler sets, and there is no tester for an `IFrame` source or for
`Download.onClick` / `OpenInNewTab.onClick`. So a test can not check that the
link delivers the right file under the right name, which is the main
behaviour of this module.
**Workaround used:** the document generation is a plain class with its own
test (`InvoicePdfTest` reads the PDFs back with PDFBox). The view tests
download the anchors and read the PDF back, but for UC2's button and UC3's
frame they check only that a handler is bound.
**Suggested API:** a download result in the browserless test kit, e.g.
`test(anchor).download()` that returns the bytes, the file name, the content
type and the disposition, and the same for an `IFrame` and for components
bound with `Download` or `OpenInNewTab`.
