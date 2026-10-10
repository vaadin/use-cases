package com.example.uc9;

import com.example.Appearance;
import com.example.BaseTheme;
import com.example.common.UseCaseDescription;
import com.example.views.ComponentShowcase;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC9 — Aura or Lumo.
 * <p>
 * Vaadin ships two themes, and a team choosing between them wants to see its
 * own screens in both. The picker swaps the theme stylesheet of the running
 * application, in every view and tab, and remembers the choice.
 * <p>
 * What survives the switch: the components, their generic variants, and any
 * application CSS that uses the shared {@code --vaadin-*} tokens. What does
 * not: theme-specific variants, and CSS written against one theme's own tokens,
 * whose names differ ({@code --aura-accent-color} versus
 * {@code --lumo-primary-color}). That is why the brand in UC1 and the customer
 * stylesheets in UC3 set both.
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Aura or Lumo")
@UseCaseDescription("Comparing the two built-in themes on the same screens")
@Menu(order = 9, title = "UC9 — Aura or Lumo")
public class ThemeSwitchView extends VerticalLayout {

    private final Select<BaseTheme> themeSelect = new Select<>();

    public ThemeSwitchView() {
        Appearance appearance = Appearance.current();

        add(new H1("UC9 — Aura or Lumo"));
        add(new Paragraph("Switch the theme and look around: every view "
                + "follows, and the choice is remembered. The list below "
                + "maps the most used tokens between the two."));

        themeSelect.setLabel("Theme");
        themeSelect.setItems(BaseTheme.values());
        themeSelect.setItemLabelGenerator(BaseTheme::title);
        themeSelect.bindValue(appearance.theme(), value -> {
            if (value != null) {
                appearance.setTheme(value);
            }
        });

        UnorderedList tokens = new UnorderedList(
                token("Accent", "--aura-accent-color", "--lumo-primary-color"),
                token("Font", "--aura-font-family", "--lumo-font-family"),
                token("Corners", "--aura-base-radius",
                        "--lumo-border-radius-m"),
                token("Size", "--aura-base-size / theme=\"small\"",
                        "--lumo-size-m / compact preset"),
                token("Both themes", "--vaadin-text-color, "
                        + "--vaadin-background-container, --vaadin-radius-m",
                        "same"));
        tokens.addClassName("status");

        add(themeSelect, tokens, new ComponentShowcase());
    }

    private static ListItem token(String what, String aura, String lumo) {
        return new ListItem(what + ": Aura " + aura + " · Lumo " + lumo);
    }

    // Package-private test seam.
    Select<BaseTheme> themeSelect() {
        return themeSelect;
    }
}
