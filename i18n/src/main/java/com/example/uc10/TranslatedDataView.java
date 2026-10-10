package com.example.uc10;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.example.MissingAPI;
import com.example.SupportedLocales;
import com.example.common.UseCaseDescription;
import com.example.uc10.CategoryCatalog.Category;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * UC10 — Translated data.
 * <p>
 * Some texts are not part of the application but of its data: product
 * categories, ticket statuses, the names of plans. An administrator maintains
 * them at runtime, in every language the application supports, and each user
 * sees them in their own language.
 * <p>
 * Each category stores its name as a {@link LocalizedText}, one value per
 * language, as an ordinary property of the category. The editor has one field
 * per language, each written in that language's direction. Users see the name
 * in their language, or the English one marked as untranslated, and the
 * coverage line tells the administrator what is still missing. The categories
 * live in an application-wide signal, so a saved edit shows up in every open
 * session at once.
 */
@Route(value = "uc10", layout = MainLayout.class)
@PageTitle("UC10 — Translated data")
@UseCaseDescription("Showing administrator-entered data in each user's language")
@Menu(order = 10, title = "UC10 — Translated data")
@StyleSheet("uc10.css")
public class TranslatedDataView extends VerticalLayout {

    private final CategoryCatalog catalog;
    private final ListBox<SharedValueSignal<Category>> list = new ListBox<>();
    private final Map<Locale, TextField> fields = new LinkedHashMap<>();
    private final Button save = new Button("Save");
    private final Button create = new Button("New category");
    private final UnorderedList preview = new UnorderedList();
    private final ComboBox<Category> picker = new ComboBox<>();
    private final Span coverage = new Span();
    private boolean refreshing;

    public TranslatedDataView(CategoryCatalog catalog) {
        this.catalog = catalog;
        UI ui = UI.getCurrent();
        addClassName("uc10-view");

        add(new H1("UC10 — Translated data"));
        add(new Paragraph("The left half is the administrator's editor: pick "
                + "a category and fill in its name per language. The right "
                + "half is what users see in the language picked in the top "
                + "bar. Open this page in a second browser: saved edits show "
                + "up there at once."));

        // The admin side: the list shows each name in the admin's language.
        list.setItemLabelGenerator(
                category -> category.peek().name().in(ui.getLocale()).text());
        list.addValueChangeListener(e -> {
            if (!refreshing) {
                edit(e.getValue());
            }
        });
        for (Locale locale : SupportedLocales.ALL) {
            TextField field = new TextField(
                    SupportedLocales.nativeName(locale));
            // An Arabic name is typed right to left even when the
            // administrator's UI is in English.
            field.getElement().setAttribute("dir",
                    MissingAPI.directionOf(locale).getClientName());
            field.setWidthFull();
            fields.put(locale, field);
        }
        save.addThemeVariants(ButtonVariant.PRIMARY);
        save.addClickListener(e -> save());
        create.addClickListener(e -> list.clear());
        VerticalLayout editor = new VerticalLayout();
        editor.setPadding(false);
        editor.add(new H2("Administrator"), list);
        fields.values().forEach(editor::add);
        editor.add(new HorizontalLayout(save, create), coverage);
        editor.addClassName("editor");

        // The user side.
        H2 usersTitle = new H2();
        usersTitle.bindText(MissingAPI.translate("uc10.categories"));
        picker.setItemLabelGenerator(
                category -> category.name().in(ui.getLocale()).text());
        Div users = new Div(usersTitle, preview, picker);
        users.addClassNames("sample", "users");

        HorizontalLayout halves = new HorizontalLayout(editor, users);
        halves.setWrap(true);
        halves.setWidthFull();
        add(halves);

        Signal.effect(this, () -> {
            Locale locale = ui.localeSignal().get();
            List<SharedValueSignal<Category>> entries = catalog.categories()
                    .get();
            List<Category> categories = entries.stream()
                    .map(SharedValueSignal::get).toList();
            show(locale, categories);
            refreshEditor(entries);
        });
        edit(null);
    }

    private void show(Locale locale, List<Category> categories) {
        preview.removeAll();
        for (Category category : categories) {
            LocalizedText.Resolved name = category.name().in(locale);
            ListItem item = new ListItem(new Span(name.text()));
            if (name.fallback()) {
                Span badge = new Span(
                        getTranslation(locale, "uc10.untranslated"));
                badge.addClassName("untranslated");
                item.add(badge);
            }
            preview.add(item);
        }
        picker.setLabel(getTranslation(locale, "uc10.pick"));
        // Setting the items again is what makes the ComboBox render the
        // labels in the new language.
        @Nullable
        Category selected = picker.getValue();
        picker.setItems(categories);
        if (selected != null) {
            categories.stream().filter(c -> c.id().equals(selected.id()))
                    .findFirst().ifPresent(picker::setValue);
        }

        coverage.setText(SupportedLocales.ALL.stream()
                .map(l -> SupportedLocales.nativeName(l) + " "
                        + categories.stream()
                                .filter(c -> c.name().get(l).isPresent())
                                .count()
                        + "/" + categories.size())
                .reduce((a, b) -> a + " · " + b).orElse(""));
    }

    private void refreshEditor(List<SharedValueSignal<Category>> entries) {
        @Nullable
        SharedValueSignal<Category> selected = list.getValue();
        // Another session's edit must not wipe what is being typed here.
        refreshing = true;
        list.setItems(entries);
        if (selected != null && entries.contains(selected)) {
            list.setValue(selected);
        }
        refreshing = false;
    }

    private void edit(@Nullable SharedValueSignal<Category> category) {
        fields.forEach((locale, field) -> field.setValue(category == null ? ""
                : category.peek().name().get(locale).orElse("")));
        save.setText(category == null ? "Add" : "Save");
    }

    private void save() {
        LocalizedText name = new LocalizedText(Map.of());
        for (Map.Entry<Locale, TextField> entry : fields.entrySet()) {
            name = name.with(entry.getKey(), entry.getValue().getValue());
        }
        TextField english = field(LocalizedText.DEFAULT);
        if (name.get(LocalizedText.DEFAULT).isEmpty()) {
            english.setErrorMessage("The English name is required");
            english.setInvalid(true);
            return;
        }
        english.setInvalid(false);
        SharedValueSignal<Category> selected = list.getValue();
        if (selected == null) {
            catalog.add(name);
            edit(null);
        } else {
            catalog.rename(selected, name);
        }
    }

    // Also a package-private test seam.
    TextField field(Locale locale) {
        return Objects.requireNonNull(fields.get(locale));
    }

    // Package-private test seams.
    ListBox<SharedValueSignal<Category>> list() {
        return list;
    }

    Button saveButton() {
        return save;
    }

    List<String> previewTexts() {
        return preview.getChildren()
                .map(item -> item.getElement().getTextRecursively()).toList();
    }

    String coverage() {
        return coverage.getText();
    }
}
