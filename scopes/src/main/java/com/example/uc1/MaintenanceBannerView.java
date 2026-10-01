package com.example.uc1;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Maintenance banner for everyone (application scope).
 * <p>
 * An operator announces planned downtime ("The system goes down for maintenance
 * at 18:00"). Every signed-in user, in every browser and session, sees the
 * banner appear immediately, and sees it disappear when the operator clears it.
 * The notice is a fact about the application, not about any user, so it lives
 * in an application-scoped (singleton) bean, {@link MaintenanceNotice}.
 * <p>
 * Open this page in two different browsers (or one normal and one private
 * window) to see the banner update in both.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Maintenance banner")
@Menu(order = 1, title = "UC1 — Maintenance banner")
public class MaintenanceBannerView extends VerticalLayout {

    public MaintenanceBannerView(MaintenanceNotice notice) {
        add(new H1("UC1 — Maintenance banner"));
        add(new Paragraph("The banner below is stored in an "
                + "application-scoped bean: one instance shared by all "
                + "users. Publish a notice here and every other browser "
                + "that has this application open shows it at once."));

        Div banner = new Div();
        banner.addClassName("maintenance-banner");
        banner.bindText(notice.message());
        banner.bindVisible(notice.message().map(text -> !text.isEmpty()));
        add(banner);

        add(new H2("Operator console"));
        TextField text = new TextField("Notice");
        text.setPlaceholder("The system goes down for maintenance at 18:00");
        text.setWidth("28rem");

        Button publish = new Button("Publish to everyone", e -> {
            notice.publish(text.getValue());
            text.clear();
        });
        publish.addThemeVariants(ButtonVariant.PRIMARY);
        Button clear = new Button("Clear notice", e -> notice.clear());

        HorizontalLayout console = new HorizontalLayout(text, publish, clear);
        console.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        add(console);
    }
}
