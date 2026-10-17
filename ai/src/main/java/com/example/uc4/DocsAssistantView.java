package com.example.uc4;

import java.util.List;
import java.util.stream.Collectors;

import com.example.ai.AiProvider;
import com.example.ai.AiProviders;
import com.example.ai.AssistantPanel;
import com.example.ai.LlmProviderFactory;
import com.example.ai.ProviderNotice;
import com.example.ai.ToolCallLog;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Answers from product docs.
 * <p>
 * A support agent or a user asks how the product works and gets an answer built
 * from the current documentation, not from what the model learned months ago.
 * The documentation is served by an MCP server ({@link DocsMcpClient}); a small
 * {@link McpToolsController} hands the server's search and read tools to an
 * {@link AIOrchestrator}. This example points at Vaadin's public documentation
 * server; an application would point it at the MCP server of its own product
 * documentation or knowledge base.
 */
@Route(value = "uc4", layout = MainLayout.class)
@UseCaseDescription("Answering how-to questions from current product documentation served by an MCP server")
@Menu(order = 4, title = "UC4 — Answers from product docs")
public class DocsAssistantView extends VerticalLayout {

    static final String SYSTEM_PROMPT = """
            You are a support assistant for Vaadin, a Java web framework. \
            Answer from the documentation: search it with the tools before \
            answering, and read a full page when the search result is not \
            enough. Answer for Vaadin 25 with Java (Flow) unless the user asks \
            otherwise. Keep answers short, include a small code example when \
            it helps, and end with the documentation pages you used. If the \
            documentation does not answer the question, say so.""";

    static final List<String> SUGGESTIONS = List.of(
            "How do I show a confirmation dialog before deleting a row?",
            "How can I let users upload a file and process it on the server?",
            "How do I make a form field required?");

    /**
     * The connection to the MCP server: connecting while both are {@code null},
     * otherwise either connected or failed.
     */
    record ConnectionState(DocsMcpClient.@Nullable Connection connection,
            @Nullable String error) {
    }

    private final ValueSignal<ConnectionState> state = new ValueSignal<>(
            new ConnectionState(null, null));
    private @Nullable AIOrchestrator orchestrator;

    public DocsAssistantView(DocsMcpClient docs,
            LlmProviderFactory llmProviders, AiProviders providers,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor) {
        add(new H1("UC4 — Answers from product docs"));
        add(new Paragraph("Ask how to do something with Vaadin. The "
                + "assistant does not answer from memory: it searches the "
                + "current documentation through an MCP server and reads the "
                + "pages it finds. Any documentation with an MCP server can "
                + "be plugged in the same way."));

        ProviderNotice notice = ProviderNotice.forMissing(providers,
                AiProvider.OPENAI);
        if (notice != null) {
            add(notice);
        }

        Paragraph connectionText = new Paragraph();
        connectionText.bindText(state.map(s -> describe(s, docs.url())));
        add(connectionText);

        ToolCallLog log = new ToolCallLog("Documentation the assistant read",
                "No calls yet. Each search or page the assistant reads "
                        + "appears here.");
        AssistantPanel panel = new AssistantPanel(log);

        // The orchestrator is built once the server's tools are known: it
        // validates a controller's tools when the controller is added.
        Signal.effect(this, () -> {
            DocsMcpClient.Connection connection = state.get().connection();
            if (connection != null && orchestrator == null) {
                AIOrchestrator created = AIOrchestrator
                        .builder(llmProviders.create(), SYSTEM_PROMPT)
                        .withMessageList(panel.messages())
                        .withInput(panel.input())
                        .withController(
                                new McpToolsController(docs, connection, log))
                        .withAssistantName("Docs assistant").build();
                panel.addSuggestions(SUGGESTIONS, created);
                orchestrator = created;
            }
            panel.setReady(notice == null && orchestrator != null);
        });

        add(panel);

        // Connecting is a network round trip; do it off the UI thread so the
        // page renders right away.
        executor.execute(() -> {
            try {
                state.set(new ConnectionState(docs.connect(), null));
            } catch (RuntimeException e) {
                state.set(new ConnectionState(null, e.getMessage()));
            }
        });
    }

    static String describe(ConnectionState state, String url) {
        if (state.connection() != null) {
            DocsMcpClient.Connection c = state.connection();
            return "Connected to " + c.serverName() + " " + c.serverVersion()
                    + " at " + url + ". The server offers " + c.toolsOnServer()
                    + " tools; the assistant may use "
                    + c.tools().stream().map(DocsMcpClient.DocsTool::name)
                            .collect(Collectors.joining(" and "))
                    + ".";
        }
        if (state.error() != null) {
            return "Could not connect to the MCP server at " + url + ": "
                    + state.error();
        }
        return "Connecting to the MCP server at " + url + "…";
    }
}
