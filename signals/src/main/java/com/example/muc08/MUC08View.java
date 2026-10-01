package com.example.muc08;

import jakarta.annotation.security.PermitAll;

import com.example.security.CurrentUserSignal;
import com.example.signals.UserSessionRegistry;
import com.example.views.ActiveUsersDisplay;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * Multi-User Case 8: Broadcast an Announcement to All Active Users
 *
 * Demonstrates pushing a message to every open session with: - A single
 * application-scoped SharedValueSignal holding the current announcement - A
 * banner bound to the signal, so late joiners see the current announcement
 * immediately - A Notification shown in every open tab when a new announcement
 * is posted - Clearing the announcement removes the banner everywhere
 *
 * Key Patterns: - Replaces the classic static Broadcaster (listener set +
 * executor + UI.access) with a shared signal - Signal.effect with
 * EffectContext.isInitialRun() to distinguish "already there" from "just
 * arrived" - Push-based real-time updates to all connected clients
 */
@Route(value = "muc-08", layout = MainLayout.class)
@PageTitle("MUC 8: Broadcast Announcement")
@Menu(order = 57, title = "MUC 8: Broadcast Announcement")
@StyleSheet("muc08.css")
@PermitAll
public class MUC08View extends VerticalLayout {

    public MUC08View(CurrentUserSignal currentUserSignal,
            MUC08Signals muc08Signals,
            UserSessionRegistry userSessionRegistry) {
        CurrentUserSignal.UserInfo userInfo = currentUserSignal.getUserSignal()
                .peek();
        if (userInfo == null || !userInfo.isAuthenticated()) {
            throw new IllegalStateException(
                    "User must be authenticated to access this view");
        }
        String currentUser = userInfo.getUsername();

        addClassName("muc08-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2(
                "Multi-User Case 8: Broadcast an Announcement to All Active Users");

        Paragraph description = new Paragraph(
                "Post an announcement and every user with this page open sees it immediately, "
                        + "as a banner and a notification. Users who open the page later still see the "
                        + "current announcement in the banner. The announcement lives in a single "
                        + "application-scoped SharedValueSignal; there is no broadcaster, listener "
                        + "registry or UI.access() code. Try opening this page in multiple browser windows.");

        ActiveUsersDisplay activeUsersDisplay = new ActiveUsersDisplay(
                userSessionRegistry, "muc-08");

        SharedValueSignal<MUC08Signals.@Nullable Announcement> announcementSignal = muc08Signals
                .getAnnouncementSignal();

        // Banner: visible only while there is an announcement
        Div banner = new Div();
        banner.addClassName("announcement-banner");
        banner.bindVisible(announcementSignal.map(a -> a != null));

        Span bannerText = new Span();
        bannerText.addClassName("announcement-text");
        bannerText.bindText(
                announcementSignal.map(a -> a == null ? "" : a.text()));

        Span bannerMeta = new Span();
        bannerMeta.addClassName("announcement-meta");
        bannerMeta.bindText(announcementSignal.map(a -> a == null ? ""
                : "Posted by " + a.author() + " at "
                        + a.getFormattedTimestamp()));

        banner.add(bannerText, bannerMeta);

        Paragraph noAnnouncement = new Paragraph("No active announcement.");
        noAnnouncement.addClassName("no-announcement");
        noAnnouncement.bindVisible(announcementSignal.map(a -> a == null));

        // Toast: only for announcements that arrive while this view is open.
        // The initial run covers late joiners, who already see the banner.
        Signal.effect(this, context -> {
            MUC08Signals.@Nullable Announcement announcement = announcementSignal
                    .get();
            if (announcement != null && !context.isInitialRun()) {
                Notification.show("Announcement: " + announcement.text(), 5000,
                        Notification.Position.TOP_CENTER);
            }
        });

        // Admin-style console
        H3 consoleTitle = new H3("Announcement console");

        TextArea messageInput = new TextArea("Message");
        messageInput.setPlaceholder("e.g. Maintenance starts in 10 minutes");
        messageInput.setWidthFull();
        messageInput.setMaxLength(280);

        Button postButton = new Button("Post announcement", event -> {
            String text = messageInput.getValue();
            if (text != null && !text.isBlank()) {
                muc08Signals.post(new MUC08Signals.Announcement(currentUser,
                        text.trim()));
                messageInput.clear();
            }
        });
        postButton.addThemeVariants(ButtonVariant.PRIMARY);

        Button clearButton = new Button("Clear announcement",
                event -> muc08Signals.clear());
        clearButton.addThemeVariants(ButtonVariant.ERROR);
        clearButton.bindEnabled(announcementSignal.map(a -> a != null));

        HorizontalLayout actions = new HorizontalLayout(postButton,
                clearButton);

        add(title, description, activeUsersDisplay, banner, noAnnouncement,
                consoleTitle, messageInput, actions);
    }
}
