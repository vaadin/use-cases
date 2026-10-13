package com.example.pdfviewer;

import java.util.List;

import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlagProvider;

/**
 * Registers the {@code pdfViewerComponent} feature flag with Flow.
 * <p>
 * The web component only defines {@code <vaadin-pdf-viewer>} once
 * {@code window.Vaadin.featureFlags.pdfViewerComponent} is set. Flow sets that
 * for the flags it knows and that are enabled, and it only knows the flags a
 * {@link FeatureFlagProvider} lists. This one is enabled in
 * {@code vaadin-featureflags.properties}. A Flow component would bring its
 * provider along (API-GAPS.md #2); delete this class with the wrapper.
 */
public class PdfViewerFeatureFlagProvider implements FeatureFlagProvider {

    /** The feature flag of {@code <vaadin-pdf-viewer>}. */
    public static final Feature PDF_VIEWER_COMPONENT = new Feature(
            "PDF viewer component", "pdfViewerComponent",
            "https://github.com/vaadin/web-components/issues/7669", true,
            PdfViewer.class.getName());

    @Override
    public List<Feature> getFeatures() {
        return List.of(PDF_VIEWER_COMPONENT);
    }
}
