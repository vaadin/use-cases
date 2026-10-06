package com.example.usecase26;

import java.util.List;

import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

@Route(value = "use-case-26", layout = MainLayout.class)
@PageTitle("Use Case 26: Lazy Component Creation")
@Menu(order = 26, title = "UC 26: Lazy Creation")
@StyleSheet("usecase26.css")
@AnonymousAllowed
public class UseCase26View extends VerticalLayout {

    private final ValueSignal<@Nullable Country> countrySignal = new ValueSignal<@Nullable Country>(
            null);
    private final ListSignal<String> creationLog = new ListSignal<>();

    public UseCase26View() {
        addClassName("usecase26-view");
        setSpacing(true);
        setPadding(true);

        var title = new H2(
                "Use Case 26: Lazy Component Creation with International Address Form");

        var description = new Paragraph(
                "This use case demonstrates lazy component creation using bindVisible().onChange(). "
                        + "Address form fields are only instantiated when a country is first selected, "
                        + "avoiding upfront creation of heavy components like ComboBoxes with many items. "
                        + "US uses create-once-keep pattern; Japan uses create-and-destroy pattern.");

        // Derived signals
        Signal<Boolean> showUS = countrySignal.map(c -> c == Country.US);
        Signal<Boolean> showJP = countrySignal.map(c -> c == Country.JAPAN);

        // Country selector
        var countrySelect = new ComboBox<Country>("Country", Country.values());
        countrySelect.setPlaceholder("Select a country...");
        countrySelect.bindValue(countrySignal, countrySignal::set);

        // Pattern A: Create once, keep (US)
        var usWrapper = new Div();
        LazyPopulate.once(usWrapper, showUS, this::populateUSForm);

        // Pattern B: Create and destroy (Japan)
        var jpWrapper = new Div();
        LazyPopulate.recreating(jpWrapper, showJP, this::populateJapanForm,
                w -> creationLog.insertLast("Destroyed Japan address form"));

        // Creation log panel
        var logPanel = new Div();
        logPanel.addClassName("log-panel");
        logPanel.add(new H3("Creation Log"));
        var logEntries = new Div();
        logEntries.bindChildren(creationLog,
                entrySignal -> new Div(new Span(entrySignal.peek())));
        logPanel.add(logEntries);

        // Main content: forms on the left, log on the right
        var formsArea = new Div(countrySelect, usWrapper, jpWrapper);
        formsArea.addClassName("forms-area");

        var contentLayout = new HorizontalLayout(formsArea, logPanel);
        contentLayout.setWidthFull();
        contentLayout.setAlignItems(HorizontalLayout.Alignment.START);

        add(title, description, contentLayout);
    }

    private void populateUSForm(Div wrapper) {
        var layout = new VerticalLayout();
        layout.setPadding(false);
        layout.add(new H3("US Address"));
        layout.add(new TextField("Street"));
        layout.add(new TextField("Apt/Suite"));
        layout.add(new TextField("City"));
        var stateCombo = new ComboBox<String>("State", List.of("Alabama",
                "Alaska", "Arizona", "Arkansas", "California", "Colorado",
                "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii",
                "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky",
                "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan",
                "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska",
                "Nevada", "New Hampshire", "New Jersey", "New Mexico",
                "New York", "North Carolina", "North Dakota", "Ohio",
                "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island",
                "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah",
                "Vermont", "Virginia", "Washington", "West Virginia",
                "Wisconsin", "Wyoming"));
        layout.add(stateCombo);
        layout.add(new TextField("ZIP"));
        wrapper.add(layout);
        creationLog.insertLast("Created US address form");
    }

    private void populateJapanForm(Div wrapper) {
        var layout = new VerticalLayout();
        layout.setPadding(false);
        layout.add(new H3("Japan Address"));
        layout.add(new TextField("Postal Code"));
        var prefectureCombo = new ComboBox<String>("Prefecture",
                List.of("Hokkaido", "Aomori", "Iwate", "Miyagi", "Akita",
                        "Yamagata", "Fukushima", "Ibaraki", "Tochigi", "Gunma",
                        "Saitama", "Chiba", "Tokyo", "Kanagawa", "Niigata",
                        "Toyama", "Ishikawa", "Fukui", "Yamanashi", "Nagano",
                        "Gifu", "Shizuoka", "Aichi", "Mie", "Shiga", "Kyoto",
                        "Osaka", "Hyogo", "Nara", "Wakayama", "Tottori",
                        "Shimane", "Okayama", "Hiroshima", "Yamaguchi",
                        "Tokushima", "Kagawa", "Ehime", "Kochi", "Fukuoka",
                        "Saga", "Nagasaki", "Kumamoto", "Oita", "Miyazaki",
                        "Kagoshima", "Okinawa"));
        layout.add(prefectureCombo);
        layout.add(new TextField("City"));
        layout.add(new TextField("Ward"));
        layout.add(new TextField("Block"));
        wrapper.add(layout);
        creationLog.insertLast("Created Japan address form");
    }
}
