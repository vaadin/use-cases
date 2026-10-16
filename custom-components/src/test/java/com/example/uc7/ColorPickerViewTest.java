package com.example.uc7;

import com.example.components.ColorPicker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = ColorPickerView.class)
class ColorPickerViewTest extends SpringBrowserlessTest {

    @Test
    void typedColorReachesThePickersState() {
        ColorPickerView view = navigate(ColorPickerView.class);
        assertEquals(ColorPicker.DEFAULT, view.picker().getColor());

        test(view.hex()).setValue("#16a34a");

        assertEquals("#16a34a", view.picker().getColor());
        assertEquals("#16a34a", view.label().getStyle().get("background"));
    }

    @Test
    void invalidHexIsIgnored() {
        ColorPickerView view = navigate(ColorPickerView.class);

        test(view.hex()).setValue("green");

        assertEquals(ColorPicker.DEFAULT, view.picker().getColor());
    }
}
