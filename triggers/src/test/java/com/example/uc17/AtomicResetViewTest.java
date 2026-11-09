package com.example.uc17;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ViewPackages(classes = AtomicResetView.class)
class AtomicResetViewTest extends SpringBrowserlessTest {

    @Test
    void serverLineShowsTheValuesTheServerHas() {
        navigate(AtomicResetView.class);
        assertEquals("UC17 — Atomic reset",
                findInView(H1.class).single().getText());
        assertNotNull(findInView(Button.class).id("reset"));
        Span server = findInView(Span.class).id("server");
        assertEquals(
                "Server sees: name=\"Ada Lovelace\", "
                        + "email=\"ada@example.com\", subscribe=true",
                server.getText());

        // What reaches the server after the browser has cleared the fields.
        test(findInView(TextField.class).id("name")).setValue("");
        test(findInView(TextField.class).id("email")).setValue("");
        test(findInView(Checkbox.class).id("subscribe")).click();

        assertEquals("Server sees: name=\"\", email=\"\", subscribe=false",
                server.getText());
    }
}
