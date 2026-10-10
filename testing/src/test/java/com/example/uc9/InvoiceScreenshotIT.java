package com.example.uc9;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.loadtest.LoadTestItHelper;

/**
 * UC9 with TestBench: {@code compareScreen} compares an element with a
 * reference image in {@code src/test/screenshots} and writes the new image and
 * a diff to {@code target/screenshot-errors} when they differ. A missing
 * reference fails the test the same way: copy the new image over to accept it.
 * <p>
 * The reference file name includes the browser's major version
 * ({@code invoice_linux_chrome_142.png}), so a browser update needs new
 * references.
 */
class InvoiceScreenshotIT extends BrowserTestBase {

    @BeforeEach
    void open() {
        getDriver().get(LoadTestItHelper.getRootURL() + "/uc9");
    }

    @BrowserTest
    void invoiceLooksLikeTheReference() throws Exception {
        TestBenchElement invoice = $(TestBenchElement.class).id("invoice");
        // TestBench cannot mask a region, so the part that changes on every
        // load is hidden before the screenshot.
        executeScript("document.querySelector('#invoice .volatile')"
                + ".style.visibility = 'hidden'");

        Assertions.assertTrue(invoice.compareScreen("invoice"),
                "The invoice differs from the reference; see "
                        + "target/screenshot-errors");
    }
}
