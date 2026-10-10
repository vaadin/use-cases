package com.example.uc8;

import java.util.UUID;
import java.util.regex.Pattern;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.testbench.loadtest.PlaywrightHelper;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UC8 with Playwright: role and label locators that work on any web page, plus
 * auto-waiting assertions. Vaadin components are web components, so a few
 * interactions (picking from a select overlay, reading a grid cell) need to
 * know their structure. This test is also the scenario the load test in UC11
 * records.
 */
class CheckoutPlaywrightIT {

    private final String customer = "Playwright "
            + UUID.randomUUID().toString().substring(0, 8);

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeEach
    void open() {
        playwright = Playwright.create();
        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(true));
        // Records the traffic into a HAR file when the load test asks for it.
        context = PlaywrightHelper.createBrowserContext(browser);
        page = context.newPage();
        page.navigate(PlaywrightHelper.getBaseUrl() + "/uc2");
    }

    @AfterEach
    void close() {
        context.close();
        browser.close();
        playwright.close();
    }

    @Test
    void placedOrderAppearsAtTheTopOfTheList() {
        page.getByLabel("Customer").fill(customer);
        page.getByLabel("Product").click();
        page.getByRole(AriaRole.OPTION,
                new Page.GetByRoleOptions().setName("Burr grinder")).click();
        page.getByLabel("Quantity").fill("2");
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Place order")).click();

        assertThat(page.locator("vaadin-notification-card"))
                .containsText(Pattern.compile("Order #\\d+ placed"));
        assertThat(page).hasURL(Pattern.compile(".*/uc4$"));
        // Grid cells show their content through slots, so a row's own text
        // is empty; the cell's accessible name includes the slotted content.
        Locator cell = page.getByRole(AriaRole.GRIDCELL,
                new Page.GetByRoleOptions().setName(customer));
        assertThat(cell).isVisible();
        // aria-rowindex 1 is the header row: the order is the first row.
        assertEquals("2", cell.evaluate(
                "cell => cell.closest('tr').getAttribute('aria-rowindex')"));
    }
}
