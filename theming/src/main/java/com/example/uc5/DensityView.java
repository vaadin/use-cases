package com.example.uc5;

import java.util.List;
import java.util.stream.IntStream;

import com.example.Appearance;
import com.example.BaseTheme;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * UC5 — Compact mode.
 * <p>
 * Screens for experts who work with a lot of data at once (dispatch, trading,
 * back office) are easier to use when more rows and fields fit on the screen.
 * The density toggle shrinks the size and spacing of every component in the
 * work area, while the rest of the application keeps its normal size.
 * <p>
 * Aura scales everything from one base size, and its {@code small} and
 * {@code large} theme names set that size for a part of the page. Lumo only has
 * a compact preset that redefines its sizes on {@code :root}: it can only
 * shrink the whole page, so with Lumo the view adds it while it is open.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Compact mode")
@UseCaseDescription("Fitting more data on screen with a compact density for one view")
@Menu(order = 5, title = "UC5 — Compact mode")
public class DensityView extends VerticalLayout {

    /** Aura's density theme names; {@code null} is the default size. */
    enum Density {
        COMPACT("Compact", "small"),
        DEFAULT("Default", null),
        COMFORTABLE("Comfortable", "large");

        private final String title;
        private final @Nullable String themeName;

        Density(String title, @Nullable String themeName) {
            this.title = title;
            this.themeName = themeName;
        }
    }

    record Shipment(String id, String destination, String carrier,
            int parcels) {
    }

    private final ValueSignal<Density> density = new ValueSignal<>(
            Density.DEFAULT);
    private final Select<Density> densitySelect = new Select<>();
    private final Div workArea = new Div();
    private final Span lumoNote = new Span();
    private @Nullable Registration lumoCompact;

    public DensityView() {
        Appearance appearance = Appearance.current();
        UI ui = UI.getCurrent();

        add(new H1("UC5 — Compact mode"));
        add(new Paragraph("Switch to compact: the grid, the form and the "
                + "buttons below shrink and more rows fit, while the menu and "
                + "this text keep their size. Comfortable does the opposite, "
                + "for touch screens."));

        densitySelect.setLabel("Density");
        densitySelect.setItems(Density.values());
        densitySelect.setItemLabelGenerator(d -> d.title);
        densitySelect.bindValue(density, value -> {
            if (value != null) {
                density.set(value);
            }
        });
        lumoNote.addClassName("status");

        Grid<Shipment> grid = new Grid<>();
        grid.addColumn(Shipment::id).setHeader("Shipment");
        grid.addColumn(Shipment::destination).setHeader("Destination");
        grid.addColumn(Shipment::carrier).setHeader("Carrier");
        grid.addColumn(Shipment::parcels).setHeader("Parcels");
        List<String> cities = List.of("Helsinki", "Tampere", "Oulu", "Turku",
                "Espoo", "Vaasa");
        grid.setItems(IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new Shipment("SH-" + (4200 + i),
                        cities.get(i % cities.size()),
                        i % 2 == 0 ? "Posti" : "DHL", i % 5 + 1))
                .toList());
        grid.setHeight("22rem");

        ComboBox<String> carrier = new ComboBox<>("Carrier", "Posti", "DHL",
                "UPS");
        carrier.setValue("Posti");
        FormLayout form = new FormLayout(
                new TextField("Shipment", "SH-4201", ""), carrier,
                new DatePicker("Pickup"), new TextField("Reference"));
        Button dispatch = new Button("Dispatch");
        dispatch.addThemeVariants(ButtonVariant.PRIMARY);

        workArea.addClassName("sample");
        workArea.add(form, new HorizontalLayout(dispatch, new Button("Hold")),
                grid);
        add(new HorizontalLayout(densitySelect, lumoNote), workArea);

        // Aura reads the density from a theme name on the container.
        workArea.getElement().bindAttribute("theme",
                density.map(d -> d.themeName));
        Signal.effect(this, () -> {
            boolean lumo = appearance.theme().get() == BaseTheme.LUMO;
            setLumoCompact(ui, lumo && density.get() == Density.COMPACT);
            lumoNote.setText(
                    lumo ? "Lumo: compact shrinks the whole page; there is no "
                            + "comfortable preset" : "");
        });
        addDetachListener(e -> setLumoCompact(ui, false));
    }

    private void setLumoCompact(UI ui, boolean on) {
        if (on && lumoCompact == null) {
            lumoCompact = ui.getPage().addStyleSheet(Lumo.COMPACT_STYLESHEET);
        } else if (!on && lumoCompact != null) {
            lumoCompact.remove();
            lumoCompact = null;
        }
    }

    // Package-private test seams.
    Select<Density> densitySelect() {
        return densitySelect;
    }

    Div workArea() {
        return workArea;
    }

    boolean lumoCompactLoaded() {
        return lumoCompact != null;
    }
}
