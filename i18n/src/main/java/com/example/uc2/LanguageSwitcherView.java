package com.example.uc2;

import java.util.Locale;
import java.util.stream.Collectors;

import com.example.LanguagePreference;
import com.example.MissingAPI;
import com.example.SupportedLocales;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC2 — Language switcher.
 * <p>
 * On the first visit the language comes from the browser: Flow matches the
 * {@code Accept-Language} header against the supported languages when the
 * session starts. Picking a language, here or in the top bar, applies it to
 * every open tab of the session at once and stores it in a cookie, so the next
 * visit starts in the chosen language even if the browser asks for another.
 * "Forget my choice" drops the cookie and goes back to the browser's language.
 * <p>
 * The view shows each of those inputs, so it is clear where the current
 * language came from.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Language switcher")
@UseCaseDescription("Letting the user pick a language and remembering the choice")
@Menu(order = 2, title = "UC2 — Language switcher")
public class LanguageSwitcherView extends VerticalLayout {

    public LanguageSwitcherView() {
        LanguagePreference preference = LanguagePreference.current();
        UI ui = UI.getCurrent();

        add(new H1("UC2 — Language switcher"));
        add(new Paragraph("Pick a language below or in the top bar. Every "
                + "open tab of this session switches with it, and the choice "
                + "is remembered for your next visit. Forget the choice to go "
                + "back to the language your browser asks for."));

        HorizontalLayout languages = new HorizontalLayout();
        languages.setWrap(true);
        for (Locale locale : SupportedLocales.ALL) {
            Button button = new Button(SupportedLocales.nativeName(locale),
                    e -> preference.choose(locale));
            button.bindThemeName(ButtonVariant.PRIMARY.getVariantName(),
                    ui.localeSignal().map(locale::equals));
            languages.add(button);
        }
        Button forget = new Button("Forget my choice",
                e -> preference.forget());
        forget.addThemeVariants(ButtonVariant.TERTIARY);
        forget.bindEnabled(preference.remembered().map(l -> l != null));

        H2 welcome = new H2();
        welcome.bindText(MissingAPI.translate("uc2.welcome"));
        Paragraph hint = new Paragraph();
        hint.bindText(MissingAPI.translate("uc2.hint"));
        Div sample = new Div(welcome, hint);
        sample.addClassName("sample");

        UnorderedList facts = new UnorderedList(
                fact("Your browser asks for",
                        preference.browserLocales().isEmpty() ? "(not known)"
                                : preference.browserLocales().stream()
                                        .map(Locale::toLanguageTag)
                                        .collect(Collectors.joining(", "))),
                fact("Matched when the session started",
                        preference.matched().toLanguageTag()),
                fact("Remembered from your choice",
                        preference.remembered().map(
                                l -> l == null ? "(none)" : l.toLanguageTag())),
                fact("Current language",
                        ui.localeSignal().map(Locale::toLanguageTag)));
        facts.addClassName("facts");

        add(languages, forget, sample, facts);
    }

    private static ListItem fact(String label, String value) {
        Span text = new Span(value);
        text.addClassName("fact-value");
        return new ListItem(new Span(label + ": "), text);
    }

    private static ListItem fact(String label, Signal<String> value) {
        Span text = new Span();
        text.addClassName("fact-value");
        text.bindText(value);
        return new ListItem(new Span(label + ": "), text);
    }
}
