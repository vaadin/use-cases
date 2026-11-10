package com.example.uc4;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.Div;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NewFeatureTest {

    private static final NewFeature FILTERS = feature("2.1", "Saved filters");
    private static final NewFeature EXPORT = feature("2.1", "Export");
    private static final NewFeature DARK = feature("2.2", "Dark mode");
    private static final NewFeature SHORTCUTS = feature("2.10", "Shortcuts");
    private static final List<NewFeature> FEATURES = List.of(FILTERS, EXPORT,
            DARK, SHORTCUTS);

    @Test
    void onlyFeaturesAfterTheLastSeenVersionAreNew() {
        assertEquals(List.of(FILTERS, EXPORT, DARK, SHORTCUTS),
                NewFeature.since(FEATURES, "2.0"));
        assertEquals(List.of(DARK, SHORTCUTS),
                NewFeature.since(FEATURES, "2.1"));
        assertEquals(List.of(), NewFeature.since(FEATURES, "2.10"));
    }

    @Test
    void versionsCompareByNumberNotByText() {
        // As text, "2.10" would come before "2.2".
        assertEquals(List.of(SHORTCUTS), NewFeature.since(FEATURES, "2.2"));
    }

    private static NewFeature feature(String since, String title) {
        return new NewFeature(since, new Div(), title, title);
    }
}
