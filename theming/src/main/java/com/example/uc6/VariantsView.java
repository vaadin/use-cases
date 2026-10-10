package com.example.uc6;

import java.util.List;

import com.example.Appearance;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC6 — Variants or custom CSS.
 * <p>
 * Most styling needs are covered by the components' built-in variants: a
 * primary or danger button, a success badge, a striped grid, an error
 * notification. They follow the theme, light and dark mode and the brand, so
 * they are the first choice. The generic variants ({@code PRIMARY},
 * {@code ERROR}, {@code ROW_STRIPES}) work in every theme; the ones prefixed
 * with a theme name ({@code AURA_DANGER}, {@code LUMO_CONTRAST}) only look
 * different in that theme, which matters when an application might switch
 * themes (UC9).
 * <p>
 * For a look no variant offers, a CSS class with a few component tokens
 * ({@code --vaadin-button-background}) does the job without reaching into the
 * component's internals.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Variants or custom CSS")
@UseCaseDescription("Choosing between built-in component variants and custom CSS")
@Menu(order = 6, title = "UC6 — Variants or custom CSS")
@StyleSheet("uc6.css")
public class VariantsView extends VerticalLayout {

    record Row(String item, String status) {
    }

    public VariantsView() {
        Appearance appearance = Appearance.current();
        addClassName("uc6-view");

        add(new H1("UC6 — Variants or custom CSS"));
        Span theme = new Span();
        theme.addClassName("status");
        theme.bindText(
                appearance.theme().map(t -> "Current theme: " + t.title()));
        add(new Paragraph("Built-in variants first, custom CSS when no "
                + "variant fits. Switch the theme in UC9 and come back: the "
                + "generic variants keep their meaning, the theme-specific "
                + "ones fall back to the default look."), theme);

        Div generic = section("Generic variants (every theme)",
                row(button("Primary", ButtonVariant.PRIMARY), button("Default"),
                        button("Tertiary", ButtonVariant.TERTIARY),
                        button("Success", ButtonVariant.SUCCESS),
                        button("Warning", ButtonVariant.WARNING),
                        button("Error", ButtonVariant.ERROR),
                        button("Small", ButtonVariant.SMALL),
                        button("Large", ButtonVariant.LARGE)),
                row(badge("Paid", BadgeVariant.SUCCESS),
                        badge("Due soon", BadgeVariant.WARNING),
                        badge("Overdue", BadgeVariant.ERROR),
                        badge("Draft", BadgeVariant.CONTRAST),
                        badge("New", BadgeVariant.FILLED)),
                row(notify("Saved", NotificationVariant.SUCCESS),
                        notify("Could not save", NotificationVariant.ERROR)),
                stripedGrid());

        Div specific = section("Theme-specific variants",
                row(button("Aura danger", ButtonVariant.AURA_DANGER),
                        button("Lumo contrast", ButtonVariant.LUMO_CONTRAST),
                        button("Lumo inline", ButtonVariant.LUMO_TERTIARY,
                                ButtonVariant.LUMO_TERTIARY_INLINE)));

        Button gradient = new Button("Upgrade plan");
        gradient.addClassName("gradient");
        Button purple = new Button("Aura accent class");
        purple.addClassName("aura-accent-purple");
        purple.addThemeVariants(ButtonVariant.PRIMARY);
        Span pill = new Span("Beta");
        pill.addClassName("pill");
        Div custom = section("Custom CSS", row(gradient, purple, pill));

        add(generic, specific, custom);
    }

    private static Div section(String title, Component... content) {
        Div section = new Div(new H2(title));
        section.add(content);
        section.addClassName("sample");
        return section;
    }

    private static HorizontalLayout row(Component... components) {
        HorizontalLayout row = new HorizontalLayout(components);
        row.setWrap(true);
        row.setAlignItems(Alignment.CENTER);
        return row;
    }

    private static Button button(String text, ButtonVariant... variants) {
        Button button = new Button(text);
        button.addThemeVariants(variants);
        return button;
    }

    private static Badge badge(String text, BadgeVariant variant) {
        Badge badge = new Badge(text);
        badge.addThemeVariants(variant);
        return badge;
    }

    private static Button notify(String text, NotificationVariant variant) {
        return new Button("Show \"" + text + "\"",
                e -> Notification
                        .show(text, 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(variant));
    }

    private static Grid<Row> stripedGrid() {
        Grid<Row> grid = new Grid<>();
        grid.addColumn(Row::item).setHeader("Item");
        grid.addColumn(Row::status).setHeader("Status");
        grid.setItems(List.of(new Row("Espresso beans", "Paid"),
                new Row("Grinder service", "Due soon"),
                new Row("Paper cups", "Overdue"),
                new Row("Milk frother", "Draft")));
        grid.addThemeVariants(GridVariant.ROW_STRIPES, GridVariant.NO_BORDER);
        grid.setAllRowsVisible(true);
        return grid;
    }
}
