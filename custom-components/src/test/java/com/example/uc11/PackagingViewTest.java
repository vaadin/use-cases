package com.example.uc11;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ViewPackages(classes = PackagingView.class)
class PackagingViewTest extends SpringBrowserlessTest {

    @Test
    void everyComponentBringsWhatItDeclares() {
        PackagingView view = navigate(PackagingView.class);
        List<String> inventory = view.inventory();

        assertEquals(PackagingView.COMPONENTS.size(), inventory.size());
        // Every declared stylesheet and module is where an add-on jar
        // would carry it.
        inventory.forEach(line -> assertFalse(line.contains("MISSING"), line));
        assertEquals("AddressForm: Java only", inventory.get(0));
        assertEquals("SignaturePad: ./signature-pad.ts", inventory.get(4));
        assertEquals("ColorPicker: ./color-picker.tsx, "
                + "npm react-colorful@5.8.1", inventory.get(6));
    }
}
