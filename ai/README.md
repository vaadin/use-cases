# AI in business apps — use cases

A standalone Spring Boot demo of AI features inside a business application, beyond a chat window: answers from the application's own data and documents, an invoice turned into form data, and generated narration and presenter videos. The assistant use cases (UC1–UC4) are built on Vaadin's `AIOrchestrator` with Spring AI; UC2 also uses `FormAIController`. UC5 and UC6 call speech and video services directly.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Ask about your orders | Questions about sales orders in plain language. The model answers by calling two `@Tool` methods of the application; a log shows each call and what it returned. |
| UC2 | Read an invoice | A PDF or photo of an invoice fills an accounts-payable form through `FormAIController`. The `Binder` validates every value, including net + VAT = total, and the clerk approves the result. |
| UC3 | Employee handbook | HR questions answered only from a handbook indexed by meaning in a vector store, with the sections used. Questions the handbook does not cover get "ask HR" instead of an invented policy. |
| UC4 | Answers from product docs | How-to questions answered from documentation served by an MCP server (Vaadin's public docs server by default). The MCP tools reach the orchestrator through a small `AIController`. |
| UC5 | Voice-over from text | Text read aloud by an ElevenLabs voice through Spring AI's `TextToSpeechModel`, played in an `Audio` player and offered as MP3. |
| UC6 | Presenter video | A script rendered as an avatar video by HeyGen. Rendering takes minutes, so the view polls the job while it is open and shows the video when it is ready. |

## Run

```
cd ai
mvn spring-boot:run
```

Open <http://localhost:8080/>.

## API keys

Each provider reads its key from an environment variable. The app starts without them; a use case whose key is missing says so and disables its actions.

- `OPENAI_API_KEY` — UC1–UC4 (chat model `gpt-5.4-mini`, embeddings `text-embedding-3-small`; change them in `application.properties`).
- `ELEVENLABS_API_KEY` — UC5.
- `HEYGEN_API_KEY` — UC6. Rendering needs API credits on the HeyGen account.

UC5 and UC6 cost money per call, so the app allows at most `app.elevenlabs.clips-per-day` voice-overs and `app.heygen.videos-per-day` videos per day, for all users together. UC4 connects to `app.docs-mcp.url` and offers the model the tools listed in `app.docs-mcp.tools`.

The orchestrator is an experimental feature; `src/main/resources/vaadin-featureflags.properties` turns on its `aiComponents` flag.

## API gaps

See [API-GAPS.md](API-GAPS.md): no MCP support in the orchestrator, eager validation of controller tools, no tool-call events, no test double for the model, an unobservable file receiver, and a feature-flag check that waits for the first prompt.
