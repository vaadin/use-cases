package com.example.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.SpringAILLMProvider;

/**
 * Creates the {@link LLMProvider} an {@code AIOrchestrator} talks to. Each view
 * gets its own provider: created from a {@link ChatModel}, the provider keeps
 * the conversation memory, so sharing one would mix the conversations of all
 * users.
 */
@Component
public class LlmProviderFactory {

    private final ObjectProvider<ChatModel> chatModel;

    public LlmProviderFactory(ObjectProvider<ChatModel> chatModel) {
        this.chatModel = chatModel;
    }

    public LLMProvider create() {
        return new SpringAILLMProvider(chatModel.getObject());
    }
}
