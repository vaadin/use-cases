package com.example.uc4;

import java.time.Instant;
import java.util.List;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.Presence;
import com.example.collab.UseCaseView;
import com.example.uc4.ChatTopic.Message;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.Signal;

/**
 * UC4 — Chat in real time. The sampler's {@code chat} sample, which is
 * {@code CollaborationMessageList} plus {@code CollaborationMessageInput}.
 * <p>
 * This one is nearly free: a chat is an append-only list, {@code MessageList}
 * binds to a signal of message signals out of the box, and {@code insertLast}
 * on a shared list is already the atomic append that a chat needs — two peers
 * sending at the same moment cannot overwrite each other, which is the whole
 * reason CE has a list type in the first place.
 * <p>
 * What the sampler does not show, and this view does, is persistence.
 * Collaboration Kit has a hook for it; signals do not, so
 * {@link ChatTopic#send} writes to the store and to the signal, and the
 * <em>Restart the server</em> button rebuilds the signal from the store.
 */
@Route(value = "chat", layout = MainLayout.class)
@RouteAlias(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Chat in real time")
@Menu(order = 4, title = "UC4 Real-time chat")
public class ChatView extends UseCaseView {

    static final String TOPIC = "uc4-chat";

    public ChatView(ChatTopic chat, Presence presence) {
        super("UC4 — Chat in real time",
                "One room, several users. Sending from any panel appends to the shared message list, so every "
                        + "other panel — and every other browser tab — shows it immediately.");

        Span stored = new Span();
        Button restart = new Button("Restart the server", event -> {
            chat.simulateRestart();
            stored.setText(
                    chat.historySize() + " message(s) reloaded from the store");
        });
        Div controls = new Div(restart, stored);
        controls.addClassName("uc-controls");

        add(new PeerRig(peer -> peerView(chat, presence, peer)), controls);

        addNote("MessageList.bindItems is core Flow, and insertLast on a shared list is an atomic append, so "
                + "the live half of a chat needs no helper code at all. Durability does: there is no persister "
                + "hook on a signal, so ChatTopic writes to its store and to the signal separately and reloads "
                + "by hand (API-GAPS.md #4).");
    }

    private static Component peerView(ChatTopic chat, Presence presence,
            Peer peer) {
        var topic = presence.topic(TOPIC);

        AvatarGroup group = new AvatarGroup();
        group.setMaxItemsVisible(4);
        group.bindItems(Presence.avatarItems(topic));

        MessageList list = new MessageList();
        list.setHeight("14rem");
        list.bindItems(items(chat));

        MessageInput input = new MessageInput();
        input.addSubmitListener(
                event -> chat.send(new Message(peer.id(), peer.name(),
                        peer.colorIndex(), event.getValue(), Instant.now())));

        Div content = new Div(group, list, input);
        presence.bind(content, TOPIC, peer);
        return content;
    }

    /**
     * The message list wants a signal of item signals, so each entry is mapped
     * on its own: appending a message re-runs the outer computation, editing
     * one would re-render only that item.
     */
    private static Signal<List<Signal<MessageListItem>>> items(ChatTopic chat) {
        return Signal.computed(() -> chat.messages().get().stream()
                .<Signal<MessageListItem>> map(entry -> entry
                        .map(message -> message == null ? new MessageListItem()
                                : message.toItem()))
                .toList());
    }
}
