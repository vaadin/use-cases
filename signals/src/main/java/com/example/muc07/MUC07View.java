package com.example.muc07;

import java.util.UUID;

import com.example.security.CurrentUserSignal;
import com.example.signals.UserSessionRegistry;
import com.example.usecase18.AbstractTaskChatView;
import com.example.usecase18.TaskLLMService;
import com.example.views.ActiveUsersDisplay;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "muc-07", layout = MainLayout.class)
@PageTitle("MUC 7: Shared LLM Task List")
@Menu(order = 56, title = "MUC 7: Shared LLM Task List")
@AnonymousAllowed
public class MUC07View extends AbstractTaskChatView {

    public MUC07View(CurrentUserSignal currentUserSignal,
            MUC07Signals muc07Signals, UserSessionRegistry userSessionRegistry,
            TaskLLMService taskLLMService) {

        // Call super with SHARED signals - must be first statement
        super(muc07Signals.getLlmTasksSignal(), // Shared task signal
                muc07Signals.getLlmChatMessagesSignal(), // Shared chat signal
                taskLLMService, getUserConversationId(currentUserSignal), // Per-user
                                                                          // conversation
                                                                          // ID
                currentUserSignal, // Current user for avatar/name
                userSessionRegistry // For display name lookup
        );

        // Add active users display
        ActiveUsersDisplay activeUsersDisplay = new ActiveUsersDisplay(
                userSessionRegistry, "Active on this view", "muc-07", true);
        addComponentAsFirst(activeUsersDisplay); // Add before chat panel
    }

    private static String getUserConversationId(
            CurrentUserSignal currentUserSignal) {
        return currentUserSignal.getUserSignal().peek().getUsername() + ":"
                + UUID.randomUUID();
    }
}
