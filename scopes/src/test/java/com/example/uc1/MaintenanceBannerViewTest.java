package com.example.uc1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MaintenanceBannerView.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MaintenanceBannerViewTest extends SpringBrowserlessTest {

    @Autowired
    private MaintenanceNotice notice;

    @Test
    void viewRendersHeadingAndConsole() {
        navigate(MaintenanceBannerView.class);

        assertEquals("UC1 — Maintenance banner",
                findInView(H1.class).single().getText());
        assertTrue(findInView(Div.class).withClassName("maintenance-banner")
                .all().isEmpty());
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Publish to everyone".equals(b.getText())));
    }

    @Test
    void publishShowsBannerAndClearHidesIt() {
        navigate(MaintenanceBannerView.class);
        runPendingSignalsTasks();

        test(findInView(TextField.class).single())
                .setValue("Maintenance at 18:00");
        test(button("Publish to everyone")).click();
        runPendingSignalsTasks();
        assertEquals("Maintenance at 18:00", banner().getText());

        test(button("Clear notice")).click();
        runPendingSignalsTasks();
        assertTrue(findInView(Div.class).withClassName("maintenance-banner")
                .all().isEmpty());
    }

    @Test
    void noticePublishedInAnotherSessionIsShown() {
        navigate(MaintenanceBannerView.class);
        runPendingSignalsTasks();

        // Another user's session publishes through the same singleton bean.
        notice.publish("Read-only mode until 9:00");
        runPendingSignalsTasks();
        assertEquals("Read-only mode until 9:00", banner().getText());

        // A brand new session sees the same notice.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(MaintenanceBannerView.class);
        runPendingSignalsTasks();
        assertEquals("Read-only mode until 9:00", banner().getText());
    }

    private Div banner() {
        return findInView(Div.class).withClassName("maintenance-banner")
                .single();
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
