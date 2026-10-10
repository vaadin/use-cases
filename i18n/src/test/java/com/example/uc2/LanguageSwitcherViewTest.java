package com.example.uc2;

import java.util.List;

import com.example.SupportedLocales;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LanguageSwitcherView.class)
class LanguageSwitcherViewTest extends SpringBrowserlessTest {

    @Test
    void rendersOneButtonPerLanguage() {
        navigate(LanguageSwitcherView.class);
        runPendingSignalsTasks();

        assertEquals("UC2 — Language switcher",
                findInView(H1.class).single().getText());
        for (String name : List.of("English", "Deutsch", "Suomi", "العربية",
                "עברית")) {
            assertTrue(findInView(Button.class).all().stream()
                    .anyMatch(b -> name.equals(b.getText())), name);
        }
        assertEquals("Welcome!", findInView(H2.class).single().getText());
        assertFalse(button("Forget my choice").isEnabled());
        assertEquals(List.of("en-US", "en-US", "(none)", "en-US"), facts());
    }

    @Test
    void choosingALanguageSwitchesTheSessionAndRemembersIt() {
        navigate(LanguageSwitcherView.class);

        test(button("Suomi")).click();
        runPendingSignalsTasks();

        assertEquals(SupportedLocales.FINNISH, UI.getCurrent().getLocale());
        assertEquals(SupportedLocales.FINNISH,
                VaadinSession.getCurrent().getLocale());
        assertEquals("Tervetuloa!", findInView(H2.class).single().getText());
        assertEquals(List.of("en-US", "en-US", "fi-FI", "fi-FI"), facts());
        assertTrue(button("Forget my choice").isEnabled());
    }

    @Test
    void forgettingTheChoiceGoesBackToTheMatchedLanguage() {
        navigate(LanguageSwitcherView.class);
        test(button("Deutsch")).click();
        runPendingSignalsTasks();

        test(button("Forget my choice")).click();
        runPendingSignalsTasks();

        assertEquals(SupportedLocales.ENGLISH, UI.getCurrent().getLocale());
        assertEquals(List.of("en-US", "en-US", "(none)", "en-US"), facts());
        assertFalse(button("Forget my choice").isEnabled());
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }

    private List<String> facts() {
        return findInView(Span.class).all().stream()
                .filter(s -> s.hasClassName("fact-value")).map(Span::getText)
                .toList();
    }
}
