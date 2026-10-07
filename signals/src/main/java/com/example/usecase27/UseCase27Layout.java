package com.example.usecase27;

import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterState;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;

/**
 * Parent layout shared by all use-case-27 routes. Demonstrates the value of
 * {@link UI#routerStateSignal()} by rendering a breadcrumb trail that updates
 * reactively as the user navigates between sub-views.
 * <p>
 * Without {@code routerStateSignal()} this layout would need to implement
 * {@code AfterNavigationObserver} and seed initial state on attach. With the
 * signal, a single {@link Signal#effect} captures both the first render and
 * every subsequent navigation.
 */
@ParentLayout(MainLayout.class)
@StyleSheet("usecase27.css")
@AnonymousAllowed
public class UseCase27Layout extends Div implements RouterLayout {

    public static final String BREADCRUMB_ID = "uc27-breadcrumb";
    public static final String UPDATE_COUNT_ID = "uc27-update-count";

    private final Div breadcrumb = new Div();
    private final Span updateBadge = new Span();
    private int updateCount;

    public UseCase27Layout() {
        addClassName("usecase27-view");

        breadcrumb.setId(BREADCRUMB_ID);
        breadcrumb.addClassName("breadcrumb");

        updateBadge.setId(UPDATE_COUNT_ID);
        updateBadge.addClassName("update-badge");

        Div header = new Div(breadcrumb, updateBadge);
        header.addClassName("layout-header");
        getElement().appendChild(header.getElement());

        Signal.effect(this, () -> {
            RouterState state = UI.getCurrent().routerStateSignal().get();
            updateCount++;
            updateBadge.setText("routerStateSignal updates: " + updateCount);
            breadcrumb.removeAll();
            breadcrumb.add(buildBreadcrumb(state));
        });
    }

    private static Component[] buildBreadcrumb(RouterState state) {
        Class<? extends Component> target = state.navigationTarget();
        if (target == null) {
            return new Component[] { new Span("(no navigation yet)") };
        }

        Anchor overview = new Anchor("use-case-27", "Use Case 27");
        overview.getElement().setAttribute("router-link", "");

        if (target == UseCase27DetailsView.class) {
            String id = state.routeParameters().get("id").orElse("?");
            return new Component[] { overview, separator(), new Span("Details"),
                    separator(), new Span("#" + id) };
        }
        if (target == UseCase27SettingsView.class) {
            return new Component[] { overview, separator(),
                    new Span("Settings") };
        }
        // UseCase27View — the landing view.
        return new Component[] { new Span("Use Case 27"), separator(),
                new Span("Overview") };
    }

    private static Span separator() {
        Span s = new Span(" › ");
        s.addClassName("breadcrumb-separator");
        return s;
    }
}
