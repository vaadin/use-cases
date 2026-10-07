package com.example.uc5;

import com.example.FullscreenTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = KioskExitDetectionView.class)
class KioskExitDetectionViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithKioskControlsAndLandingScreen() {
        navigate(KioskExitDetectionView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> h.getText().equals("UC5 — Kiosk: visitor sign-in")));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Enter kiosk".equals(b.getText())));
        assertTrue(
                findInView(Button.class).all().stream()
                        .anyMatch(b -> "Start sign-in".equals(b.getText())),
                "landing screen should expose Start sign-in");
    }

    @Test
    void signInFlowAdvancesThroughScreensAndLogs() {
        navigate(KioskExitDetectionView.class);
        runPendingSignalsTasks();

        // The sign-in flow is driven by screenSignal alone, not by the
        // fullscreen state — exercise it without entering fullscreen so the
        // (bindVisible) log Div stays in the tree for the final assertion.
        clickButton("Start sign-in");
        runPendingSignalsTasks();

        TextField name = findInView(TextField.class).all().stream()
                .filter(f -> "Your name".equals(f.getLabel())).findFirst()
                .orElseThrow();
        test(name).setValue("Ada Lovelace");

        @SuppressWarnings("unchecked")
        Select<String> purpose = findInView(Select.class).all().stream()
                .filter(s -> "Purpose of visit".equals(s.getLabel()))
                .findFirst().orElseThrow();
        purpose.setValue("Meeting");
        runPendingSignalsTasks();

        clickButton("Submit");
        runPendingSignalsTasks();

        // Confirmation screen now visible, log records the sign-in.
        assertTrue(
                findInView(Button.class).all().stream()
                        .anyMatch(b -> "New visitor".equals(b.getText())),
                "confirmation screen should expose New visitor");
        assertTrue(
                findInView(Div.class).all().stream()
                        .anyMatch(d -> d.getText() != null && d.getText()
                                .contains("Signed in: Ada Lovelace")),
                "log should record the sign-in");
    }

    @Test
    void unexpectedExitLogsWarning() {
        navigate(KioskExitDetectionView.class);
        runPendingSignalsTasks();

        clickButton("Enter kiosk");
        runPendingSignalsTasks();
        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();

        assertTrue(
                findInView(Div.class).all().stream()
                        .anyMatch(d -> d.getText() != null
                                && d.getText().contains("UNEXPECTED")),
                "log should contain an unexpected-exit entry");
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getClassNames().contains("unexpected-warning")
                        && s.getText() != null && !s.getText().isEmpty()),
                "warning span should be visible after an unexpected exit");
    }

    @Test
    void staffPinExitLogsExpected() {
        navigate(KioskExitDetectionView.class);
        runPendingSignalsTasks();

        clickButton("Enter kiosk");
        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();

        clickButton("Staff");
        runPendingSignalsTasks();

        PasswordField pin = findInView(PasswordField.class).all().stream()
                .findFirst().orElseThrow();
        test(pin).setValue(KioskExitDetectionView.STAFF_PIN);
        clickButton("Confirm");
        runPendingSignalsTasks();
        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();

        assertTrue(
                findInView(Div.class).all().stream()
                        .anyMatch(d -> d.getText() != null && d.getText()
                                .contains("Staff exit confirmed")),
                "log should record the staff exit");
        assertTrue(
                findInView(Div.class).all().stream()
                        .anyMatch(d -> d.getText() != null
                                && d.getText().contains("expected")
                                && !d.getText().contains("UNEXPECTED")),
                "log should record the session as exited-by-code");
    }

    @Test
    void wrongStaffPinIsLoggedAndDoesNotExit() {
        navigate(KioskExitDetectionView.class);
        runPendingSignalsTasks();

        // Staff button is bindVisible(isFullscreen), so enter fullscreen
        // before clicking it.
        clickButton("Enter kiosk");
        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();

        clickButton("Staff");
        runPendingSignalsTasks();

        PasswordField pin = findInView(PasswordField.class).all().stream()
                .findFirst().orElseThrow();
        test(pin).setValue("0000");
        clickButton("Confirm");
        runPendingSignalsTasks();

        assertTrue(
                findInView(Div.class).all().stream()
                        .anyMatch(d -> d.getText() != null && d.getText()
                                .contains("Failed staff exit attempt")),
                "log should record the failed staff attempt");
        // The dialog is still open (still rendering its Cancel button).
        assertTrue(
                findInView(Button.class).all().stream()
                        .anyMatch(b -> "Cancel".equals(b.getText())),
                "staff prompt should still be open after a wrong PIN");
    }

    private void clickButton(String text) {
        Button button = findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "button \"" + text + "\" not found"));
        test(button).click();
    }
}
