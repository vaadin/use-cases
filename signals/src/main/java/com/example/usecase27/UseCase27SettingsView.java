package com.example.usecase27;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Settings sub-view of {@link UseCase27View}. Sibling to
 * {@link UseCase27DetailsView}; navigating here updates the parent layout's
 * breadcrumb without re-rendering the layout itself.
 */
@Route(value = "use-case-27/settings", layout = UseCase27Layout.class)
@PageTitle("Use Case 27: Settings")
@AnonymousAllowed
public class UseCase27SettingsView extends VerticalLayout {

    public UseCase27SettingsView() {
        setSpacing(true);
        setPadding(true);

        add(new H3("Settings"),
                new Paragraph(
                        "Placeholder settings page used to demonstrate that "
                                + "the parent layout's breadcrumb tracks the "
                                + "active route reactively."),
                new RouterLink("← Back to overview", UseCase27View.class));
    }
}
