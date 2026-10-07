package com.example.uc2;

import java.math.BigDecimal;
import java.util.List;

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
    void claimIsSentWithTheReceiptsThatWereNotRemoved() {
        navigate(AttachDocumentsView.class);
        fillInFields();
        UploadTester<Upload> upload = test(findInView(Upload.class).single());

        upload.upload("taxi.pdf", "application/pdf", TestFiles.text("%PDF"));
        upload.upload("hotel.png", "image/png", TestFiles.png(10, 10));
        upload.upload("wrong.pdf", "application/pdf", TestFiles.text("%PDF"));
        upload.removeFile("wrong.pdf");
        clickSend();

        AttachDocumentsView.Claim claim = test(claims()).getRow(0);
        assertEquals("Conference trip", claim.purpose());
        assertEquals(new BigDecimal("123.45"), claim.amount());
        assertEquals(List.of("taxi.pdf", "hotel.png"), claim.receipts());
        assertTrue(test(findInView(Upload.class).single()).getFiles().isEmpty(),
                "The file list starts over after sending");
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
