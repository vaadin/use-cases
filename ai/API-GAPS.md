# AI in business apps — API gaps

Places where Vaadin's AI API made a use case awkward, or where a use case had to work around it. The API: `AIOrchestrator` (vaadin-ai-core-flow, experimental behind the `aiComponents` feature flag) connects a `MessageInput`, a `MessageList` and an optional file receiver to an `LLMProvider` (here `SpringAILLMProvider`), and takes tools as Spring AI `@Tool` objects or as `AIController#getTools()` specs; `FormAIController` (vaadin-ai-extensions-flow) fills a form from a prompt or an attachment.

UC1–UC4 are built on that API. UC5 (voice-over) and UC6 (presenter video) generate media, which the AI API does not cover; they call Spring AI's `TextToSpeechModel` and HeyGen's REST API directly and are not the source of any entry below.

## 1. No MCP support in the orchestrator

**Where it bites:** UC4 / DocsAssistantView.java
**Symptom:** Documentation, wikis and ticket systems increasingly offer their content through an MCP server. `AIOrchestrator.Builder` takes `@Tool` objects (`withTools`) and controllers (`withController`), but has no way to plug in an MCP server, so the application has to run the MCP client itself and translate every MCP tool into something the orchestrator accepts.
**Workaround used:** `McpToolsController`, an `AIController` whose `getTools()` turns each MCP tool (name, description, input schema) into an `LLMProvider.ToolSpec` and forwards `execute` to `McpSyncClient#callTool`. `DocsMcpClient` owns the connection. About 120 lines that every application with an MCP source would repeat.
**Suggested API:** an MCP-backed tool source in vaadin-ai-core-flow, for example `AIOrchestrator.Builder#withMcpServer(McpSyncClient client, Predicate<String> toolFilter)`, or a ready-made `McpToolsController`.

## 2. A controller's tools are validated when it is added to the builder

**Where it bites:** UC4 / DocsAssistantView.java
**Symptom:** `AIOrchestrator.Builder#withController` calls `controller.getTools()` right away to validate the tool names and schemas, although each request asks the controller for its tools again. A controller whose tools come from a remote server therefore has to be connected before the orchestrator can be built. Connecting in the view's constructor blocks the first render on a network round trip, and a server that is down makes navigation to the view fail with an exception.
**Workaround used:** the view connects on a background thread and builds the orchestrator in a `Signal.effect` once the server's tools are known; until then the input is disabled. `McpToolsController` takes the already-fetched tool list.
**Suggested API:** validate the tools when a request is built, or accept a `Supplier<List<ToolSpec>>`, so a controller can contribute tools that become available later.

## 3. Tool calls are not observable

**Where it bites:** UC1 / OrderAssistantView.java, UC3 / HandbookView.java, UC4 / DocsAssistantView.java
**Symptom:** Showing what the model looked up — which method it called, with which arguments, and what came back — is what makes an assistant's answer checkable in a business app. The orchestrator has a `RequestListener` and a `ResponseListener`, and `ResponseMetadata` carries the finish reason and token usage, but there is no event for a tool call.
**Workaround used:** every tool records its own call into `ToolCallLog` (`OrderTools`, `HandbookTools`, `McpToolsController`). Tools run on the provider's thread, so the log is a signal the component follows.
**Suggested API:** `AIOrchestrator.Builder#withToolCallListener(ToolCallListener)` with an event carrying the tool name, the arguments as JSON, the result and the duration, fired on the UI thread like the response listener.

## 4. No test double for the language model

**Where it bites:** every browserless test of UC1–UC4
**Symptom:** Browserless tests cannot call a real model, and the AI API offers no scripted `LLMProvider` to replace it. A test that wants to check what the view does when the model calls a tool has to write a provider that does so.
**Workaround used:** `FakeLlm` in the test sources: an `LLMProvider` lambda whose answer a test scripts, with helpers that call a request's `@Tool` object or `ToolSpec` the way a model would. The orchestrator's response then has to be flushed with `runPendingSignalsTasks()`, since tokens reach the message list through `UI.access`.
**Suggested API:** a scripted provider in browserless-test, for example `TestLLMProvider.respond(request -> ...)` with `callTool(name, json)`, and an orchestrator that delivers synchronously in tests.

## 5. The file receiver's state is not observable

**Where it bites:** UC2 / InvoiceReaderView.java
**Symptom:** With `withFileReceiver(upload)` the orchestrator keeps an uploaded file until the next prompt, but nothing tells the view whether a file is waiting, so "Read uploaded invoice" cannot be enabled only when there is something to read. The `Upload` fires no `SucceededEvent` once the orchestrator has set its own `UploadHandler`, so the usual listener does not help either.
**Workaround used:** the view counts files from the browser's `upload-success` DOM event and `Upload#addFileRemovedListener`, and resets the count after the prompt took the file.
**Suggested API:** `AIFileReceiver#hasAttachments()` plus a listener, or a signal of the pending attachments.

## 6. The feature flag is checked on the first prompt

**Where it bites:** every orchestrator view (UC1–UC4)
**Symptom:** Without `com.vaadin.experimental.aiComponents=true` in `vaadin-featureflags.properties`, the orchestrator builds and the view renders normally; `AIComponentsExperimentalFeatureException` is only thrown when the user sends the first message.
**Workaround used:** the module ships `src/main/resources/vaadin-featureflags.properties` with the flag on.
**Suggested API:** check the flag in `AIOrchestrator.Builder#build()`, so a missing flag fails when the developer opens the view rather than when a user first asks something.
