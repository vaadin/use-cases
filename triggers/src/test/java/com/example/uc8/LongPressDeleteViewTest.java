package com.example.uc8;

import com.example.TriggerCallbacks;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = LongPressDeleteView.class)
class LongPressDeleteViewTest extends SpringBrowserlessTest {

    @Test
    void aShortClickDeletesNothing() {
        navigate(LongPressDeleteView.class);
        assertEquals("UC8 — Long-press to delete",
                findInView(H1.class).single().getText());

        test(findInView(Button.class).id("delete")).click();
        runPendingSignalsTasks();

        assertEquals("0 rows deleted",
                findInView(Span.class).id("status").getText());
    }

    @Test
    void eachCompletedHoldDeletesOneRow() {
        navigate(LongPressDeleteView.class);
        Button delete = findInView(Button.class).id("delete");
        Span status = findInView(Span.class).id("status");

        TriggerCallbacks.fire(delete);
        runPendingSignalsTasks();
        assertEquals("1 row deleted", status.getText());

        TriggerCallbacks.fire(delete);
        runPendingSignalsTasks();
        assertEquals("2 rows deleted", status.getText());
    }
}
