package com.example.usecase32;

import java.util.concurrent.atomic.AtomicInteger;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Customer onboarding form rendered against two backing representations of the
 * same data: a mutable JPA-style {@link Customer} bean and an immutable
 * {@link CustomerDto} record. Both forms behave identically to the user,
 * proving that signal bindings work with either model. The "Import customer
 * record" button shows how to apply a whole-object replacement atomically:
 * {@code modify} on the mutable side and {@code update} on the immutable side
 * each fire exactly one effect notification, even though three fields change.
 */
@PageTitle("Use Case 32: Mutable vs immutable form models")
@Route(value = "use-case-32", layout = MainLayout.class)
@Menu(order = 32, title = "UC 32: Mutable vs immutable")
@AnonymousAllowed
public class UseCase32View extends VerticalLayout {

    private static final String[] PLANS = { "Free", "Pro", "Enterprise" };

    final ValueSignal<Customer> entitySignal = new ValueSignal<>(
            new Customer("Alice Anderson", "alice@example.com", "Free"));
    final ValueSignal<CustomerDto> dtoSignal = new ValueSignal<>(
            new CustomerDto("Alice Anderson", "alice@example.com", "Free"));

    private final AtomicInteger entityNotifications = new AtomicInteger();
    private final AtomicInteger dtoNotifications = new AtomicInteger();
    private final ValueSignal<Integer> entityNotificationsSignal = new ValueSignal<>(
            0);
    private final ValueSignal<Integer> dtoNotificationsSignal = new ValueSignal<>(
            0);

    public UseCase32View() {
        setSpacing(true);
        setPadding(true);

        add(new H2("Use Case 32: Mutable vs immutable form models"),
                new Paragraph(
                        "The same customer onboarding form rendered against"
                                + " two different storage models: a mutable"
                                + " JPA-style bean (left) and an immutable"
                                + " DTO record (right). Each form uses the"
                                + " matching signal-binding helper —"
                                + " modifier(setter) for the mutable bean,"
                                + " updater(withX) for the immutable record."
                                + " Both feel identical to type in."));

        HorizontalLayout panels = new HorizontalLayout(buildMutablePanel(),
                buildImmutablePanel());
        panels.setWidthFull();

        Button importBtn = new Button(
                "Import customer record (atomic — fires one notification per side)",
                e -> {
                    entitySignal.modify(c -> {
                        c.setName("Carol Chen");
                        c.setEmail("carol@example.com");
                        c.setPlan("Enterprise");
                    });
                    dtoSignal.update(prev -> new CustomerDto("Carol Chen",
                            "carol@example.com", "Enterprise"));
                });

        add(panels, importBtn, buildExplanation());
    }

    private Div buildMutablePanel() {
        Div panel = new Div();
        panel.getStyle().set("flex", "1").set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px");

        H3 header = new H3("Mutable Customer (JPA) + modifier(setter)");
        header.getStyle().set("margin-top", "0");

        TextField name = new TextField("Name");
        name.bindValue(entitySignal.map(Customer::getName),
                entitySignal.modifier(Customer::setName));
        TextField email = new TextField("Email");
        email.bindValue(entitySignal.map(Customer::getEmail),
                entitySignal.modifier(Customer::setEmail));
        Select<String> plan = new Select<>();
        plan.setLabel("Plan");
        plan.setItems(PLANS);
        plan.bindValue(entitySignal.map(Customer::getPlan),
                entitySignal.modifier(Customer::setPlan));

        Span computedSummary = new Span();
        computedSummary.bindText(entitySignal.map(c -> "Summary: " + c.getName()
                + " <" + c.getEmail() + "> [" + c.getPlan() + "]"));

        Span runs = new Span();
        runs.bindText(entityNotificationsSignal
                .map(n -> "Effect notifications: " + n));
        runs.getStyle().set("display", "block")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Signal.effect(panel, () -> {
            entitySignal.get();
            entityNotificationsSignal
                    .set(entityNotifications.incrementAndGet());
        });

        panel.add(header, name, email, plan, computedSummary, runs);
        return panel;
    }

    private Div buildImmutablePanel() {
        Div panel = new Div();
        panel.getStyle().set("flex", "1").set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px");

        H3 header = new H3("Immutable CustomerDto + updater(withX)");
        header.getStyle().set("margin-top", "0");

        TextField name = new TextField("Name");
        name.bindValue(dtoSignal.map(CustomerDto::name),
                dtoSignal.updater(CustomerDto::withName));
        TextField email = new TextField("Email");
        email.bindValue(dtoSignal.map(CustomerDto::email),
                dtoSignal.updater(CustomerDto::withEmail));
        Select<String> plan = new Select<>();
        plan.setLabel("Plan");
        plan.setItems(PLANS);
        plan.bindValue(dtoSignal.map(CustomerDto::plan),
                dtoSignal.updater(CustomerDto::withPlan));

        Span computedSummary = new Span();
        computedSummary.bindText(dtoSignal.map(c -> "Summary: " + c.name()
                + " <" + c.email() + "> [" + c.plan() + "]"));

        Span runs = new Span();
        runs.bindText(
                dtoNotificationsSignal.map(n -> "Effect notifications: " + n));
        runs.getStyle().set("display", "block")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Signal.effect(panel, () -> {
            dtoSignal.get();
            dtoNotificationsSignal.set(dtoNotifications.incrementAndGet());
        });

        panel.add(header, name, email, plan, computedSummary, runs);
        return panel;
    }

    private Div buildExplanation() {
        Div box = new Div();
        box.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px");

        H3 title = new H3("When to pick which?");
        title.getStyle().set("margin-top", "0");

        Paragraph p = new Paragraph(
                "Use modifier(setter) when the model is a mutable bean owned"
                        + " by a framework — JPA entities, JavaBeans inherited"
                        + " from an SDK, Lombok @Data classes. Use"
                        + " updater(withX) when the model is a record or any"
                        + " immutable value class — typical for DTOs sent"
                        + " over the wire. The "
                        + "'Import' button shows the matching atomic"
                        + " batch APIs: modify(c -> { c.setX(); c.setY(); })"
                        + " and update(prev -> newSnapshot) each notify"
                        + " subscribers exactly once for the whole update.");

        box.add(title, p);
        return box;
    }
}
