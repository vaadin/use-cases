package com.example.acme;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * The stage rigging of a use case: the knobs that fake the story's problem —
 * a slow backend, a failing service. It hangs off the {@link AppWindow}
 * (place it right after one) and is visibly labelled as not being part of the
 * app, so it reads as neither an Acme feature nor an observability readout.
 */
public class DemoRig extends Div {

    public DemoRig(Component... controls) {
        addClassName("app-window-rig");
        Span caption = new Span("Demo rig — not part of the app");
        caption.addClassName("app-window-rig-caption");
        add(caption);
        add(controls);
    }
}
