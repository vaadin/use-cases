package com.example.uc4;

import java.util.List;

import com.example.WebShareTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.webshare.WebShareSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = ShareListItemsView.class)
class ShareListItemsViewTest extends SpringBrowserlessTest {

    @Test
    void everyRowHasAShareButtonThatFollowsTheSupportSignal() {
        navigate(ShareListItemsView.class);
        runPendingSignalsTasks();

        assertEquals(List.of(false, false, false), rowButtonsEnabled());

        WebShareTestSupport.setSupport(WebShareSupport.SUPPORTED);
        runPendingSignalsTasks();
        assertEquals(List.of(true, true, true), rowButtonsEnabled());

        WebShareTestSupport.setSupport(WebShareSupport.UNSUPPORTED);
        runPendingSignalsTasks();
        assertEquals(List.of(false, false, false), rowButtonsEnabled());
    }

    private List<Boolean> rowButtonsEnabled() {
        return findInView(Button.class).all().stream().filter(b -> {
            String label = b.getElement().getAttribute("aria-label");
            return label != null && label.startsWith("Share \"");
        }).map(Button::isEnabled).toList();
    }
}
