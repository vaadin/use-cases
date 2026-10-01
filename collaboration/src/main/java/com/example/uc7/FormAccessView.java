package com.example.uc7;

import com.example.collab.Outcomes;
import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.Presence;
import com.example.collab.UseCaseView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.Signal;

/**
 * UC7 — Control who may edit. The sampler's {@code advanced-managers} sample,
 * which combines {@code PresenceManager} and {@code FormManager} to hand write
 * access around a form.
 * <p>
 * Signals do this better than Collaboration Kit in one respect and worse in
 * another, and the view is built to show both.
 * <p>
 * Better: the lock is real. Claiming it is {@code holder.replace(NOBODY, me)} —
 * a compare-and-set in the shared state, so two peers claiming simultaneously
 * produce one winner and one rejected operation, and the loser is told. And
 * write access is enforced where the data is: each peer writes through a handle
 * carrying a validator, so a spectator's write is refused rather than merely
 * discouraged by a disabled field.
 * <p>
 * Worse: a {@code CommandValidator} sees the command, not its author. The
 * validator can only work because it closes over the peer it was created for,
 * which makes it a capability handed to that peer rather than an access rule on
 * the data. A second handle without the validator can still write. See
 * API-GAPS.md #11.
 */
@Route(value = "form-access", layout = MainLayout.class)
@RouteAlias(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Control who may edit")
@Menu(order = 7, title = "UC7 Write access")
public class FormAccessView extends UseCaseView {

    static final String TOPIC = "uc7-access";

    public FormAccessView(AccessTopic topic, Presence presence) {
        super("UC7 — Control who may edit",
                "One user at a time may edit. Claim editing in one panel and the fields unlock there and lock "
                        + "everywhere else; claim it in a second panel while the first still holds it and the "
                        + "operation is rejected, with the reason logged.");

        add(new PeerRig(peer -> peerView(topic, presence, peer)));

        addNote("The lock is a compare-and-set on a shared value, so a lost race is reported instead of "
                + "silently overwriting. The validator that refuses a spectator's write closes over the peer, "
                + "because a CommandValidator is not told who is writing (API-GAPS.md #11).");
    }

    private static Component peerView(AccessTopic topic, Presence presence,
            Peer peer) {
        var presenceTopic = presence.topic(TOPIC);
        var form = topic.formFor(peer);
        Signal<Boolean> isHolder = topic.isHolder(peer);

        AvatarGroup group = new AvatarGroup();
        group.setMaxItemsVisible(4);
        group.bindItems(Presence.avatarItems(presenceTopic));

        Span status = new Span();
        status.bindText(topic.holder()
                .map(holder -> holder == null || holder.id() == Peer.NOBODY.id()
                        ? "Nobody is editing"
                        : holder.name() + " is editing"));

        Div log = new Div();
        log.addClassName("event-log");

        Button claim = new Button("Claim editing", event -> Outcomes.report(log,
                "claim", topic.holder().replace(Peer.NOBODY, peer)));
        claim.bindEnabled(Signal.not(isHolder));

        Button release = new Button("Release", event -> Outcomes.report(log,
                "release", topic.holder().replace(peer, Peer.NOBODY)));
        release.bindEnabled(isHolder);

        Button takeOver = new Button("Take over", event -> Outcomes.report(log,
                "take over", topic.holder().set(peer)));

        TextField name = new TextField("Name");
        name.setValueChangeMode(ValueChangeMode.EAGER);
        TextField address = new TextField("Address");
        address.setValueChangeMode(ValueChangeMode.EAGER);

        form.bindField(name, AccessTopic.NAME, peer);
        form.bindField(address, AccessTopic.ADDRESS, peer);
        name.bindReadOnly(Signal.not(isHolder));
        address.bindReadOnly(Signal.not(isHolder));

        FormLayout fields = new FormLayout(name, address);
        fields.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Div content = new Div(group, status,
                new HorizontalLayout(claim, release, takeOver), fields, log);
        presence.bind(content, TOPIC, peer);
        return content;
    }

}
