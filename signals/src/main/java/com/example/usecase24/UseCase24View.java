package com.example.usecase24;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

@Route(value = "use-case-24", layout = MainLayout.class)
@PageTitle("Use Case 24: VirtualList with Signal Data Source")
@UseCaseDescription("Feeding a virtual list from a signal of incoming notifications")
@Menu(order = 24, title = "UC 24: VirtualList Notifications")
@StyleSheet("usecase24.css")
@AnonymousAllowed
public class UseCase24View extends VerticalLayout {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("MMM d, yyyy HH:mm");

    public UseCase24View() {
        addClassName("usecase24-view");
        setSpacing(true);
        setPadding(true);

        var title = new H2("Use Case 24: VirtualList with Signal Data Source");

        var description = new Paragraph(
                "This use case demonstrates a VirtualList bound to a signal-based data source. "
                        + "A notification inbox lets you view, filter, and manage notifications with rich card rendering. "
                        + "The VirtualList receives a computed Signal<List<Notification>> that applies type and read-status filters. "
                        + "Actions (mark read/unread, dismiss) mutate items within the ListSignal.");

        // Source of truth
        var notificationsSignal = new ListSignal<Notification>();

        // Seed data
        seedNotifications(notificationsSignal);

        // Filter signals
        var typeFilterSignal = new ValueSignal<String>("All");
        var readFilterSignal = new ValueSignal<String>("All");

        // Computed filtered signal
        Signal<List<Notification>> filteredSignal = Signal.computed(() -> {
            String typeFilter = typeFilterSignal.get();
            String readFilter = readFilterSignal.get();

            return notificationsSignal.get().stream().map(ValueSignal::get)
                    .filter(n -> typeFilter.equals("All")
                            || n.type().name().equals(typeFilter))
                    .filter(n -> {
                        if (readFilter.equals("Unread")) {
                            return !n.read();
                        } else if (readFilter.equals("Read")) {
                            return n.read();
                        }
                        return true;
                    }).sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                    .toList();
        });

        // Aggregate signals
        Signal<Long> unreadCountSignal = Signal
                .computed(() -> notificationsSignal.get().stream()
                        .map(ValueSignal::get).filter(n -> !n.read()).count());

        Signal<String> countLabelSignal = Signal.computed(() -> {
            int filtered = filteredSignal.get().size();
            long unread = unreadCountSignal.get();
            return "Showing " + filtered + " notification"
                    + (filtered != 1 ? "s" : "") + " (" + unread + " unread)";
        });

        // Filter row
        var typeFilter = new ComboBox<String>("Type");
        typeFilter.setItems("All", "INFO", "WARNING", "ERROR", "SUCCESS");
        typeFilter.setWidth("150px");
        typeFilter.bindValue(typeFilterSignal, typeFilterSignal::set);

        var readFilter = new ComboBox<String>("Status");
        readFilter.setItems("All", "Unread", "Read");
        readFilter.setWidth("150px");
        readFilter.bindValue(readFilterSignal, readFilterSignal::set);

        var addInfoButton = new Button("Add Info",
                e -> addNotification(notificationsSignal,
                        NotificationType.INFO));
        addInfoButton.addThemeName("small");

        var addWarningButton = new Button("Add Warning",
                e -> addNotification(notificationsSignal,
                        NotificationType.WARNING));
        addWarningButton.addThemeName("small");
        addWarningButton.addThemeName("contrast");

        var addErrorButton = new Button("Add Error",
                e -> addNotification(notificationsSignal,
                        NotificationType.ERROR));
        addErrorButton.addThemeName("small");
        addErrorButton.addThemeName("error");

        var addSuccessButton = new Button("Add Success",
                e -> addNotification(notificationsSignal,
                        NotificationType.SUCCESS));
        addSuccessButton.addThemeName("small");
        addSuccessButton.addThemeName("success");

        var filterRow = new HorizontalLayout(typeFilter, readFilter,
                addInfoButton, addWarningButton, addErrorButton,
                addSuccessButton);
        filterRow.setAlignItems(Alignment.END);
        filterRow.setSpacing(true);

        // Action row
        var markAllReadButton = new Button("Mark All Read", e -> {
            notificationsSignal.peek().forEach(signal -> {
                Notification n = signal.peek();
                if (!n.read()) {
                    signal.set(n.withRead(true));
                }
            });
        });
        markAllReadButton.addThemeName("small");
        markAllReadButton.addThemeName("primary");

        var clearAllButton = new Button("Clear All",
                e -> notificationsSignal.clear());
        clearAllButton.addThemeName("small");
        clearAllButton.addThemeName("error");

        var actionRow = new HorizontalLayout(markAllReadButton, clearAllButton);
        actionRow.setSpacing(true);

        // Header row with count and unread badge
        var countLabel = new Span();
        countLabel.bindText(countLabelSignal);
        countLabel.addClassName("count-label");

        var unreadBadge = new Span();
        unreadBadge.bindText(unreadCountSignal.map(c -> c + " unread"));
        unreadBadge.addClassName("unread-badge");
        unreadBadge.bindVisible(unreadCountSignal.map(c -> c > 0));

        var headerRow = new HorizontalLayout(countLabel, unreadBadge);
        headerRow.setAlignItems(Alignment.CENTER);
        headerRow.setSpacing(true);

        // VirtualList
        var virtualList = new VirtualList<Notification>();
        virtualList.setHeight("500px");
        virtualList.setWidthFull();
        virtualList.setRenderer(new ComponentRenderer<>(
                notification -> createNotificationCard(notification,
                        notificationsSignal)));

        Signal.effect(virtualList,
                () -> virtualList.setItems(filteredSignal.get()));

        // Empty state
        var emptyState = new Paragraph(
                "No notifications to display. Try adjusting your filters or adding new notifications.");
        emptyState.addClassName("empty-state");
        emptyState.bindVisible(filteredSignal.map(List::isEmpty));

        // Info box
        var infoBox = new Div();
        infoBox.addClassName("info-box");
        infoBox.add(new Paragraph(
                "This use case demonstrates VirtualList bound to a signal-based data source. "
                        + "A ListSignal<Notification> is the source of truth. A computed signal filters by type and read status, "
                        + "then Signal.effect() binds the filtered list to the VirtualList. "
                        + "Each card uses a ComponentRenderer for rich per-item rendering. "
                        + "Actions find the matching ValueSignal in the ListSignal by ID and call set() or remove()."));

        add(title, description, filterRow, actionRow, headerRow, virtualList,
                emptyState, infoBox);
    }

    Div createNotificationCard(Notification notification,
            ListSignal<Notification> notificationsSignal) {
        var card = new Div();
        card.addClassName("notification-card");
        card.addClassName(notification.read() ? "is-read" : "is-unread");
        // Per-type border color is dynamic — keep inline
        card.getStyle().set("border-left",
                "4px solid " + getTypeColor(notification.type()));

        // Icon
        var icon = new Icon(getTypeIcon(notification.type()));
        icon.setSize("24px");
        icon.setColor(getTypeColor(notification.type()));
        icon.addClassName("notification-icon");

        // Content
        var content = new Div();
        content.addClassName("notification-content");

        // Title row with badge
        var titleSpan = new Span(notification.title());
        titleSpan.addClassName("notification-title");
        if (!notification.read()) {
            titleSpan.addClassName("is-unread");
        }

        var typeBadge = new Span(notification.type().name());
        typeBadge.addClassName("type-badge");
        // Per-type color is dynamic — keep inline
        typeBadge.getStyle().set("background-color",
                getTypeColor(notification.type()));

        var titleRow = new Div(titleSpan, typeBadge);
        titleRow.addClassName("notification-title-row");

        // Message
        var messageSpan = new Span(notification.message());
        messageSpan.addClassName("notification-message");

        // Timestamp
        var timestampSpan = new Span(
                notification.timestamp().format(TIME_FORMAT));
        timestampSpan.addClassName("notification-timestamp");

        content.add(titleRow, messageSpan, timestampSpan);

        // Actions
        var actions = new Div();
        actions.addClassName("notification-actions");

        var toggleReadButton = new Button(
                notification.read() ? "Mark Unread" : "Mark Read", e -> {
                    notificationsSignal.peek().stream()
                            .filter(signal -> signal.peek().id()
                                    .equals(notification.id()))
                            .findFirst().ifPresent(signal -> signal.set(signal
                                    .peek().withRead(!notification.read())));
                });
        toggleReadButton.addThemeName("small");
        toggleReadButton.addThemeName("tertiary");

        var dismissButton = new Button("Dismiss", e -> {
            notificationsSignal.peek().stream().filter(
                    signal -> signal.peek().id().equals(notification.id()))
                    .findFirst().ifPresent(notificationsSignal::remove);
        });
        dismissButton.addThemeName("small");
        dismissButton.addThemeName("tertiary");
        dismissButton.addThemeName("error");

        actions.add(toggleReadButton, dismissButton);

        card.add(icon, content, actions);
        return card;
    }

    private String getTypeColor(NotificationType type) {
        return switch (type) {
        case INFO -> "#2196F3";
        case WARNING -> "#FF9800";
        case ERROR -> "#F44336";
        case SUCCESS -> "#4CAF50";
        };
    }

    private VaadinIcon getTypeIcon(NotificationType type) {
        return switch (type) {
        case INFO -> VaadinIcon.INFO_CIRCLE;
        case WARNING -> VaadinIcon.WARNING;
        case ERROR -> VaadinIcon.EXCLAMATION_CIRCLE;
        case SUCCESS -> VaadinIcon.CHECK_CIRCLE;
        };
    }

    private void addNotification(ListSignal<Notification> notificationsSignal,
            NotificationType type) {
        var notification = new Notification(UUID.randomUUID().toString(),
                type.name().charAt(0) + type.name().substring(1).toLowerCase()
                        + " Notification",
                "This is a new " + type.name().toLowerCase()
                        + " notification added at "
                        + LocalDateTime.now().format(TIME_FORMAT) + ".",
                type, false, LocalDateTime.now());
        notificationsSignal.insertLast(notification);
    }

    private void seedNotifications(
            ListSignal<Notification> notificationsSignal) {
        var now = LocalDateTime.now();
        notificationsSignal.insertLast(new Notification(
                UUID.randomUUID().toString(), "Welcome",
                "Welcome to the notification inbox! This demo shows VirtualList with signals.",
                NotificationType.INFO, false, now.minusMinutes(5)));
        notificationsSignal.insertLast(new Notification(
                UUID.randomUUID().toString(), "System Update Available",
                "A new system update is available. Please review and apply when convenient.",
                NotificationType.WARNING, false, now.minusMinutes(15)));
        notificationsSignal.insertLast(new Notification(
                UUID.randomUUID().toString(), "Deployment Successful",
                "Version 2.4.1 has been deployed to production successfully.",
                NotificationType.SUCCESS, true, now.minusHours(1)));
        notificationsSignal.insertLast(new Notification(
                UUID.randomUUID().toString(), "Database Connection Error",
                "Failed to connect to the replica database. Primary is still operational.",
                NotificationType.ERROR, false, now.minusHours(2)));
        notificationsSignal.insertLast(new Notification(
                UUID.randomUUID().toString(), "New Team Member",
                "Alex Johnson has joined the engineering team. Say hello!",
                NotificationType.INFO, true, now.minusHours(3)));
        notificationsSignal.insertLast(new Notification(
                UUID.randomUUID().toString(), "Disk Space Warning",
                "Server disk usage is at 85%. Consider cleaning up old logs.",
                NotificationType.WARNING, false, now.minusHours(5)));
        notificationsSignal.insertLast(new Notification(
                UUID.randomUUID().toString(), "Build Completed",
                "CI pipeline build #1234 completed with all tests passing.",
                NotificationType.SUCCESS, true, now.minusHours(8)));
        notificationsSignal.insertLast(new Notification(
                UUID.randomUUID().toString(), "API Rate Limit Exceeded",
                "External API rate limit reached. Requests are being throttled.",
                NotificationType.ERROR, false, now.minusDays(1)));
    }
}
