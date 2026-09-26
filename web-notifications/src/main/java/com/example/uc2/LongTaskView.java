package com.example.uc2;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;

import com.example.MissingAPI;
import com.example.MissingAPI.NotificationOptions;
import com.example.MissingAPI.NotificationPermission;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — Notify when a long-running task finishes.
 * <p>
 * The user starts a slow server-side job (here: generating a report that
 * takes ten seconds) and, as users do, switches to another tab while waiting.
 * When the job finishes the server pushes the result to the UI via
 * {@link UI#accessLater} and shows a native OS notification, so the user
 * learns about it without having to keep checking the tab. The same click that
 * starts the job also asks for notification permission if it hasn't been
 * decided yet — the moment the user wants the result is the moment the prompt
 * makes sense. Without permission the result is announced with an in-app
 * notification instead.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Task finished")
@Menu(order = 2, title = "UC2 — Task finished")
public class LongTaskView extends VerticalLayout {

    static final Duration TASK_DURATION = Duration.ofSeconds(10);
    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    enum TaskState {
        IDLE, RUNNING, DONE
    }

    private final TaskScheduler taskScheduler;
    private final ValueSignal<TaskState> taskState = new ValueSignal<>(
            TaskState.IDLE);

    private final Button startButton = new Button("Generate report");
    private final ProgressBar progress = new ProgressBar();
    private final Span statusLabel = new Span();
    private final Span permissionBadge = new Span();
    private final Div log = new Div();

    private @Nullable ScheduledFuture<?> pending;

    public LongTaskView(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;

        add(new H1("UC2 — Notify when a long-running task finishes"));
        add(new Paragraph("Click \"Generate report\" and switch to another "
                + "tab. The report takes ten seconds to build on the "
                + "server; when it's done you get an OS notification. If "
                + "notifications haven't been decided yet, the same click "
                + "asks for permission."));

        startButton.addThemeVariants(ButtonVariant.PRIMARY);
        progress.setIndeterminate(true);
        progress.setWidth("320px");
        permissionBadge.addClassName("status-badge");

        // Ask inside the click gesture; the server listener starts the task.
        MissingAPI.requestPermissionOnClick(startButton);
        startButton.addClickListener(e -> start(UI.getCurrent()));

        HorizontalLayout row = new HorizontalLayout(startButton,
                new Span("Notifications:"), permissionBadge);
        row.setAlignItems(Alignment.CENTER);

        log.addClassName("notification-log");
        add(row, progress, statusLabel, new H2("Delivery log"), log);

        startButton.bindEnabled(taskState.map(s -> s != TaskState.RUNNING));
        progress.bindVisible(taskState.map(s -> s == TaskState.RUNNING));
        statusLabel.bindText(taskState.map(s -> switch (s) {
        case IDLE -> "No report generated yet.";
        case RUNNING -> "Generating report… feel free to switch tabs.";
        case DONE -> "Report ready.";
        }));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<NotificationPermission> permission = MissingAPI
                .permissionSignal(attachEvent.getUI());
        permissionBadge.bindText(permission.map(p -> switch (p) {
        case GRANTED -> "on";
        case DENIED -> "blocked — in-app fallback";
        case UNSUPPORTED -> "unsupported — in-app fallback";
        case DEFAULT -> "not decided";
        case UNKNOWN -> "checking…";
        }));
        permissionBadge.bindClassName("paused",
                permission.map(p -> p != NotificationPermission.GRANTED));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
        super.onDetach(detachEvent);
    }

    private void start(UI ui) {
        taskState.set(TaskState.RUNNING);
        appendLog("report started");
        pending = taskScheduler.schedule(
                ui.accessLater(() -> finish(ui), null),
                Instant.now().plus(TASK_DURATION));
    }

    // Package-private so tests can complete the task without waiting.
    void finish(UI ui) {
        pending = null;
        taskState.set(TaskState.DONE);
        String body = "Q3 sales report finished at "
                + LocalTime.now().format(TIME) + ".";
        if (MissingAPI.permissionSignal(ui)
                .peek() == NotificationPermission.GRANTED) {
            MissingAPI.showNotification(ui, "Your report is ready",
                    NotificationOptions.body(body).withTag("report-ready"))
                    .onError(reason -> {
                        inApp(body);
                        appendLog("OS notification refused (" + reason
                                + ") → in-app");
                    });
            appendLog("report finished → OS notification");
        } else {
            inApp(body);
            appendLog("report finished → in-app notification");
        }
    }

    private static void inApp(String body) {
        Notification notification = Notification.show(
                "Your report is ready. " + body, 5000,
                Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.SUCCESS);
    }

    private void appendLog(String line) {
        Div entry = new Div(LocalTime.now().format(TIME) + "  " + line);
        log.addComponentAsFirst(entry);
    }
}
