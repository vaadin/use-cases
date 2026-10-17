package com.example.uc4;

import com.example.TriggerCallbacks;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = DoubleClickCopyView.class)
class DoubleClickCopyViewTest extends SpringBrowserlessTest {

    @Test
    void statusShowsWhatTheBrowserCopied() {
        navigate(DoubleClickCopyView.class);
        assertEquals("UC4 — Double-click copy",
                findInView(H1.class).single().getText());
        Span status = findInView(Span.class).id("status");
        assertEquals("(nothing copied yet)", status.getText());

        TriggerCallbacks.resolve(findInView(Div.class).id("target"),
                "SPRING-SALE-25");

        assertEquals("Copied: SPRING-SALE-25", status.getText());
    }
}
