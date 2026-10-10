package com.example.uc1;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Translated texts.
 * <p>
 * An order confirmation whose texts come from
 * {@code vaadin-i18n/translations*.properties} through {@link #getTranslation}.
 * The view implements {@link LocaleChangeObserver}, so picking another language
 * rewrites every text in place, without a reload.
 * <p>
 * The texts show the cases a real application runs into: parameters (the
 * customer's name and the order number), a plural that depends on a number the
 * user changes, a date formatted inside a sentence, a key that only the English
 * fallback file has, and a key that no file has at all.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Translated texts")
@UseCaseDescription("Showing texts from translation files in the user's language")
@Menu(order = 1, title = "UC1 — Translated texts")
public class TranslatedTextView extends VerticalLayout
        implements LocaleChangeObserver {

    static final String CUSTOMER = "Maria";
    static final String ORDER = "#1042";
    static final LocalDate DELIVERY = LocalDate.of(2026, 3, 5);

    private final H2 title = new H2();
    private final Paragraph greeting = new Paragraph();
    private final IntegerField quantity = new IntegerField();
    private final Paragraph items = new Paragraph();
    private final Paragraph delivery = new Paragraph();
    private final Paragraph returns = new Paragraph();
    private final Paragraph newsletter = new Paragraph();
    private final Button track = new Button();

    public TranslatedTextView() {
        add(new H1("UC1 — Translated texts"));
        add(new Paragraph("Pick another language in the top bar: every text "
                + "in the order confirmation below is rewritten in place. "
                + "Change the number of items to see the plural follow it. "
                + "The returns line exists only in the English file, so other "
                + "languages fall back to it; the newsletter line has no "
                + "translation anywhere and shows Flow's missing-key marker."));

        quantity.setValue(3);
        quantity.setMin(0);
        quantity.setStepButtonsVisible(true);
        quantity.addValueChangeListener(e -> updateItems());

        returns.addClassName("fallback");
        newsletter.addClassName("missing");

        Div sample = new Div(title, greeting, quantity, items, delivery,
                returns, newsletter, track);
        sample.addClassName("sample");
        add(sample);
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        title.setText(getTranslation("uc1.title"));
        greeting.setText(getTranslation("uc1.greeting", CUSTOMER, ORDER));
        quantity.setLabel(getTranslation("uc1.quantity"));
        updateItems();
        // MessageFormat formats java.util.Date, not java.time, and in the
        // JVM's default time zone: midnight there keeps the day unchanged.
        delivery.setText(getTranslation("uc1.delivery", Date.from(
                DELIVERY.atStartOfDay(ZoneId.systemDefault()).toInstant())));
        returns.setText(getTranslation("uc1.returns"));
        newsletter.setText(getTranslation("uc1.newsletter"));
        track.setText(getTranslation("uc1.track"));
    }

    private void updateItems() {
        Integer count = quantity.getValue();
        items.setText(getTranslation("uc1.items", count == null ? 0 : count));
    }
}
