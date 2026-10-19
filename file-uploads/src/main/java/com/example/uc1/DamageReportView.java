package com.example.uc1;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.example.Images;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.geolocation.GeolocationOptions;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadCapture;
import com.vaadin.flow.component.upload.UploadManager;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC1 — Report damage in the city.
 * <p>
 * Somebody standing in front of a pothole or a broken streetlight reports it
 * from their phone: pick what is broken, photograph it, pin the location and
 * send. {@link UploadButton#setCapture(UploadCapture) setCapture(ENVIRONMENT)}
 * makes "Take photo" open the rear camera straight away instead of the file
 * browser; "Choose existing" shares the same {@link UploadManager} but leaves
 * the choice to the user. On a desktop both open the file chooser, because
 * browsers ignore {@code capture} where there is no camera.
 * <p>
 * The photos are checked by their leading bytes before they are kept, and
 * previewed with a remove button each, so the reporter can retake a blurry one
 * before sending. The location is optional and comes from the Geolocation API,
 * as in the geolocation module's form-field use case.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Report damage")
@UseCaseDescription("Photographing a problem on the spot and sending it with its location")
@Menu(order = 1, title = "UC1 — Report damage")
@StyleSheet("photos.css")
public class DamageReportView extends VerticalLayout {

    static final int MAX_PHOTOS = 3;
    static final int MAX_PHOTO_BYTES = 20 * 1024 * 1024;

    enum Category {
        POTHOLE("Pothole"),
        STREETLIGHT("Broken streetlight"),
        GRAFFITI("Graffiti"),
        FALLEN_TREE("Fallen tree"),
        OTHER("Something else");

        private final String label;

        Category(String label) {
            this.label = label;
        }
    }

    record Photo(String fileName, byte[] bytes) {
    }

    record Report(Category category, String description, int photos,
            @Nullable String location) {
    }

    private final ValueSignal<@Nullable Category> categorySignal = new ValueSignal<@Nullable Category>(
            null);
    private final ValueSignal<String> descriptionSignal = new ValueSignal<>("");
    private final ValueSignal<List<Photo>> photosSignal = new ValueSignal<>(
            List.of());
    private final ValueSignal<@Nullable GeolocationPosition> locationSignal = new ValueSignal<@Nullable GeolocationPosition>(
            null);

    private final UploadManager uploadManager;
    private final List<Report> reports = new ArrayList<>();
    private final Grid<Report> grid = new Grid<>(Report.class, false);

    public DamageReportView() {
        add(new H1("UC1 — Report damage in the city"));
        add(new Paragraph("Spotted a pothole, a broken streetlight or a "
                + "fallen tree? Say what it is, photograph it and send it to "
                + "the city's maintenance crew. On a phone, \"Take photo\" "
                + "opens the camera directly."));

        Select<Category> category = new Select<>();
        category.setLabel("What is broken?");
        category.setItems(Category.values());
        category.setItemLabelGenerator(c -> c.label);
        category.setRequiredIndicatorVisible(true);
        category.bindValue(categorySignal, categorySignal::set);

        TextArea description = new TextArea("Details (optional)");
        description.setPlaceholder("e.g. deep hole in the cycle lane");
        description.setWidthFull();
        description.bindValue(descriptionSignal, descriptionSignal::set);

        uploadManager = new UploadManager(this,
                UploadHandler.inMemory(this::photoReceived).validateHeader(
                        Images.HEADER_SIZE, Images::rejectUnlessImage));
        uploadManager.setAcceptedMimeTypes("image/*");
        uploadManager.setMaxFileSize(MAX_PHOTO_BYTES);
        uploadManager.addFileRejectedListener(e -> Notification.show(
                e.getFileName() + " was not added: " + switch (e.getReason()) {
                case FILE_TOO_LARGE -> "photos can be at most 20 MB.";
                case INCORRECT_FILE_TYPE -> "only photos can be attached.";
                // No maxFiles is set: the total is counted in photoReceived.
                case TOO_MANY_FILES, UNKNOWN -> "it could not be added.";
                }));

        UploadButton takePhoto = new UploadButton("Take photo", uploadManager);
        takePhoto.setCapture(UploadCapture.ENVIRONMENT);
        takePhoto.addThemeVariants(ButtonVariant.PRIMARY);
        UploadButton chooseExisting = new UploadButton("Choose existing",
                uploadManager);
        Signal<Boolean> roomForMore = photosSignal
                .map(photos -> photos.size() < MAX_PHOTOS);
        takePhoto.bindEnabled(roomForMore);
        chooseExisting.bindEnabled(roomForMore);

        Div previews = new Div();
        previews.addClassName("photo-strip");
        Signal.effect(previews, () -> {
            previews.removeAll();
            photosSignal.get().forEach(photo -> previews.add(preview(photo)));
        });
        Span photoCount = new Span(photosSignal.map(photos -> photos.isEmpty()
                ? "No photos yet — at least one is needed."
                : photos.size() + " of " + MAX_PHOTOS + " photos"));

        Button pin = new Button("Add my location", e -> pinLocation());
        Span locationLabel = new Span(locationSignal.map(
                p -> p == null ? "No location — describe where it is instead."
                        : "Location: " + format(p)));

        Button send = new Button("Send report", e -> send());
        send.addThemeVariants(ButtonVariant.PRIMARY);
        send.bindEnabled(() -> categorySignal.get() != null
                && !photosSignal.get().isEmpty());

        add(category, description, new H2("Photos"),
                new HorizontalLayout(takePhoto, chooseExisting), photoCount,
                previews, new H2("Where?"), new HorizontalLayout(pin),
                locationLabel, send);

        add(new H2("Sent reports"));
        grid.addColumn(r -> r.category().label).setHeader("Category")
                .setAutoWidth(true);
        grid.addColumn(Report::description).setHeader("Details");
        grid.addColumn(Report::photos).setHeader("Photos").setAutoWidth(true);
        grid.addColumn(r -> r.location() == null ? "—" : r.location())
                .setHeader("Location").setAutoWidth(true);
        grid.setAllRowsVisible(true);
        grid.setItems(reports);
        add(grid);
    }

    private void photoReceived(UploadMetadata metadata, byte[] bytes) {
        // Photos taken one by one and photos picked several at once all
        // arrive here, so this is where the total is enforced.
        if (photosSignal.peek().size() >= MAX_PHOTOS) {
            Notification.show("A report can have at most " + MAX_PHOTOS
                    + " photos. Remove one first.");
            return;
        }
        photosSignal.update(photos -> {
            List<Photo> updated = new ArrayList<>(photos);
            updated.add(new Photo(metadata.fileName(), bytes));
            return List.copyOf(updated);
        });
    }

    private Div preview(Photo photo) {
        Image image = new Image(photo.bytes(), photo.fileName());
        image.addClassName("photo-thumbnail");
        Button remove = new Button("Remove", e -> photosSignal.update(
                photos -> photos.stream().filter(p -> p != photo).toList()));
        remove.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        remove.setAriaLabel("Remove " + photo.fileName());
        Div tile = new Div(image, remove);
        tile.addClassName("photo-tile");
        return tile;
    }

    private void pinLocation() {
        GeolocationOptions options = GeolocationOptions.builder()
                .highAccuracy(true).timeout(Duration.ofSeconds(10))
                .maximumAge(Duration.ofMinutes(1)).build();
        Geolocation.getPosition(locationSignal::set,
                error -> Notification
                        .show("Could not get your location. Please mention the "
                                + "street in the details instead."),
                options);
    }

    private void send() {
        Category category = categorySignal.peek();
        if (category == null || photosSignal.peek().isEmpty()) {
            return;
        }
        GeolocationPosition location = locationSignal.peek();
        reports.add(new Report(category, descriptionSignal.peek(),
                photosSignal.peek().size(),
                location == null ? null : format(location)));
        grid.getDataProvider().refreshAll();
        Notification.show("Thank you — the report has been sent.");

        categorySignal.set(null);
        descriptionSignal.set("");
        photosSignal.set(List.of());
        locationSignal.set(null);
        uploadManager.clearFileList();
    }

    private static String format(GeolocationPosition position) {
        return "%.5f, %.5f (±%.0f m)".formatted(position.coords().latitude(),
                position.coords().longitude(), position.coords().accuracy());
    }
}
