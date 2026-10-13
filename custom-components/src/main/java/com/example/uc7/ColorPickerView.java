package com.example.uc7;

import com.example.common.UseCaseDescription;
import com.example.components.ColorPicker;
import com.example.views.BuiltWith;
import com.example.views.MainLayout;
import com.example.views.Sample;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC7 — Wrapping a React component.
 * <p>
 * Choosing the color of a ticket label with a React color picker from npm, used
 * as a Vaadin component through {@link ColorPicker}. Dragging in the picker
 * updates the server, and typing a hex value updates the picker: the state is
 * shared both ways.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — React component")
@UseCaseDescription("Using a React component from npm as a Vaadin component")
@Menu(order = 7, title = "UC7 — React component")
@StyleSheet("uc7.css")
public class ColorPickerView extends VerticalLayout {

    private final ColorPicker picker = new ColorPicker();
    private final TextField hex = new TextField("Hex");
    private final Div label = new Div("Urgent");

    public ColorPickerView() {
        add(new H1("UC7 — React component"));
        add(new Paragraph("Drag in the picker or type a color: both stay "
                + "in sync, and the label below uses the color."));

        label.addClassName("uc7-label");
        hex.setValue(ColorPicker.DEFAULT);
        hex.setPattern("#[0-9a-fA-F]{6}");
        hex.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue().matches("#[0-9a-fA-F]{6}")) {
                picker.setColor(e.getValue());
                apply(e.getValue());
            }
        });
        picker.addColorChangeListener(color -> {
            hex.setValue(color);
            apply(color);
        });
        apply(ColorPicker.DEFAULT);

        HorizontalLayout row = new HorizontalLayout(picker, hex, label);
        row.setAlignItems(Alignment.CENTER);
        add(new Sample("Label color", row));
        add(new BuiltWith("ColorPicker",
                "Wrapped React component · ReactAdapterComponent")
                .point("color-picker.tsx renders react-colorful's picker inside a custom element through ReactAdapterElement.")
                .point("The color is named state shared both ways: setState in Java, useState in React.")
                .point("addStateChangeListener tells the server when the user drags in the picker.")
                .java("components/ColorPicker")
                .resource("META-INF/resources/frontend/color-picker.tsx"));
    }

    private void apply(String color) {
        label.getStyle().set("background", color);
    }

    // Package-private test seams.
    ColorPicker picker() {
        return picker;
    }

    TextField hex() {
        return hex;
    }

    Div label() {
        return label;
    }
}
