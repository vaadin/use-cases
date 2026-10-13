# API Gaps

Every module records in `API-GAPS.md` where the feature's API made a use case
impossible, awkward or surprising. The file is as much a deliverable as the
views: it is what the people building the feature read.

## An entry

```markdown
## <N>. <Short title of the gap>

**Where it bites:** UC<N> / <Name>View.java
**Symptom:** what the use case could not do, or had to work around.
**Workaround used:** e.g. a helper in `MissingAPI`, an `executeJs` call.
**Suggested API:** the signature or behaviour that would remove the gap.
Upstream: https://github.com/vaadin/<repo>/issues/<n>
```

- Number the entries, so views, Javadoc and the README can refer to them
  (`API-GAPS.md #8`).
- Ground each entry in the feature's source: name the class or method that
  falls short, not only the symptom.
- Link the upstream issue. When none exists, open one in the feature's
  repository and link it, so the gap gets fixed and not only documented.
- Gap classes worth checking for every feature: no test simulator for the
  browserless tests, a missing signal-based binding where the rest of Flow has
  one, a one-shot API for a naturally continuous feature (or the other way
  round), support or permission state not exposed, no server-side feature
  detection, an unclear push or `UI.access` story.

## Keeping it current

A gap that has been fixed upstream is wrong information. When a fix lands:

- remove the entry. If part of the gap remains, rewrite the entry so it
  describes only what is still missing;
- remove the workaround and use the new API;
- update every place that repeats the gap: view text, class Javadoc, README
  rows.

Some older `API-GAPS.md` files still keep entries marked "closed"; remove
them when you next touch the module.

Removing an entry renumbers nothing: keep the numbers of the remaining
entries, so references to them stay valid.

When a rename or refactoring changes a name an entry refers to (a meter, a
class, a route), update the entry in the same pull request.

## `MissingAPI`

A workaround that a view needs until the real API exists lives in the module's
`com.example.MissingAPI` class, or next to the one use case that needs it. Its
Javadoc names the upstream issue and says how to migrate once the API lands, so
it can be deleted without detective work.
