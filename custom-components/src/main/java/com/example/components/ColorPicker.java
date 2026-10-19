package com.example.components;

import java.util.Objects;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * A React component from npm, {@code react-colorful}'s color picker, used as a
 * Vaadin component (UC7).
 * <p>
 * {@link ReactAdapterComponent} shares named state with the React side: the
 * server sets {@code color} and the picker shows it, the user drags and the
 * server is told. The picker's value is a {@code #rrggbb} string.
 */
@Tag("color-picker")
@NpmPackage(value = "react-colorful", version = "5.8.1")
@JsModule("./color-picker.tsx")
public class ColorPicker extends ReactAdapterComponent {

    public static final String DEFAULT = "#2563eb";

    public ColorPicker() {
        setColor(DEFAULT);
    }

    public String getColor() {
        return Objects.requireNonNullElse(getState("color", String.class),
                DEFAULT);
    }

    public void setColor(String color) {
        setState("color", color);
    }

    /** Called with the new color whenever the user picks one. */
    public Registration addColorChangeListener(
            SerializableConsumer<String> listener) {
        return addStateChangeListener("color", String.class, listener);
    }
}
