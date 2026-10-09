package com.example.usecase22;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

@Route(value = "use-case-22", layout = MainLayout.class)
@PageTitle("Use Case 22: Two-Way Mapped Signals")
@Menu(order = 22, title = "UC22 — Two-Way Mapped Signals")
@UseCaseDescription("Editing the fields of a record through two-way bindings")
@StyleSheet("usecase22.css")
@AnonymousAllowed
public class UseCase22View extends VerticalLayout {

    public UseCase22View() {
        addClassName("usecase22-view");
        setSpacing(true);
        setPadding(true);

        var title = new H2("Use Case 22: Two-Way Mapped Signals");

        var description = new Paragraph(
                "This use case demonstrates the signal.map(getter) + signal.updater(merger) pattern for two-way field bindings. "
                        + "Each form field reads from a mapped signal and writes back via an updater on the parent signal. "
                        + "Changes propagate bidirectionally: editing a field updates the record, and the record state is always consistent.");

        // Main person signal - the single source of truth
        var personSignal = new ValueSignal<>(new Person("John", "Doe",
                "john.doe@example.com", 30,
                new Address("123 Main St", "Springfield", "12345", "USA")));

        // Create the form sections
        var formLayout = new HorizontalLayout();
        formLayout.setWidthFull();
        formLayout.setSpacing(true);

        // Personal info section
        var personalSection = createPersonalInfoSection(personSignal);

        // Address section with nested mapping
        var addressSection = createAddressSection(personSignal);

        formLayout.add(personalSection, addressSection);
        formLayout.setFlexGrow(1, personalSection, addressSection);

        // Live state display
        var stateSection = createStateDisplay(personSignal);

        // Pattern explanation
        var patternSection = createPatternExplanation();

        add(title, description, formLayout, stateSection, patternSection);
    }

    private VerticalLayout createPersonalInfoSection(
            ValueSignal<Person> personSignal) {
        var section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.addClassName("personal-section");

        var sectionTitle = new H3("Personal Information");
        sectionTitle.addClassName("section-title");

        // First name - direct field mapping
        var firstNameField = new TextField("First Name");
        firstNameField.setWidthFull();
        firstNameField.bindValue(personSignal.map(Person::firstName),
                personSignal.updater(Person::withFirstName));

        // Last name - direct field mapping
        var lastNameField = new TextField("Last Name");
        lastNameField.setWidthFull();
        lastNameField.bindValue(personSignal.map(Person::lastName),
                personSignal.updater(Person::withLastName));

        // Email - direct field mapping
        var emailField = new TextField("Email");
        emailField.setWidthFull();
        emailField.bindValue(personSignal.map(Person::email),
                personSignal.updater(Person::withEmail));

        // Age - integer field mapping
        var ageField = new IntegerField("Age");
        ageField.setWidthFull();
        ageField.setMin(0);
        ageField.setMax(150);
        ageField.setStepButtonsVisible(true);
        ageField.bindValue(personSignal.map(Person::age),
                personSignal.updater(Person::withAge));

        // Computed full name display
        var fullNameLabel = new Span();
        fullNameLabel.bindText(Signal.computed(() -> {
            Person p = personSignal.get();
            return "Full name: " + p.firstName() + " " + p.lastName();
        }));
        fullNameLabel.addClassName("full-name-label");

        section.add(sectionTitle, firstNameField, lastNameField, emailField,
                ageField, fullNameLabel);
        return section;
    }

    private VerticalLayout createAddressSection(
            ValueSignal<Person> personSignal) {
        var section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.addClassName("address-section");

        var sectionTitle = new H3("Address (Nested Mapping)");
        sectionTitle.addClassName("section-title");

        // Read-only mapped signal for Address
        Signal<Address> addressSignal = personSignal.map(Person::address);

        // Map Address -> individual fields (read via map, write via updater)
        var streetField = new TextField("Street");
        streetField.setWidthFull();
        streetField.bindValue(addressSignal.map(Address::street),
                personSignal.updater((person, street) -> person
                        .withAddress(person.address().withStreet(street))));

        var cityField = new TextField("City");
        cityField.setWidthFull();
        cityField.bindValue(addressSignal.map(Address::city),
                personSignal.updater((person, city) -> person
                        .withAddress(person.address().withCity(city))));

        var zipCodeField = new TextField("ZIP Code");
        zipCodeField.setWidthFull();
        zipCodeField.bindValue(addressSignal.map(Address::zipCode),
                personSignal.updater((person, zip) -> person
                        .withAddress(person.address().withZipCode(zip))));

        var countryField = new TextField("Country");
        countryField.setWidthFull();
        countryField.bindValue(addressSignal.map(Address::country),
                personSignal.updater((person, country) -> person
                        .withAddress(person.address().withCountry(country))));

        // Computed formatted address
        var formattedAddressLabel = new Span();
        formattedAddressLabel.bindText(Signal.computed(() -> {
            Address addr = addressSignal.get();
            return "Formatted: " + addr.street() + ", " + addr.city() + " "
                    + addr.zipCode() + ", " + addr.country();
        }));
        formattedAddressLabel.addClassName("formatted-address-label");

        section.add(sectionTitle, streetField, cityField, zipCodeField,
                countryField, formattedAddressLabel);
        return section;
    }

    private Div createStateDisplay(Signal<Person> personSignal) {
        var stateBox = new Div();
        stateBox.addClassName("state-box");

        var stateTitle = new H3("Live State (Single Source of Truth)");
        stateTitle.addClassName("section-title");

        var stateDisplay = new Pre();
        stateDisplay.addClassName("state-display");
        stateDisplay.bindText(
                Signal.computed(() -> formatPersonAsJson(personSignal.get())));

        stateBox.add(stateTitle, stateDisplay);
        return stateBox;
    }

    private Div createPatternExplanation() {
        var infoBox = new Div();
        infoBox.addClassName("info-box");

        var patternTitle = new H3("Pattern: Two-Way Mapped Signals");
        patternTitle.addClassName("section-title");

        var codeExample = new Pre();
        codeExample.addClassName("code-example");
        codeExample.setText(
                """
                        // Direct field binding with map + updater
                        textField.bindValue(
                            personSignal.map(Person::firstName),      // read: Person -> String
                            personSignal.updater(Person::withFirstName) // write: (Person, String) -> Person
                        );

                        // Nested mapping (Person -> Address -> City)
                        Signal<Address> addressSignal = personSignal.map(Person::address);
                        cityField.bindValue(
                            addressSignal.map(Address::city),
                            personSignal.updater((person, city) ->
                                person.withAddress(person.address().withCity(city)))
                        );""");

        var explanation = new Paragraph(
                "The two-way binding pattern uses signal.map(getter) for reading and "
                        + "signal.updater(merger) for writing. When the field changes, the updater "
                        + "function creates a new immutable record, which updates the parent signal. "
                        + "This eliminates manual synchronization code and keeps your data model immutable.");

        var whenToUse = new H3("When to Use");
        whenToUse.addClassName("when-to-use");

        var useCases = new Paragraph(
                "Use two-way mapped signals for simple field-to-record binding without validation. "
                        + "For complex validation requirements, continue using Vaadin Binder (see UC09).");

        infoBox.add(patternTitle, codeExample, explanation, whenToUse,
                useCases);
        return infoBox;
    }

    private String formatPersonAsJson(Person person) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "email": "%s",
                  "age": %d,
                  "address": {
                    "street": "%s",
                    "city": "%s",
                    "zipCode": "%s",
                    "country": "%s"
                  }
                }""".formatted(person.firstName(), person.lastName(),
                person.email(), person.age(), person.address().street(),
                person.address().city(), person.address().zipCode(),
                person.address().country());
    }
}
