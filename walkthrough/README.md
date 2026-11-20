# Walkthroughs & product tours — use cases

A standalone Spring Boot demo of walkthroughs in Vaadin Flow: tours that point
at the parts of the UI one at a time and say what each is for. Vaadin has no
tour component, so the tours are built from `Popover` with a small `Tour`
helper (`com.example.tour`). Each view exercises a single realistic scenario
on a made-up order app. Where the API falls short is recorded in
[API-GAPS.md](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | App tour | The tour of the whole app fits the user: the full five-step tour for someone who never saw it, only the features added since for someone who saw an older version (with "New" badges), and none for someone who saw the current one. The version seen is remembered per account on the server or per browser in local storage. The help menu replays the tour or resumes a skipped one. |
| UC2 | Tour of a busy view | A view's own tour starts on the first visit in the session, opens a closed panel and another tab to reach its targets, and scrolls them into view. |
| UC3 | Learn by doing | A tutorial whose steps move on when the user does what they say: click "New task", type a title, click "Save". |
| UC4 | Tour across views | A tour that navigates to the next route and continues there, and goes back with Back. |
| UC5 | Hotspots | Pulsing dots next to new features; a click opens the hint, "Got it" removes the dot for the session. |
| UC6 | Spotlight | A tour that dims all but its target and blocks the page, opens the menu drawer when it is closed and scrolls to a button at the end of a long form. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl walkthrough spring-boot:run
```

Open <http://localhost:8080/>.
