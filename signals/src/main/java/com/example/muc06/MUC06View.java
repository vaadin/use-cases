package com.example.muc06;

import java.time.LocalDate;
import java.util.UUID;

import com.example.common.UseCaseDescription;
import com.example.security.CurrentUserSignal;
import com.example.signals.UserSessionRegistry;
import com.example.views.ActiveUsersDisplay;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * Multi-User Case 6: Shared Task List with Inline Editing
 *
 * Demonstrates collaborative task list editing with: - Shared task list signal
 * across all users - Inline editing with immediate updates - Real-time
 * synchronization of changes - Multiple users editing different tasks
 * simultaneously
 *
 * Key Patterns: - Shared ListSignal<Task> across sessions - Inline field
 * editing without save buttons - Real-time reactive statistics - Server-side
 * signal coordination - Push-based real-time updates
 */
@Route(value = "muc-06", layout = MainLayout.class)
@PageTitle("MUC 6: Shared Task List")
@UseCaseDescription("Editing a shared task list together with live statistics")
@Menu(order = 55, title = "MUC 6: Shared Task List")
@StyleSheet("muc06.css")
@AnonymousAllowed
public class MUC06View extends VerticalLayout {

    private final String currentUser;
    private final MUC06Signals muc06Signals;
    private final UserSessionRegistry userSessionRegistry;

    public MUC06View(CurrentUserSignal currentUserSignal,
            MUC06Signals muc06Signals,
            UserSessionRegistry userSessionRegistry) {
        this.currentUser = currentUserSignal.getUserSignal().peek()
                .getUsername();
        this.muc06Signals = muc06Signals;
        this.userSessionRegistry = userSessionRegistry;

        addClassName("muc06-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Multi-User Case 6: Shared Task List");

        Paragraph description = new Paragraph(
                "This use case demonstrates collaborative task list editing. "
                        + "The task list is shared across all users - any changes made by one user "
                        + "are immediately visible to all other users viewing this page. "
                        + "Try opening this page in multiple browser windows (different users) to see real-time collaboration. "
                        + "All edits are instant with no save button required.");

        // Initialize sample tasks if list is empty
        muc06Signals.initializeSampleTasks();

        SharedListSignal<MUC06Signals.Task> tasksSignal = muc06Signals
                .getTasksSignal();

        // Computed signals for statistics
        Signal<Integer> totalSignal = tasksSignal.map(list -> list.size());
        Signal<Integer> completedSignal = Signal
                .computed(() -> (int) tasksSignal.get().stream()
                        .filter(t -> t.get().completed()).count());
        Signal<Integer> pendingSignal = Signal
                .computed(() -> totalSignal.get() - completedSignal.get());

        // User count display
        ActiveUsersDisplay userCountBox = new ActiveUsersDisplay(
                userSessionRegistry, "Active on this view", "muc-06", true);

        // Statistics panel
        Div statsBox = new Div();
        statsBox.addClassName("stats-box");

        Span totalLabel = new Span();
        totalLabel.bindText(totalSignal.map(n -> "Total: " + n));
        totalLabel.addClassName("stat-label");

        Span completedLabel = new Span();
        completedLabel.bindText(completedSignal.map(n -> "Completed: " + n));
        completedLabel.addClassName("stat-label");
        completedLabel.addClassName("stat-completed");

        Span pendingLabel = new Span();
        pendingLabel.bindText(pendingSignal.map(n -> "Pending: " + n));
        pendingLabel.addClassName("stat-label");
        pendingLabel.addClassName("stat-pending");

        statsBox.add(totalLabel, completedLabel, pendingLabel);

        // Tasks section
        H3 tasksTitle = new H3("Shared Tasks");

        Div tasksContainer = new Div();
        tasksContainer.addClassName("tasks-container");

        tasksContainer.bindChildren(tasksSignal,
                taskSignal -> createTaskRow(taskSignal, tasksSignal));

        // Add task button
        Button addButton = new Button("Add Task", event -> {
            String id = "task-" + UUID.randomUUID().toString();
            MUC06Signals.Task newTask = new MUC06Signals.Task(id, "", false,
                    LocalDate.now());
            tasksSignal.insertLast(newTask);
        });
        addButton.addThemeVariants(ButtonVariant.PRIMARY);

        // Info box
        Div infoBox = new Div();
        infoBox.addClassName("info-box");
        infoBox.add(new Paragraph(
                "💡 This demonstrates real-time collaborative editing with shared signals. "
                        + "In production:\n"
                        + "• Signal is application-scoped Spring component (injected)\n"
                        + "• Vaadin Push broadcasts all changes to connected clients\n"
                        + "• Tasks would be persisted to database\n"
                        + "• Conflict resolution could be added for simultaneous edits\n"
                        + "• Field locking could prevent editing conflicts (see MUC 4)\n"
                        + "• All synchronization is automatic via the Signal API"));

        add(title, description, userCountBox, statsBox, tasksTitle,
                tasksContainer, addButton, infoBox);
    }

    private HorizontalLayout createTaskRow(
            SharedValueSignal<MUC06Signals.Task> taskSignal,
            SharedListSignal<MUC06Signals.Task> tasksSignal) {

        // Checkbox for completed status - two-way binding via map + updater
        Checkbox checkbox = new Checkbox();
        checkbox.setAriaLabel("Task completed");
        checkbox.bindValue(taskSignal.map(MUC06Signals.Task::completed),
                taskSignal.updater((task, completed) -> new MUC06Signals.Task(
                        task.id(), task.title(), completed, task.dueDate())));

        // TextField for title - two-way binding via map + updater
        TextField titleField = new TextField();
        titleField.setPlaceholder("Task title...");
        titleField.setWidth("400px");
        titleField.bindValue(
                taskSignal.map(t -> t.title() != null ? t.title() : ""),
                taskSignal.updater((task, title) -> new MUC06Signals.Task(
                        task.id(), title, task.completed(), task.dueDate())));

        // Reactive strikethrough styling - direct style binding
        titleField.getStyle().bind("text-decoration",
                taskSignal.map(t -> t.completed() ? "line-through" : "none"));

        // DatePicker for due date - two-way binding via map + updater
        DatePicker datePicker = new DatePicker();
        datePicker.setPlaceholder("Due date");
        datePicker.setWidth("180px");
        datePicker.bindValue(taskSignal.map(MUC06Signals.Task::dueDate),
                taskSignal.updater((task, dueDate) -> new MUC06Signals.Task(
                        task.id(), task.title(), task.completed(), dueDate)));

        // Delete button
        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.ERROR,
                ButtonVariant.SMALL);
        deleteButton.setAriaLabel("Delete task");
        deleteButton.addClickListener(e -> tasksSignal.remove(taskSignal));

        // Layout with styling
        HorizontalLayout row = new HorizontalLayout(checkbox, titleField,
                datePicker, deleteButton);
        row.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        row.setWidthFull();
        row.setPadding(true);
        row.addClassName("task-row");

        return row;
    }
}
