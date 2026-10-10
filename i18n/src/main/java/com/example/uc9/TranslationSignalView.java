package com.example.uc9;

import com.example.MissingAPI;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC9 — Translations as signals.
 * <p>
 * The same kind of texts as in UC1, without a {@code localeChange} method that
 * has to remember every text in the view. Each text is bound once to a
 * translation signal ({@link MissingAPI#translate}), which depends on
 * {@link UI#localeSignal()} and on any signal passed as a parameter. Picking a
 * language and changing the basket both update exactly the texts that depend on
 * them.
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Translations as signals")
@UseCaseDescription("Binding texts to translations that follow the language by themselves")
@Menu(order = 9, title = "UC9 — Translations as signals")
public class TranslationSignalView extends VerticalLayout {

    private final ValueSignal<Integer> count = new ValueSignal<>(1);
    private final H2 title = new H2();
    private final Paragraph items = new Paragraph();
    private final Button addItem = new Button("+");
    private final Button removeItem = new Button("−");
    private final Button checkout = new Button();

    public TranslationSignalView() {
        add(new H1("UC9 — Translations as signals"));
        add(new Paragraph("Each text below is bound once, and follows both "
                + "the language and the number of items without any "
                + "LocaleChangeObserver code."));
        add(new Pre("""
                title.bindText(translate("uc9.title"));
                items.bindText(translate("uc9.items", count));
                checkout.bindText(translate("uc9.checkout"));"""));

        title.bindText(MissingAPI.translate("uc9.title"));
        items.bindText(MissingAPI.translate("uc9.items", count));
        checkout.bindText(MissingAPI.translate("uc9.checkout"));
        checkout.addThemeVariants(ButtonVariant.PRIMARY);
        checkout.bindEnabled(count.map(c -> c > 0));

        addItem.addClickListener(e -> count.update(c -> c + 1));
        removeItem.addClickListener(e -> count.update(c -> Math.max(0, c - 1)));
        removeItem.bindEnabled(count.map(c -> c > 0));
        addItem.setAriaLabel("Add an item");
        removeItem.setAriaLabel("Remove an item");

        Div sample = new Div(title, new HorizontalLayout(removeItem, addItem),
                items, checkout);
        sample.addClassName("sample");
        add(sample);
    }

    // Package-private test seams.
    String titleText() {
        return title.getText();
    }

    String itemsText() {
        return items.getText();
    }

    Button addItem() {
        return addItem;
    }

    Button removeItem() {
        return removeItem;
    }

    Button checkout() {
        return checkout;
    }

}
