package com.example.uc3;

import com.example.FullscreenTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DistractionFreeEditorView.class)
class DistractionFreeEditorViewTest extends SpringBrowserlessTest {

    @Test
    void doneButtonAppearsOnlyWhileFullscreen() {
        navigate(DistractionFreeEditorView.class);
        runPendingSignalsTasks();

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertTrue(
                findInView(Button.class).all().stream()
                        .anyMatch(b -> "Done".equals(b.getText())),
                "Done button should be visible while fullscreen");

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertFalse(
                findInView(Button.class).all().stream()
                        .anyMatch(b -> "Done".equals(b.getText())),
                "Done button should hide again when leaving fullscreen");
    }

    @Test
    void wordCountUpdatesOnTextChange() {
        navigate(DistractionFreeEditorView.class);
        runPendingSignalsTasks();

        TextArea editor = findInView(TextArea.class).single();
        test(editor).setValue("hello world this is fullscreen");
        runPendingSignalsTasks();

        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> "5 words".equals(s.getText())),
                "word count should report 5 words after typing");
    }

    @Test
    void badgeSaysWhenFullscreenIsNotSupported() {
        navigate(DistractionFreeEditorView.class);
        runPendingSignalsTasks();

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertBadgeContains("Click Expand");

        FullscreenTestSupport.setFullscreenState(FullscreenState.UNSUPPORTED);
        runPendingSignalsTasks();
        assertBadgeContains("not supported");
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getClassNames().contains("status-badge")
                        && s.getText() != null
                        && s.getText().contains(fragment)),
                "expected status badge to contain \"" + fragment + "\"");
    }
}
