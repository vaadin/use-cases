package com.example.uc2;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.Presence;
import com.example.collab.UseCaseView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

/**
 * UC2 — Join and leave on purpose. The sampler's {@code custom-avatar-group}
 * sample: {@code PresenceManager.markAsPresent(boolean)} behind a checkbox.
 * <p>
 * The distinction it draws is the point. "Attached" and "present" are not the
 * same thing: a user can be looking at the view without being listed in it, and
 * the sampler's own wording — <em>join and leave the group
 * programmatically</em> — is about exactly that.
 * <p>
 * Signals make the distinction obvious, because presence is a map somebody has
 * to write to. UC1 writes to it on attach; this view writes to it when the
 * checkbox changes. What still has to be wired either way is the detach: an
 * opted-in peer that goes away has to be taken out of the map, or it lingers.
 */
@Route(value = "presence-opt-in", layout = MainLayout.class)
@RouteAlias(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Join and leave on purpose")
@Menu(order = 2, title = "UC2 Join and leave")
public class OptInPresenceView extends UseCaseView {

    static final String TOPIC = "uc2-opt-in";

    public OptInPresenceView(Presence presence) {
        super("UC2 — Join and leave on purpose",
                "Each user is connected to the topic but only appears in the avatar group after ticking the box. "
                        + "Untick it and the avatar disappears for everybody while that user keeps watching.");

        add(new PeerRig(peer -> peerView(presence, peer)));

        addNote("markAsPresent(true/false) becomes put/remove on the presence map. Because membership is no "
                + "longer tied to attach, the detach listener has to be kept anyway — otherwise a peer that "
                + "joined and then vanished is still in the map (API-GAPS.md #1).");
    }

    private static Component peerView(Presence presence, Peer peer) {
        var topic = presence.topic(TOPIC);

        AvatarGroup group = new AvatarGroup();
        group.setMaxItemsVisible(4);
        group.bindItems(Presence.avatarItems(topic));

        Span count = new Span();
        count.bindText(Presence.peers(topic)
                .map(peers -> peers.size() + " user(s) listed"));

        Checkbox join = new Checkbox("Show me in the group");
        join.addValueChangeListener(event -> {
            if (Boolean.TRUE.equals(event.getValue())) {
                topic.put(peer.key(), peer);
            } else {
                topic.remove(peer.key());
            }
        });

        Div content = new Div(join, group, count);
        // Opting in is the user's decision, leaving is not: a peer that is
        // gone cannot be present, whatever the checkbox said.
        content.addDetachListener(event -> topic.remove(peer.key()));
        return content;
    }
}
