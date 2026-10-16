package com.example.uc3;

import java.util.List;

import com.example.AppearanceSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = TenantThemeView.class)
class TenantThemeViewTest extends SpringBrowserlessTest {

    @Autowired
    private TenantStyleSheetController controller;

    @Test
    void rendersTheDefaultLookWithoutACustomer() {
        navigate(TenantThemeView.class);
        runPendingSignalsTasks();

        assertEquals("UC3 — Theme per customer",
                findInView(H1.class).single().getText());
        assertTrue(hasHeading("Orders portal"));
        assertEquals(List.of("aura/aura.css"),
                AppearanceSetup.styleSheets(UI.getCurrent()));
    }

    @Test
    void signingInLoadsTheCustomerStylesheet() {
        TenantThemeView view = navigate(TenantThemeView.class);

        test(view.tenantSelect()).selectItem("Globex Bank");
        runPendingSignalsTasks();

        assertEquals(List.of("aura/aura.css", "tenant-theme/globex.css"),
                AppearanceSetup.styleSheets(UI.getCurrent()));
        assertTrue(hasHeading("Globex Bank portal"));
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "GB".equals(s.getText())));
        assertTrue(view.css().contains("--aura-accent-color-light: #4338ca;"));
    }

    @Test
    void controllerServesTheGeneratedStylesheet() {
        var found = controller.styleSheet("acme");
        assertEquals(HttpStatus.OK, found.getStatusCode());
        assertEquals(TenantStyleSheetController.TEXT_CSS,
                found.getHeaders().getContentType());
        assertTrue(found.getBody().contains("--aura-base-radius: 2;"));

        assertEquals(HttpStatus.NOT_FOUND,
                controller.styleSheet("unknown").getStatusCode());
    }

    @Test
    void customerDataCannotInjectCss() {
        assertThrows(IllegalArgumentException.class,
                () -> new Tenant("evil", "Evil", "red;}body{display:none",
                        "#000000", "#ffffff", 4, Tenant.Font.SANS));
        assertThrows(IllegalArgumentException.class, () -> new Tenant("../x",
                "X", "#000000", "#000000", "#ffffff", 4, Tenant.Font.SANS));
    }

    private boolean hasHeading(String text) {
        return findInView(H2.class).all().stream()
                .anyMatch(h -> text.equals(h.getText()));
    }
}
