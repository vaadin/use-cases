package com.example.uc8;

import java.util.ArrayList;
import java.util.List;

import com.example.Appearance;
import com.example.Colors;
import com.example.MissingAPI;
import com.example.common.UseCaseDescription;
import com.example.uc3.Tenant;
import com.example.uc3.Tenants;
import com.example.views.ComponentShowcase;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC8 — Accessible theming.
 * <p>
 * Some users need stronger contrast, some get dizzy from animations, and
 * keyboard users rely on a clearly visible focus ring. Operating systems have
 * settings for these; the browser exposes them as media queries, and the view
 * shows what it reports. The application offers its own switches as well (high
 * contrast raises Aura's contrast level and widens the focus ring; reduce
 * motion turns off animations), which a user can set from their system settings
 * with one click.
 * <p>
 * Colors chosen for a brand, a customer or by a user have to stay readable: the
 * contrast check computes the WCAG ratio of each accent color against the white
 * text drawn on it.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Accessible theming")
@UseCaseDescription("Respecting contrast, motion and focus needs, and checking brand colors")
@Menu(order = 8, title = "UC8 — Accessible theming")
public class AccessibleThemingView extends VerticalLayout {

    static final String MORE_CONTRAST = "(prefers-contrast: more)";
    static final String REDUCED_MOTION = "(prefers-reduced-motion: reduce)";
    static final String FORCED_COLORS = "(forced-colors: active)";
    static final String BRAND_ACCENT = "#0f766e";
    static final double AA = 4.5;

    private final Checkbox highContrast = new Checkbox("High contrast");
    private final Checkbox reduceMotion = new Checkbox("Reduce motion");
    private final UnorderedList checks = new UnorderedList();

    public AccessibleThemingView(Tenants tenants) {
        Appearance appearance = Appearance.current();

        add(new H1("UC8 — Accessible theming"));
        add(new Paragraph("Turn on high contrast or reduced motion, or take "
                + "them over from your system settings. Tab through the form "
                + "below to see the focus ring. The contrast check tells "
                + "which accent colors are too light for white text."));

        Signal<@Nullable Boolean> moreContrast = MissingAPI.mediaQuery(this,
                MORE_CONTRAST);
        Signal<@Nullable Boolean> lessMotion = MissingAPI.mediaQuery(this,
                REDUCED_MOTION);
        Signal<@Nullable Boolean> forcedColors = MissingAPI.mediaQuery(this,
                FORCED_COLORS);
        UnorderedList system = new UnorderedList(
                setting("Prefers more contrast", moreContrast),
                setting("Prefers reduced motion", lessMotion),
                setting("Forced colors (Windows high contrast)", forcedColors));

        highContrast.bindValue(appearance.highContrast(),
                appearance::setHighContrast);
        reduceMotion.bindValue(appearance.reduceMotion(),
                appearance::setReduceMotion);
        Button fromSystem = new Button("Use my system settings", e -> {
            appearance
                    .setHighContrast(Boolean.TRUE.equals(moreContrast.peek()));
            appearance.setReduceMotion(Boolean.TRUE.equals(lessMotion.peek()));
        });

        Div settings = new Div(new H2("Your system"), system, highContrast,
                reduceMotion, fromSystem);
        settings.addClassName("sample");

        Div contrast = new Div(new H2("Accent colors with white text"), checks);
        contrast.addClassName("sample");
        Signal.effect(this, () -> {
            List<String[]> colors = new ArrayList<>();
            String own = appearance.accent().get();
            if (own != null) {
                colors.add(new String[] { "Your accent (UC4)", own });
            }
            colors.add(new String[] { "Brand (UC1)", BRAND_ACCENT });
            for (Tenant tenant : tenants.all()) {
                colors.add(new String[] { tenant.name() + " (UC3)",
                        tenant.accentLight() });
                colors.add(new String[] { tenant.name() + " dark mode",
                        tenant.accentDark() });
            }
            checks.removeAll();
            colors.forEach(c -> checks.add(check(c[0], c[1])));
        });

        add(settings, contrast, new ComponentShowcase());
    }

    private static ListItem setting(String label,
            Signal<@Nullable Boolean> value) {
        Span text = new Span();
        text.addClassName("status");
        text.bindText(
                value.map(v -> v == null ? "not known yet" : v ? "yes" : "no"));
        return new ListItem(new Span(label + ": "), text);
    }

    static ListItem check(String label, String color) {
        double ratio = Colors.contrastRatio(color, "#ffffff");
        Span swatch = new Span("Aa");
        swatch.addClassName("swatch");
        swatch.getStyle().set("background", color).set("color", "white")
                .set("padding", "0 0.5rem").set("border-radius", "4px")
                .set("margin-inline-end", "0.5rem");
        Badge verdict = new Badge(ratio >= AA ? "AA" : "Fails AA");
        verdict.addThemeVariants(
                ratio >= AA ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);
        return new ListItem(swatch,
                new Span("%s %s · %.1f:1 ".formatted(label, color, ratio)),
                verdict);
    }

    // Package-private test seams.
    Checkbox highContrast() {
        return highContrast;
    }

    Checkbox reduceMotion() {
        return reduceMotion;
    }

    List<String> checkTexts() {
        return checks.getChildren()
                .map(item -> item.getElement().getTextRecursively()).toList();
    }
}
