package com.example.ai;

import java.util.List;

import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * The layout the assistant use cases share: the conversation with suggested
 * questions and an input on one side, the {@link ToolCallLog} on the other. The
 * views hand {@link #messages()} and {@link #input()} to their
 * {@link AIOrchestrator}, which does the rest.
 */
public class AssistantPanel extends Div {

    private final MessageList messages = new MessageList();
    private final MessageInput input = new MessageInput();
    private final FlexLayout suggestions = new FlexLayout();

    public AssistantPanel(ToolCallLog log) {
        addClassName("assistant-panel");
        messages.setMarkdown(true);
        messages.addClassName("assistant-messages");
        input.setWidthFull();
        suggestions.addClassName("assistant-suggestions");

        VerticalLayout chat = new VerticalLayout(messages, suggestions, input);
        chat.setPadding(false);
        add(chat, log);
    }

    public MessageList messages() {
        return messages;
    }

    public MessageInput input() {
        return input;
    }

    /**
     * Adds a button per question that asks it, so the reader can try the use
     * case without thinking of a question first.
     */
    public void addSuggestions(List<String> questions,
            AIOrchestrator orchestrator) {
        questions.forEach(question -> {
            Button button = new Button(question,
                    click -> orchestrator.prompt(question));
            button.addThemeVariants(ButtonVariant.TERTIARY);
            suggestions.add(button);
        });
    }

    /**
     * Enables or disables asking, for example while the model is not
     * configured.
     */
    public void setReady(boolean ready) {
        input.setEnabled(ready);
        suggestions.setEnabled(ready);
    }
}
