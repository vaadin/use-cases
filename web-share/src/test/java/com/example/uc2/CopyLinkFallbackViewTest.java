package com.example.uc2;

import java.util.List;

import com.example.WebShareTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.webshare.WebShareSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CopyLinkFallbackView.class)
class CopyLinkFallbackViewTest extends SpringBrowserlessTest {

    @Test
    void oneButtonIsShownForEachSupportState() {
        navigate(CopyLinkFallbackView.class);
        runPendingSignalsTasks();

        assertEquals(List.of("Detecting…"), visibleButtons());

        setSupport(WebShareSupport.SUPPORTED);
        assertEquals(List.of("Share"), visibleButtons());
        assertTrue(hint().startsWith("This browser has navigator.share"));

        setSupport(WebShareSupport.UNSUPPORTED);
        assertEquals(List.of("Copy link"), visibleButtons());
        assertTrue(hint().startsWith("This browser has no navigator.share"));
    }

    private List<String> visibleButtons() {
        return findInView(Button.class).all().stream()
                .filter(Component::isVisible).map(Button::getText).toList();
    }

    private String hint() {
        return findInView(Paragraph.class).all().stream()
                .filter(p -> p.hasClassName("uc2-hint")).findFirst()
                .orElseThrow().getText();
    }

    private void setSupport(WebShareSupport state) {
        WebShareTestSupport.setSupport(state);
        runPendingSignalsTasks();
    }
}
