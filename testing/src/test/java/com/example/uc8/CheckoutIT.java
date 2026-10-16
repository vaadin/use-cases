package com.example.uc8;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.grid.testbench.GridElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.flow.component.select.testbench.SelectElement;
import com.vaadin.flow.component.textfield.testbench.IntegerFieldElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import com.vaadin.testbench.loadtest.LoadTestItHelper;

/**
 * UC8 with TestBench: Vaadin-aware element classes find components by type and
 * label and wait for the server round trip on their own.
 */
class CheckoutIT extends BrowserTestBase {

    /** Each run has a customer of its own, so runs never see each other. */
    private final String customer = "TestBench "
            + UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void open() {
        // Opens the page, through the load test recorder when one is set up.
        setDriver(LoadTestItHelper.openWithProxy(getDriver(),
                LoadTestItHelper.getRootURL() + "/uc2"));
    }

    @BrowserTest
    void placedOrderAppearsAtTheTopOfTheList() {
        $(TextFieldElement.class).withLabel("Customer").single()
                .setValue(customer);
        $(SelectElement.class).withLabel("Product").single()
                .selectByText("Burr grinder");
        $(IntegerFieldElement.class).withLabel("Quantity").single()
                .setValue("2");
        $(ButtonElement.class).withText("Place order").single().click();

        NotificationElement notification = $(NotificationElement.class)
                .waitForFirst();
        Assertions.assertTrue(
                notification.getText().matches("Order #\\d+ placed"),
                notification.getText());

        GridElement grid = $(GridElement.class).waitForFirst();
        waitUntil(driver -> getDriver().getCurrentUrl().endsWith("/uc4"));
        // BrowserTestBase has assertEquals overloads of its own for elements.
        Assertions.assertEquals(customer, grid.getCell(0, 1).getText());
        Assertions.assertEquals("Burr grinder", grid.getCell(0, 2).getText());
    }
}
