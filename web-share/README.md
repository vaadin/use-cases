# Web Share API — use cases

A standalone Spring Boot demo of the `com.vaadin.flow.component.webshare`
API in Vaadin Flow 25.2: `WebShare.onClick(button).share(ShareContent...)`
binds a share to a user gesture, and `WebShare.supportSignal()` reports
whether `navigator.share` is available. Each view exercises one realistic
Web Share scenario.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Share this page | Single Share button reflecting `WebShare.supportSignal()`; hands the current URL + title to the native share sheet. |
| UC2 | Copy-link fallback | Signal-driven swap between native Share button and Copy-link button for browsers without `navigator.share`. |
| UC3 | Share a custom message | Form lets the user fill any of title/text/url, bound live to the share via `ShareContent.create().title(field)…`, with a JSON preview. |
| UC4 | Per-item share in a list | A feed of three articles, each row with its own Share icon bound to that row's payload. |
| UC5 | Share with completion feedback | Uses the observed `share(content, onShared, onError)` form and surfaces success/cancel/error in a log. |
| UC6 | Share an invite link | Generates a fresh join code, builds an invite URL, then shares it. |

## Run

```
cd web-share
mvn spring-boot:run
```

Open <http://localhost:8080/>. Use a mobile browser (or recent
Safari/Edge) for the share sheet to actually appear; desktop Firefox is
the easiest browser to verify the fallback path in UC2.

## API gaps

See [API-GAPS.md](API-GAPS.md) for everything we wanted but the current
API doesn't expose (file sharing / Web Share Level 2, `navigator.canShare`
pre-check, browserless test simulator, SSR-time feature detection, …).
