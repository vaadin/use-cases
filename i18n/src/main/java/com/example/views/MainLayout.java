package com.example.views;

import java.util.Locale;

import com.example.LanguagePreference;
import com.example.SupportedLocales;
import com.example.common.BaseMainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;

@PageTitle("Internationalization Use Cases")
public class MainLayout extends BaseMainLayout {

    public MainLayout() {
        super("i18n", "Internationalization Use Cases");
        addToNavbar(languagePicker());
    }

    /**
     * The language picker every use case relies on. UC2 explains what picking a
     * language does.
     */
    private static Div languagePicker() {
        Select<Locale> picker = new Select<>();
        picker.addClassName("language-picker");
        picker.setItems(SupportedLocales.ALL);
        picker.setItemLabelGenerator(SupportedLocales::nativeName);
        picker.setPrefixComponent(VaadinIcon.GLOBE.create());
        picker.setAriaLabel("Language");
        picker.bindValue(UI.getCurrent().localeSignal(),
                locale -> LanguagePreference.current().choose(locale));
        Div wrapper = new Div(picker);
        wrapper.addClassName("language-picker-wrapper");
        return wrapper;
    }
}
