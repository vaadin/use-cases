package com.example.uc7;

import java.util.Locale;

import com.example.LanguagePreference;
import com.example.MissingAPI;
import com.example.SupportedLocales;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC7 — Mixed-direction text.
 * <p>
 * Translations are written in one direction, but the data inside them often is
 * not: a Latin company name or a ticket number inside a Hebrew sentence, an
 * Arabic customer name inside an English one. Inserted as is, such a value
 * borrows the direction of the text around it, and its punctuation lands on the
 * wrong side: "Acme Ltd." becomes ".Acme Ltd" and "#4521-B" turns into
 * "4521-B#".
 * <p>
 * The view shows the same message twice. The naive version inserts the values
 * as they are; the isolated one wraps each value in Unicode isolates
 * ({@link MissingAPI#isolate}). A value shown on its own goes into a
 * {@code <bdi>} element, and free text the user wrote gets {@code dir="auto"},
 * so its alignment follows its own first strong character instead of the page.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Mixed-direction text")
@UseCaseDescription("Keeping user-entered text readable inside translated sentences")
@Menu(order = 7, title = "UC7 — Mixed-direction text")
@StyleSheet("uc7.css")
public class MixedDirectionView extends VerticalLayout {

    private final ValueSignal<String> customer = new ValueSignal<>("Acme Ltd.");
    private final ValueSignal<String> ticket = new ValueSignal<>("#4521-B");
    private final ValueSignal<String> comment = new ValueSignal<>(
            "The espresso machine leaks again (3rd time!)");
    private final TextField customerField = new TextField();
    private final TextField ticketField = new TextField();
    private final TextField commentField = new TextField();
    private final Span naive = new Span();
    private final Span isolated = new Span();
    private final Element bdi = new Element("bdi");

    public MixedDirectionView() {
        UI ui = UI.getCurrent();
        LanguagePreference preference = LanguagePreference.current();
        addClassName("uc7-view");

        add(new H1("UC7 — Mixed-direction text"));
        add(new Paragraph("Switch to Hebrew or Arabic and compare the two "
                + "messages: in the naive one the period of \"Acme Ltd.\" "
                + "and the # of the ticket number jump to the wrong end. "
                + "Type a name in Hebrew while the page is in English to see "
                + "the same problem the other way round."));

        HorizontalLayout languages = new HorizontalLayout(
                new Button(SupportedLocales.nativeName(SupportedLocales.HEBREW),
                        e -> preference.choose(SupportedLocales.HEBREW)),
                new Button(SupportedLocales.nativeName(SupportedLocales.ARABIC),
                        e -> preference.choose(SupportedLocales.ARABIC)),
                new Button(
                        SupportedLocales.nativeName(SupportedLocales.ENGLISH),
                        e -> preference.choose(SupportedLocales.ENGLISH)));

        customerField.bindValue(customer, customer::set);
        ticketField.bindValue(ticket, ticket::set);
        commentField.bindValue(comment, comment::set);
        commentField.setWidthFull();
        HorizontalLayout fields = new HorizontalLayout(customerField,
                ticketField);

        naive.bindText(
                Signal.computed(() -> getTranslation(ui.localeSignal().get(),
                        "uc7.ticket", customer.get(), ticket.get())));
        isolated.bindText(
                Signal.computed(() -> getTranslation(ui.localeSignal().get(),
                        "uc7.ticket", MissingAPI.isolate(customer.get()),
                        MissingAPI.isolate(ticket.get()))));

        // Gap: Flow has no component for <bdi>, nor a setter for an element's
        // own dir attribute.
        Signal.effect(this, () -> bdi.setText(customer.get()));
        Span standalone = new Span();
        standalone.getElement().appendChild(bdi);
        Paragraph commentText = new Paragraph();
        commentText.getElement().setAttribute("dir", "auto");
        commentText.bindText(comment);

        Div sample = new Div(fields, commentField, example("Naive", naive),
                example("Isolated", isolated), example("<bdi>", standalone),
                example("dir=\"auto\"", commentText));
        sample.addClassName("sample");
        add(languages, sample);

        Signal.effect(this, () -> {
            Locale locale = ui.localeSignal().get();
            customerField.setLabel(getTranslation(locale, "uc7.customer"));
            ticketField.setLabel(getTranslation(locale, "uc7.ticket-id"));
            commentField.setLabel(getTranslation(locale, "uc7.comment"));
        });
    }

    private static Div example(String title, Component content) {
        H3 heading = new H3(title);
        heading.getElement().setAttribute("dir", "ltr");
        Div example = new Div(heading, content);
        example.addClassName("example");
        return example;
    }

    // Package-private test seams.
    TextField customerField() {
        return customerField;
    }

    String naiveText() {
        return naive.getText();
    }

    String isolatedText() {
        return isolated.getText();
    }

    String bdiText() {
        return bdi.getText();
    }
}
