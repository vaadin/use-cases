# Web Share API — use cases

A standalone Spring Boot demo of the `com.vaadin.flow.component.webshare`
API in Vaadin Flow (resolved from the parent pom's `vaadin.version`
property): `WebShare.onClick(button).share(ShareContent...)` binds a share to
a click, and `WebShare.supportSignal()` reports whether `navigator.share` is
available. Each view shows one realistic Web Share scenario. Where the API
falls short is recorded in [API-GAPS.md](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Share this page | One Share button, enabled only while `WebShare.supportSignal()` is `SUPPORTED`, that shares the title and the address of the page you are on. |
| UC2 | Copy-link fallback | The support signal swaps a native Share button for a Copy link button (`Clipboard.onClick`) in browsers without `navigator.share`. |
| UC3 | Share a custom message | A form whose title, text and URL fields are bound to the share with `ShareContent.create().title(field)…`, with a JSON preview of what is shared. |
| UC4 | Per-item share | A feed of three articles, each row with its own Share button bound to that row's title and URL. |
| UC5 | Completion feedback | The observed `share(content, onShared, onError)` form writes success, cancel and failure to a log. Separate "Simulate" buttons stand in where there is no share sheet (API-GAPS.md #3). |
| UC6 | Share invite link | Generates a new join code on the server, shows the invite link and shares it. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl web-share spring-boot:run
```

Open <http://localhost:8080/>. Use a mobile browser, Safari or Edge to see
the share sheet. Desktop Firefox has no `navigator.share`, so it is the
easiest browser to try the fallback in UC2.

## API gaps

See [API-GAPS.md](API-GAPS.md): no file sharing (#1), no
`navigator.canShare` check for one payload (#2), no browserless test
simulator (#3), no signal as share content (#4) and no way to leave out an
empty field (#5).
