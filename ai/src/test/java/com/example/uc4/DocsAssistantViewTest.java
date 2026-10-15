package com.example.uc4;

import java.util.List;

import com.example.AiTestConfiguration;
import com.example.FakeLlm;
import com.example.ai.ToolCallLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(AiTestConfiguration.class)
@ViewPackages(classes = DocsAssistantView.class)
class DocsAssistantViewTest extends SpringBrowserlessTest {

    @Autowired
    private FakeLlm llm;

    @BeforeEach
    void resetLlm() {
        llm.reset();
    }

    @Test
    void mcpToolsAreOfferedToTheModelAndItsCallsAreForwardedAndLogged() {
        llm.respond(request -> {
            assertEquals(List.of("search_docs"), request.explicitTools()
                    .stream().map(t -> t.getName()).toList());
            String docs = FakeLlm.callTool(request, "search_docs",
                    "{\"question\": \"confirm before delete\"}");
            return "Use a ConfirmDialog. " + docs;
        });
        navigate(DocsAssistantView.class);
        runPendingSignalsTasks();
        assertTrue(find(MessageInput.class).single().isEnabled(),
                "asking is possible once the server's tools are known");

        test(find(Button.class)
                .withText(DocsAssistantView.SUGGESTIONS.getFirst()).single())
                .click();
        runPendingSignalsTasks();

        ToolCallLog.ToolCall call = find(ToolCallLog.class).single().getCalls()
                .getFirst();
        assertEquals("search_docs", call.tool());
        assertEquals("{\"question\":\"confirm before delete\"}", call.input());
        assertTrue(find(MessageList.class).single().getItems().stream()
                .map(MessageListItem::getText).toList().getLast()
                .contains("docs/dialog.adoc"));
    }

    @Test
    void connectionLineNamesTheServerAndTheToolsTheAssistantMayUse() {
        navigate(DocsAssistantView.class);
        runPendingSignalsTasks();

        assertTrue(find(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText()
                        .equals("Connected to example-docs 1.0 at "
                                + "https://docs.example.com/mcp. The server "
                                + "offers 5 tools; the assistant may use "
                                + "search_docs.")));
    }

    @Test
    void failedConnectionIsExplainedInsteadOfLeavingTheReaderWaiting() {
        assertEquals(
                "Could not connect to the MCP server at "
                        + "https://docs.example.com/mcp: timed out",
                DocsAssistantView
                        .describe(
                                new DocsAssistantView.ConnectionState(null,
                                        "timed out"),
                                "https://docs.example.com/mcp"));
    }
}
