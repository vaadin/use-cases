package com.example.uc4;

import java.util.List;
import java.util.Objects;

import com.example.Appearance;
import com.example.Colors;
import com.example.FontSize;
import com.example.common.UseCaseDescription;
import com.example.views.ComponentShowcase;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC4 — User's own look.
 * <p>
 * A settings page where each user picks an accent color and a text size, and
 * the whole application changes while they pick. The choices are token
 * overrides set inline on the page's root element, so they win over the theme,
 * the brand and the customer styling, and they are remembered for the next
 * visit.
 * <p>
 * The text size changes Aura's {@code --aura-base-font-size}, from which every
 * font size, line height and component size is derived. That is different from
 * the browser's zoom: the layout keeps its width and only the text and controls
 * grow.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — User's own look")
@UseCaseDescription("Letting each user pick an accent color and text size, applied live")
@Menu(order = 4, title = "UC4 — User's own look")
@StyleSheet("uc4.css")
public class UserPreferencesView extends VerticalLayout {

    static final List<String> SWATCHES = List.of("#2563eb", "#0f766e",
            "#7c3aed", "#c2410c", "#be123c", "#374151");

    private final Input custom = new Input(ValueChangeMode.ON_CHANGE);
    private final Select<FontSize> fontSize = new Select<>();
    private final Span contrast = new Span();

    public UserPreferencesView() {
        Appearance appearance = Appearance.current();
        addClassName("uc4-view");

        add(new H1("UC4 — User's own look"));
        add(new Paragraph("Pick an accent color or a text size: the whole "
                + "application follows at once, in every tab, and keeps your "
                + "choice after a reload."));

        HorizontalLayout swatches = new HorizontalLayout();
        swatches.setAlignItems(Alignment.CENTER);
        for (String color : SWATCHES) {
            Button swatch = new Button();
            swatch.addClassName("swatch");
            swatch.getStyle().set("--swatch-color", color);
            swatch.setAriaLabel("Accent " + color);
            swatch.bindClassName("selected",
                    appearance.accent().map(color::equalsIgnoreCase));
            swatch.addClickListener(e -> appearance.setAccent(color));
            swatches.add(swatch);
        }
        // Gap: Vaadin has no color picker component.
        custom.setType("color");
        custom.setId("custom-accent");
        custom.bindValue(
                appearance.accent().map(
                        color -> Objects.requireNonNullElse(color, "#2563eb")),
                value -> {
                    if (value != null && Colors.isHex(value)) {
                        appearance.setAccent(value);
                    }
                });
        NativeLabel customLabel = new NativeLabel("Custom");
        customLabel.setFor(custom);
        swatches.add(customLabel, custom);

        fontSize.setLabel("Text size");
        fontSize.setItems(FontSize.values());
        fontSize.setItemLabelGenerator(FontSize::title);
        fontSize.bindValue(appearance.fontSize(), value -> {
            if (value != null) {
                appearance.setFontSize(value);
            }
        });

        Button reset = new Button("Reset to default", e -> {
            appearance.setAccent(null);
            appearance.setFontSize(FontSize.DEFAULT);
        });
        reset.addThemeVariants(ButtonVariant.TERTIARY);

        contrast.addClassName("status");
        contrast.bindText(appearance.accent()
                .map(color -> color == null ? "Theme accent color"
                        : "%s · contrast with white text %.1f:1".formatted(
                                color,
                                Colors.contrastRatio(color, "#ffffff"))));

        Div settings = new Div(new NativeLabel("Accent color"), swatches,
                contrast, fontSize, reset);
        settings.addClassName("settings");
        add(settings, new ComponentShowcase());
    }

    // Package-private test seams.
    Input custom() {
        return custom;
    }

    Select<FontSize> fontSize() {
        return fontSize;
    }

    String contrast() {
        return contrast.getText();
    }
}
