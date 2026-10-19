package com.example.uc2;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.example.common.UseCaseDescription;
import com.example.uc2.PresenceRegistry.Presence;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.PageVisibility;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * UC2 — Presence.
 * <p>
 * Each browser tab that opens this view registers a presence in
 * {@link PresenceRegistry}. The avatar strip is bound to the registry's
 * cross-session {@link com.vaadin.flow.signals.shared.SharedListSignal
 * SharedListSignal}, so every UI in any {@code VaadinSession} sees the same set
 * of avatars. The visibility signal of each tab drives the styling of its own
 * avatar; the change is broadcast to all other UIs through the registry signal.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Presence")
@UseCaseDescription("Showing which users are present or away across browser tabs")
@Menu(order = 2, title = "UC2 — Presence")
@StyleSheet("uc2.css")
public class PresenceAvatarsView extends VerticalLayout {

    private static final List<String> ANIMALS = List.of("Otter", "Falcon",
            "Lynx", "Beaver", "Heron", "Marten", "Badger", "Hare", "Wolf",
            "Owl", "Fox", "Crane", "Stoat", "Eagle", "Mole");
    private static final List<String> COLORS = List.of("var(--aura-blue)",
            "var(--aura-green)", "var(--aura-red)", "var(--aura-orange)",
            "var(--aura-purple)");

    private final String id = UUID.randomUUID().toString();

    public PresenceAvatarsView(PresenceRegistry registry) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        String name = ANIMALS.get(rnd.nextInt(ANIMALS.size())) + "-"
                + (100 + rnd.nextInt(900));
        String color = COLORS.get(rnd.nextInt(COLORS.size()));

        addClassName("uc2-view");
        add(new H1("UC2 — Presence"));
        add(new Paragraph("Open this page in two or more browsers. Each "
                + "tab gets a random colour and animal name. Hide a tab "
                + "or move focus away from a window, and the matching "
                + "avatar greys out for everyone else within ~100 ms."));

        H2 youHeader = new H2("You");
        Span youLabel = new Span("Joined as ");
        Span youName = new Span(name);
        youName.addClassName("uc2-you-name");
        // Dynamic per-instance color is fed to the CSS rule as a custom
        // property — the .uc2-you-name rule consumes var(--you-color).
        youName.getStyle().set("--you-color", color);
        Div youRow = new Div(youLabel, youName);

        H2 strip = new H2("In the room");
        Div avatarStrip = new Div();
        avatarStrip.addClassName("uc2-avatar-strip");
        registry.bindTo(avatarStrip, PresenceAvatarsView::renderAvatar);

        add(youHeader, youRow, strip, avatarStrip);

        Signal<PageVisibility> visibility = UI.getCurrent().getPage()
                .pageVisibilitySignal();
        whenAttached(ui -> {
            registry.join(new Presence(id, name, color, visibility.peek()));
            return () -> registry.leave(id);
        });
        Signal.effect(this, () -> registry.updateState(id, visibility.get()));
    }

    private static Component renderAvatar(SharedValueSignal<Presence> entry) {
        Presence p = entry.peek();
        ColoredAvatar avatar = new ColoredAvatar(p.name(), p.color(), 56);
        avatar.bindState(entry.map(Presence::state));
        return avatar;
    }
}
