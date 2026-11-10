package com.example.uc4;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.Component;

/**
 * A feature of the demo app, with the version of the app it came with.
 *
 * @param since
 *            the app version the feature came with, such as {@code 2.1}
 * @param target
 *            the component of the feature
 * @param title
 *            the heading of the feature's tour step
 * @param text
 *            what the step says about the feature
 */
record NewFeature(String since, Component target, String title, String text) {

    /**
     * The features that came after the given version, in the given order.
     */
    static List<NewFeature> since(List<NewFeature> features,
            String lastSeenVersion) {
        int[] lastSeen = parse(lastSeenVersion);
        return features.stream()
                .filter(f -> Arrays.compare(parse(f.since()), lastSeen) > 0)
                .toList();
    }

    /** A version such as {@code 2.10} as its numbers, so 2.10 is after 2.2. */
    private static int[] parse(String version) {
        return Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt)
                .toArray();
    }
}
