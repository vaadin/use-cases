package com.example.uc3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = RegistrationView.class)
class RegistrationViewTest extends SpringBrowserlessTest {

    @Test
    void invalidEmailShowsItsMessage() {
        navigate(RegistrationView.class);
        EmailField email = findInView(EmailField.class).single();

        test(email).setValue("not-an-email");

        assertTrue(email.isInvalid());
        assertEquals(RegistrationView.INVALID_EMAIL, email.getErrorMessage());
    }

    @Test
    void shortPasswordAndYoungAgeAreRejected() {
        navigate(RegistrationView.class);

        test(password("Password")).setValue("short");
        test(findInView(IntegerField.class).single()).setValue(16);

        assertEquals("At least 8 characters",
                password("Password").getErrorMessage());
        assertEquals("You must be between 18 and 120",
                findInView(IntegerField.class).single().getErrorMessage());
    }

    @Test
    void confirmationIsRecheckedWhenThePasswordChanges() {
        navigate(RegistrationView.class);

        test(password("Password")).setValue("correct horse");
        test(password("Confirm password")).setValue("correct horse");
        assertFalse(password("Confirm password").isInvalid());

        test(password("Password")).setValue("battery staple");

        assertTrue(password("Confirm password").isInvalid());
        assertEquals(RegistrationView.PASSWORD_MISMATCH,
                password("Confirm password").getErrorMessage());
    }

    @Test
    void buttonIsEnabledOnlyForAFullyValidFormWhichWritesTheBean() {
        RegistrationView view = navigate(RegistrationView.class);
        Button create = findInView(Button.class).single();
        assertFalse(create.isEnabled());

        test(findInView(EmailField.class).single()).setValue("ada@example.com");
        test(password("Password")).setValue("correct horse");
        test(password("Confirm password")).setValue("correct horse");
        test(findInView(IntegerField.class).single()).setValue(36);
        assertFalse(create.isEnabled(), "terms not accepted yet");
        assertNull(view.created());

        test(findInView(Checkbox.class).single()).click();
        assertTrue(create.isEnabled());
        test(create).click();

        RegistrationView.Registration created = view.created();
        assertNotNull(created);
        assertEquals("ada@example.com", created.getEmail());
        assertEquals(36, created.getAge());
    }

    private PasswordField password(String label) {
        return findInView(PasswordField.class).all().stream()
                .filter(f -> label.equals(f.getLabel())).findFirst()
                .orElseThrow();
    }
}
