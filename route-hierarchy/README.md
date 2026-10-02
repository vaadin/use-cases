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

UC5, UC7 and UC8 are not breadcrumbs: they consume the same route-hierarchy API
directly — `UpLink` calls `RouteConfiguration.getRouteParent` for a single
up-one-level control, `SitemapView` uses `getRouteHierarchy` as a
graph-builder, and `HierarchicalMenuView` renders
`MenuConfiguration.getMenuEntriesTree()` as a nested `SideNav`. UC5 and UC7
label their crumbs with `Router.resolvePageTitle(class, parameters)`, which
resolves a title without instantiating the view (UC8 gets its labels from
`MenuEntry.title()`). No view here imports `internal`; see
[`API-GAPS.md`](API-GAPS.md).

UC9 is where the two hierarchies come apart: `@Menu(parent = ...)` regroups an
entry in the menu tree only, leaving the route — and therefore the breadcrumb —
untouched. Use `@RouteParent` when a page really does live under another page,
and `@Menu(parent = ...)` when only the navigation menu should group it that
way.

The hierarchical menu and `@Menu(parent = ...)` have not landed in the baseline
Flow version yet, so this module pins `flow.version` to
`25.3.hierarchical-menu-SNAPSHOT` (see [`pom.xml`](pom.xml)).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | URL-prefix trail | `uc1` → `uc1/electronics` → `uc1/electronics/laptops` with no annotations; the trail comes purely from `getRouteHierarchy`'s URL-prefix fallback. |
| UC2 | `@RouteParent` override | A detail page at `order-detail/:orderId` whose URL shares no prefix with its conceptual parent `uc2`; `@RouteParent(OrdersView.class)` is what links them. |
| UC3 | Dynamic leaf label | A profile at `uc3/:userId` implementing `HasDynamicTitle`; the current crumb shows the resolved person name, not the static `@PageTitle`. |
| UC4 | Parameter-preserving links | A four-level parameterised hierarchy where each ancestor link keeps the live `:projectId` (and only the parameters its own template needs). |
| UC5 | Up-one-level button | A single "↑ Up to <parent>" control built from `getRouteParent(...)`, hidden at the hierarchy root. |
| UC6 | Layout-wide auto breadcrumbs | One `Breadcrumbs` component in a parent layout, shared by every child view; in `ROUTER` mode it rebuilds on every navigation with no `AfterNavigationObserver` and no manual seeding. |
| UC7 | Route-tree sitemap | `getRouteHierarchy` used as a graph-builder: leaf routes from across the demo are expanded and merged into a nested sitemap tree. |
| UC8 | Hierarchical menu | `MenuConfiguration.getMenuEntriesTree()` renders the same `@Menu` set the flat drawer shows, nested by route hierarchy via `MenuEntry.children()`. |
| UC9 | Menu-only nesting | `@Menu(parent = ...)` nests a top-level route (`/forecast`) under Reports in the menu tree while its route hierarchy — and its breadcrumb — leave it at the top level. |

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
