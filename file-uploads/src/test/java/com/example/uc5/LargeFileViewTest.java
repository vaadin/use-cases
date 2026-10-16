package com.example.uc5;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LargeFileView.class)
class LargeFileViewTest extends SpringBrowserlessTest {

    @Test
    void completeFile_isAnsweredWithItsSizeAndChecksum() throws Exception {
        navigate(LargeFileView.class);
        byte[] content = new byte[3 * 1024 * 1024];
        new Random(42).nextBytes(content);

        upload().upload("export.zip", "application/zip", content);

        String checksum = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
        assertTrue(find(Span.class)
                .withText("Received export.zip (3.0 MB). SHA-256: " + checksum)
                .exists());
        assertEquals("export.zip received completely", find(Span.class)
                .withAttribute("aria-live", "polite").single().getText());
        assertFalse(find(ProgressBar.class).exists());
        assertFalse(find(Button.class).withText("Cancel upload").exists());
    }

    @Test
    void abortedUpload_tellsTheUserToStartOver() {
        navigate(LargeFileView.class);

        upload().uploadAborted("recording.mp4", "video/mp4");

        assertTrue(find(Span.class).withTextContaining(
                "The upload of recording.mp4 stopped before it was complete")
                .exists());
        assertEquals("Upload stopped", find(Span.class)
                .withAttribute("aria-live", "polite").single().getText());
    }

    private UploadTester<Upload> upload() {
        return test(findInView(Upload.class).single());
    }
}
