package com.example.collab;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * One simulated user's window onto a use case: a header naming the peer and a
 * body holding that peer's own instance of the view under demonstration.
 * <p>
 * The <em>Disconnect</em> button is the interesting part. It removes the body
 * from the DOM, which detaches it, which disposes the effects inside it and
 * fires the detach listeners that presence and field highlighting hang off —
 * the same sequence as closing a browser tab, only observable from a test. It
 * is how every use case here demonstrates a user leaving without needing a
 * second session.
 */
public class PeerPanel extends Div {

    private final Peer peer;
    private final SerializableFunction<Peer, Component> contentFactory;
    private final Div body = new Div();
    private final ValueSignal<Boolean> connected = new ValueSignal<>(
            Boolean.TRUE);

    public PeerPanel(Peer peer,
            SerializableFunction<Peer, Component> contentFactory) {
        this.peer = peer;
        this.contentFactory = contentFactory;

        addClassName("peer-panel");
        getStyle().set("--peer-color", peer.cssColor());

        Avatar avatar = new Avatar(peer.name());
        avatar.setAbbreviation(peer.abbreviation());
        avatar.setColorIndex(peer.colorIndex());
        avatar.setThemeName("small");

        Span name = new Span(peer.name());
        name.addClassName("peer-name");

        Button toggle = new Button();
        toggle.addThemeVariants(ButtonVariant.LUMO_SMALL,
                ButtonVariant.LUMO_TERTIARY);
        toggle.addClickListener(event -> setConnected(!isConnected()));
        Signal.effect(this, () -> toggle
                .setText(connected.get() ? "Disconnect" : "Connect"));

        HorizontalLayout header = new HorizontalLayout(avatar, name, toggle);
        header.addClassName("peer-header");
        header.setAlignItems(Alignment.CENTER);

        body.addClassName("peer-body");
        body.add(contentFactory.apply(peer));

        add(header, body);
    }

    public Peer getPeer() {
        return peer;
    }

    public boolean isConnected() {
        return Boolean.TRUE.equals(connected.peek());
    }

    /**
     * Connects or disconnects this peer. Disconnecting throws the body away
     * rather than hiding it: a hidden component stays attached, and an attached
     * component is still present as far as every use case here is concerned.
     */
    public void setConnected(boolean value) {
        if (value == isConnected()) {
            return;
        }
        body.removeAll();
        if (value) {
            body.add(contentFactory.apply(peer));
        }
        connected.set(value);
    }
}
