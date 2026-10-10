package com.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.signals.Signal;

/**
 * Wires the application's translations and the per-session language handling.
 */
@Configuration
public class I18nConfig {

    /**
     * Flow creates a {@link DefaultI18NProvider} on its own when it finds
     * {@code vaadin-i18n/translations*.properties}, but then the default locale
     * is whichever file it happens to list first. Declaring the bean fixes the
     * order, so a browser asking for an unsupported language gets English
     * rather than, say, Arabic.
     */
    @Bean
    I18NProvider i18nProvider() {
        return new DefaultI18NProvider(SupportedLocales.ALL);
    }

    @Bean
    VaadinServiceInitListener languageSetup() {
        return event -> {
            // Flow has already picked the session's locale from the browser's
            // Accept-Language header; a language the user chose on an earlier
            // visit overrides it.
            event.getSource()
                    .addSessionInitListener(sessionInit -> LanguagePreference
                            .init(sessionInit.getSession(),
                                    sessionInit.getRequest()));
            // The page's text direction follows the language: switching to
            // Arabic or Hebrew mirrors the whole application.
            event.getSource().addUIInitListener(uiInit -> {
                UI ui = uiInit.getUI();
                Signal.effect(ui, () -> ui.setDirection(
                        MissingAPI.directionOf(ui.localeSignal().get())));
            });
        };
    }
}
