package com.example.usecase02;

import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.local.ValueSignal;

@Route(value = "use-case-02", layout = MainLayout.class)
@PageTitle("Use Case 2: Progressive Disclosure with Nested Conditions")
@Menu(order = 2, title = "UC 2: Nested Conditions")
@StyleSheet("usecase02.css")
@AnonymousAllowed
public class UseCase02View extends VerticalLayout {

    public UseCase02View() {
        addClassName("usecase02-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2(
                "Use Case 2: Progressive Disclosure with Nested Conditions");

        Paragraph description = new Paragraph(
                "This use case demonstrates progressive disclosure with multi-level conditional forms. "
                        + "Fields appear and disappear reactively based on previous selections. "
                        + "Check the visa sponsorship checkbox to reveal nested visa type selection, "
                        + "which then reveals type-specific fields in a cascading manner.");

        // Create binder and bean
        Binder<VisaApplicationData> binder = new Binder<>(
                VisaApplicationData.class);
        VisaApplicationData data = new VisaApplicationData();
        binder.setBean(data);

        // Base question: needs visa sponsorship
        Checkbox needsVisaCheckbox = new Checkbox(
                "Do you require visa sponsorship?");
        var needsVisaSignal = binder.forField(needsVisaCheckbox)
                .bind(VisaApplicationData::getNeedsVisa,
                        VisaApplicationData::setNeedsVisa)
                .valueSignal();

        // Level 1: Visa-related fields (shown when needsVisa is true)
        VerticalLayout visaSection = new VerticalLayout();

        ComboBox<VisaType> visaTypeSelect = new ComboBox<>("Visa Type",
                VisaType.values());
        visaTypeSelect.setValue(VisaType.H1B);
        var visaTypeSignal = binder.forField(visaTypeSelect)
                .bind(VisaApplicationData::getVisaType,
                        VisaApplicationData::setVisaType)
                .valueSignal();

        TextField currentVisaStatus = new TextField("Current Visa Status");
        binder.forField(currentVisaStatus).bind(
                VisaApplicationData::getCurrentVisaStatus,
                VisaApplicationData::setCurrentVisaStatus);

        visaSection.add(visaTypeSelect, currentVisaStatus);
        visaSection.bindVisible(needsVisaSignal);

        // Level 2: H1-B specific fields (shown when visa type is H1B)
        VerticalLayout h1bSection = new VerticalLayout();

        Checkbox hasH1BPreviouslyCheckbox = new Checkbox(
                "Have you held an H1-B visa before?");
        var hasH1BPreviouslySignal = binder.forField(hasH1BPreviouslyCheckbox)
                .bind(VisaApplicationData::getHasH1BPreviously,
                        VisaApplicationData::setHasH1BPreviously)
                .valueSignal();

        TextField h1bSpecialtyOccupation = new TextField(
                "Specialty Occupation");
        binder.forField(h1bSpecialtyOccupation).bind(
                VisaApplicationData::getH1bSpecialtyOccupation,
                VisaApplicationData::setH1bSpecialtyOccupation);

        h1bSection.add(hasH1BPreviouslyCheckbox, h1bSpecialtyOccupation);
        h1bSection.bindVisible(() -> needsVisaSignal.get()
                && visaTypeSignal.get() == VisaType.H1B);

        // Level 3: Previous H1-B details (shown when has H1-B previously)
        VerticalLayout previousH1BSection = new VerticalLayout();

        TextField previousEmployer = new TextField("Previous Employer");
        binder.forField(previousEmployer).bind(
                VisaApplicationData::getPreviousEmployer,
                VisaApplicationData::setPreviousEmployer);

        TextField previousPetitionNumber = new TextField(
                "Previous Petition Number");
        binder.forField(previousPetitionNumber).bind(
                VisaApplicationData::getPreviousPetitionNumber,
                VisaApplicationData::setPreviousPetitionNumber);

        TextField previousH1BStartDate = new TextField(
                "Previous H1-B Start Date");
        binder.forField(previousH1BStartDate).bind(
                VisaApplicationData::getPreviousH1BStartDate,
                VisaApplicationData::setPreviousH1BStartDate);

        previousH1BSection.add(previousEmployer, previousPetitionNumber,
                previousH1BStartDate);
        previousH1BSection.bindVisible(() -> needsVisaSignal.get()
                && visaTypeSignal.get() == VisaType.H1B
                && hasH1BPreviouslySignal.get());

        // Level 2: L1 specific fields (shown when visa type is L1)
        VerticalLayout l1Section = new VerticalLayout();

        TextField parentCompanyName = new TextField("Parent Company Name");
        binder.forField(parentCompanyName).bind(
                VisaApplicationData::getParentCompanyName,
                VisaApplicationData::setParentCompanyName);

        TextField yearsWithParentCompany = new TextField(
                "Years with Parent Company");
        binder.forField(yearsWithParentCompany).bind(
                VisaApplicationData::getYearsWithParentCompany,
                VisaApplicationData::setYearsWithParentCompany);

        ComboBox<String> l1Category = new ComboBox<>("L1 Category",
                List.of("L1-A (Manager)", "L1-B (Specialized Knowledge)"));
        binder.forField(l1Category).bind(VisaApplicationData::getL1Category,
                VisaApplicationData::setL1Category);

        l1Section.add(parentCompanyName, yearsWithParentCompany, l1Category);
        l1Section.bindVisible(() -> needsVisaSignal.get()
                && visaTypeSignal.get() == VisaType.L1);

        // Level 2: O1 specific fields (shown when visa type is O1)
        VerticalLayout o1Section = new VerticalLayout();

        TextField fieldOfExtraordinaryAbility = new TextField(
                "Field of Extraordinary Ability");
        binder.forField(fieldOfExtraordinaryAbility).bind(
                VisaApplicationData::getFieldOfExtraordinaryAbility,
                VisaApplicationData::setFieldOfExtraordinaryAbility);

        TextField majorAwards = new TextField("Major Awards/Recognition");
        binder.forField(majorAwards).bind(VisaApplicationData::getMajorAwards,
                VisaApplicationData::setMajorAwards);

        TextField publications = new TextField(
                "Publications or Media Coverage");
        binder.forField(publications).bind(VisaApplicationData::getPublications,
                VisaApplicationData::setPublications);

        o1Section.add(fieldOfExtraordinaryAbility, majorAwards, publications);
        o1Section.bindVisible(() -> needsVisaSignal.get()
                && visaTypeSignal.get() == VisaType.O1);

        // Display area for collected values
        var resultTextSignal = new ValueSignal<>("");

        Div resultDisplay = new Div();
        resultDisplay.addClassName("result-display");
        resultDisplay
                .bindVisible(resultTextSignal.map(text -> !text.isBlank()));

        Pre resultText = new Pre(resultTextSignal);
        resultText.addClassName("result-text");

        resultDisplay.add(new H3("Collected Form Data"), resultText);

        // Show Values button
        Button showValuesButton = new Button("Show Collected Values", event -> {
            resultTextSignal.set(data.toString());
            Notification.show("Form data displayed below", 2000,
                    Notification.Position.BOTTOM_START);
        });
        showValuesButton.addThemeName("primary");

        add(title, description, needsVisaCheckbox, visaSection, h1bSection,
                previousH1BSection, l1Section, o1Section, showValuesButton,
                resultDisplay);
    }
}
