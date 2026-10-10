package com.example.uc3;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import com.example.views.TestsNote;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * UC3 — Forms and validation.
 * <p>
 * A sign-up form with the usual rules: a valid email, a password of at least
 * eight characters, a confirmation that matches it (a rule across two fields),
 * an age between 18 and 120, and accepted terms. A {@link Binder} holds the
 * rules, and "Create account" is enabled only while all of them pass.
 * <p>
 * The tests check each rule through what the user sees: the field turns invalid
 * with the right message, the button stays disabled, and only a fully valid
 * form writes the bean.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Forms and validation")
@UseCaseDescription("Testing validation rules, error messages and the bean a form writes")
@Menu(order = 3, title = "UC3 — Forms and validation")
@AnonymousAllowed
public class RegistrationView extends VerticalLayout {

    /** What the form writes. */
    public static class Registration {
        private String email = "";
        private String password = "";
        private String confirm = "";
        private @Nullable Integer age;
        private boolean terms;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getConfirm() {
            return confirm;
        }

        public void setConfirm(String confirm) {
            this.confirm = confirm;
        }

        public @Nullable Integer getAge() {
            return age;
        }

        public void setAge(@Nullable Integer age) {
            this.age = age;
        }

        public boolean isTerms() {
            return terms;
        }

        public void setTerms(boolean terms) {
            this.terms = terms;
        }
    }

    static final String INVALID_EMAIL = "Not a valid email";
    static final String PASSWORD_MISMATCH = "Passwords don't match";

    private final EmailField email = new EmailField("Email");
    private final PasswordField password = new PasswordField("Password");
    private final PasswordField confirm = new PasswordField("Confirm password");
    private final IntegerField age = new IntegerField("Age");
    private final Checkbox terms = new Checkbox("I accept the terms");
    private final Button create = new Button("Create account");
    private final Binder<Registration> binder = new Binder<>();
    private @Nullable Registration created;

    public RegistrationView() {
        add(new H1("UC3 — Forms and validation"));
        add(new Paragraph("Each rule shows its message on the field, and the "
                + "button is enabled only when every rule passes. The tests "
                + "go through the rules one by one."));

        // EmailField checks the format itself before the Binder does, and
        // shows its own message, which is empty unless set here.
        email.setI18n(new EmailField.EmailFieldI18n()
                .setPatternErrorMessage(INVALID_EMAIL));
        binder.forField(email).asRequired("Email is required")
                .withValidator(new EmailValidator(INVALID_EMAIL))
                .bind(Registration::getEmail, Registration::setEmail);
        binder.forField(password).asRequired("Password is required")
                .withValidator(new StringLengthValidator(
                        "At least 8 characters", 8, null))
                .bind(Registration::getPassword, Registration::setPassword);
        Binder.Binding<Registration, String> confirmBinding = binder
                .forField(confirm).asRequired("Confirm the password")
                .withValidator(value -> value.equals(password.getValue()),
                        PASSWORD_MISMATCH)
                .bind(Registration::getConfirm, Registration::setConfirm);
        // A change of the password re-checks the confirmation.
        password.addValueChangeListener(e -> {
            if (!confirm.isEmpty()) {
                confirmBinding.validate();
            }
        });
        binder.forField(age).asRequired("Age is required")
                .withValidator(new IntegerRangeValidator(
                        "You must be between 18 and 120", 18, 120))
                .bind(Registration::getAge, Registration::setAge);
        binder.forField(terms)
                .withValidator(Boolean::booleanValue,
                        "Accept the terms to continue")
                .bind(Registration::isTerms, Registration::setTerms);
        binder.setBean(new Registration());

        create.addThemeVariants(ButtonVariant.PRIMARY);
        create.setEnabled(false);
        binder.addStatusChangeListener(
                e -> create.setEnabled(binder.isValid()));
        create.addClickListener(e -> {
            if (binder.validate().isOk()) {
                created = binder.getBean();
                Notification.show("Account created for " + created.getEmail());
            }
        });

        Div sample = new Div(new H2("Create an account"),
                new FormLayout(email, password, confirm, age), terms, create);
        sample.addClassName("sample");
        add(sample, new TestsNote(
                "View test: uc3/RegistrationViewTest (browserless)"));
    }

    // Package-private test seam.
    @Nullable
    Registration created() {
        return created;
    }
}
