package com.example.uc2;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

/**
 * UC2 — {@code @RouteParent} override (parent, {@code uc2}).
 * <p>
 * The detail page in this use case lives at {@code order-detail/:orderId} — a
 * URL that is <em>not</em> a prefix-descendant of {@code uc2}. URL-prefix
 * walking alone would never connect the two. {@link OrderDetailView} therefore
 * declares {@code @RouteParent(OrdersView.class)}, and {@code getRouteParent}
 * consults that annotation before it ever tries the URL fallback.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("Orders")
@Menu(order = 2, title = "UC2 — @RouteParent override")
public class OrdersView extends VerticalLayout {

    public OrdersView() {
        add(new Breadcrumbs());
        add(new H1("Orders"));
        add(new Paragraph(
                "The order detail page lives at order-detail/:orderId, which "
                        + "does not start with uc2/. Click an order below: the "
                        + "breadcrumb there still reads Orders › Order #… "
                        + "because OrderDetailView is annotated "
                        + "@RouteParent(OrdersView.class), and getRouteParent "
                        + "honours that before falling back to the URL."));
        add(new RouterLink("Open order #1001", OrderDetailView.class,
                new RouteParameters("orderId", "1001")));
        add(new RouterLink("Open order #1002", OrderDetailView.class,
                new RouteParameters("orderId", "1002")));
    }
}
