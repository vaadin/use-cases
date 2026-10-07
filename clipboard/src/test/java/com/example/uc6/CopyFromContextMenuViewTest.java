package com.example.uc6;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Pre;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CopyFromContextMenuView.class)
class CopyFromContextMenuViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndCopyTarget() {
        navigate(CopyFromContextMenuView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC6 — Copy via a context-menu item".equals(h.getText())));
        assertTrue(findInView(Pre.class).all().stream()
                .anyMatch(p -> "secret-token-9f8e7a6b".equals(p.getText())));
    }
}
