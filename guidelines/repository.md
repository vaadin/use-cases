# Repository

## Layout

The repository is a Maven multi-module build. The parent `pom.xml` pins Spring
Boot and the Vaadin, Flow, Flow Components and browserless-test versions
(usually the current snapshot), and configures Spotless and the nullability
plugin for every module.

- `common/` — shared code every app depends on: `BaseMainLayout` (side
  navigation, application selector, "View source" and "Feedback"),
  `BaseHomeView` (home page cards), `@UseCaseDescription`, `AppCatalog` (the
  list of all apps), `OpenGraphTags` (link previews) and `BuiltWith` (the
  "how it is built" section of a view).
- `<module>/` — one standalone Spring Boot app per feature area (`clipboard`,
  `observability`, `multimedia`, …). Each is built, tested and deployed on its
  own.

A module looks like this:

```
<module>/
├── API-GAPS.md
├── Dockerfile
├── README.md
├── fly.toml
├── package.json, package-lock.json, vite.config.ts, tsconfig.json, types.d.ts
├── pom.xml                         # artifactId <module>-use-cases
└── src/
    ├── main/java/com/example/
    │   ├── Application.java        # @SpringBootApplication + AppShellConfigurator
    │   ├── MissingAPI.java         # optional, workarounds for missing API
    │   ├── home/HomeView.java      # extends BaseHomeView, route ""
    │   ├── views/MainLayout.java   # extends BaseMainLayout
    │   └── uc<N>/<Name>View.java   # one package per use case
    ├── main/resources/
    │   ├── application.properties
    │   └── META-INF/resources/     # styles.css, uc<N>.css, og-image.jpg
    └── test/java/com/example/uc<N>/<Name>ViewTest.java
```

`page-visibility/` is the cleanest reference for a small module. `signals/` is
older and uses a different package scheme (`usecase<NN>`, `muc<NN>`); do not
copy its structure into new modules.

Code that only one use case needs lives in that use case's package. Code shared
by several use cases of a module lives in a module-level package
(`com.example.views`, or a package named after the module's story). Code shared
by several modules lives in `common`.

## Adding a module

1. Create `<module>/` by copying `page-visibility/` and renaming: `artifactId`
   `<module>-use-cases`, the module name in the `Dockerfile`, `app =
   '<module>-cases'` in `fly.toml`, the titles in `MainLayout`, `HomeView` and
   `@PWA`.
2. Add the module to the root `pom.xml` `<modules>`.
3. Add the app to `AppCatalog.APPS`, in alphabetical order, with the Fly URL
   `https://<fly app>.fly.dev/`. `AppCatalogTest` fails when a module is
   missing or its URL does not match `fly.toml`.
4. Add an `og-image.jpg` for link previews.
5. Write the `README.md` table and an `API-GAPS.md`.

When something changes for every module on `main` (the `Dockerfile`, the
Vaadin version, a new requirement of `AppCatalogTest`), apply it to the new
module too when you merge `main` into a long-lived branch.

## CI and deployment

- `build.yml` builds every module separately with `./mvnw -pl <module> -am
  install`. A change in `common` therefore has to keep every module building
  and passing its tests.
- `fly-deploy.yml` deploys every directory holding a `fly.toml` when `main`
  changes, and every app when `common` or the parent pom changes.
- `pr-preview.yml` deploys preview apps (`<app>-pr-<number>.fly.dev`) for a
  pull request with the `deploy preview` label. When a pull request changes
  only shared files, one app is deployed so the change can still be tried.
  Anything a feature needs at runtime (secrets, environment variables) has to
  be staged in both deploy workflows.

## Dependencies

Feature APIs come from the Vaadin snapshot pinned in the parent pom. A pull
request may build against a branch snapshot while the Flow change it needs is
under review. Every view that uses the unreleased API must then show the
unreleased-API banner: annotate the view class with
`@UnreleasedApi("<pull request URL>")` and `BaseMainLayout` shows a banner
that links to the pull request. Views of the module that use only released
API get no annotation. Once that change is merged, switch back to the mainline
snapshot, remove the annotations and remove the workarounds the change makes
unnecessary.

### Testing against a local Flow or browserless-test build

To try a use case against a Flow or browserless-test change that exists only
on your machine, keep the local build in its own Maven repository. A plain
`mvn install` puts it in `~/.m2/repository` as the same `25.4-SNAPSHOT` that
the next online build replaces with the published snapshot, and a build with
`-o` alone fails on artifacts that were never downloaded.

```bash
# 1. Once, online: download everything the module needs into ~/.m2
./mvnw -pl <module> -am test

# 2. Install the upstream change into ~/m2-local, reading the rest from ~/.m2
(cd ../flow && mvn -pl flow-server -am install -DskipTests \
   -Dmaven.repo.local=$HOME/m2-local -Dmaven.repo.local.tail=$HOME/.m2/repository)

# 3. Build the use case offline: the local build wins, nothing is replaced
./mvnw -o -pl <module> -am test \
   -Dmaven.repo.local=$HOME/m2-local -Dmaven.repo.local.tail=$HOME/.m2/repository
```

Repeat steps 2 and 3 after each upstream change. The upstream branch must
have the version the parent pom pins (`flow.version`,
`browserless-test.version`). Delete `~/m2-local` when you are done.

The Vaadin build can regenerate the module's `package.json` and
`package-lock.json`. Restore them with `git checkout <module>/package.json
<module>/package-lock.json` before committing (see
[Generated files](#generated-files)).

After the parent pom moves to a newer snapshot, an app can start with a
frontend bundle built against the old client. The browser then shows
client-side errors from Vaadin internals, such as `e.Atmosphere is not a
constructor`, that do not say the bundle is stale. Before looking for another
cause, delete the module's `target/`, `src/main/bundles/` and
`src/main/frontend/generated/` and start the app again.

When a use case needs a standard format or algorithm (source maps, an image
hash, PDF output), look for a small, maintained library first. Compare size
and transitive dependencies, and explain the choice in the pull request. A
dependency that lives outside Maven Central gets its repository declared in the
module pom only, releases only.

## Generated files

Never commit build output: `target/`, `node_modules/`, `src/main/bundles/`,
`src/main/frontend/generated/`.

`package.json`, `package-lock.json` and `vite.config.ts` are generated by the
Vaadin build but committed. Regenerate them for all modules when the Vaadin
version changes, in the same pull request as the version change. Keep
regenerated copies out of unrelated pull requests, and do not delete them from
a single module.

## Code style

- Run `./mvnw spotless:apply` before committing (Eclipse formatter, fixed
  import order, no wildcard imports).
- Code is null-marked through the nullability plugin; annotate nullable
  fields, parameters and return values with JSpecify `@Nullable`.
- Never build JSON by string concatenation; use Jackson.
