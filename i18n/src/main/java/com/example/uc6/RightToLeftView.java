package com.example.uc6;

import java.util.Locale;

import com.example.LanguagePreference;
import com.example.MissingAPI;
import com.example.SupportedLocales;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC6 — Right-to-left layout.
 * <p>
 * Arabic and Hebrew are written from right to left, and so are the applications
 * in those languages: the drawer moves to the right, form labels and text align
 * right, and "back" points to the right. The application switches
 * {@link UI#setDirection} together with the locale (see {@code I18nConfig}),
 * and the Vaadin components mirror themselves.
 * <p>
 * The application's own styling has to cooperate. The two cards show why: the
 * left one uses physical CSS ({@code margin-left}, {@code border-left},
 * {@code text-align: left}) and stays put, the right one uses logical
 * properties ({@code margin-inline-start}, {@code border-inline-start},
 * {@code text-align: start}) and flips. Icons that point in the reading
 * direction (arrows, chevrons) have to be mirrored as well, while icons that do
 * not (search, a check mark, a phone) must stay as they are.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Right-to-left layout")
@UseCaseDescription("Mirroring the layout and icons for Arabic and Hebrew")
@Menu(order = 6, title = "UC6 — Right-to-left layout")
@StyleSheet("uc6.css")
public class RightToLeftView extends VerticalLayout {

    private final Span direction = new Span();
    private final ValueSignal<Boolean> mirror = new ValueSignal<>(true);
    private final Checkbox mirrorIcons = new Checkbox("Mirror direction icons");
    private final Div sample = new Div();
    private final H2 title = new H2();
    private final Span step = new Span();
    private final TextField name = new TextField();
    private final TextField phone = new TextField();
    private final EmailField email = new EmailField();
    private final TextField search = new TextField();
    private final Button back = new Button(mirrored(VaadinIcon.ARROW_LEFT));
    private final Button next = new Button(mirrored(VaadinIcon.ARROW_RIGHT));
    private final Button confirm = new Button(VaadinIcon.CHECK.create());
    private final Paragraph physicalNote = new Paragraph();
    private final Paragraph logicalNote = new Paragraph();

    public RightToLeftView() {
        UI ui = UI.getCurrent();
        LanguagePreference preference = LanguagePreference.current();
        addClassName("uc6-view");

        add(new H1("UC6 — Right-to-left layout"));
        add(new Paragraph("Switch to Arabic or Hebrew and the whole "
                + "application mirrors: the drawer, the form, the buttons. "
                + "Compare the two cards to see which CSS follows along, and "
                + "turn off icon mirroring to see arrows pointing the wrong "
                + "way."));

        HorizontalLayout languages = new HorizontalLayout();
        for (Locale locale : SupportedLocales.ALL) {
            Button button = new Button(SupportedLocales.nativeName(locale),
                    e -> preference.choose(locale));
            button.bindThemeName(ButtonVariant.PRIMARY.getVariantName(),
                    ui.localeSignal().map(locale::equals));
            languages.add(button);
        }
        languages.setWrap(true);

        direction.addClassName("direction");
        // Code, not prose: it reads left to right in every language.
        direction.getElement().setAttribute("dir", "ltr");
        direction.bindText(ui.localeSignal().map(locale -> "dir=\""
                + MissingAPI.directionOf(locale).getClientName() + "\""));

        sample.addClassName("sample");
        mirrorIcons.bindValue(mirror, mirror::set);
        sample.bindClassName("mirror-icons", mirror);
        add(languages, new HorizontalLayout(direction, mirrorIcons), sample);

        step.addClassName("step");
        phone.setPrefixComponent(VaadinIcon.PHONE.create());
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        FormLayout form = new FormLayout(name, phone, email, search);
        next.setIconAfterText(true);
        next.addThemeVariants(ButtonVariant.PRIMARY);
        HorizontalLayout buttons = new HorizontalLayout(back, next, confirm);
        Div cards = new Div(
                card("physical", "margin-left, border-left, text-align: left",
                        physicalNote),
                card("logical", "margin-inline-start, border-inline-start, "
                        + "text-align: start", logicalNote));
        cards.addClassName("cards");
        sample.add(title, step, form, buttons, cards);

        Signal.effect(this, () -> localize(ui.localeSignal().get()));
    }

    private void localize(Locale locale) {
        title.setText(getTranslation(locale, "uc6.shipping"));
        step.setText(getTranslation(locale, "uc6.step", 2, 3));
        name.setLabel(getTranslation(locale, "uc6.name"));
        phone.setLabel(getTranslation(locale, "uc6.phone"));
        email.setLabel(getTranslation(locale, "uc6.email"));
        search.setLabel(getTranslation(locale, "uc6.search"));
        back.setText(getTranslation(locale, "uc6.back"));
        next.setText(getTranslation(locale, "uc6.next"));
        confirm.setText(getTranslation(locale, "uc6.confirm"));
        physicalNote.setText(getTranslation(locale, "uc6.note"));
        logicalNote.setText(getTranslation(locale, "uc6.note"));
    }

    /** An icon that points in the reading direction, so it has to mirror. */
    private static Icon mirrored(VaadinIcon icon) {
        Icon component = icon.create();
        component.addClassName("mirror-rtl");
        return component;
    }

    private static Component card(String kind, String css, Paragraph note) {
        H3 title = new H3(css);
        title.getElement().setAttribute("dir", "ltr");
        Div card = new Div(title, note);
        card.addClassNames("note-card", kind);
        return card;
    }

    // Package-private test seams.
    String direction() {
        return direction.getText();
    }

    Div sample() {
        return sample;
    }

    Checkbox mirrorIcons() {
        return mirrorIcons;
    }
}
