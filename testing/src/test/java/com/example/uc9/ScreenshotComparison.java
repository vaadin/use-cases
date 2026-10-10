package com.example.uc9;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compares a screenshot with a reference image for the Playwright tests, which
 * (unlike Playwright for JavaScript) have no screenshot assertion. A pixel
 * counts as different when a color channel differs by more than a small
 * threshold, which absorbs anti-aliasing noise; the images match when at most
 * {@code tolerance} of the pixels differ.
 */
final class ScreenshotComparison {

    private static final int CHANNEL_THRESHOLD = 16;

    private ScreenshotComparison() {
    }

    /**
     * Compares {@code actual} with {@code reference/<name>.png}. When they
     * differ, or there is no reference yet, the actual image is written to
     * {@code errors/<name>.png} and an {@link AssertionError} explains how to
     * accept it.
     */
    static void assertMatches(byte[] actual, String name, double tolerance)
            throws IOException {
        Path reference = Path.of(System.getProperty("screenshots.reference"),
                name + ".png");
        Path errors = Path.of(System.getProperty("screenshots.errors"));
        BufferedImage actualImage = ImageIO
                .read(new ByteArrayInputStream(actual));
        if (!Files.exists(reference)) {
            fail(actual, errors, name, "No reference image " + reference);
        }
        double difference = difference(ImageIO.read(reference.toFile()),
                actualImage);
        if (difference > tolerance) {
            fail(actual, errors, name, "%.2f%% of the pixels differ from %s"
                    .formatted(difference * 100, reference));
        }
    }

    /** The fraction of pixels that differ; 1 when the sizes differ. */
    static double difference(BufferedImage expected, BufferedImage actual) {
        if (expected.getWidth() != actual.getWidth()
                || expected.getHeight() != actual.getHeight()) {
            return 1;
        }
        long different = 0;
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                if (differs(expected.getRGB(x, y), actual.getRGB(x, y))) {
                    different++;
                }
            }
        }
        return (double) different
                / ((long) expected.getWidth() * expected.getHeight());
    }

    private static boolean differs(int a, int b) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs(((a >> shift) & 0xff)
                    - ((b >> shift) & 0xff)) > CHANNEL_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private static void fail(byte[] actual, Path errors, String name,
            String reason) throws IOException {
        Files.createDirectories(errors);
        Path written = errors.resolve(name + ".png");
        Files.write(written, actual);
        throw new AssertionError(reason + ". New screenshot: " + written
                + " (copy it to src/test/screenshots to accept it)");
    }
}
