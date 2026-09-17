package com.example.uc1;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.Presence;
import com.example.collab.UseCaseView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

/**
 * UC1 — Show who is here. The sampler's {@code avatar-group} sample, which is
 * one line of Collaboration Kit: {@code new CollaborationAvatarGroup(user,
 * topic)}.
 * <p>
 * With signals it is a presence map and a binding. The interesting part is that
 * no component is involved in the sharing: the group renders a
 * {@code Signal<List<Signal<AvatarGroupItem>>>}, and {@code AvatarGroup} itself
 * knows how to follow one — {@code bindItems} is core Flow API, not something
 * this module had to write.
 * <p>
 * Joining and leaving is where the work moved. CE's avatar group joins the
 * topic when it attaches and leaves when it detaches, including when the tab
 * closes. Here {@link Presence#bind} does the attach half with listeners, and
 * the tab-closed half only works because the session eventually expires and
 * detaches the UI. Nothing removes a peer whose server died — API-GAPS.md #1.
 */
@Route(value = "presence", layout = MainLayout.class)
@RouteAlias(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Show who is here")
@Menu(order = 1, title = "UC1 Show who is here")
public class PresenceView extends UseCaseView {

    static final String TOPIC = "uc1-presence";

    public PresenceView(Presence presence) {
        super("UC1 — Show who is here",
                "Every panel is a separate user looking at the same topic. Each one shows an AvatarGroup of "
                        + "everybody currently present, so adding a user or disconnecting one changes all the other "
                        + "panels at once. Open a second browser tab to see the same avatars there.");

        add(new PeerRig(peer -> peerView(presence, peer)));

        addNote("The group follows the presence map through AvatarGroup.bindItems, which core Flow provides. "
                + "Presence itself is a SharedMapSignal<Peer> keyed by peer, joined on attach and left on detach — "
                + "the one thing Collaboration Kit does that this cannot: an entry whose owner disappears without "
                + "detaching stays in the map forever (API-GAPS.md #1).");
    }

    private static Component peerView(Presence presence, Peer peer) {
        var topic = presence.topic(TOPIC);

        AvatarGroup group = new AvatarGroup();
        group.setMaxItemsVisible(4);
        group.bindItems(Presence.avatarItems(topic));

        Span count = new Span();
        count.bindText(Presence.peers(topic)
                .map(peers -> peers.size() + " user(s) present"));

        Div content = new Div(group, count);
        // The content component, not the panel: disconnecting a peer removes
        // this from the DOM, and the detach is what leaves the topic.
        presence.bind(content, TOPIC, peer);
        return content;
    }
}
