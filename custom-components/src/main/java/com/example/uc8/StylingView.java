package com.example.uc8;

import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.components.QrCode;
import com.example.components.RatingStars;
import com.example.components.RatingStarsVariant;
import com.example.components.SignaturePad;
import com.example.components.Sparkline;
import com.example.components.SparklineVariant;
import com.example.views.MainLayout;
import com.example.views.Sample;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC8 — Styling and theming.
 * <p>
 * A component used in many applications needs a public styling API, so that
 * users style it without depending on its internals. These components offer
 * three kinds:
 * <ul>
 * <li>theme variants, as a Java enum ({@code RatingStarsVariant.LARGE},
 * {@code SparklineVariant.ERROR});</li>
 * <li>CSS variables ({@code --rating-stars-color},
 * {@code --signature-pad-ink});</li>
 * <li>shadow parts ({@code signature-pad::part(canvas)},
 * {@code qr-code::part(svg)}).</li>
 * </ul>
 * Their default styles use the theme's tokens, so they follow light and dark
 * mode, which the checkbox switches.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Styling and theming")
@UseCaseDescription("Giving components theme variants, CSS variables and parts that follow the theme")
@Menu(order = 8, title = "UC8 — Styling and theming")
@StyleSheet("uc8.css")
public class StylingView extends VerticalLayout {

    public StylingView() {
        addClassName("uc8-view");
        add(new H1("UC8 — Styling and theming"));
        add(new Paragraph("The same components with their variants, with "
                + "their CSS variables changed, and with a part restyled. "
                + "Switch to dark mode: the defaults follow the theme."));

        Checkbox dark = new Checkbox("Dark mode");
        dark.addValueChangeListener(e -> UI.getCurrent().getPage()
                .setColorScheme(e.getValue() ? ColorScheme.Value.DARK
                        : ColorScheme.Value.LIGHT));

        RatingStars small = rating(3);
        small.addThemeVariants(RatingStarsVariant.SMALL);
        RatingStars large = rating(4);
        large.addThemeVariants(RatingStarsVariant.LARGE);
        RatingStars hearts = rating(5);
        hearts.addClassName("hearts");

        List<Double> values = List.of(3.0, 5.0, 4.0, 8.0, 6.0, 9.0, 7.0);
        Sparkline plain = new Sparkline(values, "Plain");
        Sparkline success = new Sparkline(values, "Success");
        success.addThemeVariants(SparklineVariant.SUCCESS,
                SparklineVariant.FILLED);
        Sparkline error = new Sparkline(values, "Error");
        error.addThemeVariants(SparklineVariant.ERROR);

        SignaturePad blueInk = new SignaturePad();
        blueInk.addClassName("blue-ink");
        blueInk.setWidth("16rem");
        QrCode brand = new QrCode("https://vaadin.com");
        brand.addClassName("brand");

        add(dark,
                new Sample("Theme variants", row("small", small),
                        row("large", large), row("success + filled", success),
                        row("error", error), row("default", plain)),
                new Sample("CSS variables", row("--rating-stars-color", hearts),
                        row("--signature-pad-ink (draw)", blueInk)),
                new Sample("Shadow parts", row("qr-code::part(svg)", brand)));
    }

    private static RatingStars rating(int value) {
        RatingStars rating = new RatingStars("Example rating");
        rating.setValue(value);
        return rating;
    }

    private static HorizontalLayout row(String label, Component component) {
        Span name = new Span(label);
        name.addClassName("status");
        name.setWidth("14rem");
        HorizontalLayout row = new HorizontalLayout(name, component);
        row.setAlignItems(Alignment.CENTER);
        return row;
    }
}
