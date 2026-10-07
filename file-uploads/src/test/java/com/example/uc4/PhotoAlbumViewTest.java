package com.example.uc4;

import java.util.List;

import com.example.TestFiles;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadDropZone;
import com.vaadin.flow.component.upload.UploadTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PhotoAlbumView.class)
class PhotoAlbumViewTest extends SpringBrowserlessTest {

    @Test
    void droppedPhotosWithTheSameName_areKeptAsSeparatePhotos() {
        navigate(PhotoAlbumView.class);
        assertTrue(find(Span.class).withText("0 photos").exists());

        test(dropZone()).drop("IMG_0001.png", "image/png",
                TestFiles.png(800, 600));
        test(dropZone()).drop("IMG_0001.png", "image/png",
                TestFiles.png(300, 300));
        test(find(UploadButton.class).withText("Add photos").single())
                .upload("beach.png", "image/png", TestFiles.png(50, 50));

        assertEquals(List.of("IMG_0001.png", "IMG_0001 (2).png", "beach.png"),
                names());
        assertTrue(find(Span.class).withText("3 photos").exists());
    }

    @Test
    void clickingAPhoto_opensItFullSize_andRemoveDeletesOnlyThatPhoto() {
        navigate(PhotoAlbumView.class);
        test(dropZone()).drop("one.png", "image/png", TestFiles.png(20, 20));
        test(dropZone()).drop("two.png", "image/png", TestFiles.png(20, 20));

        test(find(Button.class).withAttribute("aria-label", "Open two.png")
                .single()).click();
        Dialog dialog = find(Dialog.class).single();
        assertEquals("two.png", dialog.getHeaderTitle());
        assertEquals(1, find(Image.class).from(dialog).all().size());
        test(find(Button.class).withText("Close").from(dialog).single())
                .click();

        test(find(Button.class).withAttribute("aria-label", "Remove one.png")
                .single()).click();
        assertEquals(List.of("two.png"), names());
    }

    @Test
    void fileThatIsNotAPhoto_isNotAddedToTheAlbum() {
        navigate(PhotoAlbumView.class);

        test(dropZone()).drop("holiday.jpg", "image/jpeg",
                TestFiles.text("not a photo"));

        assertEquals(UploadTester.UploadStatus.REJECTED,
                test(dropZone()).getLastUploadStatus().get(0).status());
        assertTrue(names().isEmpty());
    }

    private UploadDropZone dropZone() {
        return findInView(UploadDropZone.class).single();
    }

    private List<String> names() {
        return find(Span.class).withClassName("album-name").all().stream()
                .map(Span::getText).toList();
    }
}
