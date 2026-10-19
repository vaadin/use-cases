package com.example.views;

import com.example.Appearance;
import com.example.BaseTheme;
import com.example.common.BaseMainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;

@PageTitle("Theming Use Cases")
public class MainLayout extends BaseMainLayout {

    public MainLayout() {
        super("theming", "Theming Use Cases");
        addToNavbar(themePicker());
    }

    /**
     * Aura or Lumo for the whole application, from every view. UC9 explains
     * what switching does.
     */
    private static Div themePicker() {
        Appearance appearance = Appearance.current();
        Select<BaseTheme> picker = new Select<>();
        picker.addClassName("theme-picker");
        picker.setItems(BaseTheme.values());
        picker.setItemLabelGenerator(BaseTheme::title);
        picker.setAriaLabel("Theme");
        picker.bindValue(appearance.theme(), value -> {
            if (value != null) {
                appearance.setTheme(value);
            }
        });
        Div wrapper = new Div(picker);
        wrapper.addClassName("theme-picker-wrapper");
        return wrapper;
    }
}
