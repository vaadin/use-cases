# API gaps — route hierarchy + page titles

Rough edges hit while building the use cases on the route-hierarchy and
page-title API.

**No gaps are open in the Flow API itself.** Everything these use cases need is
public, supported API, and no view in this module imports `internal`:

- `com.vaadin.flow.router` — the `@RouteParent` annotation (with a dynamic
  `resolver()`), `RouteParentResolver` / `RouteParentContext`, and
  `RouteReference` (a navigation target paired with its `RouteParameters`).
- `RouteConfiguration#getRouteParent` / `#getRouteHierarchy` resolve the
  hierarchy — UC5's `UpLink` and UC7's `SitemapView` build their controls
  straight on those.
- `Router#resolvePageTitle(Class, RouteParameters)` (@since 25.2) resolves an
  ancestor's title without instantiating the view, honouring `@PageTitle`,
  `@DynamicPageTitle` and `PageTitleGenerator`. It returns an empty `Optional`
  when the target declares no title, so a caller picks its own fallback;
  the internal `MenuRegistry#getTitle` is just this call with
  `getSimpleName()` as that fallback.
- `MenuConfiguration#getMenuEntriesTree()` + `MenuEntry#children()` give the
  menu as a tree (UC8), which is also how `MainLayout` keeps the flat main
  navigation to top-level entries — depth comes from the tree rather than from
  re-deriving it out of URL paths.

The one gap left is in the **test** API, not the product API: `BreadcrumbsTester`
exposes no per-crumb path accessor, so UC4 has to assert
parameter-preserving ancestor links through `BreadcrumbsItem#getPath()` instead
of through the tester. See the testing notes in [`README.md`](README.md) for
the details and the suggested `getItemPaths()` addition.
