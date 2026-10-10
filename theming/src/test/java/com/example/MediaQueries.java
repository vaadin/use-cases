package com.example;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.Component;

/**
 * Plays the browser's answer to {@link MissingAPI#mediaQuery}, which a
 * browserless test cannot evaluate.
 */
public final class MediaQueries {

    private MediaQueries() {
    }

    public static void answer(Component owner, String query, boolean matches) {
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put("event.detail", matches);
        new ComponentTester<Component>(owner) {
            void fire() {
                fireDomEvent(MissingAPI.mediaQueryEvent(query), data);
            }
        }.fire();
    }
}
