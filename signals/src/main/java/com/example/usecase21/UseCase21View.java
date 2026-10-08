package com.example.usecase21;

import java.util.Locale;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;

import static com.example.usecase21.TranslationService.translate;

@Route(value = "use-case-21", layout = MainLayout.class)
@PageTitle("Use Case 21: Signals-Based i18n")
@Menu(order = 21, title = "UC21 — Signals-Based i18n")
@UseCaseDescription("Switching the UI language without reloading the page")
@StyleSheet("usecase21.css")
@AnonymousAllowed
public class UseCase21View extends VerticalLayout {

    public UseCase21View() {
        addClassName("usecase21-view");
        setSpacing(true);
        setPadding(true);

        // Title and description
        H2 title = new H2();
        title.bindText(translate("uc21.title"));

        Paragraph description = new Paragraph();
        description.bindText(translate("uc21.description"));

        add(title, description);

        // Welcome section
        add(createWelcomeSection());

        // How It Works section
        add(createHowItWorksSection());

        // Sample Form section
        add(createSampleFormSection());

        // Status section
        add(createStatusSection());
    }

    private Div createWelcomeSection() {
        Div card = createCard();

        H3 heading = new H3();
        heading.bindText(translate("uc21.welcome.heading"));

        Paragraph text = new Paragraph();
        text.bindText(translate("uc21.welcome.text"));

        card.add(heading, text);
        return card;
    }

    private Div createHowItWorksSection() {
        Div card = createCard();

        H3 heading = new H3();
        heading.bindText(translate("uc21.howItWorks.heading"));

        Paragraph step1 = new Paragraph();
        step1.bindText(translate("uc21.howItWorks.step1"));

        Paragraph step2 = new Paragraph();
        step2.bindText(translate("uc21.howItWorks.step2"));

        Paragraph step3 = new Paragraph();
        step3.bindText(translate("uc21.howItWorks.step3"));

        Paragraph step4 = new Paragraph();
        step4.bindText(translate("uc21.howItWorks.step4"));

        card.add(heading, step1, step2, step3, step4);
        return card;
    }

    private Div createSampleFormSection() {
        Div card = createCard();

        H3 heading = new H3();
        heading.bindText(translate("uc21.sampleForm.heading"));

        // Name field
        TextField nameField = new TextField();
        nameField.setWidthFull();
        Signal.effect(nameField, () -> nameField
                .setLabel(translate("uc21.sampleForm.nameLabel").get()));
        nameField.bindPlaceholder(translate("uc21.sampleForm.namePlaceholder"));

        // Email field
        EmailField emailField = new EmailField();
        emailField.setWidthFull();
        Signal.effect(emailField, () -> emailField
                .setLabel(translate("uc21.sampleForm.emailLabel").get()));
        emailField
                .bindPlaceholder(translate("uc21.sampleForm.emailPlaceholder"));

        // Buttons
        Button submitButton = new Button();
        submitButton.addThemeVariants(ButtonVariant.PRIMARY);
        submitButton.bindText(translate("uc21.sampleForm.submitButton"));

        Button cancelButton = new Button();
        cancelButton.bindText(translate("uc21.sampleForm.cancelButton"));

        HorizontalLayout buttonLayout = new HorizontalLayout(submitButton,
                cancelButton);
        buttonLayout.setSpacing(true);

        VerticalLayout formLayout = new VerticalLayout(nameField, emailField,
                buttonLayout);
        formLayout.setSpacing(true);
        formLayout.setPadding(false);
        formLayout.setMaxWidth("400px");

        card.add(heading, formLayout);
        return card;
    }

    private Div createStatusSection() {
        Div card = createCard();

        H3 heading = new H3();
        heading.bindText(translate("uc21.status.heading"));

        // Language display
        HorizontalLayout languageRow = new HorizontalLayout();
        languageRow.setAlignItems(Alignment.CENTER);
        languageRow.setSpacing(true);

        Span languageLabel = new Span();
        languageLabel
                .bindText(translate("uc21.status.language").map(s -> s + ":"));
        languageLabel.addClassName("row-label");

        Span languageValue = new Span();
        Signal<String> languageNameSignal = Signal.computed(() -> {
            Locale locale = UI.getCurrent().localeSignal().get();
            return locale.getDisplayLanguage(locale);
        });
        languageValue.bindText(languageNameSignal);

        languageRow.add(languageLabel, languageValue);

        // Locale code display
        HorizontalLayout localeRow = new HorizontalLayout();
        localeRow.setAlignItems(Alignment.CENTER);
        localeRow.setSpacing(true);

        Span localeLabel = new Span();
        localeLabel.bindText(translate("uc21.status.locale").map(s -> s + ":"));
        localeLabel.addClassName("row-label");

        Span localeValue = new Span();
        localeValue.bindText(
                UI.getCurrent().localeSignal().map(Locale::toLanguageTag));
        localeValue.addClassName("locale-code");

        localeRow.add(localeLabel, localeValue);

        card.add(heading, languageRow, localeRow);
        return card;
    }

    private Div createCard() {
        Div card = new Div();
        card.addClassName("card");
        return card;
    }
}
