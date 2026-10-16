package com.example.uc2;

import java.util.Locale;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PageVisibility;
import com.vaadin.flow.signals.Signal;

/**
 * Colored circular avatar showing the initial of a participant's name. The
 * border ring is the participant's assigned color and the
 * {@link PageVisibility} state controls a CSS class that desaturates / dims the
 * avatar.
 */
class ColoredAvatar extends Div {

    private final String name;

    ColoredAvatar(String name, String color, int sizePx) {
        this.name = name;
        addClassName("uc2-avatar");

        Span initial = new Span(name.isEmpty() ? "?"
                : name.substring(0, 1).toUpperCase(Locale.ROOT));
        initial.addClassName("uc2-avatar-initial");
        add(initial);

        // Per-instance color and size are dynamic, so they're set as CSS
        // custom properties consumed by the .uc2-avatar rules.
        getStyle().set("--avatar-color", color);
        getStyle().set("--avatar-size", sizePx + "px");
    }

    ColoredAvatar bindState(Signal<PageVisibility> state) {
        bindClassName("uc2-avatar-visible",
                state.map(s -> s == PageVisibility.VISIBLE));
        bindClassName("uc2-avatar-blurred",
                state.map(s -> s == PageVisibility.VISIBLE_NOT_FOCUSED));
        bindClassName("uc2-avatar-hidden",
                state.map(s -> s == PageVisibility.HIDDEN));
        bindClassName("uc2-avatar-unknown",
                state.map(s -> s == PageVisibility.UNKNOWN));
        // No higher-level Component API for the native title attribute, so
        // this stays at the Element level.
        getElement().bindAttribute("title", state.map(s -> name + " — "
                + s.name().toLowerCase(Locale.ROOT).replace('_', ' ')));
        return this;
    }
}
