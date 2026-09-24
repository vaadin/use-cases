package com.example.uc3;

import com.example.uc3.ExportPreferences.Column;
import com.example.views.MainLayout;

import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC3 — Export settings that a plain HTTP endpoint also reads (Spring HTTP
 * session scope).
 * <p>
 * A back-office user picks which columns go into their order export and which
 * delimiter their spreadsheet expects, then clicks a download link. The file is
 * produced by an ordinary Spring MVC controller
 * ({@link OrderExportController}), not by Vaadin, so the settings have to live
 * somewhere both can see: the HTTP session. {@link ExportPreferences} is
 * therefore Spring's {@code @SessionScope}, not {@code @VaadinSessionScope}.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Export settings")
@Menu(order = 3, title = "UC3 — Export settings")
public class OrderExportView extends VerticalLayout {

    public OrderExportView(ExportPreferences preferences) {
        add(new H1("UC3 — Export settings"));
        add(new Paragraph("These settings are stored in a Spring "
                + "@SessionScope bean. The download link is served by a "
                + "plain Spring MVC controller outside Vaadin, which reads "
                + "the same bean from the HTTP session."));

        CheckboxGroup<Column> columns = new CheckboxGroup<>("Columns");
        columns.setItems(Column.values());
        columns.setItemLabelGenerator(Column::label);
        columns.setValue(preferences.getColumns());
        columns.addValueChangeListener(
                e -> preferences.setColumns(e.getValue()));

        RadioButtonGroup<String> delimiter = new RadioButtonGroup<>(
                "Delimiter");
        delimiter.setItems(",", ";");
        delimiter.setItemLabelGenerator(
                d -> ",".equals(d) ? "Comma (,)" : "Semicolon (;)");
        delimiter.setValue(preferences.getDelimiter());
        delimiter.addValueChangeListener(
                e -> preferences.setDelimiter(e.getValue()));

        Anchor download = new Anchor(OrderExportController.PATH,
                "Download orders.csv");
        download.setRouterIgnore(true);

        add(columns, delimiter, download);
    }
}
