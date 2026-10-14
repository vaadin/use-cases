# API gaps — route hierarchy + page titles

Rough edges hit while building the use cases on the route-hierarchy and
page-title API.

**No gaps are open in the Flow API itself.** Everything these use cases need is
public, supported API, and no view in this module imports `internal`:

- `com.vaadin.flow.router` — the `@RouteParent` annotation (with a dynamic
  `resolver()`), `RouteParentResolver` / `RouteParentContext`, `RouteReference`
  (a navigation target paired with its `RouteParameters`), and instance-free
  dynamic titles via `@DynamicPageTitle` + `PageTitleGenerator` /
  `PageTitleContext`.
- `RouteConfiguration#getRouteParent` / `#getRouteHierarchy` resolve the
  hierarchy — UC5's `UpLink` and UC7's `SitemapView` build their controls
  straight on those.
- `Router#resolvePageTitle(Class, RouteParameters)` (@since 25.2) resolves an
  ancestor's title without instantiating the view, honouring `@PageTitle`,
  `@DynamicPageTitle` and `PageTitleGenerator`. It returns an empty `Optional`
  when the target declares no title, so a caller picks its own fallback.

## Resolved: the resolution entry points were internal API

`UpLink` (UC5) and `SitemapView` (UC7) used to call
`com.vaadin.flow.router.internal.RouteUtil#getRouteParent` /
`#getRouteHierarchy` and `com.vaadin.flow.internal.menu.MenuRegistry#getTitle`
directly. Those are now promoted to `RouteConfiguration`
([flow#24550](https://github.com/vaadin/flow/pull/24550)) and
`Router#resolvePageTitle`
([flow#24647](https://github.com/vaadin/flow/pull/24647)), and both views use
the public methods.
