package com.example.uc9;

import java.util.List;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.testbench.loadtest.PlaywrightHelper;

/**
 * UC9 with Playwright: the element screenshot masks the volatile part itself,
 * but Playwright for Java has no comparison, so {@link ScreenshotComparison}
 * does it. The reference does not depend on the browser version, only on the
 * rendering, so the test runs in a fixed viewport and device scale.
 */
class InvoiceScreenshotPlaywrightIT {

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeEach
    void open() {
        playwright = Playwright.create();
        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage(new Browser.NewPageOptions()
                .setViewportSize(1280, 900).setDeviceScaleFactor(1));
        page.navigate(PlaywrightHelper.getBaseUrl() + "/uc9");
    }

    @AfterEach
    void close() {
        browser.close();
        playwright.close();
    }

    @Test
    void invoiceLooksLikeTheReference() throws Exception {
        Locator invoice = page.locator("#invoice");
        // Web fonts must have loaded, or the first screenshot differs.
        page.evaluate("document.fonts.ready");

        byte[] screenshot = invoice.screenshot(new Locator.ScreenshotOptions()
                .setMask(List.of(page.locator("#invoice .volatile")))
                .setAnimations(
                        com.microsoft.playwright.options.ScreenshotAnimations.DISABLED));

        ScreenshotComparison.assertMatches(screenshot, "invoice-playwright",
                0.01);
    }
}
