package com.example.uc7;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PasteFilesView.class)
class PasteFilesViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingDropZoneAndLog() {
        navigate(PasteFilesView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC7 — Paste images and files".equals(h.getText())));
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getClassNames().contains("drop-zone") && "0"
                        .equals(d.getElement().getAttribute("tabindex"))));
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getClassNames().contains("file-log")));
    }
}
