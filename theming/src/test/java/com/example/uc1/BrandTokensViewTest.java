package com.example.uc1;

import java.util.List;

import com.example.AppearanceSetup;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = BrandTokensView.class)
class BrandTokensViewTest extends SpringBrowserlessTest {

    @Test
    void rendersWithTheBaseThemeOnly() {
        navigate(BrandTokensView.class);
        runPendingSignalsTasks();

        assertEquals("UC1 — Brand with design tokens",
                findInView(H1.class).single().getText());
        assertEquals("Order #1042", findInView(H2.class).first().getText());
        assertEquals(List.of("aura/aura.css"),
                AppearanceSetup.styleSheets(UI.getCurrent()));
    }

    @Test
    void showsTheBrandStylesheetAsWritten() {
        navigate(BrandTokensView.class);

        List<String> files = findInView(Details.class).all().stream()
                .map(Details::getSummaryText).toList();
        assertTrue(files.stream().anyMatch(f -> f.startsWith("brand.css · ")),
                files.toString());
        // The stylesheet as written, not the minified copy that is served.
        assertTrue(findInView(Pre.class).all().stream().anyMatch(pre -> pre
                .getText().contains("--aura-accent-color-light: #0f766e;")));
    }

    @Test
    void brandStylesheetIsAddedAfterTheThemeAndRemovedAgain() {
        BrandTokensView view = navigate(BrandTokensView.class);

        test(view.brandToggle()).click();
        runPendingSignalsTasks();
        assertEquals(List.of("aura/aura.css", "brand.css"),
                AppearanceSetup.styleSheets(UI.getCurrent()));

        test(view.brandToggle()).click();
        runPendingSignalsTasks();
        assertEquals(List.of("aura/aura.css"),
                AppearanceSetup.styleSheets(UI.getCurrent()));
    }
}
