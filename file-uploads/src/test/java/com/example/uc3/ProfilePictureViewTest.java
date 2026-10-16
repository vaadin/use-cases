package com.example.uc3;

import java.awt.image.BufferedImage;

import com.example.Images;
import com.example.TestFiles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ProfilePictureView.class)
class ProfilePictureViewTest extends SpringBrowserlessTest {

    private static final String NO_PICTURE = "No picture yet — others see your initials.";

    @Autowired
    private ApplicationContext context;

    @Test
    void uploadedPhoto_isStoredAsSquareThumbnail_andStillShownAfterComingBack() {
        navigate(ProfilePictureView.class);
        assertTrue(find(Span.class).withText(NO_PICTURE).exists());
        assertFalse(find(Button.class).withText("Remove picture").exists());

        upload().upload("me.png", "image/png", TestFiles.png(600, 400));

        byte[] stored = context.getBean(ProfilePictureStore.class).get();
        assertNotNull(stored);
        BufferedImage picture = Images.read(stored);
        assertNotNull(picture);
        assertEquals(ProfilePictureView.SIZE, picture.getWidth());
        assertEquals(ProfilePictureView.SIZE, picture.getHeight());
        assertTrue(upload().getFiles().isEmpty(),
                "The upload is ready for the next replacement");

        navigate(ProfilePictureView.class);
        assertTrue(find(Span.class).withText("This is how others see you.")
                .exists());

        test(find(Button.class).withText("Remove picture").single()).click();
        assertNull(context.getBean(ProfilePictureStore.class).get());
        assertTrue(find(Span.class).withText(NO_PICTURE).exists());
    }

    @Test
    void filesThatAreNotUsablePictures_areRefused() {
        navigate(ProfilePictureView.class);

        upload().upload("tiny.png", "image/png", TestFiles.png(40, 40));
        assertEquals(UploadTester.UploadStatus.REJECTED,
                upload().getLastUploadStatus().get(0).status());

        upload().upload("me.png", "image/png", TestFiles.text("not a photo"));
        assertEquals(UploadTester.UploadStatus.REJECTED,
                upload().getLastUploadStatus().get(0).status());

        assertNull(context.getBean(ProfilePictureStore.class).get());
        assertTrue(find(Span.class).withText(NO_PICTURE).exists());
    }

    private UploadTester<Upload> upload() {
        return test(findInView(Upload.class).single());
    }
}
