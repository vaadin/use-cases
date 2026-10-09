package com.example.muc01;

import com.example.common.UseCaseDescription;
import com.example.security.CurrentUserSignal;
import com.example.signals.SessionIdHelper;
import com.example.signals.UserSessionRegistry;
import com.example.views.ActiveUsersDisplay;
import com.example.views.ColoredAvatar;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Multi-User Case 1: Shared Chat/Message List
 *
 * Demonstrates a collaborative chat interface with: - Append-only shared
 * message list signal - Multiple users adding messages - Server-side signal
 * source with Push updates - Each user can only append, not modify history
 *
 * Key Patterns: - Application-scoped SharedListSignal&lt;Message&gt; across
 * sessions - Append-only operations (insertLast) - Server-side signal
 * coordination via Spring @Component - Push-based real-time updates to all
 * connected clients
 */
@Route(value = "muc-01", layout = MainLayout.class)
@PageTitle("MUC 1: Shared Chat")
@UseCaseDescription("Chatting between users with a shared, append-only message list")
@Menu(order = 50, title = "MUC 1: Shared Chat")
@StyleSheet("muc01.css")
@AnonymousAllowed
public class MUC01View extends VerticalLayout {

    private final String currentUser;
    private final MUC01Signals muc01Signals;
    private final UserSessionRegistry userSessionRegistry;
    private @Nullable String sessionId;

    public MUC01View(CurrentUserSignal currentUserSignal,
            MUC01Signals muc01Signals,
            UserSessionRegistry userSessionRegistry) {
        this.currentUser = currentUserSignal.getUserSignal().peek()
                .getUsername();
        this.muc01Signals = muc01Signals;
        this.userSessionRegistry = userSessionRegistry;

        addClassName("muc01-view");
        setSpacing(true);
        setPadding(true);
        setHeight("100%");

        H2 title = new H2("Multi-User Case 1: Shared Chat/Message List");

        Paragraph description = new Paragraph(
                "This use case demonstrates a collaborative chat with an append-only shared message list. "
                        + "Messages are stored in an application-scoped SharedListSignal and broadcast to all users via Push. "
                        + "Try opening this page in multiple browser windows (different users) to see real-time collaboration.");

        // Active users display
        ActiveUsersDisplay activeUsersDisplay = new ActiveUsersDisplay(
                userSessionRegistry, "muc-01");

        // Message display area
        Div messagesContainer = new Div();
        messagesContainer.setWidthFull();
        messagesContainer.addClassName("messages-container");

        // Bind message list to UI
        messagesContainer.bindChildren(muc01Signals.getMessagesSignal(),
                msgSignal -> createMessageComponent(msgSignal.peek()));

        // Message input
        TextField messageInput = new TextField();
        messageInput.setPlaceholder("Type your message and press Enter...");
        messageInput.setWidthFull();
        messageInput.setMaxLength(500);

        Runnable sendMessage = () -> {
            String text = messageInput.getValue();
            if (text != null && !text.trim().isEmpty() && sessionId != null) {
                String displayName = getCurrentDisplayName();
                muc01Signals.appendMessage(new MUC01Signals.Message(currentUser,
                        displayName, text.trim()));
                messageInput.clear();
            }
        };

        messageInput.addKeyPressListener(Key.ENTER, event -> sendMessage.run());

        Button sendButton = new Button("Send Message",
                event -> sendMessage.run());
        sendButton.addThemeName("primary");

        Button clearButton = new Button("Clear All Messages", event -> {
            muc01Signals.clearMessages();
        });
        clearButton.addThemeName("error");
        clearButton.addThemeName("small");

        // Message count
        Div messageCount = new Div();
        messageCount.bindText(muc01Signals.getMessagesSignal()
                .map(messages -> "Total messages: " + messages.size()));
        messageCount.addClassName("message-count");

        add(title, description, activeUsersDisplay, new H3("Messages"),
                messagesContainer, messageCount, messageInput, sendButton,
                clearButton);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.sessionId = SessionIdHelper.getCurrentSessionId();
    }

    private Div createMessageComponent(MUC01Signals.Message message) {
        String senderColor = userSessionRegistry
                .getUserColorByUsername(message.username());

        Div messageDiv = new Div();
        messageDiv.addClassName("message-row");
        // Per-user color is dynamic — keep inline
        messageDiv.getStyle().set("border-left", "3px solid " + senderColor);

        // Avatar
        ColoredAvatar avatar = new ColoredAvatar(message.username(),
                senderColor, 40);

        // Content area (header + text)
        Div contentArea = new Div();
        contentArea.addClassName("message-content");

        Div header = new Div();
        header.addClassName("message-header");

        Div author = new Div();
        author.setText(message.author());
        author.addClassName("message-author");
        // Per-user color is dynamic — keep inline
        author.getStyle().set("color", senderColor);

        Div timestamp = new Div();
        timestamp.setText(message.getFormattedTimestamp());
        timestamp.addClassName("message-timestamp");

        header.add(author, timestamp);

        Div text = new Div();
        text.setText(message.text());
        text.addClassName("message-text");

        contentArea.add(header, text);
        messageDiv.add(avatar, contentArea);
        return messageDiv;
    }

    private String getCurrentDisplayName() {
        if (sessionId == null) {
            return currentUser;
        }

        var users = userSessionRegistry.getActiveUsersSignal().peek();
        var displayNames = userSessionRegistry.getDisplayNamesSignal().peek();

        // Find matching session key for current user
        String sessionKey = currentUser + ":" + sessionId;
        for (int i = 0; i < users.size() && i < displayNames.size(); i++) {
            if (sessionKey.equals(users.get(i).peek().getCompositeKey())) {
                return displayNames.get(i);
            }
        }

        // Fallback to username
        return currentUser;
    }
}
