# Walkthroughs & product tours — use cases

A standalone Spring Boot demo of walkthroughs in Vaadin Flow: tours that point
at the parts of the UI one at a time and say what each is for. Vaadin has no
tour component, so the tours are built from `Popover` with a small `Tour`
helper (`com.example.tour`). Each view exercises a single realistic scenario
on a made-up order app. Where the API falls short is recorded in
[API-GAPS.md](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | First-run tour | A five-step tour of the app opens with the page: Back, Next, Skip tour and Escape, with a status line that says whether the tour was finished or skipped, and where. |
| UC2 | Show it only once | The tour opens only until it was finished or skipped, remembered per account on the server or per browser in local storage. Switching the account shows the difference. |
| UC3 | Replay from help | The help menu starts the tour from the beginning, or resumes it at the step where it was skipped. |
| UC4 | What's new | After an update, the tour shows only the features added since the version the user used last, marked with a "New" badge until seen. |
| UC5 | Tour of a busy view | A view's own tour starts on the first visit in the session and opens a closed panel and another tab to reach its targets. |
| UC6 | Learn by doing | A tutorial whose steps move on when the user does what they say: click "New task", type a title, click "Save". |
| UC7 | Tour across views | A tour that navigates to the next route and continues there, and goes back with Back. |
| UC8 | Hotspots | Pulsing dots next to new features; a click opens the hint, "Got it" removes the dot for the session. |
| UC9 | Spotlight | A tour that dims all but its target and blocks the page, opens the menu drawer on a phone and scrolls to a button at the end of a long form. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl walkthrough spring-boot:run
```

Open <http://localhost:8080/>.
