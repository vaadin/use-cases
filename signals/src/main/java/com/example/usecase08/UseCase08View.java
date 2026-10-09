package com.example.usecase08;

import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

@Route(value = "use-case-08", layout = MainLayout.class)
@PageTitle("Use Case 8: Multi-Step Wizard with Validation")
@UseCaseDescription("Guiding users through a multi-step wizard with per-step validation")
@Menu(order = 8, title = "UC 8: Multi-Step Wizard")
@StyleSheet("usecase08.css")
@AnonymousAllowed
public class UseCase08View extends VerticalLayout {

    public UseCase08View() {
        addClassName("usecase08-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Use Case 8: Multi-Step Wizard with Validation");

        Paragraph description = new Paragraph(
                "This use case demonstrates a multi-step wizard form using Binder for each step's validation. "
                        + "Complete each step to unlock the next one. "
                        + "Each step has its own Binder with validation rules, and the Next button is enabled "
                        + "only when the current step's Binder validation passes. "
                        + "Review your information in the final step before submitting.");

        // Create shared form data bean
        FormData formData = new FormData();

        // Create signal for current step
        var currentStepSignal = new ValueSignal<>(Step.PERSONAL_INFO);

        // Step 1: Personal Info with Binder
        VerticalLayout step1Layout = new VerticalLayout();
        step1Layout.add(new H3("Step 1: Personal Information"));

        Binder<FormData> step1Binder = new Binder<>(FormData.class);

        TextField firstNameField = new TextField("First Name");
        step1Binder.forField(firstNameField)
                .withValidator(value -> value != null && value.length() >= 2,
                        "First name must be at least 2 characters")
                .bind(FormData::getFirstName, FormData::setFirstName);

        TextField lastNameField = new TextField("Last Name");
        step1Binder.forField(lastNameField)
                .withValidator(value -> value != null && value.length() >= 2,
                        "Last name must be at least 2 characters")
                .bind(FormData::getLastName, FormData::setLastName);

        EmailField emailField = new EmailField("Email");
        step1Binder.forField(emailField)
                .withValidator(new EmailValidator("Please enter a valid email"))
                .bind(FormData::getEmail, FormData::setEmail);

        step1Binder.setBean(formData);

        step1Layout.add(firstNameField, lastNameField, emailField);
        step1Layout.bindVisible(
                () -> currentStepSignal.get() == Step.PERSONAL_INFO);

        // Step 2: Company Info with Binder
        VerticalLayout step2Layout = new VerticalLayout();
        step2Layout.add(new H3("Step 2: Company Information"));

        Binder<FormData> step2Binder = new Binder<>(FormData.class);

        TextField companyNameField = new TextField("Company Name");
        step2Binder.forField(companyNameField)
                .withValidator(
                        value -> value != null && !value.trim().isEmpty(),
                        "Company name is required")
                .bind(FormData::getCompanyName, FormData::setCompanyName);

        ComboBox<String> companySizeSelect = new ComboBox<>("Company Size",
                List.of("1-10", "11-50", "51-200", "201-1000", "1000+"));
        step2Binder.forField(companySizeSelect)
                .withValidator(value -> value != null && !value.isEmpty(),
                        "Please select a company size")
                .bind(FormData::getCompanySize, FormData::setCompanySize);

        ComboBox<String> industrySelect = new ComboBox<>("Industry",
                List.of("Technology", "Healthcare", "Finance", "Retail",
                        "Manufacturing", "Other"));
        step2Binder.forField(industrySelect)
                .withValidator(value -> value != null && !value.isEmpty(),
                        "Please select an industry")
                .bind(FormData::getIndustry, FormData::setIndustry);

        step2Binder.setBean(formData);

        step2Layout.add(companyNameField, companySizeSelect, industrySelect);
        step2Layout.bindVisible(
                () -> currentStepSignal.get() == Step.COMPANY_INFO);

        // Step 3: Plan Selection with Binder
        VerticalLayout step3Layout = new VerticalLayout();
        step3Layout.add(new H3("Step 3: Select Your Plan"));

        Binder<FormData> step3Binder = new Binder<>(FormData.class);

        ComboBox<Plan> planSelect = new ComboBox<>("Plan", Plan.values());
        var planSignal = step3Binder.forField(planSelect)
                .withValidator(value -> value != null, "Please select a plan")
                .bind(FormData::getPlan, FormData::setPlan).valueSignal();

        step3Binder.setBean(formData);

        Span planDescription = new Span(() -> switch (planSignal.get()) {
        case STARTER -> "Perfect for small teams - $29/month";
        case PROFESSIONAL -> "For growing businesses - $99/month";
        case ENTERPRISE -> "Custom solutions - Contact sales";
        case null -> "";
        });

        step3Layout.add(planSelect, planDescription);
        step3Layout.bindVisible(
                () -> currentStepSignal.get() == Step.PLAN_SELECTION);

        // Step 4: Review
        VerticalLayout step4Layout = new VerticalLayout();
        step4Layout.add(new H3("Step 4: Review Your Information"));

        Div reviewDiv = new Div();
        reviewDiv.addClassName("review-content");
        // Update review when entering this step
        Signal.effect(reviewDiv, () -> {
            if (currentStepSignal.get() == Step.REVIEW) {
                String reviewText = "Name: " + formData.getFirstName() + " "
                        + formData.getLastName() + "\n" + "Email: "
                        + formData.getEmail() + "\n" + "Company: "
                        + formData.getCompanyName() + "\n" + "Size: "
                        + formData.getCompanySize() + "\n" + "Industry: "
                        + formData.getIndustry() + "\n" + "Plan: "
                        + formData.getPlan();
                reviewDiv.setText(reviewText);
            }
        });

        step4Layout.add(reviewDiv);
        step4Layout.bindVisible(() -> currentStepSignal.get() == Step.REVIEW);

        // Validation signals using Binder's validationStatusSignal
        Signal<Boolean> step1ValidSignal = step1Binder.validationStatusSignal()
                .map(BinderValidationStatus::isOk);
        Signal<Boolean> step2ValidSignal = step2Binder.validationStatusSignal()
                .map(BinderValidationStatus::isOk);
        Signal<Boolean> step3ValidSignal = step3Binder.validationStatusSignal()
                .map(BinderValidationStatus::isOk);

        // Navigation buttons
        HorizontalLayout navigationLayout = new HorizontalLayout();

        Button previousButton = new Button("Previous", e -> {
            Step current = currentStepSignal.peek();
            switch (current) {
            case COMPANY_INFO -> currentStepSignal.set(Step.PERSONAL_INFO);
            case PLAN_SELECTION -> currentStepSignal.set(Step.COMPANY_INFO);
            case REVIEW -> currentStepSignal.set(Step.PLAN_SELECTION);
            default -> {
            }
            }
        });
        previousButton.bindVisible(
                () -> currentStepSignal.get() != Step.PERSONAL_INFO);

        Button nextButton = new Button("Next", e -> {
            Step current = currentStepSignal.peek();
            switch (current) {
            case PERSONAL_INFO -> currentStepSignal.set(Step.COMPANY_INFO);
            case COMPANY_INFO -> currentStepSignal.set(Step.PLAN_SELECTION);
            case PLAN_SELECTION -> currentStepSignal.set(Step.REVIEW);
            default -> {
            }
            }
        });
        nextButton.bindVisible(() -> currentStepSignal.get() != Step.REVIEW);
        nextButton.bindEnabled(Signal.computed(() -> {
            Step current = currentStepSignal.get();
            return switch (current) {
            case PERSONAL_INFO -> step1ValidSignal.get();
            case COMPANY_INFO -> step2ValidSignal.get();
            case PLAN_SELECTION -> step3ValidSignal.get();
            case REVIEW -> false;
            };
        }));

        Button submitButton = new Button("Submit", e -> {
            // Handle form submission
            Notification.show(
                    "Form submitted successfully for " + formData.getFirstName()
                            + " " + formData.getLastName() + "!");
        });
        submitButton.addThemeName("primary");
        submitButton.bindVisible(() -> currentStepSignal.get() == Step.REVIEW);

        navigationLayout.add(previousButton, nextButton, submitButton);

        // Progress indicator
        Span progressIndicator = new Span(() -> {
            int stepNumber = switch (currentStepSignal.get()) {
            case PERSONAL_INFO -> 1;
            case COMPANY_INFO -> 2;
            case PLAN_SELECTION -> 3;
            case REVIEW -> 4;
            };
            return "Step " + stepNumber + " of 4";
        });
        progressIndicator.addClassName("progress-indicator");

        add(title, description, progressIndicator, step1Layout, step2Layout,
                step3Layout, step4Layout, navigationLayout);
    }
}
