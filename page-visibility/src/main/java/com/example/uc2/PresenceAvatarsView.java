package com.example.uc2;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.example.uc2.PresenceRegistry.Presence;
import com.example.views.ColoredAvatar;
import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.PageVisibility;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * UC2 — Presence / "away" status.
 * <p>
 * Each browser tab that opens this view registers a presence in
 * {@link PresenceRegistry}. The avatar strip is bound to the registry's
 * cross-session {@link com.vaadin.flow.signals.shared.SharedListSignal
 * SharedListSignal}, so every UI in any {@code VaadinSession} sees the same set
 * of avatars. The visibility signal of each tab drives the styling of its own
 * avatar; the change is broadcast to all other UIs through the registry signal.
 */
@Route(value = "uc2", layout = MainLayout.class)
@Menu(order = 2, title = "UC2 — Presence")
@StyleSheet("uc2.css")
public class PresenceAvatarsView extends VerticalLayout {

    private static final List<String> ANIMALS = List.of("Otter", "Falcon",
            "Lynx", "Beaver", "Heron", "Marten", "Badger", "Hare", "Wolf",
            "Owl", "Fox", "Crane", "Stoat", "Eagle", "Mole");
    private static final List<String> COLORS = List.of("#1976d2", "#388e3c",
            "#d81b60", "#f57c00", "#7b1fa2", "#0097a7", "#5d4037", "#455a64");

    private final PresenceRegistry registry;
    private final String id = UUID.randomUUID().toString();
    private final String name;
    private final String color;
    private final Div avatarStrip = new Div();

    public PresenceAvatarsView(PresenceRegistry registry) {
        this.registry = registry;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        this.name = ANIMALS.get(rnd.nextInt(ANIMALS.size())) + "-"
                + (100 + rnd.nextInt(900));
        this.color = COLORS.get(rnd.nextInt(COLORS.size()));

        addClassName("uc2-view");
        add(new H1("UC2 — Presence avatars"));
        add(new Paragraph("Open this page in two or more browsers. Each "
                + "tab gets a random colour and animal name. Hide a tab "
                + "or move focus away from a window, and the matching "
                + "avatar greys out for everyone else within ~100 ms."));

        H2 youHeader = new H2("You");
        Span youLabel = new Span("Joined as ");
        Span youName = new Span(name);
        youName.addClassName("presence-you-name");
        // Dynamic per-instance color is fed to the CSS rule as a custom
        // property — the .presence-you-name rule consumes var(--you-color).
        youName.getStyle().set("--you-color", color);
        Div youRow = new Div(youLabel, youName);

        H2 strip = new H2("In the room");
        avatarStrip.addClassName("avatar-strip");

        add(youHeader, youRow, strip, avatarStrip);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registry.join(new Presence(id, name, color, PageVisibility.VISIBLE));
        registry.bindTo(avatarStrip, this::renderAvatar);

        Signal.effect(this, () -> {
            PageVisibility state = attachEvent.getUI().getPage()
                    .pageVisibilitySignal().get();
            registry.updateState(id, state);
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        registry.leave(id);
        super.onDetach(detachEvent);
    }

    private Component renderAvatar(SharedValueSignal<Presence> entry) {
        Presence p = entry.peek();
        ColoredAvatar avatar = new ColoredAvatar(p.name(), p.color(), 56);
        avatar.bindState(entry.map(Presence::state));
        return avatar;
    }
}
