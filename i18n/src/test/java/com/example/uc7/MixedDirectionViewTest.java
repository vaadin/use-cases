package com.example.uc7;

import com.example.SupportedLocales;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MixedDirectionView.class)
class MixedDirectionViewTest extends SpringBrowserlessTest {

    private static final String FSI = "⁨";
    private static final String PDI = "⁩";

    @Test
    void naiveMessageInsertsValuesAsTheyAre() {
        MixedDirectionView view = navigate(MixedDirectionView.class);
        runPendingSignalsTasks();

        assertEquals("UC7 — Mixed-direction text",
                findInView(H1.class).single().getText());
        assertEquals("Acme Ltd. opened ticket #4521-B.", view.naiveText());
        assertEquals("Acme Ltd.", view.bdiText());
    }

    @Test
    void isolatedMessageWrapsEachValue() {
        MixedDirectionView view = navigate(MixedDirectionView.class);

        UI.getCurrent().setLocale(SupportedLocales.HEBREW);
        runPendingSignalsTasks();

        assertEquals("Acme Ltd. פתח את הפנייה #4521-B.", view.naiveText());
        assertEquals(FSI + "Acme Ltd." + PDI + " פתח את הפנייה " + FSI
                + "#4521-B" + PDI + ".", view.isolatedText());
        assertEquals("לקוח", view.customerField().getLabel());
    }

    @Test
    void messagesFollowWhatTheUserTypes() {
        MixedDirectionView view = navigate(MixedDirectionView.class);

        test(view.customerField()).setValue("שלמה כהן");
        runPendingSignalsTasks();

        assertEquals(FSI + "שלמה כהן" + PDI + " opened ticket " + FSI
                + "#4521-B" + PDI + ".", view.isolatedText());
        assertEquals("שלמה כהן", view.bdiText());
    }

    @Test
    void freeTextFollowsItsOwnDirection() {
        navigate(MixedDirectionView.class);
        runPendingSignalsTasks();

        assertTrue(findInView(Paragraph.class).all().stream().anyMatch(
                p -> "auto".equals(p.getElement().getAttribute("dir"))));
    }
}
