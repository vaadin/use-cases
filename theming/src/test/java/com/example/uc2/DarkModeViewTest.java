package com.example.uc2;

import com.example.Appearance;
import com.example.MediaQueries;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.page.ColorScheme;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = DarkModeView.class)
class DarkModeViewTest extends SpringBrowserlessTest {

    @Test
    void followsTheSystemByDefault() {
        DarkModeView view = navigate(DarkModeView.class);
        runPendingSignalsTasks();

        assertEquals("UC2 — Dark mode",
                findInView(H1.class).single().getText());
        assertEquals(ColorScheme.Value.SYSTEM, view.scheme().getValue());
        assertEquals(ColorScheme.Value.SYSTEM,
                UI.getCurrent().getPage().getColorScheme());
        assertEquals("System prefers not known yet · showing the system's",
                view.status());
    }

    @Test
    void statusFollowsTheSystemPreference() {
        DarkModeView view = navigate(DarkModeView.class);

        MediaQueries.answer(view, DarkModeView.DARK_QUERY, true);
        runPendingSignalsTasks();
        assertEquals("System prefers dark · showing dark", view.status());

        MediaQueries.answer(view, DarkModeView.DARK_QUERY, false);
        runPendingSignalsTasks();
        assertEquals("System prefers light · showing light", view.status());
    }

    @Test
    void choosingASchemeOverridesTheSystem() {
        DarkModeView view = navigate(DarkModeView.class);
        MediaQueries.answer(view, DarkModeView.DARK_QUERY, false);

        test(view.scheme()).selectItem("Dark");
        runPendingSignalsTasks();

        assertEquals(ColorScheme.Value.DARK,
                Appearance.current().colorScheme().peek());
        assertEquals(ColorScheme.Value.DARK,
                UI.getCurrent().getPage().getColorScheme());
        assertEquals("System prefers light · showing dark", view.status());
    }
}
