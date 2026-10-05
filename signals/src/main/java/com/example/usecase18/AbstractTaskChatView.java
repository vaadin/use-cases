package com.example.usecase18;

import java.time.Instant;
import java.time.LocalDate;

import com.example.security.CurrentUserSignal;
import com.example.signals.SessionIdHelper;
import com.example.signals.UserSessionRegistry;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.shared.SharedListSignal;

public abstract class AbstractTaskChatView extends VerticalLayout {

    private static final Logger logger = LoggerFactory
            .getLogger(AbstractTaskChatView.class);

    // Signals injected via constructor
    protected final SharedListSignal<Task> tasksSignal;
    protected final SharedListSignal<ChatMessageData> chatMessagesSignal;
    protected final String conversationId;
    protected final CurrentUserSignal currentUserSignal;
    protected final UserSessionRegistry userSessionRegistry;

    // Session ID for display name lookup
    private @Nullable String sessionId;

    // Computed signals for statistics
    private Signal<Integer> totalTasksSignal;
    private Signal<Integer> completedTasksSignal;
    private Signal<Integer> pendingTasksSignal;

    // Services
    protected final TaskLLMService taskLLMService;

    // UI Components
    private MessageList messageList = new MessageList();
    private MessageInput messageInput = new MessageInput();

    // Signals for UI state
    private final ValueSignal<Boolean> messageInputEnabledSignal = new ValueSignal<>(
            true);

    // Constructor with signal injection
    protected AbstractTaskChatView(SharedListSignal<Task> tasksSignal,
            SharedListSignal<ChatMessageData> chatMessagesSignal,
            TaskLLMService taskLLMService, String conversationId,
            CurrentUserSignal currentUserSignal,
            UserSessionRegistry userSessionRegistry) {

        this.tasksSignal = tasksSignal;
        this.chatMessagesSignal = chatMessagesSignal;
        this.taskLLMService = taskLLMService;
        this.conversationId = conversationId;
        this.currentUserSignal = currentUserSignal;
        this.userSessionRegistry = userSessionRegistry;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Set up computed signals
        totalTasksSignal = tasksSignal.map(list -> list.size());
        completedTasksSignal = Signal.computed(() -> (int) tasksSignal.get()
                .stream().filter(t -> t.get().isCompleted()).count());
        pendingTasksSignal = Signal.computed(
                () -> totalTasksSignal.get() - completedTasksSignal.get());

        // Build UI - Statistics on top, then AI and Grid side by side
        HorizontalLayout statsSection = buildStatisticsSection();
        HorizontalLayout mainPanel = buildMainPanel();

        add(statsSection);
        add(mainPanel);

        setFlexGrow(0, statsSection); // Don't grow
        setFlexGrow(1, mainPanel); // Take remaining space
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.sessionId = SessionIdHelper.getCurrentSessionId();
    }

    private String getCurrentDisplayName() {
        String username = currentUserSignal.getUserSignal().get().getUsername();
        if (sessionId == null) {
            return username;
        }

        var users = userSessionRegistry.getActiveUsersSignal().get();
        var displayNames = userSessionRegistry.getDisplayNamesSignal().get();

        // Find matching session key for current user
        String sessionKey = username + ":" + sessionId;
        for (int i = 0; i < users.size() && i < displayNames.size(); i++) {
            if (sessionKey.equals(users.get(i).get().getCompositeKey())) {
                return displayNames.get(i);
            }
        }

        return username;
    }

    private HorizontalLayout buildMainPanel() {
        HorizontalLayout mainPanel = new HorizontalLayout();
        mainPanel.setWidthFull();
        mainPanel.setHeightFull();
        mainPanel.setSpacing(true);
        mainPanel.setAlignItems(Alignment.STRETCH);
        mainPanel.addClassName("main-panel");

        // Left side: AI Chat
        VerticalLayout chatPanel = buildChatPanel();

        // Right side: Task Grid
        VerticalLayout taskPanel = buildTaskPanel();

        mainPanel.add(chatPanel, taskPanel);
        mainPanel.setFlexGrow(1, chatPanel); // 50% of horizontal space
        mainPanel.setFlexGrow(1, taskPanel); // 50% of horizontal space

        return mainPanel;
    }

    private VerticalLayout buildChatPanel() {
        VerticalLayout chatPanel = new VerticalLayout();
        chatPanel.setHeightFull();
        chatPanel.setPadding(false);
        chatPanel.setSpacing(true);
        chatPanel.addClassName("chat-panel");

        H2 chatTitle = new H2("AI Task Assistant");
        chatTitle.addClassName("panel-title");

        Paragraph chatDescription = new Paragraph(
                "Chat with the AI to manage your tasks. Try commands like 'Add a task to buy groceries' or 'List my tasks'.");

        VerticalLayout chatSection = buildChatSection();
        chatPanel.add(chatTitle, chatDescription, chatSection);
        chatPanel.setFlexGrow(1, chatSection);

        return chatPanel;
    }

    private VerticalLayout buildTaskPanel() {
        VerticalLayout taskPanel = new VerticalLayout();
        taskPanel.setHeightFull();
        taskPanel.setPadding(false);
        taskPanel.setSpacing(true);
        taskPanel.addClassName("task-panel");

        H2 title = new H2("Task Management");
        title.addClassName("panel-title");

        VerticalLayout gridContainer = buildTaskGrid();

        Button addTaskButton = new Button("Add New Task",
                VaadinIcon.PLUS.create());
        addTaskButton.addThemeVariants(ButtonVariant.PRIMARY);
        addTaskButton.addClickListener(e -> openAddTaskDialog());

        taskPanel.add(title, gridContainer, addTaskButton);
        taskPanel.setFlexGrow(1, gridContainer);

        return taskPanel;
    }

    private HorizontalLayout buildStatisticsSection() {
        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.setSpacing(true);

        Div totalCard = createStatCard("Total", totalTasksSignal,
                "var(--aura-accent-color)");
        Div completedCard = createStatCard("Completed", completedTasksSignal,
                "var(--aura-green)");
        Div pendingCard = createStatCard("Pending", pendingTasksSignal,
                "color-mix(in srgb, var(--vaadin-text-color) 60%, transparent)");

        statsLayout.add(totalCard, completedCard, pendingCard);
        return statsLayout;
    }

    private Div createStatCard(String label, Signal<Integer> valueSignal,
            String color) {
        Div card = new Div();
        card.addClassName("stat-card");

        Span valueLabel = new Span();
        valueLabel.bindText(valueSignal.map(String::valueOf));
        valueLabel.addClassName("stat-card-value");
        // Per-card color is dynamic — keep inline
        valueLabel.getStyle().set("color", color);

        Span titleLabel = new Span(label);
        titleLabel.addClassName("stat-card-label");

        card.add(valueLabel, titleLabel);
        return card;
    }

    private void openAddTaskDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Task");
        dialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        TextField titleField = new TextField("Title");
        titleField.setPlaceholder("Enter task title");
        titleField.setWidthFull();
        titleField.setAutofocus(true);

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setPlaceholder("Enter task description");
        descriptionField.setWidthFull();
        descriptionField.setMaxHeight("150px");

        DatePicker dueDatePicker = new DatePicker("Due Date");
        dueDatePicker.setValue(LocalDate.now().plusDays(7));
        dueDatePicker.setWidthFull();

        formLayout.add(titleField, descriptionField, dueDatePicker);

        Button saveButton = new Button("Add Task", e -> {
            String title = titleField.getValue();
            String description = descriptionField.getValue();
            LocalDate dueDate = dueDatePicker.getValue();

            if (title != null && !title.isBlank()) {
                Task newTask = Task.create(title, description)
                        .withDueDate(dueDate);
                tasksSignal.insertLast(newTask);
                dialog.close();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        dialog.add(formLayout);
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private VerticalLayout buildTaskGrid() {
        VerticalLayout gridContainer = new VerticalLayout();
        gridContainer.setWidthFull();
        gridContainer.setPadding(false);
        gridContainer.setSpacing(false);
        gridContainer.addClassName("grid-container");

        Grid<Task> grid = new Grid<>(Task.class, false);
        grid.addColumn(Task::title).setHeader("Title").setFlexGrow(2)
                .setAutoWidth(true);
        grid.addColumn(Task::description).setHeader("Description")
                .setFlexGrow(3);
        grid.addColumn(Task::status).setHeader("Status").setFlexGrow(1)
                .setAutoWidth(true);
        grid.addColumn(Task::dueDate).setHeader("Due Date").setFlexGrow(1)
                .setAutoWidth(true);

        grid.addComponentColumn(task -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.TERTIARY,
                    ButtonVariant.SMALL);
            editButton.addClickListener(e -> openEditDialog(task));

            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.ERROR,
                    ButtonVariant.SMALL);
            deleteButton.addClickListener(e -> {
                tasksSignal.peek().stream()
                        .filter(sig -> sig.peek().equals(task)).findFirst()
                        .ifPresent(tasksSignal::remove);
            });

            actions.add(editButton, deleteButton);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setAutoWidth(true);

        grid.addThemeVariants(GridVariant.ROW_STRIPES);

        Signal.effect(grid,
                () -> grid.setItems(tasksSignal.getValues().toList()));

        gridContainer.add(grid);
        gridContainer.setFlexGrow(1, grid);
        return gridContainer;
    }

    private void openEditDialog(Task task) {
        // Find the signal for this task
        var taskSignalOpt = tasksSignal.peek().stream()
                .filter(sig -> sig.peek().id().equals(task.id())).findFirst();

        if (taskSignalOpt.isEmpty()) {
            return;
        }

        var taskSignal = taskSignalOpt.get();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Task");
        dialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();

        // Bind fields using map (read) + updater (write)
        TextField titleField = new TextField("Title");
        titleField.setWidthFull();
        titleField.bindValue(taskSignal.map(Task::title),
                taskSignal.updater(Task::withTitle));

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setMaxHeight("100px");
        descriptionField.bindValue(taskSignal.map(Task::description),
                taskSignal.updater(Task::withDescription));

        ComboBox<Task.TaskStatus> statusCombo = new ComboBox<>("Status");
        statusCombo.setItems(Task.TaskStatus.values());
        statusCombo.setWidthFull();
        statusCombo.bindValue(taskSignal.map(Task::status),
                taskSignal.updater(Task::withStatus));

        DatePicker dueDatePicker = new DatePicker("Due Date");
        dueDatePicker.setWidthFull();
        dueDatePicker.bindValue(taskSignal.map(Task::dueDate),
                taskSignal.updater(Task::withDueDate));

        formLayout.add(titleField, descriptionField, statusCombo,
                dueDatePicker);

        // Changes are saved automatically via two-way binding
        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(formLayout);
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private VerticalLayout buildChatSection() {
        VerticalLayout chatContainer = new VerticalLayout();
        chatContainer.setWidthFull();
        chatContainer.setPadding(false);
        chatContainer.setSpacing(true);
        chatContainer.addClassName("chat-container");

        // Message list
        messageList = new MessageList();
        messageList.setWidthFull();
        messageList.setHeightFull();
        messageList.setMarkdown(true);

        // Reactively update message list using Signal.effect
        Signal.effect(messageList, () -> {
            var msgSignals = chatMessagesSignal.get();
            if (msgSignals != null) {
                CurrentUserSignal.UserInfo userInfo = currentUserSignal
                        .getUserSignal().get();

                var items = msgSignals.stream().map(msgSignal -> {
                    ChatMessageData msg = msgSignal.get();
                    MessageListItem item = new MessageListItem(
                            msg.content().isBlank() ? "_typing..._"
                                    : msg.content(),
                            msg.timestamp(), msg.role());

                    if (msg.role().equals("You") && userInfo != null) {
                        item.setUserColorIndex(0);
                        String displayName = getCurrentDisplayName();
                        String username = userInfo.getUsername();
                        if (displayName != null && !displayName.isBlank()) {
                            item.setUserName(displayName);
                        }
                        if (username != null && !username.isBlank()) {
                            String userImage = MainLayout
                                    .getProfilePicturePath(username);
                            if (userImage != null && !userImage.isBlank()) {
                                item.setUserImage(userImage);
                            }
                        }
                    } else {
                        item.setUserColorIndex(1);
                    }

                    return item;
                }).toList();
                messageList.setItems(items);
            }
        });

        // Message input
        messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInput.bindEnabled(messageInputEnabledSignal);
        messageInput.addSubmitListener(this::onMessageSubmit);

        chatContainer.add(messageList, messageInput);
        chatContainer.setFlexGrow(1, messageList);

        return chatContainer;
    }

    private void onMessageSubmit(MessageInput.SubmitEvent event) {
        String userMessage = event.getValue();

        if (userMessage == null || userMessage.isBlank()) {
            return;
        }

        // Disable input while processing
        messageInputEnabledSignal.set(false);

        // Add user message
        chatMessagesSignal.insertLast(
                new ChatMessageData("You", userMessage, Instant.now()));

        // Create assistant message placeholder
        Instant assistantTimestamp = Instant.now();
        chatMessagesSignal.insertLast(
                new ChatMessageData("Assistant", "", assistantTimestamp));

        // Get reference to the last message signal (assistant message) for
        // updates
        var msgs = chatMessagesSignal.peek();
        var assistantMessageSignal = msgs.get(msgs.size() - 1);

        // Accumulate streaming content
        StringBuilder streamingContent = new StringBuilder();

        // Stream responses from LLM with consistent conversation ID for memory
        taskLLMService
                .streamMessage(userMessage, createTaskContext(), conversationId)
                .subscribe(token -> {
                    // Append token and update the message in the signal
                    streamingContent.append(token);
                    assistantMessageSignal.set(new ChatMessageData("Assistant",
                            streamingContent.toString(), assistantTimestamp));
                }, error -> {
                    // Update with error message
                    streamingContent.append("\n\n❌ Error: ")
                            .append(error.getMessage());
                    assistantMessageSignal.set(new ChatMessageData("Assistant",
                            streamingContent.toString(), assistantTimestamp));
                    messageInputEnabledSignal.set(true);
                }, () -> {
                    // Streaming complete - re-enable input
                    if (streamingContent.isEmpty()) {
                        assistantMessageSignal.set(new ChatMessageData(
                                "Assistant", "Done!", assistantTimestamp));
                    }
                    messageInputEnabledSignal.set(true);
                });
    }

    private void updateTaskField(String taskId,
            java.util.function.Function<Task, Task> updater) {
        var tasks = tasksSignal.peek();
        var match = tasks.stream().filter(sig -> sig.peek().id().equals(taskId))
                .findFirst();

        if (match.isEmpty()) {
            logger.warn("Task not found: {} (available: {})", taskId,
                    tasks.stream().map(sig -> sig.peek().id()).toList());
            return;
        }

        var signal = match.get();
        Task oldValue = signal.peek();
        Task newValue = updater.apply(oldValue);
        logger.debug("Updating task {}: {} -> {}", taskId, oldValue.status(),
                newValue.status());
        signal.set(newValue);
    }

    private TaskContext createTaskContext() {
        return new TaskContext() {
            @Override
            public java.util.List<Task> getAllTasks() {
                return tasksSignal.peek().stream().map(sig -> sig.peek())
                        .toList();
            }

            @Override
            public void addTask(Task task) {
                tasksSignal.insertLast(task);
            }

            @Override
            public void removeTask(String taskId) {
                var match = tasksSignal.peek().stream()
                        .filter(sig -> sig.peek().id().equals(taskId))
                        .findFirst();
                if (match.isEmpty()) {
                    logger.warn("Cannot remove task, not found: {}", taskId);
                    return;
                }
                logger.debug("Removing task: {}", taskId);
                tasksSignal.remove(match.get());
            }

            @Override
            public void updateTask(String taskId, String title,
                    String description) {
                updateTaskField(taskId, task -> task.withTitle(title)
                        .withDescription(description));
            }

            @Override
            public void changeStatus(String taskId, Task.TaskStatus status) {
                updateTaskField(taskId, task -> task.withStatus(status));
            }

            @Override
            public void updateDueDate(String taskId,
                    java.time.LocalDate dueDate) {
                updateTaskField(taskId, task -> task.withDueDate(dueDate));
            }
        };
    }
}
