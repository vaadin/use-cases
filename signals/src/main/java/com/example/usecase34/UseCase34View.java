package com.example.usecase34;

import com.example.views.MainLayout;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;

/**
 * Feature flag admin + preview. The {@link FeatureFlagService} owns the
 * writable flag signals; this view receives only read-only {@link Signal}
 * references from the service — it can't accidentally mutate flag state by
 * touching the signal. Flips happen through dedicated service methods that the
 * admin UI calls.
 * <p>
 * Two preview sections subscribe to the read-only signals: a checkout summary
 * that branches on the new-flow flag, and a "Beta" badge visible when the beta
 * UI is enabled. Both are passive consumers — the flags shape the UI but only
 * the admin can flip them.
 */
@PageTitle("Use Case 34: Feature flag service")
@Route(value = "use-case-34", layout = MainLayout.class)
@Menu(order = 34, title = "UC 34: Feature flags")
@AnonymousAllowed
public class UseCase34View extends VerticalLayout {

    final FeatureFlagService flags;
    final Signal<Boolean> newCheckoutFlowSignal;
    final Signal<Boolean> betaUiSignal;

    public UseCase34View(FeatureFlagService flags) {
        this.flags = flags;
        this.newCheckoutFlowSignal = flags.newCheckoutFlowSignal();
        this.betaUiSignal = flags.betaUiSignal();

        setSpacing(true);
        setPadding(true);

        add(new H2("Use Case 34: Feature flag service"), new Paragraph(
                "The FeatureFlagService bean owns the writable signals;"
                        + " views and components receive only Signal references"
                        + " via asReadonly(). The admin panel below flips flags"
                        + " through service methods; the two preview sections"
                        + " subscribe but can't mutate."));

        add(buildAdminPanel(), buildCheckoutPreview(), buildBetaBadgePreview(),
                buildExplanation());
    }

    private Div buildAdminPanel() {
        Div panel = new Div();
        panel.getStyle().set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px");

        H3 header = new H3("Admin: flip flags");
        header.getStyle().set("margin-top", "0");

        Checkbox newCheckout = new Checkbox("Enable new checkout flow");
        newCheckout.bindValue(newCheckoutFlowSignal, flags::setNewCheckoutFlow);

        Checkbox beta = new Checkbox("Enable beta UI");
        beta.bindValue(betaUiSignal, flags::setBetaUi);

        panel.add(header, newCheckout, beta);
        return panel;
    }

    private Div buildCheckoutPreview() {
        Div panel = new Div();
        panel.getStyle().set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("border", "1px solid var(--lumo-contrast-20pct)");

        H3 header = new H3("Customer checkout preview");
        header.getStyle().set("margin-top", "0");

        Span flowLabel = new Span();
        flowLabel.bindText(newCheckoutFlowSignal
                .map(enabled -> enabled ? "Variant: NEW one-page checkout"
                        : "Variant: classic multi-step checkout"));
        flowLabel.getStyle().set("display", "block").set("font-weight", "bold");

        Div newFlow = new Div();
        newFlow.bindVisible(newCheckoutFlowSignal);
        newFlow.add(
                new Paragraph("1) Cart + shipping + payment on a single page."),
                new Paragraph("2) Apple Pay / Google Pay buttons at top."),
                new Paragraph(
                        "3) Order placed without an extra confirmation step."));

        Div oldFlow = new Div();
        oldFlow.bindVisible(Signal.not(newCheckoutFlowSignal));
        oldFlow.add(new Paragraph("1) Review cart on its own page."),
                new Paragraph("2) Enter shipping address."),
                new Paragraph("3) Enter payment details."),
                new Paragraph("4) Confirm order."));

        panel.add(header, flowLabel, newFlow, oldFlow);
        return panel;
    }

    private Div buildBetaBadgePreview() {
        Div panel = new Div();
        panel.getStyle().set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("border", "1px solid var(--lumo-contrast-20pct)");

        H3 header = new H3("Navbar preview");
        header.getStyle().set("margin-top", "0");

        HorizontalLayout navbar = new HorizontalLayout();
        navbar.setAlignItems(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        navbar.add(new Span("Logo"), new Span("Catalog"), new Span("Pricing"));

        Span betaBadge = new Span("BETA");
        betaBadge.bindVisible(betaUiSignal);
        betaBadge.getStyle()
                .set("background-color", "var(--lumo-primary-color)")
                .set("color", "white").set("padding", "2px 8px")
                .set("border-radius", "12px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "bold");

        navbar.add(betaBadge);
        panel.add(header, navbar);
        return panel;
    }

    private Div buildExplanation() {
        Div box = new Div();
        box.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px");

        H3 title = new H3("Why asReadonly()?");
        title.getStyle().set("margin-top", "0");

        Paragraph p = new Paragraph(
                "FeatureFlagService holds SharedValueSignal<Boolean> internally"
                        + " and exposes Signal<Boolean> to consumers via"
                        + " asReadonly(). View code cannot call set(), update(),"
                        + " or replace() on the flag — the methods aren't on"
                        + " the Signal interface. Flag changes can only happen"
                        + " through dedicated service methods, which is exactly"
                        + " the kind of write-channel separation you want for"
                        + " any feature-flag, auth, or theme service.");

        box.add(title, p);
        return box;
    }
}
