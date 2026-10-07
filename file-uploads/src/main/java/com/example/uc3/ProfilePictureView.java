package com.example.uc3;

import java.awt.image.BufferedImage;
import java.io.IOException;

import com.example.Images;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadContent;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;

/**
 * UC3 — Change my profile picture.
 * <p>
 * The page shows the current picture (or the user's initials) and lets the user
 * replace or remove it. The uploaded file is checked twice before it is kept:
 * its leading bytes must be an image, whatever its name, and it must decode to
 * at least {@value #MIN_SIDE} × {@value #MIN_SIDE} pixels. The server then
 * crops the largest centred square and scales it to {@value #SIZE} ×
 * {@value #SIZE}, so a 12-megapixel phone photo is stored as a small PNG.
 * <p>
 * {@link Upload} has no notion of a current value: it cannot be told that a
 * picture is already set, and with {@code maxFiles = 1} its single slot stays
 * taken by the last upload. The view shows the current picture in an
 * {@link Avatar} next to it and clears the upload's file list once the new
 * picture has been stored.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Profile picture")
@UseCaseDescription("Replacing a single picture and resizing it on the server")
@Menu(order = 3, title = "UC3 — Profile picture")
public class ProfilePictureView extends VerticalLayout {

    static final String USER_NAME = "Alex Rivera";
    static final int SIZE = 256;
    static final int MIN_SIDE = 128;

    private final ProfilePictureStore store;
    private final Avatar avatar = new Avatar(USER_NAME);
    private final Span status = new Span();
    private final Button remove = new Button("Remove picture",
            e -> setPicture(null));
    private final Upload upload;

    public ProfilePictureView(ProfilePictureStore store) {
        this.store = store;
        add(new H1("UC3 — Change my profile picture"));
        add(new Paragraph("Upload a photo of yourself. It is cropped to a "
                + "square and resized on the server; anything that is not a "
                + "readable image of at least " + MIN_SIDE + " × " + MIN_SIDE
                + " pixels is refused."));

        avatar.addThemeVariants(AvatarVariant.XLARGE);
        remove.addThemeVariants(ButtonVariant.TERTIARY);
        VerticalLayout current = new VerticalLayout(status, remove);
        current.setPadding(false);
        current.setSpacing(false);
        HorizontalLayout header = new HorizontalLayout(avatar, current);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        upload = new Upload(UploadHandler.inMemory(this::pictureReceived)
                .validateHeader(Images.HEADER_SIZE, Images::rejectUnlessImage)
                .validateComplete(ProfilePictureView::rejectUnlessUsable));
        upload.setMaxFiles(1);
        upload.setMaxFileSize(10 * 1024 * 1024);
        upload.setAcceptedMimeTypes("image/jpeg", "image/png", "image/gif");
        upload.setAcceptedFileExtensions(".jpg", ".jpeg", ".png", ".gif");
        upload.addFileRejectedListener(e -> Notification
                .show(e.getFileName() + ": " + e.getErrorMessage()));

        add(header, upload);
        showPicture(store.get());
    }

    private static void rejectUnlessUsable(UploadEvent event,
            UploadContent content) throws IOException {
        BufferedImage image = Images
                .read(content.getInputStream().readAllBytes());
        if (image == null) {
            event.reject("This image cannot be read. Try a JPEG or PNG.");
        } else if (image.getWidth() < MIN_SIDE
                || image.getHeight() < MIN_SIDE) {
            event.reject("The picture is too small (%d × %d pixels)."
                    .formatted(image.getWidth(), image.getHeight()));
        }
    }

    private void pictureReceived(UploadMetadata metadata, byte[] bytes) {
        BufferedImage image = Images.read(bytes);
        if (image == null) {
            return; // already refused by the validator
        }
        setPicture(Images.squareCrop(image, SIZE));
        upload.clearFileList();
        Notification.show("Profile picture updated.");
    }

    private void setPicture(byte @Nullable [] picture) {
        store.set(picture);
        showPicture(picture);
    }

    private void showPicture(byte @Nullable [] picture) {
        if (picture == null) {
            avatar.setImageHandler(null);
            status.setText("No picture yet — others see your initials.");
        } else {
            avatar.setImageHandler(
                    Images.download(picture, "profile.png", "image/png"));
            status.setText("This is how others see you.");
        }
        remove.setVisible(picture != null);
    }
}
