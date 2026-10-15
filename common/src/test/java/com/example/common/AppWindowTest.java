package com.example.common;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppWindowTest {

    @Test
    void theAddressPillShowsTheAddressAndHidesWithoutOne() {
        AppWindow window = new AppWindow("Order Desk", "acme.example/orders");
        Span pill = byClass(window, "app-window-url", Span.class);
        assertEquals("acme.example/orders", pill.getText());
        assertTrue(pill.isVisible());

        window.setAddress(null);
        assertFalse(pill.isVisible());
    }

    @Test
    void setContentReplacesTheSceneAndTitleBarComponentsStayInTheChrome() {
        Span first = new Span("first");
        AppWindow window = new AppWindow("Order Desk", null, first);
        Button close = new Button("Close");
        window.addToTitleBar(close);

        Span second = new Span("second");
        window.setContent(second);

        Component body = byClass(window, "app-window-body", Component.class);
        assertEquals(List.of(second), body.getChildren().toList());
        assertEquals(byClass(window, "app-window-chrome", Component.class),
                close.getParent().orElseThrow());
    }

    private static <T extends Component> T byClass(Component root,
            String className, Class<T> type) {
        return type.cast(root.getChildren()
                .flatMap(child -> Stream.concat(Stream.of(child),
                        child.getChildren()))
                .filter(child -> child.getElement().getClassList()
                        .contains(className))
                .findFirst().orElseThrow());
    }
}
