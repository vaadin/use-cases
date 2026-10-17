package com.example.uc1;

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
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(AiTestConfiguration.class)
@ViewPackages(classes = OrderAssistantView.class)
class OrderAssistantViewTest extends SpringBrowserlessTest {

    @Autowired
    private FakeLlm llm;

    @BeforeEach
    void resetLlm() {
        llm.reset();
    }

    @Test
    void suggestedQuestionIsAnsweredFromTheOrderToolsAndTheCallIsLogged() {
        llm.respond(request -> {
            String late = FakeLlm.toolObject(request, OrderTools.class)
                    .findOrders(null, null, true);
            return "Late orders: " + late;
        });
        navigate(OrderAssistantView.class);

        test(find(Button.class).withText("Which orders are late?").single())
                .click();
        runPendingSignalsTasks();

        assertEquals(OrderAssistantView.SYSTEM_PROMPT,
                llm.requests().getFirst().systemPrompt());
        List<String> messages = find(MessageList.class).single().getItems()
                .stream().map(MessageListItem::getText).toList();
        assertEquals("Which orders are late?", messages.getFirst());
        assertTrue(messages.getLast().startsWith("Late orders: "),
                messages.getLast());

        List<ToolCallLog.ToolCall> calls = find(ToolCallLog.class).single()
                .getCalls();
        assertEquals(1, calls.size());
        assertEquals("findOrders", calls.getFirst().tool());
        assertEquals("lateOnly=true", calls.getFirst().input());
        // Exactly the open orders whose due date has passed.
        List.of("SO-1004", "SO-1007", "SO-1009", "SO-1011").forEach(
                id -> assertTrue(calls.getFirst().output().contains(id), id));
        List.of("SO-1002", "SO-1003", "SO-1012").forEach(
                id -> assertTrue(!calls.getFirst().output().contains(id), id));
    }

    @Test
    void unknownOrderIdIsReportedToTheModelInsteadOfFailing() {
        llm.respond(request -> FakeLlm.toolObject(request, OrderTools.class)
                .getOrder("SO-9999"));
        navigate(OrderAssistantView.class);

        test(find(Button.class).withText("Which orders are late?").single())
                .click();
        runPendingSignalsTasks();

        assertEquals("No order with id SO-9999", find(ToolCallLog.class)
                .single().getCalls().getFirst().output());
    }
}
