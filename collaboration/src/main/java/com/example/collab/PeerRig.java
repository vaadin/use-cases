package com.example.collab;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableFunction;

/**
 * The demo harness borrowed from the Collaboration Engine Sampler: several
 * simulated users side by side in one page, with an <em>Add user</em> button
 * that adds another one.
 * <p>
 * Every use case in this module is built on it, for two reasons. It is how the
 * sampler demonstrates collaboration without asking the reader to open a second
 * browser, and it is what makes the repository's browserless testing convention
 * able to assert propagation: drive peer A's field, assert peer B's panel, one
 * session, no polling.
 * <p>
 * The panels share nothing but the application-scoped signals the use case
 * uses. Two peers here are as separate as two browsers would be — except for
 * the session, which is why {@code Peer} identity is explicit everywhere
 * instead of read from {@code VaadinSession}.
 */
public class PeerRig extends VerticalLayout {

    private final SerializableFunction<Peer, Component> contentFactory;
    private final Div strip = new Div();

    public PeerRig(SerializableFunction<Peer, Component> contentFactory) {
        this(contentFactory, 2);
    }

    public PeerRig(SerializableFunction<Peer, Component> contentFactory,
            int initialPeers) {
        this.contentFactory = contentFactory;

        setPadding(false);
        setSpacing(false);
        addClassName("peer-rig");

        Button addUser = new Button("Add user", event -> addPeer());
        addUser.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div toolbar = new Div(addUser);
        toolbar.addClassName("peer-toolbar");

        strip.addClassName("peer-strip");

        add(toolbar, strip);

        for (int i = 0; i < initialPeers; i++) {
            addPeer();
        }
    }

    public PeerPanel addPeer() {
        PeerPanel panel = new PeerPanel(PeerNames.next(), contentFactory);
        strip.add(panel);
        return panel;
    }

    /** The panels currently in the rig, in the order they were added. */
    public List<PeerPanel> getPanels() {
        return strip.getChildren().filter(PeerPanel.class::isInstance)
                .map(PeerPanel.class::cast).toList();
    }

    public PeerPanel getPanel(int index) {
        return getPanels().get(index);
    }
}
