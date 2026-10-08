package com.example.uc1;

import java.util.stream.IntStream;

import com.example.TestFiles;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.GeolocationSimulator;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadCapture;
import com.vaadin.flow.component.upload.UploadTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DamageReportView.class)
class DamageReportViewTest extends SpringBrowserlessTest {

    @Test
    void takePhotoOpensTheRearCamera_chooseExistingLeavesTheChoiceToTheUser() {
        navigate(DamageReportView.class);

        assertEquals(UploadCapture.ENVIRONMENT, takePhoto().getCapture());
        assertNull(find(UploadButton.class).withText("Choose existing").single()
                .getCapture());
    }

    @Test
    void photoAndCategory_enableSend_andTheReportIsFiledWithItsLocation() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        geolocation.setLocation(60.16952, 24.93545, 8.0);
        navigate(DamageReportView.class);

        Button send = find(Button.class).withText("Send report").single();
        assertFalse(send.isEnabled());

        test(takePhoto()).upload("pothole.png", "image/png",
                TestFiles.png(40, 30));
        test(takePhoto()).ensureUploaded();
        assertEquals(1, findInView(Image.class).all().size());
        assertFalse(send.isEnabled(), "A category is still missing");

        @SuppressWarnings("unchecked")
        Select<DamageReportView.Category> category = findInView(Select.class)
                .single();
        test(category).selectItem("Pothole");
        test(find(Button.class).withText("Add my location").single()).click();
        assertTrue(find(Span.class).withTextContaining("60.16952").exists());
        assertTrue(send.isEnabled());

        test(send).click();

        @SuppressWarnings("unchecked")
        Grid<DamageReportView.Report> reports = find(Grid.class).single();
        DamageReportView.Report report = test(reports).getRow(0);
        assertEquals(DamageReportView.Category.POTHOLE, report.category());
        assertEquals(1, report.photos());
        assertTrue(report.location().startsWith("60.16952, 24.93545"));
        assertTrue(findInView(Image.class).all().isEmpty(),
                "The form starts over after sending");
        assertFalse(send.isEnabled());
    }

    @Test
    void fileThatIsNotAPhoto_isRejectedEvenWithAnImageName() {
        navigate(DamageReportView.class);

        test(takePhoto()).upload("pothole.jpg", "image/jpeg",
                TestFiles.text("not a photo"));
        assertEquals(UploadTester.UploadStatus.REJECTED,
                test(takePhoto()).getLastUploadStatus().get(0).status());

        // A video shares its leading box with HEIC photos
        test(takePhoto()).upload("pothole.heic", "image/heic", TestFiles.mp4());
        assertEquals(UploadTester.UploadStatus.REJECTED,
                test(takePhoto()).getLastUploadStatus().get(0).status());
        assertTrue(findInView(Image.class).all().isEmpty());
    }

    @Test
    void pickingMorePhotosThanAllowedAtOnce_keepsOnlyTheMaximum() {
        navigate(DamageReportView.class);

        test(find(UploadButton.class).withText("Choose existing").single())
                .uploadAll(IntStream
                        .rangeClosed(1, DamageReportView.MAX_PHOTOS + 1)
                        .mapToObj(i -> TestFiles.file("photo" + i + ".png",
                                TestFiles.png(10, 10)))
                        .toList());

        assertEquals(DamageReportView.MAX_PHOTOS,
                findInView(Image.class).all().size());
    }

    @Test
    void removingAPreview_dropsThePhotoAndFreesItsSlot() {
        navigate(DamageReportView.class);
        for (int i = 1; i <= DamageReportView.MAX_PHOTOS; i++) {
            test(takePhoto()).upload("photo" + i + ".png", "image/png",
                    TestFiles.png(10, 10));
        }
        assertFalse(takePhoto().isEnabled(), "The report is full");

        test(find(Button.class).withAttribute("aria-label", "Remove photo2.png")
                .single()).click();

        assertEquals(DamageReportView.MAX_PHOTOS - 1,
                findInView(Image.class).all().size());
        assertTrue(takePhoto().isEnabled());
    }

    private UploadButton takePhoto() {
        return find(UploadButton.class).withText("Take photo").single();
    }
}
