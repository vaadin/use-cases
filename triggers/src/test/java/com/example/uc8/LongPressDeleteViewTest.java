package com.example.uc8;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LongPressDeleteView.class)
class LongPressDeleteViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithDeleteButtonAndStatus() {
        navigate(LongPressDeleteView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC8 — Long-press to delete".equals(h.getText())));
        assertNotNull(findInView(Button.class).id("delete"));
        assertEquals("0 rows deleted",
                findInView(Span.class).id("status").getText());
    }
}
