package com.example.uc2;

import java.math.BigDecimal;
import java.util.List;

import com.example.BrowserUpload;
import com.example.TestFiles;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = AttachDocumentsView.class)
class AttachDocumentsViewTest extends SpringBrowserlessTest {

    @Test
    void receiptsAreOnlySentOnSend_andTheClaimIsSavedOnceAllHaveArrived() {
        navigate(AttachDocumentsView.class);
        fillInFields();
        queue("taxi.pdf", "application/pdf", TestFiles.text("%PDF-1.7"));
        queue("hotel.png", "image/png", TestFiles.png(10, 10));
        queue("wrong.pdf", "application/pdf", TestFiles.text("%PDF-1.7"));
        upload().removeFile("wrong.pdf");
        browser().reportQueued(2);
        assertEquals(0, test(claims()).size(), "Nothing is sent yet");

        clickSend();
        Button sending = find(Button.class).withText("Sending receipts…")
                .single();
        assertFalse(sending.isEnabled());
        assertFalse(
                find(Button.class).withText("Add receipts…").single()
                        .isEnabled(),
                "Files added now would not be part of the claim");
        assertFalse(findInView(Upload.class).single().isDropAllowed());
        assertEquals(0, test(claims()).size());

        // The browser runs the queued uploads that Send started
        upload().startUpload("taxi.pdf");
        upload().startUpload("hotel.png");

        AttachDocumentsView.Claim claim = test(claims()).getRow(0);
        assertEquals("Conference trip", claim.purpose());
        assertEquals(new BigDecimal("123.45"), claim.amount());
        assertEquals(List.of("taxi.pdf", "hotel.png"), claim.receipts());
        assertTrue(upload().getFiles().isEmpty(),
                "The file list starts over after sending");
    }

    @Test
    void receiptThatIsNotAPdfOrPhoto_keepsTheWholeClaimFromBeingSaved() {
        navigate(AttachDocumentsView.class);
        fillInFields();
        queue("taxi.pdf", "application/pdf", TestFiles.text("%PDF-1.7"));
        queue("dinner.pdf", "application/pdf", TestFiles.text("not a pdf"));
        browser().reportQueued(2);

        clickSend();
        upload().startUpload("taxi.pdf");
        upload().startUpload("dinner.pdf");

        assertEquals(0, test(claims()).size());
        assertEquals(
                "The claim was not sent: dinner.pdf is not a PDF or a photo. "
                        + "Remove or replace the file and send again.",
                test(find(Notification.class).single()).getText());
        assertTrue(
                find(Button.class).withText("Send claim").single().isEnabled());

        // Sending again without fixing it retries only the bad file, which the
        // browser reports as failed again
        clickSend();
        assertTrue(find(Button.class).withText("Sending receipts…").exists());
        browser().reportFailed("dinner.pdf");
        assertEquals(0, test(claims()).size());
        assertTrue(
                find(Button.class).withText("Send claim").single().isEnabled());

        // The user removes the bad file; the good one has already arrived
        upload().removeFile("dinner.pdf");
        browser().reportQueued(0);
        clickSend();

        assertEquals(List.of("taxi.pdf"), test(claims()).getRow(0).receipts());
    }

    @Test
    void receiptCancelledWhileSending_isReportedInsteadOfWaitingForever() {
        navigate(AttachDocumentsView.class);
        fillInFields();
        queue("taxi.pdf", "application/pdf", TestFiles.text("%PDF-1.7"));
        queue("hotel.png", "image/png", TestFiles.png(10, 10));
        browser().reportQueued(2);

        clickSend();
        upload().startUpload("taxi.pdf");
        browser().reportAborted("hotel.png");

        assertEquals(0, test(claims()).size());
        assertEquals(
                "The claim was not sent: hotel.png was cancelled. "
                        + "Remove or replace the file and send again.",
                test(find(Notification.class).single()).getText());
        assertTrue(find(Button.class).withText("Add receipts…").single()
                .isEnabled());
    }

    @Test
    void sendingWithoutAReceipt_showsTheRequiredError() {
        navigate(AttachDocumentsView.class);
        fillInFields();
        assertFalse(find(Span.class).withText("Attach at least one receipt.")
                .exists());

        clickSend();

        assertTrue(find(Span.class).withText("Attach at least one receipt.")
                .exists());
        assertEquals(0, test(claims()).size());

        queue("taxi.pdf", "application/pdf", TestFiles.text("%PDF-1.7"));
        browser().reportQueued(1);
        assertFalse(find(Span.class).withText("Attach at least one receipt.")
                .exists(), "Picking a receipt clears the error");
    }

    @Test
    void filesRejectedInOneSelection_areReportedInOneNotification() {
        navigate(AttachDocumentsView.class);

        test(findInView(Upload.class).single()).uploadAll(List.of(
                TestFiles.file("notes.txt", TestFiles.text("notes")),
                TestFiles.file("script.exe", TestFiles.text("script"))));
        roundTrip();

        List<Notification> notifications = find(Notification.class).all();
        assertEquals(1, notifications.size());
        String text = test(notifications.get(0)).getText();
        assertTrue(text.startsWith("2 files were not attached"), text);
        assertTrue(text.contains("notes.txt") && text.contains("script.exe"),
                text);
    }

    private void queue(String fileName, String contentType, byte[] content) {
        upload().upload(fileName, contentType, content);
    }

    private UploadTester<Upload> upload() {
        return test(findInView(Upload.class).single());
    }

    private BrowserUpload browser() {
        return new BrowserUpload(findInView(Upload.class).single());
    }

    private void fillInFields() {
        test(find(TextField.class).withCaption("What was it for?").single())
                .setValue("Conference trip");
        test(find(BigDecimalField.class).withCaption("Amount (EUR)").single())
                .setValue(new BigDecimal("123.45"));
    }

    private void clickSend() {
        test(find(Button.class).withText("Send claim").single()).click();
    }

    @SuppressWarnings("unchecked")
    private Grid<AttachDocumentsView.Claim> claims() {
        return find(Grid.class).single();
    }
}
