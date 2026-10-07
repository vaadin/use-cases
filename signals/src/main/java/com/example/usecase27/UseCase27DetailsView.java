package com.example.usecase27;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Detail sub-view of {@link UseCase27View}. Demonstrates that
 * {@code UI.routerStateSignal()} fires for navigations to the same view class
 * with different {@code :id} route parameters — the parent layout's breadcrumb
 * updates each time.
 */
@Route(value = "use-case-27/details/:id", layout = UseCase27Layout.class)
@PageTitle("Use Case 27: Details")
@AnonymousAllowed
public class UseCase27DetailsView extends VerticalLayout
        implements BeforeEnterObserver {

    private final H3 heading = new H3();

    public UseCase27DetailsView() {
        setSpacing(true);
        setPadding(true);

        Paragraph description = new Paragraph(
                "Detail view for a single order. The parent layout's "
                        + "breadcrumb above is driven by routerStateSignal, so "
                        + "navigating to a different :id updates it without "
                        + "this view registering anything.");

        RouterLink back = new RouterLink("← Back to overview",
                UseCase27View.class);

        add(heading, description, back);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String id = event.getRouteParameters().get("id").orElse("?");
        heading.setText("Order #" + id);
    }
}
