package com.example.uc6;

import com.example.views.MainLayout;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.signals.Signal;

/**
 * UC6 — Layout-wide auto breadcrumbs.
 * <p>
 * One {@link Breadcrumbs} component lives in this parent layout, shared by every
 * child view ({@code uc6}, {@code uc6/team}, {@code uc6/team/:member}). The child
 * views build no breadcrumb of their own.
 * <p>
 * The {@link Breadcrumbs} component in {@code ROUTER} mode already subscribes to
 * the router and rebuilds the shared trail on every navigation — no
 * {@code AfterNavigationObserver} registration or manual seeding step. The
 * {@link Signal#effect} added here is independent of the breadcrumb: it
 * subscribes to {@link UI#routerStateSignal()} purely to drive the rebuild
 * counter, kept to make the per-navigation fire visible from the tests.
 */
@ParentLayout(MainLayout.class)
public class TeamLayout extends Div implements RouterLayout {

    public static final String REBUILD_BADGE_ID = "uc6-rebuilds";

    private final Breadcrumbs breadcrumbs = new Breadcrumbs();
    private final Span rebuildBadge = new Span();
    private final Div content = new Div();
    private int rebuilds;

    public TeamLayout() {
        addClassName("team-layout");
        rebuildBadge.setId(REBUILD_BADGE_ID);
        rebuildBadge.addClassName("rebuild-badge");
        Div header = new Div(breadcrumbs, rebuildBadge);
        header.addClassName("team-header");
        content.addClassName("team-content");
        add(header, content);

        Signal.effect(this, () -> {
            UI.getCurrent().routerStateSignal().get();
            rebuilds++;
            rebuildBadge.setText("breadcrumb rebuilds: " + rebuilds);
        });
    }

    @Override
    public void showRouterLayoutContent(HasElement routerLayoutContent) {
        content.getElement().removeAllChildren();
        if (routerLayoutContent != null) {
            content.getElement().appendChild(routerLayoutContent.getElement());
        }
    }
}
