package com.example.uc7;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.ClipboardTestSupport;
import com.example.ClipboardTestSupport.ClipboardFile;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PasteFilesView.class)
class PasteFilesViewTest extends SpringBrowserlessTest {

    private static final ClipboardFile SCREENSHOT = new ClipboardFile(
            "screenshot.png", "image/png", new byte[] { 1, 2, 3, 4 });
    private static final ClipboardFile NOTES = new ClipboardFile("notes.txt",
            "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

    @Test
    void pastedFilesAreListedAndImagesPreviewed() {
        navigate(PasteFilesView.class);

        ClipboardTestSupport.pasteFiles(dropZone(), List.of(SCREENSHOT, NOTES));

        assertEquals("""
                screenshot.png — image/png (4 bytes)
                notes.txt — text/plain (5 bytes)
                """, fileLog().getText());
        List<Image> previews = findInView(Image.class).all();
        assertEquals(1, previews.size());
        assertEquals("screenshot.png",
                previews.getFirst().getAlt().orElse(null));
        assertEquals("Pasted 2 files.", statusLine().getText());
        assertTrue(findInView(ProgressBar.class).all().isEmpty(),
                "the progress bar is hidden again");
    }

    @Test
    void aNewPasteReplacesTheFilesOfTheLastOne() {
        navigate(PasteFilesView.class);

        ClipboardTestSupport.pasteFiles(dropZone(), List.of(SCREENSHOT));
        ClipboardTestSupport.pasteFiles(dropZone(), List.of(NOTES));

        assertEquals("notes.txt — text/plain (5 bytes)\n", fileLog().getText());
        assertEquals(0, findInView(Image.class).all().size());
        assertEquals("Pasted 1 file.", statusLine().getText());
    }

    private Div dropZone() {
        return findInView(Div.class).all().stream()
                .filter(div -> div.hasClassName("paste-drop-zone")).findFirst()
                .orElseThrow();
    }

    private Span fileLog() {
        return findInView(Span.class).all().stream()
                .filter(span -> span.hasClassName("uc7-file-log")).findFirst()
                .orElseThrow();
    }

    private Span statusLine() {
        return findInView(Span.class).all().stream()
                .filter(span -> span.getText().startsWith("Pasted")).findFirst()
                .orElseThrow();
    }
}
