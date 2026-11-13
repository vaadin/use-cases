package com.example.muc04;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MUC04View.class)
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MUC04ViewTest extends SpringBrowserlessTest {

    @Autowired
    private MUC04Signals muc04Signals;

    @Autowired
    private ApplicationContext applicationContext;

    private TextField getFieldByLabel(String label) {
        return findInView(TextField.class).all().stream()
                .filter(f -> label.equals(f.getLabel())).findFirst()
                .orElseThrow();
    }

    @Test
    void viewRendersWithThreeEditableFields() {
        navigate(MUC04View.class);

        assertEquals(3, findInView(TextField.class).all().size());
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Save Changes".equals(b.getText())));
    }

    @Test
    void fieldsEnabledWhenNoEditors() {
        navigate(MUC04View.class);
        runPendingSignalsTasks();

        findInView(TextField.class).all().forEach(f -> assertTrue(f.isEnabled(),
                "Field '" + f.getLabel() + "' should be enabled"));
    }

    @Test
    void fieldsRemainEnabledWhenLockingDisabled() {
        try (var app = createApp()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(MUC04View.class);
            bob.navigate(MUC04View.class);

            bob.test(field(bob, "Company Name")).focus();
            bob.runPendingSignalsTasks();

            assertEquals(1,
                    alice.findInView(Span.class).withClassName("editor-name")
                            .all().size(),
                    "The other user's edit should be listed");
            assertTrue(field(alice, "Company Name").isEnabled(),
                    "Field should remain enabled when locking is off");
            assertTrue(field(alice, "Address").isEnabled());
            assertTrue(field(alice, "Phone Number").isEnabled());
        }
    }

    @Test
    void fieldDisabledWhenLockingEnabledAndOtherUserEdits() {
        try (var app = createApp()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(MUC04View.class);
            bob.navigate(MUC04View.class);

            alice.test(alice.findInView(Checkbox.class).first()).click();
            alice.runPendingSignalsTasks();

            bob.test(field(bob, "Company Name")).focus();
            bob.runPendingSignalsTasks();

            assertFalse(field(alice, "Company Name").isEnabled(),
                    "Field should be disabled when locking is on and another user is editing");
            assertTrue(field(alice, "Address").isEnabled());
            assertTrue(field(alice, "Phone Number").isEnabled());
            // The editing user keeps the field
            assertTrue(field(bob, "Company Name").isEnabled());
        }
    }

    @Test
    void fieldReenabledWhenLockingToggledOff() {
        try (var app = createApp()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(MUC04View.class);
            bob.navigate(MUC04View.class);

            Checkbox lockingCheckbox = alice.findInView(Checkbox.class).first();
            alice.test(lockingCheckbox).click();
            bob.test(field(bob, "Company Name")).focus();
            bob.runPendingSignalsTasks();

            assertFalse(field(alice, "Company Name").isEnabled());

            alice.test(lockingCheckbox).click();
            alice.runPendingSignalsTasks();

            assertTrue(field(alice, "Company Name").isEnabled(),
                    "Field should be re-enabled when locking is toggled off");
        }
    }

    @Test
    void editingFieldUpdatesSharedSignalValue() {
        navigate(MUC04View.class);
        runPendingSignalsTasks();

        TextField companyNameField = getFieldByLabel("Company Name");
        test(companyNameField).setValue("Acme Corp");
        runPendingSignalsTasks();

        assertEquals("Acme Corp", muc04Signals.getCompanyNameSignal().peek());
    }

    @Test
    void sharedValueFromOtherUserAppearsInField() {
        try (var app = createApp()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(MUC04View.class);
            bob.navigate(MUC04View.class);

            bob.test(field(bob, "Address")).setValue("123 Main St");
            bob.runPendingSignalsTasks();

            assertEquals("123 Main St", field(alice, "Address").getValue());
        }
    }

    private BrowserlessApplicationContext createApp() {
        return SpringBrowserlessApplicationContext.create(applicationContext,
                b -> b.withViewPackages(MUC04View.class)
                        .withSessionLockIsolation(true));
    }

    private static TextField field(BrowserlessUIContext window, String label) {
        return window.findInView(TextField.class).all().stream()
                .filter(f -> label.equals(f.getLabel())).findFirst()
                .orElseThrow();
    }
}
