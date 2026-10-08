package com.example.usecase13;

import java.util.HashMap;
import java.util.Map;

import com.example.common.UseCaseDescription;
import com.example.signals.SessionIdHelper;
import com.example.signals.UserInfo;
import com.example.signals.UserSessionRegistry;
import com.example.views.ColoredAvatar;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * Use Case 13: Real-Time Active Users Tracking
 *
 * Demonstrates application-scoped signals tracking all logged-in users with
 * automatic real-time updates when users join/leave or change views.
 *
 * Key Patterns: - Application-scoped ListSignal tracking all active users -
 * Automatic reactivity without polling or manual refresh - Multi-user real-time
 * synchronization - Computed signals for derived values - Reactive list
 * rendering with bindChildren
 */
@Route(value = "use-case-13", layout = MainLayout.class)
@PageTitle("Use Case 13: Real-Time Active Users")
@Menu(order = 13, title = "UC13 — Real-Time Active Users")
@UseCaseDescription("Showing who is online and which view they are on")
@StyleSheet("usecase13.css")
@AnonymousAllowed
public class UseCase13View extends VerticalLayout {

    private final UserSessionRegistry userSessionRegistry;
    private final Map<String, String> routeToTitleMap;

    public UseCase13View(UserSessionRegistry userSessionRegistry) {
        this.userSessionRegistry = userSessionRegistry;
        this.routeToTitleMap = buildRouteToTitleMap();

        addClassName("usecase13-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Use Case 13: Real-Time Active Users Tracking");

        Paragraph description = new Paragraph(
                "This use case demonstrates application-scoped signals tracking all logged-in users "
                        + "with automatic real-time updates when users join/leave or change views. "
                        + "Features include tab visibility tracking (Page Visibility API) and last interaction time. "
                        + "The ListSignal reactively propagates changes to all sessions without polling. "
                        + "Try opening multiple browser tabs with different users to see live synchronization!");

        Div counterBox = new Div();
        counterBox.addClassName("counter-box");

        H3 counterTitle = new H3(() -> "👥 Currently Online: "
                + userSessionRegistry.getActiveUsersSignal().get().size());
        counterTitle.addClassName("counter-title");

        counterBox.add(counterTitle);

        // User list container
        H3 userListTitle = new H3("Active Users");
        userListTitle.addClassName("user-list-title");

        Div userListContainer = new Div();
        userListContainer.addClassName("user-list-container");

        // Get current session ID for highlighting
        String currentSessionId = SessionIdHelper.getCurrentSessionId();

        // Reactively bind children to active users list
        userListContainer.bindChildren(
                userSessionRegistry.getActiveUsersSignal(),
                userSignal -> createUserCard(userSignal, currentSessionId));

        // Educational info box
        Div infoBox = new Div();
        infoBox.addClassName("info-box");

        H3 infoTitle = new H3("💡 How This Works");
        infoTitle.addClassName("info-title");

        VerticalLayout infoContent = new VerticalLayout();
        infoContent.setSpacing(false);
        infoContent.setPadding(false);

        infoContent.add(new Paragraph(
                "• Application-scoped ListSignal tracks all logged-in users globally"),
                new Paragraph(
                        "• Automatically updates when users join/leave without polling"),
                new Paragraph(
                        "• Tab visibility tracked using Page Visibility API (🟢 = active, ⚫ = inactive)"),
                new Paragraph(
                        "• Last interaction time shows when user last sent a server event"),
                new Paragraph(
                        "• Each user session is tracked with unique ID (vaadinSessionId:uiId)"),
                new Paragraph(
                        "• Try opening multiple browser tabs with different users and switching tabs!"));

        infoBox.add(infoTitle, infoContent);

        add(title, description, counterBox, userListTitle, userListContainer,
                infoBox);
    }

    private Card createUserCard(SharedValueSignal<UserInfo> userSignal,
            String currentSessionId) {
        Card card = new Card();
        card.addClassName("user-card");

        // These never change, so no need for signal bindings
        boolean isCurrentSession = userSignal.peek().sessionId()
                .equals(currentSessionId);
        String username = userSignal.peek().username();

        // Highlight current user's session
        if (isCurrentSession) {
            card.addClassName("current-session");
        }

        // Dim inactive tabs
        var isTabActive = userSignal.map(info -> info.isTabActive());
        card.getClassNames().bind("tab-inactive", isTabActive.map(a -> !a));

        // Header: Tab Status + Avatar + Username/Nickname + Role Badge
        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        // Tab activity indicator
        Span tabIndicator = new Span(() -> isTabActive.get() ? "🟢" : "⚫");
        tabIndicator.getElement().bindAttribute("title",
                () -> isTabActive.get() ? "Tab is active" : "Tab is inactive");
        tabIndicator.addClassName("tab-indicator");

        // Avatar with colored ring
        ColoredAvatar avatar = new ColoredAvatar(username,
                userSignal.peek().cssColor(), 40);

        Span nameSpan = new Span(() -> {
            var info = userSignal.get();
            // Display name with nickname if available
            String displayName = info.nickname() != null
                    && !info.nickname().isEmpty()
                            ? info.username() + " (" + info.nickname() + ")"
                            : info.username();

            // Add session number if multiple sessions for same user
            long sessionCount = userSessionRegistry.getActiveUsersSignal().get()
                    .stream()
                    .filter(us -> us.get().username().equals(info.username()))
                    .count();

            if (sessionCount > 1) {
                // Find this session's number
                long sessionNumber = 0;
                for (var us : userSessionRegistry.getActiveUsersSignal()
                        .get()) {
                    UserInfo u = us.get();
                    if (u.username().equals(info.username())) {
                        sessionNumber++;
                        if (u.sessionId().equals(info.sessionId())) {
                            break;
                        }
                    }
                }
                displayName = info.username() + " #" + sessionNumber;
                if (info.nickname() != null && !info.nickname().isEmpty()) {
                    displayName += " (" + info.nickname() + ")";
                }
            }

            if (isCurrentSession) {
                displayName += " (YOU)";
            }
            return displayName;
        });
        nameSpan.addClassName("name-span");

        // Role badge
        Span roleBadge = createRoleBadge(username);

        header.add(tabIndicator, avatar, nameSpan, roleBadge);
        card.setHeader(header);

        // Content: Current view and session duration
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);

        // Current view
        HorizontalLayout viewRow = new HorizontalLayout();
        viewRow.setAlignItems(FlexComponent.Alignment.CENTER);
        viewRow.setSpacing(false);
        viewRow.addClassName("row-with-gap");

        Span viewIcon = new Span("📍");
        Signal<@Nullable String> currentView = userSignal
                .map(UserInfo::currentView);
        Span viewText = new Span(() -> "Viewing: "
                + (currentView.get() != null ? formatViewName(currentView.get())
                        : "Unknown"));
        viewText.addClassName("secondary-text");

        viewRow.add(viewIcon, viewText);

        // Session duration
        HorizontalLayout durationRow = new HorizontalLayout();
        durationRow.setAlignItems(FlexComponent.Alignment.CENTER);
        durationRow.setSpacing(false);
        durationRow.addClassName("row-with-gap");

        Span durationIcon = new Span("🕐");
        Span durationTextSpan = new Span(
                () -> "Online for " + formatDuration(System.currentTimeMillis()
                        - userSignal.get().sessionStartTime()));
        durationTextSpan.addClassName("secondary-muted");

        durationRow.add(durationIcon, durationTextSpan);

        // Last interaction time
        HorizontalLayout interactionRow = new HorizontalLayout();
        interactionRow.setAlignItems(FlexComponent.Alignment.CENTER);
        interactionRow.setSpacing(false);
        interactionRow.addClassName("row-with-gap");

        Span interactionIcon = new Span("⚡");
        Span interactionTextSpan = new Span(() -> {
            long timeSinceInteraction = System.currentTimeMillis()
                    - userSignal.get().lastInteractionTime();
            String interactionText = timeSinceInteraction < 5000 ? "Just now"
                    : "Last active " + formatDuration(timeSinceInteraction)
                            + " ago";
            return interactionText;
        });
        interactionTextSpan.addClassName("secondary-muted");

        interactionRow.add(interactionIcon, interactionTextSpan);

        content.add(viewRow, durationRow, interactionRow);
        card.add(content);

        return card;
    }

    private Span createRoleBadge(String username) {
        // Get role based on username (matches SecurityConfiguration)
        String role = getRoleForUser(username);
        String badgeColor = switch (role) {
        case "SUPER_ADMIN" -> "#9C27B0";
        case "ADMIN" -> "#F44336";
        case "EDITOR" -> "#FF9800";
        case "VIEWER" -> "#2196F3";
        default -> "#757575";
        };

        String badgeLabel = switch (role) {
        case "SUPER_ADMIN" -> "S";
        case "ADMIN" -> "A";
        case "EDITOR" -> "E";
        case "VIEWER" -> "V";
        default -> "?";
        };

        Span badge = new Span("[" + badgeLabel + "]");
        badge.addClassName("role-badge");
        // Per-role color is dynamic — keep inline
        badge.getStyle().set("background", badgeColor);

        return badge;
    }

    private String getRoleForUser(String username) {
        // Map username to role based on SecurityConfiguration
        return switch (username) {
        case "superadmin" -> "SUPER_ADMIN";
        case "admin" -> "ADMIN";
        case "editor" -> "EDITOR";
        case "viewer" -> "VIEWER";
        default -> "VIEWER"; // Default role
        };
    }

    private String formatViewName(String viewRoute) {
        if (viewRoute == null || viewRoute.isEmpty()) {
            return "Unknown";
        }

        // Use cached route-to-title map
        return routeToTitleMap.getOrDefault(viewRoute, viewRoute);
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds != 1 ? "s" : "");
        }
    }

    private Map<String, String> buildRouteToTitleMap() {
        var routeToTitleMap = new HashMap<String, String>();
        MenuConfiguration.getMenuEntries().forEach(entry -> {
            // Store both with and without leading slash for flexible lookup
            String path = entry.path();
            String title = entry.title();
            routeToTitleMap.put(path, title);
            if (path.startsWith("/")) {
                routeToTitleMap.put(path.substring(1), title);
            } else {
                routeToTitleMap.put("/" + path, title);
            }
        });
        return routeToTitleMap;
    }
}
