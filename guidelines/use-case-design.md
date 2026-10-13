# Use-Case Design

A use-case app is read by developers who want to know whether a Vaadin feature
solves their problem, and by the people building the feature, who want to know
where it falls short. Both read the running app first and the code second, if
at all.

## Choosing use cases

Start from what the feature is for: the real things an application would do
with it. List them in plain English ("Export the filtered and sorted grid to
Excel", not "call `GridExporter.export(...)`"), make sure each one is distinct
rather than a cosmetic variant of another, and that each fits in one view.

Then check what the current API supports. A use case that is fully supported
shows how to do it. One that is partly supported shows the workaround and
records the rest as a gap. One that is not supported at all is still worth an
`API-GAPS.md` entry: the gap is the finding, not a reason to drop the use case.

A module can grow over time. Name it after the area, not after its first
example (`reporting`, not `invoicing`), so later use cases have a home.

## Telling the story in the UI

Assume the reader has not cloned the repository and will not click "View
source". Each view therefore explains itself:

- An intro paragraph that says what problem the view shows and what to try.
- The example itself, with the feature's effect visible on screen (a badge
  that changes, a counter, a log of what the server saw).
- Where several steps build on each other, numbered steps that appear in
  order, each saying what to look at.
- Where the interesting part is how it is built, a short "how it is built"
  section on the page: the technique in one line, the few points that make it
  work, and the relevant source collapsed below it. Read the source shown on
  the page from the files packaged with the app, so it can not drift from the
  code that runs.

Keep it from becoming overwhelming: collapse code and detail by default, and
show one idea per view.

When a story needs a fake problem — a slow service, a failing call, a flood of
users — put the controls that cause it in a visibly separate area, so a reader
does not mistake them for the feature.

## Telling the truth

Everything the app says is a claim a reader will rely on: view text, step
instructions, README rows, Javadoc and `API-GAPS.md`. Before writing one, check
it against the feature's source, not against your expectation of it.

- If a view says the feature records "every failed or slow interaction with
  its stack frame", check that slow interactions carry a stack frame too.
- If a view tells the reader to do something and expect a result, make sure
  the result also appears in the default setup (e.g. without a license key in
  development), or tell the reader why it does not.
- If a value shown is application-wide rather than about this view, say so.
- When the feature changes, update every place that repeats the claim.

Remove code the reader can never reach: a styling branch that can not
apply, a method only a test calls, an option nobody asked for.

Do not show a broken example. If a media file can not be seeked in Safari,
serve it so it can. If a panel always shows the first record, bind it to the
selection.

## Independence

Each use case should work and read correctly no matter which use cases the
reader opened before. Scope numbers to the view — by route, tag or a dedicated
meter — instead of reading application-wide totals, unless the point of the
view is the application-wide picture, in which case say so.

Empty states should distinguish "nothing happened yet" from "the feature is
not active here", so the reader is never asked to retry something that can not
work.

## Naming and navigation

- `@PageTitle` and `@Menu` use `UC<N> — <short name>`. The short name must fit
  on one line in the side navigation.
- Name the problem, not the mechanism: "Slow user actions" rather than
  "Interaction latency", "From error to code line" rather than "Failure
  insights". Two use cases about related problems should have names that tell
  them apart.
- The full problem statement goes in `@UseCaseDescription`, which feeds the
  home card, the nav tooltip and the link preview.
- The home page starts with an intro that says what the page is for, then
  shows one card per use case. When there are many, group the cards under
  short headings by kind of problem; the side navigation keeps `UC1`…`UC<N>`
  order.

## Mobile

Every view must work on a phone. Check at 390 px wide: side-by-side layouts
stack, wide content (code, tables) scrolls inside its own box, and buttons
overlaying the navbar do not cover the title.

## Copy

Write for non-native English readers: short sentences, common words. Do not
promise what the app does not do — a feedback form does not promise a reply, a
demo does not promise production behaviour. Prefer the simplest form that
covers the use case; a choice the reader does not need is noise.
