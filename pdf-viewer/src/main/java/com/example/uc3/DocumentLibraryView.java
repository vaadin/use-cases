package com.example.uc3;

import java.util.List;

import com.example.common.UnreleasedApi;
import com.example.common.UseCaseDescription;
import com.example.documents.Pdf;
import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewer;
import com.example.pdfviewer.PdfViewer.DocumentErrorEvent;
import com.example.views.ApiGap;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * UC3 — Document library.
 * <p>
 * A list of files with a preview next to it, like the attachments of a case or
 * the documents of a project. Not every file in such a list is fine: the
 * library has one that was cut off during upload, one that was deleted from the
 * server, and one that is protected with a password.
 * <p>
 * {@code document-error} tells the application why a file could not be shown
 * ({@code network}, {@code invalid} or {@code password}), so it can say what to
 * do about it in its own words, next to the viewer's generic message. For the
 * protected file it offers the download the viewer can not.
 * <p>
 * The viewer's header shows the file name, {@code travel-policy.pdf}, not the
 * name the library gives the file. The header has no title of its own, and
 * setting the file name to "Travel policy" would also change the name the file
 * is downloaded under (API-GAPS.md #17).
 */
@UnreleasedApi("https://github.com/vaadin/web-components/pull/13053")
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Document library")
@UseCaseDescription("Previewing the files of a list, and explaining the ones that do not open")
@Menu(order = 3, title = "UC3 — Document library")
@StyleSheet("uc3.css")
public class DocumentLibraryView extends VerticalLayout {

    /** A file in the library. The note says what is wrong with it, if any. */
    record LibraryFile(String name, String note, @Nullable Pdf pdf) {

        DownloadHandler handler() {
            return pdf == null ? Pdf.missing(name) : pdf.handler();
        }
    }

    static final List<LibraryFile> FILES = List.of(
            new LibraryFile("Travel policy", "",
                    SampleDocuments.travelPolicy()),
            new LibraryFile("Price list 2026", "", SampleDocuments.priceList()),
            new LibraryFile("Quarterly report Q3", "Upload was cut off",
                    SampleDocuments.damagedReport()),
            new LibraryFile("Old brochure", "Deleted from the server", null),
            new LibraryFile("Payslip September", "Password protected",
                    SampleDocuments.payslip()));

    private final PdfViewer viewer = new PdfViewer();
    private final Div message = new Div();
    private final Grid<LibraryFile> grid = new Grid<>();

    public DocumentLibraryView() {
        addClassName("uc3-view");
        add(new H1("UC3 — Document library"));
        add(new Paragraph("Pick a file to preview it. The last three files do "
                + "not open, each for a different reason; the note column "
                + "says why. The message under the list is the app's own, "
                + "based on the reason the viewer reports."));

        grid.addColumn(LibraryFile::name).setHeader("File");
        grid.addColumn(LibraryFile::note).setHeader("Note");
        grid.setItems(FILES);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.WRAP_CELL_CONTENT);
        grid.addClassName("uc3-grid");
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                open(event.getValue());
            }
        });

        message.addClassName("uc3-message");
        viewer.addClassName("uc3-viewer");
        viewer.setFileNameVisible(true);
        viewer.addDocumentLoadListener(
                event -> message.setText("Opened: " + event.getPageCount()
                        + (event.getPageCount() == 1 ? " page" : " pages")
                        + (event.getTitle().isEmpty() ? ""
                                : ", titled \"" + event.getTitle() + "\"")
                        + "."));
        viewer.addDocumentErrorListener(this::explain);

        Div list = new Div(grid, message);
        list.addClassName("uc3-list");
        Div layout = new Div(list, viewer);
        layout.addClassNames("uc3-layout", "side-by-side");
        add(layout);
        add(ApiGap.note(6, "the viewer can not open a password-protected "
                + "PDF; it only reports the reason \"password\"."));
        add(ApiGap.note(17, "the viewer's header shows the file name. It has "
                + "no title of its own, so it can not show the name the "
                + "library uses without also downloading under that name."));
        grid.select(FILES.getFirst());
    }

    private void open(LibraryFile file) {
        message.setText("Loading " + file.name() + "…");
        viewer.setSrc(file.handler());
    }

    private void explain(DocumentErrorEvent event) {
        LibraryFile file = grid.asSingleSelect().getValue();
        message.removeAll();
        switch (event.getReason()) {
        case NETWORK -> message.setText("The file could not be fetched. It "
                + "may have been deleted: ask its owner to share it again.");
        case INVALID -> message.setText("The file is damaged or is not a "
                + "PDF. Ask the sender for a new copy.");
        case PASSWORD -> {
            message.add(new Span("The file is protected with a password, "
                    + "and the viewer can not open protected files. "));
            Pdf pdf = file == null ? null : file.pdf();
            if (pdf != null) {
                // The viewer's own download button needs a loaded document.
                // The browser's built-in viewer asks for the password.
                Anchor download = new Anchor(pdf.handler().inline(),
                        "Open it in the browser instead");
                download.setTarget("_blank");
                message.add(download);
                message.add(new Span(" (the password is lind-0926)."));
            }
        }
        }
    }
}
