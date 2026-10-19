package com.example.uc9;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.BoundingBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * What only exists in the browser: drawing on the pad with a pointer, and the
 * finished stroke arriving on the server as the field's value.
 */
class SignaturePadPlaywrightIT {

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeEach
    void open() {
        playwright = Playwright.create();
        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage();
        page.navigate(System.getProperty("app.url") + "/uc9");
    }

    @AfterEach
    void close() {
        browser.close();
        playwright.close();
    }

    @Test
    void drawingSignsThePad() {
        Locator pad = page.locator("signature-pad");
        assertThat(page.locator("#pad-status")).hasText("blank");
        BoundingBox box = pad.boundingBox();

        page.mouse().move(box.x + 30, box.y + 60);
        page.mouse().down();
        page.mouse().move(box.x + 120, box.y + 40,
                new Mouse.MoveOptions().setSteps(5));
        page.mouse().move(box.x + 220, box.y + 70,
                new Mouse.MoveOptions().setSteps(5));
        page.mouse().up();

        assertThat(page.locator("#pad-status")).hasText("signed");
    }
}
