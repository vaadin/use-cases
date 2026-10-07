package com.example.uc5;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouteReference;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.RouterState;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.signals.Signal;

/**
 * A single "↑ Up to &lt;parent&gt;" control — the minimal back-navigation
 * helper the route-hierarchy PR names as a target consumer.
 * <p>
 * Unlike a full breadcrumb it only needs the <em>immediate</em> parent, so it
 * calls {@code RouteUtil.getRouteParent(class, parameters)} rather than the
 * whole hierarchy. When the current view is already a hierarchy root, that
 * returns {@link Optional#empty()} and the control renders a plain "top level"
 * note instead of a link.
 * <p>
 * The control wires itself to {@link UI#routerStateSignal()} via a single
 * {@link Signal#effect}; the parent is recomputed from the current
 * {@link RouterState#currentView()} on every navigation. Views just
 * {@code add(new UpLink())} — no per-view plumbing.
 */
public class UpLink extends Div {

    public UpLink() {
        addClassName("up-link");

        Signal.effect(this, () -> {
            RouterState state = UI.getCurrent().routerStateSignal().get();
            rebuild(state);
        });
    }

    private void rebuild(RouterState state) {
        removeAll();
        HasElement leaf = state.currentView().orElse(null);
        if (!(leaf instanceof Component leafView)) {
            return;
        }
        RouteRegistry registry = VaadinService.getCurrent().getRouter()
                .getRegistry();
        Optional<RouteReference> parent = RouteUtil.getRouteParent(registry,
                leafView.getClass(), state.routeParameters());
        if (parent.isPresent()) {
            RouteReference ref = parent.get();
            String label = "↑ Up to " + MenuRegistry
                    .getTitle(ref.navigationTarget(), ref.routeParameters());
            RouteParameters parameters = ref.routeParameters();
            RouterLink link = parameters.getParameterNames().isEmpty()
                    ? new RouterLink(label, ref.navigationTarget())
                    : new RouterLink(label, ref.navigationTarget(), parameters);
            add(link);
        } else {
            add(new Span("You are at the top level."));
        }
    }
}
