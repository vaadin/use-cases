package com.example.common;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the cross-app selector against drifting out of sync with the modules
 * that are actually built and deployed.
 */
class AppCatalogTest {

    private static final Path ROOT = Path.of("..");
    private static final Pattern FLY_APP = Pattern
            .compile("(?m)^app\\s*=\\s*['\"]([^'\"]+)['\"]");

    @Test
    void everyAppModuleIsListedInTheCatalog() throws Exception {
        List<String> catalogIds = AppCatalog.APPS.stream()
                .map(AppCatalog.App::id).toList();
        List<String> appModules = appModules();

        List<String> missing = appModules.stream()
                .filter(module -> !catalogIds.contains(module)).toList();
        assertTrue(missing.isEmpty(),
                "Modules missing from AppCatalog.APPS: " + missing);

        List<String> unknown = catalogIds.stream()
                .filter(id -> !appModules.contains(id)).toList();
        assertTrue(unknown.isEmpty(),
                "AppCatalog.APPS entries without a module: " + unknown);
    }

    @Test
    void catalogUrlsPointToTheModulesFlyApp() throws IOException {
        for (AppCatalog.App app : AppCatalog.APPS) {
            Optional<String> flyApp = flyAppName(app.id());
            if (flyApp.isPresent()) {
                assertEquals("https://" + flyApp.get() + ".fly.dev/", app.url(),
                        "URL of " + app.id());
            }
        }
    }

    private static List<String> appModules() throws Exception {
        Document pom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(ROOT.resolve("pom.xml").toFile());
        NodeList modules = pom.getElementsByTagName("module");
        List<String> result = new ArrayList<>();
        for (int i = 0; i < modules.getLength(); i++) {
            String module = modules.item(i).getTextContent().trim();
            if (!module.equals("common")) {
                result.add(module);
            }
        }
        return result;
    }

    private static Optional<String> flyAppName(String module)
            throws IOException {
        Path flyToml = ROOT.resolve(module).resolve("fly.toml");
        if (!Files.exists(flyToml)) {
            return Optional.empty();
        }
        Matcher matcher = FLY_APP.matcher(Files.readString(flyToml));
        return matcher.find() ? Optional.of(matcher.group(1))
                : Optional.empty();
    }
}
