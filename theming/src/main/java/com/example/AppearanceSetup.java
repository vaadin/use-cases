package com.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;

/**
 * Applies the session's {@link Appearance} to every UI: the stylesheets in
 * order, light or dark, and the user's own token overrides. Each part is an
 * effect, so a change made in one tab reaches every tab of the session.
 */
@Component
public class AppearanceSetup implements VaadinServiceInitListener {

    static final String BRAND_STYLESHEET = "brand.css";
    static final String REDUCE_MOTION_STYLESHEET = "reduce-motion.css";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(
                init -> Appearance.init(init.getSession(), init.getRequest()));
        event.getSource().addUIInitListener(init -> apply(init.getUI()));
    }

    private static void apply(UI ui) {
        Appearance appearance = Appearance.current();
        StyleSheets sheets = new StyleSheets();
        ComponentUtil.setData(ui, StyleSheets.class, sheets);

        Signal.effect(ui, () -> sheets.set(ui, styleSheets(appearance)));
        Signal.effect(ui, () -> ui.getPage()
                .setColorScheme(appearance.colorScheme().get()));
        Signal.effect(ui, () -> MissingAPI.setRootProperties(ui,
                rootProperties(appearance)));
    }

    /**
     * The stylesheets for an appearance, in the order they must be loaded: the
     * base theme first, then the brand or customer styling that overrides its
     * tokens.
     */
    static List<String> styleSheets(Appearance appearance) {
        List<String> sheets = new ArrayList<>();
        sheets.add(appearance.theme().get().styleSheet());
        if (appearance.brand().get()) {
            sheets.add(BRAND_STYLESHEET);
        }
        String tenant = appearance.tenant().get();
        if (tenant != null) {
            sheets.add("tenant-theme/" + tenant + ".css");
        }
        if (appearance.reduceMotion().get()) {
            sheets.add(REDUCE_MOTION_STYLESHEET);
        }
        return sheets;
    }

    /**
     * The user's own adjustments, set inline on the root element so they win
     * over every stylesheet. Each property is listed even when unset, so that a
     * removed adjustment is removed in the browser too.
     */
    static Map<String, @Nullable String> rootProperties(Appearance appearance) {
        Map<String, @Nullable String> properties = new LinkedHashMap<>();
        String accent = appearance.accent().get();
        properties.put("--aura-accent-color-light", accent);
        properties.put("--aura-accent-color-dark", accent);
        properties.put("--lumo-primary-color", accent);
        properties.put("--lumo-primary-text-color", accent);
        FontSize fontSize = appearance.fontSize().get();
        properties.put("--aura-base-font-size",
                fontSize == FontSize.DEFAULT ? null
                        : String.valueOf(fontSize.baseSize()));
        boolean highContrast = appearance.highContrast().get();
        properties.put("--aura-contrast-level", highContrast ? "3" : null);
        properties.put("--vaadin-focus-ring-width",
                highContrast ? "3px" : null);
        return properties;
    }

    /** The stylesheets currently added to a UI, in order. */
    public static List<String> styleSheets(UI ui) {
        StyleSheets sheets = ComponentUtil.getData(ui, StyleSheets.class);
        return sheets == null ? List.of() : sheets.urls();
    }

    private static final class StyleSheets {

        private final List<String> urls = new ArrayList<>();
        private final List<Registration> registrations = new ArrayList<>();

        void set(UI ui, List<String> wanted) {
            // A stylesheet added later wins over an earlier one with the same
            // specificity, so everything after the first difference is
            // removed and re-added in order. The base theme stays loaded.
            int keep = 0;
            while (keep < urls.size() && keep < wanted.size()
                    && urls.get(keep).equals(wanted.get(keep))) {
                keep++;
            }
            while (urls.size() > keep) {
                registrations.removeLast().remove();
                urls.removeLast();
            }
            for (String url : wanted.subList(keep, wanted.size())) {
                registrations.add(ui.getPage().addStyleSheet(url));
                urls.add(url);
            }
        }

        List<String> urls() {
            return List.copyOf(urls);
        }
    }
}
