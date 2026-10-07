package com.example.uc5;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SettingsView.class)
class UpOneLevelTest extends SpringBrowserlessTest {

    @Test
    void leafUpLinkPointsAtImmediateParentNotRoot() {
        navigate(SessionsView.class);

        RouterLink up = upLink();
        assertEquals("↑ Up to Security", up.getText());
        assertTrue(up.getHref().endsWith("uc5/security"),
                "up link must target the immediate parent: " + up.getHref());
    }

    @Test
    void midUpLinkClimbsOneLevel() {
        navigate(SecurityView.class);

        RouterLink up = upLink();
        assertEquals("↑ Up to Settings", up.getText());
        assertTrue(up.getHref().endsWith("uc5"), up.getHref());
    }

    @Test
    void rootHasNoParentWithinTheUseCase() {
        navigate(SettingsView.class);

        List<RouterLink> links = find(UpLink.class).single().getChildren()
                .filter(RouterLink.class::isInstance)
                .map(RouterLink.class::cast).toList();
        assertTrue(links.isEmpty(),
                "getRouteParent must find no parent for the use-case root");

        boolean hasTopLevelNote = find(UpLink.class).single().getChildren()
                .filter(Span.class::isInstance).map(Span.class::cast)
                .anyMatch(s -> s.getText().contains("top level"));
        assertTrue(hasTopLevelNote, "root must show the top-level note");
    }

    private RouterLink upLink() {
        return find(UpLink.class).single().getChildren()
                .filter(RouterLink.class::isInstance)
                .map(RouterLink.class::cast).toList().get(0);
    }
}
