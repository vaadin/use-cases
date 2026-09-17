package com.example.uc3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.Presence;
import com.example.collab.UseCaseView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.Signal;

/**
 * UC3 — Render presence my own way. The sampler's {@code custom-user-list}
 * sample, where {@code PresenceManager.setPresenceHandler} drives a hand-built
 * list of users.
 * <p>
 * Rendering is the easy half: presence is data, so a list of rows is a
 * {@code Signal.effect} over the same map UC1 gave to an {@code AvatarGroup}.
 * Renaming shows the other reason presence is a map — a rename is a {@code put}
 * on the peer's own key, so it updates that row and reorders nothing.
 * <p>
 * The hard half is the log. CE's presence handler is called <em>per user, with
 * a callback for that user leaving</em>, so "Ada joined" and "Ada left" fall
 * out of the API. An effect is called with the new state and nothing else, so
 * this view keeps a snapshot of what it last rendered and diffs against it.
 * That is API-GAPS.md #9: signals carry state, not change events.
 */
@Route(value = "presence-custom", layout = MainLayout.class)
@RouteAlias(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Render presence my own way")
@Menu(order = 3, title = "UC3 Custom user list")
public class CustomUserListView extends UseCaseView {

    static final String TOPIC = "uc3-custom-list";

    public CustomUserListView(Presence presence) {
        super("UC3 — Render presence my own way",
                "The same presence map as UC1, rendered as a list instead of an avatar group, with an event log "
                        + "beside it. Rename a user to see the entry update in every panel, and disconnect one to "
                        + "see it leave.");

        add(new PeerRig(peer -> peerView(presence, peer)));

        addNote("The list is an effect over the presence map; the log is not. An effect sees the new state, so "
                + "'who joined' and 'who left' are computed by diffing against the previously rendered snapshot — "
                + "work CE's per-user presence handler does for you (API-GAPS.md #9).");
    }

    private static Component peerView(Presence presence, Peer peer) {
        var topic = presence.topic(TOPIC);

        Div userList = new Div();
        userList.addClassName("user-list");

        Div log = new Div();
        log.addClassName("event-log");

        TextField rename = new TextField("Rename me");
        rename.setPlaceholder(peer.name());
        rename.setValueChangeMode(
                com.vaadin.flow.data.value.ValueChangeMode.ON_CHANGE);
        rename.addValueChangeListener(event -> {
            String value = event.getValue().trim();
            topic.put(peer.key(),
                    value.isEmpty() ? peer : peer.withName(value));
        });

        Signal<List<Peer>> peers = Presence.peers(topic);

        // What this panel last rendered. Not a signal: it is the effect's own
        // memory of the previous state, which is the only way to turn "here is
        // the new set" back into "somebody joined".
        Map<Integer, Peer> rendered = new LinkedHashMap<>();

        Div content = new Div(rename, userList, log);
        presence.bind(content, TOPIC, peer);

        Signal.effect(content, () -> {
            List<Peer> current = peers.get();

            userList.removeAll();
            current.forEach(entry -> userList.add(row(entry, peer)));

            describeChanges(rendered, current)
                    .forEach(line -> log.addComponentAsFirst(new Div(line)));
            rendered.clear();
            current.forEach(entry -> rendered.put(entry.id(), entry));
        });

        return content;
    }

    private static Component row(Peer entry, Peer localPeer) {
        Avatar avatar = new Avatar(entry.name());
        avatar.setAbbreviation(entry.abbreviation());
        avatar.setColorIndex(entry.colorIndex());
        avatar.setThemeName("xsmall");

        Span name = new Span(entry.name());
        if (entry.id() == localPeer.id()) {
            name.setText(entry.name() + " (you)");
        }

        HorizontalLayout row = new HorizontalLayout(avatar, name);
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        row.addClassName("user-row");
        return row;
    }

    /**
     * Turns two snapshots into the joined / left / renamed lines the sampler's
     * presence handler would have reported directly.
     */
    private static List<String> describeChanges(Map<Integer, Peer> previous,
            List<Peer> current) {
        List<String> lines = new ArrayList<>();
        Map<Integer, Peer> now = new LinkedHashMap<>();
        current.forEach(entry -> now.put(entry.id(), entry));

        now.forEach((id, entry) -> {
            Peer before = previous.get(id);
            if (before == null) {
                lines.add(entry.name() + " joined");
            } else if (!before.name().equals(entry.name())) {
                lines.add(before.name() + " is now " + entry.name());
            }
        });
        previous.forEach((id, entry) -> {
            if (!now.containsKey(id)) {
                lines.add(entry.name() + " left");
            }
        });
        return lines;
    }
}
