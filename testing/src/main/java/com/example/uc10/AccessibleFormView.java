package com.example.uc10;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import com.example.views.TestsNote;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * UC10 — Accessibility checks.
 * <p>
 * A contact form that works for screen reader and keyboard users: every field
 * has a label, icon buttons have an accessible name, the text has enough
 * contrast, and the form can be filled in and sent with the keyboard alone.
 * <p>
 * The browser tests run axe-core over the page and fail on any violation, and
 * walk through the form with Tab and Enter. Opening the view with
 * {@code ?broken} adds an icon button without a name and a grey-on-grey hint,
 * the kind of mistake the check exists to catch, and the tests check that axe
 * reports both.
 */
@Route(value = "uc10", layout = MainLayout.class)
@PageTitle("UC10 — Accessibility checks")
@UseCaseDescription("Checking views with axe-core and testing keyboard-only use")
@Menu(order = 10, title = "UC10 — Accessibility checks")
@AnonymousAllowed
public class AccessibleFormView extends VerticalLayout
        implements BeforeEnterObserver {

    private final Div sample = new Div();
    private final HorizontalLayout actions = new HorizontalLayout();

    public AccessibleFormView() {
        add(new H1("UC10 — Accessibility checks"));
        add(new Paragraph("Fill in the form with the keyboard only: Tab "
                + "moves between fields, Enter in a field or on the button "
                + "sends. Add ?broken to the address to see what the "
                + "accessibility check catches."));

        TextField name = new TextField("Name");
        EmailField email = new EmailField("Email");
        TextArea message = new TextArea("Message");
        Button send = new Button("Send", e -> Notification.show(
                "Thanks " + name.getValue() + ", we will get back to you"));
        send.addThemeVariants(ButtonVariant.PRIMARY);
        // Enter sends from the single-line fields only: in the message it
        // starts a new line, and on the focused button the button itself
        // already reacts to it (a page-wide shortcut would send twice).
        send.addClickShortcut(Key.ENTER).listenOn(name, email);
        Button attach = new Button(VaadinIcon.PAPERCLIP.create());
        attach.setAriaLabel("Attach a file");
        actions.add(send, attach);

        sample.addClassName("sample");
        sample.setId("contact-form");
        sample.add(new H2("Contact us"), new FormLayout(name, email, message),
                actions);
        add(sample,
                new TestsNote(
                        "Playwright + axe-core: "
                                + "uc10/AccessibilityPlaywrightIT",
                        "Run: mvn -pl testing -am verify -Pit"));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters()
                .containsKey("broken")) {
            // No accessible name: a screen reader announces just "button".
            actions.add(new Button(VaadinIcon.TRASH.create()));
            Span hint = new Span("We answer within two working days.");
            hint.addClassName("low-contrast");
            hint.getStyle().set("color", "#c8c8c8").set("background",
                    "#e6e6e6");
            sample.add(hint);
        }
    }
}
