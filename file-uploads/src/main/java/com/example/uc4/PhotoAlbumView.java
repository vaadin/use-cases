package com.example.uc4;

import com.example.Images;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadDropZone;
import com.vaadin.flow.component.upload.UploadFileList;
import com.vaadin.flow.component.upload.UploadFileListVariant;
import com.vaadin.flow.component.upload.UploadManager;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;

/**
 * UC4 — Keep a photo album.
 * <p>
 * Photos are added in bulk, by dropping them anywhere on the album or with the
 * "Add photos" button, which share one {@link UploadManager}. While a batch is
 * uploading, an {@link UploadFileList} with
 * {@link UploadFileListVariant#THUMBNAILS thumbnails} shows the photos and
 * their progress; once the batch has finished the list is cleared and the
 * photos appear in the album as server-made thumbnails. Clicking one opens the
 * full-size photo; each can be removed on its own.
 * <p>
 * The album, not the upload, owns the photos: it gives each one an id and
 * renames duplicate file names, because the upload identifies files only by
 * name. See {@code API-GAPS.md}.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Photo album")
@UseCaseDescription("Adding many photos at once and browsing them as thumbnails")
@Menu(order = 4, title = "UC4 — Photo album")
@StyleSheet("photos.css")
public class PhotoAlbumView extends VerticalLayout {

    private final PhotoAlbum album;
    private final UploadManager uploadManager;
    private final Div tiles = new Div();
    private final Span count = new Span();

    public PhotoAlbumView(PhotoAlbum album) {
        this.album = album;
        add(new H1("UC4 — Keep a photo album"));
        add(new Paragraph("Drop photos anywhere on the album, or add them "
                + "with the button. Click a photo to see it full size."));

        uploadManager = new UploadManager(this,
                UploadHandler.inMemory(this::photoReceived).validateHeader(
                        Images.HEADER_SIZE, Images::rejectUnlessImage));
        uploadManager.setAcceptedMimeTypes("image/*");
        uploadManager.setMaxFileSize(20 * 1024 * 1024);
        uploadManager.addFileRejectedListener(e -> Notification
                .show(e.getFileName() + " is not a photo of up to 20 MB."));
        uploadManager
                .addAllFinishedListener(e -> uploadManager.clearFileList());

        UploadButton addPhotos = new UploadButton("Add photos", uploadManager);
        addPhotos.addThemeVariants(ButtonVariant.PRIMARY);
        HorizontalLayout toolbar = new HorizontalLayout(addPhotos, count);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        UploadFileList inProgress = new UploadFileList(uploadManager);
        inProgress.addThemeVariants(UploadFileListVariant.THUMBNAILS);

        tiles.addClassName("album-grid");
        UploadDropZone dropZone = new UploadDropZone(tiles, uploadManager);
        dropZone.addClassName("album-drop-zone");
        dropZone.setWidthFull();

        add(toolbar, inProgress, dropZone);
        render();
    }

    private void photoReceived(UploadMetadata metadata, byte[] bytes) {
        album.add(metadata.fileName(), metadata.contentType(), bytes);
        render();
    }

    private void render() {
        tiles.removeAll();
        var photos = album.photos();
        count.setText(
                photos.size() == 1 ? "1 photo" : photos.size() + " photos");
        if (photos.isEmpty()) {
            Paragraph empty = new Paragraph("No photos yet. Drop some here.");
            empty.addClassName("album-empty");
            tiles.add(empty);
        }
        photos.forEach(photo -> tiles.add(tile(photo)));
    }

    private Div tile(PhotoAlbum.Photo photo) {
        Component preview;
        byte[] thumbnail = photo.thumbnail();
        if (thumbnail == null) {
            preview = new Span("No preview");
        } else {
            preview = new Image(
                    Images.download(thumbnail,
                            "thumbnail-" + photo.name() + ".png", "image/png"),
                    photo.name());
        }
        Button open = new Button(preview, e -> openFullSize(photo));
        open.addThemeVariants(ButtonVariant.TERTIARY);
        open.addClassName("album-thumbnail");
        open.setAriaLabel("Open " + photo.name());

        Span name = new Span(photo.name());
        name.addClassName("album-name");
        Button remove = new Button("Remove", e -> {
            album.remove(photo.id());
            render();
        });
        remove.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        remove.setAriaLabel("Remove " + photo.name());

        Div tile = new Div(open, name, remove);
        tile.addClassName("album-tile");
        return tile;
    }

    private void openFullSize(PhotoAlbum.Photo photo) {
        Image full = new Image(Images.download(photo.original(), photo.name(),
                photo.contentType()), photo.name());
        full.addClassName("album-full-size");
        Dialog dialog = new Dialog(full);
        dialog.setHeaderTitle(photo.name());
        dialog.getFooter().add(new Button("Close", e -> dialog.close()));
        dialog.open();
    }
}
