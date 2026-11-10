package com.example.ai;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;

/**
 * Explains that a use case needs an API key the app was started without. The
 * view still renders, so the reader can see what the use case would do.
 */
public class ProviderNotice extends Div {

    public ProviderNotice(AiProvider provider) {
        addClassName("provider-notice");
        add(new Paragraph("This use case calls " + provider.displayName()
                + ", and this app was started without an API key for it. Set "
                + provider.keyVariable() + " and restart the app to try it."));
    }

    /**
     * Returns a notice for the first of the given providers that has no key, or
     * {@code null} when all of them are configured.
     */
    public static @Nullable ProviderNotice forMissing(AiProviders providers,
            AiProvider... required) {
        for (AiProvider provider : required) {
            if (!providers.isConfigured(provider)) {
                return new ProviderNotice(provider);
            }
        }
        return null;
    }
}
