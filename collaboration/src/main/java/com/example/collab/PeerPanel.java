package com.example.collab;

import com.example.common.AppWindow;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * One simulated user's browser window onto a use case: an {@link AppWindow}
 * titled with the peer's name and showing the view's address, holding that
 * peer's own instance of the view under demonstration.
 * <p>
 * The <em>Disconnect</em> button is the interesting part. It removes the body
 * from the DOM, which detaches it, which disposes the effects inside it and
 * runs the detach cleanups that presence and field highlighting hang off — the
 * same sequence as closing a browser tab, only observable from a test. It is
 * how every use case here demonstrates a user leaving without needing a second
 * session.
 */
public class PeerPanel extends AppWindow {

    private final Peer peer;
    private final SerializableFunction<Peer, Component> contentFactory;
    private final Div body = new Div();
    private final ValueSignal<Boolean> connected = new ValueSignal<>(
            Boolean.TRUE);

    public PeerPanel(Peer peer,
            SerializableFunction<Peer, Component> contentFactory) {
        super(peer.name(), null);
        this.peer = peer;
        this.contentFactory = contentFactory;

        addClassName("peer-panel");
        getStyle().set("--peer-color", peer.cssColor());
        // Every peer has the same page open, as two browsers would. The name
        // goes back to the pool when the window is gone, so that the next
        // visitor does not get "Ada Bergström 2" while nobody is Ada.
        whenAttached(ui -> {
            setAddress(
                    "collab.example/" + ui.getActiveViewLocation().getPath());
            PeerNames.claim(peer);
            return () -> PeerNames.release(peer);
        });

        Button toggle = new Button();
        toggle.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        toggle.addClickListener(event -> setConnected(!isConnected()));
        Signal.effect(this, () -> toggle
                .setText(connected.get() ? "Disconnect" : "Connect"));
        addToTitleBar(toggle);

        body.addClassName("peer-body");
        body.add(contentFactory.apply(peer));
        setContent(body);
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
