package com.example.uc8;

import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC8 — Sorting and searching names.
 * <p>
 * Alphabetical order depends on the language. German sorts "Ängström" next to
 * "Ahlström", Finnish puts Å, Ä and Ö after Z, and nobody expects lowercase
 * "van Dijk" after every capitalised name. {@link String#compareTo} compares
 * UTF-16 code units, which is right for none of them; a {@link Collator} for
 * the user's locale is right for each. (The JDK's collation rules predate CLDR
 * and put Ø and Ł after Z in every language; ICU4J's {@code Collator} follows
 * CLDR where that matters.)
 * <p>
 * Searching has the opposite need: a user typing "angstrom" or "celik" expects
 * to find "Ängström" and "Çelik". The search strips accents and ignores case
 * before it compares. Letters that are not a base letter plus an accent, such
 * as Ø and Ł, still only match themselves.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Sorting and searching names")
@UseCaseDescription("Sorting by the user's alphabet and searching without accents")
@Menu(order = 8, title = "UC8 — Sorting and searching names")
public class LocaleAwareSortingView extends VerticalLayout {

    record Person(String name, String city) {
    }

    static final List<Person> PEOPLE = List.of(new Person("Zeller", "Zürich"),
            new Person("Åberg", "Stockholm"), new Person("Aalto", "Espoo"),
            new Person("Ängström", "Uppsala"), new Person("Ahlström", "Turku"),
            new Person("Öberg", "Göteborg"), new Person("Olsen", "Oslo"),
            new Person("Ødegaard", "Bergen"), new Person("Écuyer", "Lyon"),
            new Person("Ebner", "Wien"), new Person("Eriksson", "Malmö"),
            new Person("Çelik", "İzmir"), new Person("Chávez", "Sevilla"),
            new Person("van Dijk", "Utrecht"), new Person("Łukasik", "Łódź"),
            new Person("Lund", "Aarhus"));

    private final ValueSignal<Boolean> localeAware = new ValueSignal<>(true);
    private final ValueSignal<String> search = new ValueSignal<>("");
    private final Checkbox localeAwareBox = new Checkbox(
            "Sort with the language's alphabet");
    private final TextField searchField = new TextField();
    private final Grid<Person> grid = new Grid<>();
    private final GridListDataView<Person> dataView = grid.setItems(PEOPLE);
    private final Column<Person> nameColumn;
    private final Column<Person> cityColumn;

    public LocaleAwareSortingView() {
        UI ui = UI.getCurrent();

        add(new H1("UC8 — Sorting and searching names"));
        add(new Paragraph("Switch between English, German and Finnish and "
                + "watch where Å, Ä and Ö go. Untick the checkbox for plain "
                + "String.compareTo order. Search for \"angstrom\", \"celik\" "
                + "or \"ecuyer\" without typing any accents."));

        nameColumn = grid.addColumn(Person::name);
        cityColumn = grid.addColumn(Person::city);
        grid.setAllRowsVisible(true);

        localeAwareBox.bindValue(localeAware, localeAware::set);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.setClearButtonVisible(true);
        searchField.bindValue(search, search::set);

        HorizontalLayout controls = new HorizontalLayout(searchField,
                localeAwareBox);
        controls.setAlignItems(Alignment.BASELINE);
        Div sample = new Div(controls, grid);
        sample.addClassName("sample");
        add(sample);

        Signal.effect(this, () -> {
            Locale locale = ui.localeSignal().get();
            nameColumn.setHeader(getTranslation(locale, "uc8.name"));
            cityColumn.setHeader(getTranslation(locale, "uc8.city"));
            searchField.setLabel(getTranslation(locale, "uc8.search"));
            dataView.setSortComparator(
                    order(locale, localeAware.get())::compare);
            String term = fold(search.get(), locale);
            dataView.setFilter(
                    person -> fold(person.name(), locale).contains(term)
                            || fold(person.city(), locale).contains(term));
        });
    }

    static Comparator<Person> order(Locale locale, boolean localeAware) {
        if (!localeAware) {
            return Comparator.comparing(Person::name);
        }
        Collator collator = Collator.getInstance(locale);
        return Comparator.comparing(Person::name, collator);
    }

    /** Lowercase without accents: "Ängström" becomes "angstrom". */
    static String fold(String text, Locale locale) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(locale);
    }

    // Package-private test seams.
    List<String> names() {
        return dataView.getItems().map(Person::name).toList();
    }

    Grid<Person> grid() {
        return grid;
    }

    TextField searchField() {
        return searchField;
    }

    Checkbox localeAwareBox() {
        return localeAwareBox;
    }
}
