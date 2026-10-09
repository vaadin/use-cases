package com.example.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards that every use case can describe itself outside the app: the
 * {@link UseCaseDescription} is what a link preview (Open Graph tags) shows
 * for a use case, so a view listed in the menu without one would unfurl with
 * no description.
 */
class UseCaseDescriptionTest {

    private static final Path ROOT = Path.of("..");
    private static final Pattern MENU = Pattern.compile("@Menu\\s*\\(");
    private static final Pattern HOME_MENU = Pattern
            .compile("@Menu\\s*\\([^)]*title\\s*=\\s*\"Home\"");

    @Test
    void everyUseCaseViewHasADescription() throws IOException {
        List<Path> missing;
        try (Stream<Path> files = Files.walk(ROOT)) {
            missing = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString()
                            .contains("/src/main/java/"))
                    .filter(UseCaseDescriptionTest::isUseCaseView)
                    .map(ROOT::relativize).sorted().toList();
        }
        assertTrue(missing.isEmpty(), missing.size()
                + " use-case views without @UseCaseDescription: " + missing);
    }

    private static boolean isUseCaseView(Path source) {
        try {
            String code = Files.readString(source);
            return MENU.matcher(code).find()
                    && !HOME_MENU.matcher(code).find()
                    && !code.contains("@UseCaseDescription(");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
