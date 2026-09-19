# Invoicing — use cases

A standalone Spring Boot demo of the invoicing side of a business
application: turning data into a document, handing that document to the
customer's browser, checking it first, running a month of them at once, and
knowing that it arrived.

Flow 25's `DownloadHandler` does the delivery well, so this module is mostly
about what surrounds it. The invoice PDF itself is application code
(`com.example.pdf.InvoicePdf`, plain Apache PDFBox), and
[API-GAPS.md](API-GAPS.md) records the six places where Vaadin stopped
helping — most usefully the one the community already filed,
[vaadin/flow#21929](https://github.com/vaadin/flow/issues/21929).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Download the invoice | A PDF generated per row and delivered through `DownloadHandler.fromInputStream`, with the invoice number as the file name. |
| UC2 | Open it in a new tab | Inline delivery via `AttachmentType.INLINE`, and the hidden-anchor trick the server needs because a handler has no URL it can pass to `Page#open`. |
| UC3 | Check it before sending | The real generated document in an `IFrame`, beside the application's own rendering of the same invoice. |
| UC4 | The monthly billing run | A multi-select billing run rendered as one document with continuous page numbers, plus transfer progress. |
| UC5 | Know that it arrived | An invoice marked sent only when the transfer completed, in an application-scoped book every session sees. |

This module is about producing and delivering a *document*. Printing from the
browser is the `printing` module; exporting a Grid to CSV or Excel is
[vaadin/platform#7196](https://github.com/vaadin/platform/issues/7196) and the
`grid-export` module.

## Run

```
cd invoicing
mvn spring-boot:run
```

Open <http://localhost:8080/>.

## Tests

```
./mvnw -pl invoicing -am test -DskipFrontend=true
```

One browserless test per use case, plus `InvoicePdfTest`, which renders the
documents and reads them back with PDFBox: the invoice number, the customer,
the letterhead, every line, the total the UI shows, and "Page 3 of 3" across a
three-invoice batch. UC5 also covers the cross-session case — a delivery
recorded in one session is visible in the next.

A download cannot be driven from a browserless test (see API-GAPS.md), so the
view tests assert the parts that stay on the server: that the handler resolves
to a URL, that the preview regenerates when another invoice is chosen, and
that the delivery book is what the badges read from.

The four delivery paths were also checked against the running application in
headless Chromium, by fetching what each link and the preview frame point at:
UC1 answers `attachment; filename="invoice-2026-0001.pdf"`, UC2 and UC3
`inline; filename="invoice-2026-0001.pdf"`, UC4
`attachment; filename="billing-run.pdf"`, and all four return a real
`%PDF-` document.
