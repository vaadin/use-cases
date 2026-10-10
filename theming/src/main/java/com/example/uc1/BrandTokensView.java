package com.example.uc1;

import com.example.Appearance;
import com.example.common.UseCaseDescription;
import com.example.views.ComponentShowcase;
import com.example.views.MainLayout;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Brand with design tokens.
 * <p>
 * Making an application look like the company's own rarely needs more than a
 * handful of CSS custom properties: the accent color, the background, the
 * corner radius and the font. The theme derives everything else from them:
 * hover and focus colors, the text color on accent backgrounds, the dark
 * variants. The brand lives in a stylesheet ({@code brand.css}) loaded after
 * the theme; in a real application it would simply be listed with
 * {@code @StyleSheet} next to the theme.
 * <p>
 * The checkbox adds or removes that stylesheet at runtime, so the difference is
 * visible on the same screen. It applies to every view, which is what a brand
 * does.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Brand with design tokens")
@UseCaseDescription("Making the application look like the company's own with a few CSS variables")
@Menu(order = 1, title = "UC1 — Brand with design tokens")
public class BrandTokensView extends VerticalLayout {

    static final String TOKENS = """
            :root {
                --aura-accent-color-light: #0f766e;
                --aura-accent-color-dark: #2dd4bf;
                --aura-background-color-light: #f6f1e9;
                --aura-background-color-dark: #1c1917;
                --aura-base-radius: 8;
                --aura-font-family: ui-rounded, "Nunito", system-ui;
            }""";

    private final Checkbox brand = new Checkbox("Apply our brand");

    public BrandTokensView() {
        Appearance appearance = Appearance.current();

        add(new H1("UC1 — Brand with design tokens"));
        add(new Paragraph("Tick the checkbox to load brand.css on top of the "
                + "theme. Six tokens change the accent, the background, the "
                + "corners and the font of every component, in light and in "
                + "dark mode. The brand sets Lumo's tokens too, so it also "
                + "works after switching themes in UC9."));

        brand.bindValue(appearance.brand(), appearance::setBrand);
        add(brand, new Pre(TOKENS), new ComponentShowcase());
    }

    // Package-private test seam.
    Checkbox brandToggle() {
        return brand;
    }
}
