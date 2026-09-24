package com.example.uc3;

import java.util.EnumSet;

import com.example.uc3.ExportPreferences.Column;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = OrderExportView.class)
class OrderExportViewTest extends SpringBrowserlessTest {

    @Autowired
    private ExportPreferences preferences;

    @Autowired
    private OrderExportController controller;

    @Test
    void viewRendersSettingsAndDownloadLink() {
        navigate(OrderExportView.class);

        assertEquals("UC3 — Export settings",
                findInView(H1.class).single().getText());
        assertEquals(OrderExportController.PATH,
                findInView(Anchor.class).single().getHref());
        assertEquals(EnumSet.allOf(Column.class),
                findInView(CheckboxGroup.class).single().getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void controllerExportsWithSettingsChosenInTheView() {
        navigate(OrderExportView.class);

        test(findInView(CheckboxGroup.class).single())
                .deselectItem(Column.DATE.label());
        test(findInView(CheckboxGroup.class).single())
                .deselectItem(Column.CUSTOMER.label());
        test(findInView(RadioButtonGroup.class).single())
                .selectItem("Semicolon (;)");

        // The controller resolves the same HTTP-session-scoped instance.
        String csv = controller.exportOrders().getBody();
        assertEquals("""
                Order;Total
                A-1001;1249.00
                A-1002;86.50
                A-1003;430.20
                """, csv);
        assertEquals(";", preferences.getDelimiter());
    }
}
