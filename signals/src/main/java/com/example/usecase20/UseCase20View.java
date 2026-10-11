package com.example.usecase20;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.common.UseCaseDescription;
import com.example.preferences.UserPreferences;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.local.ValueSignal;

@Route(value = "use-case-20", layout = MainLayout.class)
@PageTitle("Use Case 20: Session-scoped User Preferences")
@UseCaseDescription("Keeping user preferences in a session-scoped signal across views")
@Menu(order = 20, title = "UC 20: User Preferences")
@StyleSheet("usecase20.css")
@AnonymousAllowed
public class UseCase20View extends VerticalLayout {

    private static final Map<String, String> PRESET_COLORS = createPresetColors();

    private static Map<String, String> createPresetColors() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Default (Theme Base)", UserPreferences.DEFAULT_COLOR);
        map.put("Warm Amber", "#ffd54f");
        map.put("Fresh Green", "#81c784");
        map.put("Sky Blue", "#64b5f6");
        map.put("Soft Purple", "#ce93d8");
        map.put("Coral", "#ffab91");
        return map;
    }

    public UseCase20View(UserPreferences preferences) {
        addClassName("usecase20-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Use Case 20: Session-scoped User Preferences");

        Paragraph description = new Paragraph(
                "Choose a background color for your session. The selected color is stored "
                        + "in a session-scoped state and applied across all views using this layout. "
                        + "Other users keep their own color. Changes are reactive without reload.");

        // Local signal mirroring the session-scoped preference for binding
        // convenience
        ValueSignal<String> colorSignal = preferences.backgroundColorSignal();

        // Color selection control
        RadioButtonGroup<String> colorPicker = new RadioButtonGroup<>(
                "Background Color");
        colorPicker.setItems(PRESET_COLORS.values());
        colorPicker.setRenderer(new ComponentRenderer<>(item -> {
            String label = PRESET_COLORS.entrySet().stream()
                    .filter(e -> e.getValue().equals(item))
                    .map(Map.Entry::getKey).findFirst().orElse(item);
            Div tile = new Div();
            tile.addClassName("color-tile");
            // Per-item color is dynamic — keep inline
            tile.getStyle().set("background-color", item);
            String textColor = chooseTextColor(item);
            Paragraph text = new Paragraph(label);
            text.addClassName("color-tile-text");
            // Per-item text color is dynamic — keep inline
            text.getStyle().set("color", textColor);
            tile.add(text);
            return tile;
        }));
        colorPicker.bindValue(colorSignal, colorSignal::set);

        add(title, description, colorPicker);
    }

    private static String chooseTextColor(String background) {
        if (background == null) {
            return "var(--vaadin-text-color)";
        }
        String bg = background.trim().toLowerCase();
        // If CSS var, use default text color
        if (bg.startsWith("var(")) {
            return "var(--vaadin-text-color)";
        }
        // Expect #rrggbb
        if (bg.startsWith("#") && (bg.length() == 7)) {
            try {
                int r = Integer.parseInt(bg.substring(1, 3), 16);
                int g = Integer.parseInt(bg.substring(3, 5), 16);
                int b = Integer.parseInt(bg.substring(5, 7), 16);
                // Calculate relative luminance (sRGB)
                double rs = r / 255.0;
                double gs = g / 255.0;
                double bs = b / 255.0;
                // simple luminance approximation
                double luminance = 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
                return luminance > 0.6 ? "#000000" : "#ffffff";
            } catch (Exception ignored) {
                // fall through
            }
        }
        return "var(--vaadin-text-color)";
    }
}
