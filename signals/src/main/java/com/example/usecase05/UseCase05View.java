package com.example.usecase05;

import java.util.List;
import java.util.Map;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

@Route(value = "use-case-05", layout = MainLayout.class)
@PageTitle("Use Case 5: Cascading Location Selector")
@UseCaseDescription("Keeping dependent location selectors in sync as the parent changes")
@Menu(order = 5, title = "UC 5: Cascading Selector")
@AnonymousAllowed
public class UseCase05View extends VerticalLayout {

    public UseCase05View() {
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Use Case 5: Cascading Location Selector");

        Paragraph description = new Paragraph(
                "This use case demonstrates cascading dropdowns where each selection filters the next dropdown. "
                        + "Select a country to see states, then select a state to see cities. "
                        + "All filtering is reactive through computed signals.");

        // Create signals for selections - use first country as default
        List<String> countries = loadCountries();
        ValueSignal<String> countrySignal = new ValueSignal<>(countries.get(0));
        ValueSignal<@Nullable String> stateSignal = new ValueSignal<@Nullable String>(
                null);
        ValueSignal<@Nullable String> citySignal = new ValueSignal<@Nullable String>(
                null);

        var states = countrySignal.map(country -> loadStates(country));
        var cities = Signal.computed(
                () -> loadCities(countrySignal.get(), stateSignal.get()));

        // Country selector
        ComboBox<String> countrySelect = new ComboBox<>("Country");
        countrySelect.setItems(countries);
        countrySelect.bindValue(countrySignal, countrySignal::set);

        // State selector - computed items based on country
        ComboBox<String> stateSelect = new ComboBox<>("State/Province");
        Signal.effect(stateSelect, () -> stateSelect.setItems(states.get()));
        stateSelect.bindValue(stateSignal, stateSignal::set);

        // City selector - computed items based on state
        ComboBox<String> citySelect = new ComboBox<>("City");
        Signal.effect(citySelect, () -> citySelect.setItems(cities.get()));
        citySelect.bindValue(citySignal, citySignal::set);
        citySelect.bindEnabled(() -> !cities.get().isEmpty());

        add(title, description, countrySelect, stateSelect, citySelect);
    }

    private List<String> loadCountries() {
        // Stub implementation - returns mock data
        return List.of("United States", "Canada", "United Kingdom", "Germany");
    }

    private List<String> loadStates(@Nullable String country) {
        if (country == null) {
            return List.of();
        }

        // Stub implementation - returns mock data
        Map<String, List<String>> statesByCountry = Map.of("United States",
                List.of("California", "Texas", "New York", "Florida"), "Canada",
                List.of("Ontario", "Quebec", "British Columbia", "Alberta"),
                "United Kingdom",
                List.of("England", "Scotland", "Wales", "Northern Ireland"),
                "Germany", List.of("Bavaria", "Berlin", "Hamburg", "Hesse"));
        return statesByCountry.getOrDefault(country, List.of());
    }

    private List<String> loadCities(@Nullable String country,
            @Nullable String state) {
        if (country == null || state == null) {
            return List.of();
        }

        // Stub implementation - returns mock data
        Map<String, Map<String, List<String>>> citiesByState = Map.of(
                "United States",
                Map.of("California",
                        List.of("Los Angeles", "San Francisco", "San Diego",
                                "Sacramento"),
                        "Texas",
                        List.of("Houston", "Austin", "Dallas", "San Antonio"),
                        "New York",
                        List.of("New York City", "Buffalo", "Rochester",
                                "Albany"),
                        "Florida",
                        List.of("Miami", "Orlando", "Tampa", "Jacksonville")),
                "Canada",
                Map.of("Ontario",
                        List.of("Toronto", "Ottawa", "Mississauga", "Hamilton"),
                        "Quebec",
                        List.of("Montreal", "Quebec City", "Laval", "Gatineau"),
                        "British Columbia",
                        List.of("Vancouver", "Victoria", "Kelowna", "Burnaby"),
                        "Alberta", List.of("Calgary", "Edmonton", "Red Deer",
                                "Lethbridge")));
        return citiesByState.getOrDefault(country, Map.of()).getOrDefault(state,
                List.of());
    }
}
