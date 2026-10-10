package com.example.uc7;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SingleInstanceView.class)
class SingleInstanceViewTest extends SpringBrowserlessTest {

    @Test
    void rowsGetPartNamesFromTheirData() {
        SingleInstanceView view = navigate(SingleInstanceView.class);

        assertEquals("UC7 — Styling one component",
                findInView(H1.class).single().getText());
        var parts = view.grid().getPartNameGenerator();
        assertEquals("overdue",
                parts.apply(SingleInstanceView.INVOICES.get(0)));
        assertNull(parts.apply(SingleInstanceView.INVOICES.get(1)));
        assertEquals("paid", parts.apply(SingleInstanceView.INVOICES.get(2)));
        assertEquals("overdue",
                parts.apply(SingleInstanceView.INVOICES.get(3)));
    }

    @Test
    void oneFieldIsStyledOnItsOwn() {
        navigate(SingleInstanceView.class);

        assertEquals(1, findInView(TextField.class).all().stream()
                .filter(f -> f.hasClassName("needs-attention")).count());
    }

    @Test
    void stockCardTurnsRedBelowTheThreshold() {
        SingleInstanceView view = navigate(SingleInstanceView.class);
        runPendingSignalsTasks();
        assertFalse(view.stockCard().hasClassName("low"));

        test(findInView(IntegerField.class).single()).setValue(4);
        runPendingSignalsTasks();

        assertTrue(view.stockCard().hasClassName("low"));
    }
}
