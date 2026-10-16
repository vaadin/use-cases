package com.example.uc10;

import java.util.List;
import java.util.stream.Collectors;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.deque.html.axecore.results.AxeResults;
import com.deque.html.axecore.results.Rule;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.vaadin.testbench.loadtest.PlaywrightHelper;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC10: axe-core checks the rendered page in a real browser (names, roles,
 * contrast, landmarks), and the keyboard test fills in and sends the form with
 * Tab and Enter only.
 */
class AccessibilityPlaywrightIT {

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeEach
    void open() {
        playwright = Playwright.create();
        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage();
    }

    @AfterEach
    void close() {
        browser.close();
        playwright.close();
    }

    @ParameterizedTest
    @ValueSource(strings = { "uc10", "uc2", "uc3", "uc4" })
    void viewsHaveNoViolations(String route) {
        page.navigate(PlaywrightHelper.getBaseUrl() + "/" + route);
        page.locator(".sample").first().waitFor();

        AxeResults results = new AxeBuilder(page).include(".sample")
                .withTags(List.of("wcag2a", "wcag2aa")).analyze();

        assertEquals(List.of(), ids(results.getViolations()),
                () -> describe(results.getViolations()));
    }

    @Test
    void checkCatchesAnUnnamedButtonAndLowContrast() {
        page.navigate(PlaywrightHelper.getBaseUrl() + "/uc10?broken");
        page.locator("#contact-form").waitFor();

        AxeResults results = new AxeBuilder(page).include("#contact-form")
                .analyze();

        List<String> violations = ids(results.getViolations());
        // A vaadin-button is a custom element with role="button", which axe
        // checks with its "aria-command-name" rule.
        assertTrue(violations.contains("aria-command-name"),
                violations::toString);
        assertTrue(violations.contains("color-contrast"), violations::toString);
    }

    @Test
    void formCanBeFilledInAndSentWithTheKeyboardOnly() {
        page.navigate(PlaywrightHelper.getBaseUrl() + "/uc10");
        page.getByLabel("Name").focus();

        page.keyboard().type("Ada");
        page.keyboard().press("Tab");
        page.keyboard().type("ada@example.com");
        page.keyboard().press("Tab");
        page.keyboard().type("Hello");
        page.keyboard().press("Tab");
        page.keyboard().press("Enter");

        // Exactly once: the button reacts to Enter, and no page-wide
        // shortcut sends a second time.
        assertThat(page.locator("vaadin-notification-card")).hasCount(1);
        assertThat(page.locator("vaadin-notification-card"))
                .containsText("Thanks Ada");
    }

    @Test
    void enterInAFieldSendsButInTheMessageStartsANewLine() {
        page.navigate(PlaywrightHelper.getBaseUrl() + "/uc10");
        // The text area's label also names its host element, so the
        // textbox role picks the one to type into.
        Locator message = page.getByRole(AriaRole.TEXTBOX,
                new Page.GetByRoleOptions().setName("Message"));
        message.fill("Line one");
        message.press("Enter");
        assertThat(page.locator("vaadin-notification-card")).hasCount(0);

        page.getByLabel("Name").fill("Ada");
        page.getByLabel("Name").press("Enter");
        assertThat(page.locator("vaadin-notification-card")).hasCount(1);
    }

    private static List<String> ids(List<Rule> rules) {
        return rules.stream().map(Rule::getId).toList();
    }

    private static String describe(List<Rule> rules) {
        return rules.stream()
                .map(rule -> rule.getId() + ": " + rule.getHelp() + " "
                        + rule.getNodes().stream()
                                .map(node -> String.valueOf(node.getTarget()))
                                .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }
}
