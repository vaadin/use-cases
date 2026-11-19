package com.example.uc6;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;

/**
 * Client-side action that reads {@code navigator.onLine} at fire time and sets
 * the target's {@code textContent} and {@code className} accordingly. Stays
 * entirely in the browser — important when the trigger is meant to react to
 * going offline (a server callback would be the wrong tool).
 */
public class ApplyNetworkStatusAction extends Action {

    private final Element target;

    public ApplyNetworkStatusAction(Component target) {
        this(Objects.requireNonNull(target).getElement());
    }

    public ApplyNetworkStatusAction(Element target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsSnippets.of(NetworkStatusJs.class).applyNetworkStatus(target);
    }

    @JsDefinition
    private interface NetworkStatusJs extends Serializable {

        @JsExpression("""
                const online = navigator.onLine;
                $0.textContent = online ? 'Online' : 'Offline';
                $0.className = online ? 'uc6-status uc6-online' : 'uc6-status uc6-offline';""")
        JsFunction applyNetworkStatus(Element target);
    }
}
