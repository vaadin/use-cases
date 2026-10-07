package com.example.usecase11;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.splitlayout.SplitLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase11View.class)
@WithMockUser
class UseCase11ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithSplitLayout() {
        navigate(UseCase11View.class);
        assertEquals(1, findInView(SplitLayout.class).all().size());
    }

    @Test
    void initialContainerSizeIsSmall() {
        navigate(UseCase11View.class);
        runPendingSignalsTasks();

        // Initial size from sizeSignal is (0, 0) which is small (< 400px)
        // until the ResizeObserver reports the actual size from the browser
        assertTrue(findInView(H3.class).all().stream()
                .anyMatch(h -> h.getText() != null
                        && h.getText().contains("Small Width Layout")));
        assertFalse(findInView(H3.class).all().stream()
                .anyMatch(h -> h.getText() != null
                        && h.getText().contains("Medium Width Layout")));
        assertFalse(findInView(H3.class).all().stream()
                .anyMatch(h -> h.getText() != null
                        && h.getText().contains("Large Width Layout")));
    }
}
