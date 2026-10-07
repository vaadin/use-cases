# Route Hierarchy — use cases

A standalone Spring Boot demo of the **Breadcrumbs** component (new in Vaadin
25.2) and the route-hierarchy API it builds on: the `@RouteParent` annotation
and the URL-prefix fallback that together place each route under its parent.

The Breadcrumbs component is still a preview feature, enabled here with the
`com.vaadin.experimental.breadcrumbsComponent` feature flag (see
`src/main/resources/vaadin-featureflags.properties`).

Most views just `add(new Breadcrumbs())`. In its default `ROUTER` mode the
component subscribes to the router and, on every navigation, walks the route
hierarchy (`@RouteParent` first, URL-prefix matching as the fallback) and labels
each crumb with the route's page title — including instance-free
`PageTitleGenerator`s, resolved without instantiating ancestor views. No
`BeforeEnterObserver`, no manual seeding.

UC5 and UC7 are not breadcrumbs: they consume the same route-hierarchy API
directly — `UpLink` calls `getRouteParent` for a single up-one-level control,
and `SitemapView` uses `getRouteHierarchy` as a graph-builder. Those still reach
for internal API; see [`API-GAPS.md`](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | URL-prefix trail | `uc1` → `uc1/electronics` → `uc1/electronics/laptops` with no annotations; the trail comes purely from `getRouteHierarchy`'s URL-prefix fallback. |
| UC2 | `@RouteParent` override | A detail page at `order-detail/:orderId` whose URL shares no prefix with its conceptual parent `uc2`; `@RouteParent(OrdersView.class)` is what links them. |
| UC3 | Dynamic leaf label | A profile at `uc3/:userId` implementing `HasDynamicTitle`; the current crumb shows the resolved person name, not the static `@PageTitle`. |
| UC4 | Parameter-preserving links | A four-level parameterised hierarchy where each ancestor link keeps the live `:projectId` (and only the parameters its own template needs). |
| UC5 | Up-one-level button | A single "↑ Up to <parent>" control built from `getRouteParent(...)`, hidden at the hierarchy root. |
| UC6 | Layout-wide auto breadcrumbs | One `Breadcrumbs` component in a parent layout, shared by every child view; in `ROUTER` mode it rebuilds on every navigation with no `AfterNavigationObserver` and no manual seeding. |
| UC7 | Route-tree sitemap | `getRouteHierarchy` used as a graph-builder: leaf routes from across the demo are expanded and merged into a nested sitemap tree. |

## Run

```
cd route-hierarchy
mvn spring-boot:run
```

Open <http://localhost:8080/>.

## Testing notes — what's missing in `BreadcrumbsTester`

The browserless tests drive the component through `BreadcrumbsTester`
(`browserless-test-shared`) via `test(breadcrumbs)`. Its current API covers
labels and navigation only:

- `List<String> getItemTexts()` — the ordered crumb labels
- `clickItem(String | int)` — navigate by clicking a crumb

That is enough for UC1–UC3 and UC6, which assert the **exact trail** with
`getItemTexts()` (in `ROUTER` mode the last item is the current page and the
only non-link, so the linked crumbs are simply "all but the last"). UC1 also
exercises `clickItem(...)` to verify a crumb navigates.

UC4 is the exception: it verifies that ancestor **links carry the right route
parameters** (`:projectId` is preserved, `:taskId` is not), which needs each
crumb's resolved `href` — something the tester does not expose. That one test
therefore drops to the component API (`BreadcrumbsItem#getPath()`).

**What would close the gap (browserless project):** add a path accessor to
`BreadcrumbsTester`, e.g.

- `List<String> getItemPaths()` — the resolved `href` per crumb (empty for the
  current page), so parameter-preserving links can be asserted through the
  tester.

(No "current item" accessor is needed: the current page is by definition the
last, path-less crumb.) With `getItemPaths()`, UC4 could use the tester alone
and no breadcrumb test would need `getChildren()` / `BreadcrumbsItem`.
