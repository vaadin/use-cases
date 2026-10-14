package com.example.usecase18;

import java.time.LocalDate;
import java.util.UUID;

import com.example.common.UseCaseDescription;
import com.example.security.CurrentUserSignal;
import com.example.signals.UserSessionRegistry;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.shared.SharedListSignal;

@Route(value = "use-case-18", layout = MainLayout.class)
@PageTitle("Use Case 18: LLM-Powered Task List")
@UseCaseDescription("Letting an LLM chat edit a task list through signals")
@Menu(order = 18, title = "UC 18: LLM Task List")
@StyleSheet("usecase18.css")
@AnonymousAllowed
public class UseCase18View extends AbstractTaskChatView {

    public UseCase18View(TaskLLMService taskLLMService,
            CurrentUserSignal currentUserSignal,
            UserSessionRegistry userSessionRegistry) {
        super(new SharedListSignal<>(Task.class), // View-local task signal
                new SharedListSignal<>(ChatMessageData.class), // View-local
                                                               // chat signal
                taskLLMService, UUID.randomUUID().toString(), // Per-instance
                                                              // conversation ID
                currentUserSignal, // Current user for avatar/name
                userSessionRegistry // For display name lookup
        );
        addClassName("usecase18-view");

        // Initialize sample tasks for single-user view
        tasksSignal.insertLast(Task
                .create("Review pull requests",
                        "Review and merge pending pull requests")
                .withDueDate(LocalDate.now().plusDays(2)));
        tasksSignal.insertLast(Task
                .create("Write unit tests", "Add unit tests for new features")
                .withStatus(Task.TaskStatus.IN_PROGRESS)
                .withDueDate(LocalDate.now().plusDays(5)));
        tasksSignal.insertLast(Task
                .create("Deploy to staging",
                        "Deploy latest changes to staging environment")
                .withDueDate(LocalDate.now().plusDays(7)));
    }
}
