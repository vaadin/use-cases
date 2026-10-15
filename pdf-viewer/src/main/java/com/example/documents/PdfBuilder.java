package com.example.documents;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.jspecify.annotations.Nullable;

/**
 * Lays out simple text documents with PDFBox: a title, headings, wrapped
 * paragraphs, two-column rows and a page number in the footer.
 * <p>
 * Just enough layout for the sample documents of the use cases. The viewer
 * shows whatever PDF it is given; how the PDF is made is not part of the story.
 */
public final class PdfBuilder {

    private sealed interface Block {
    }

    private record Heading(String text) implements Block {
    }

    private record Paragraph(String text, FontName font,
            float size) implements Block {
    }

    private record Row(String left, String right,
            boolean bold) implements Block {
    }

    private record Bars() implements Block {
    }

    private record PageBreak() implements Block {
    }

    private static final FontName REGULAR = FontName.HELVETICA;
    private static final FontName BOLD = FontName.HELVETICA_BOLD;

    private final String title;
    private final List<Block> blocks = new ArrayList<>();
    private PDRectangle pageSize = PDRectangle.A4;
    private float margin = 56;
    private float bodySize = 11;
    private boolean outline;
    private boolean pageNumbers = true;
    private boolean chaptersOnNewPages;
    private boolean contentsPage;
    private @Nullable String password;
    private final Map<String, Integer> headingPages = new LinkedHashMap<>();

    /**
     * Starts a document. The title is the first line of the first page and the
     * title in the document's metadata.
     *
     * @param title
     *            the title of the document
     */
    public PdfBuilder(String title) {
        this.title = title;
    }

    public PdfBuilder pageSize(PDRectangle pageSize, float margin) {
        this.pageSize = pageSize;
        this.margin = margin;
        return this;
    }

    public PdfBuilder bodySize(float bodySize) {
        this.bodySize = bodySize;
        return this;
    }

    /** Adds a bookmark for every heading, shown in the viewer's sidebar. */
    public PdfBuilder withOutline() {
        this.outline = true;
        return this;
    }

    /**
     * Lists the headings on the first page, below the title, each linking to
     * its heading. The rest of the document starts on the second page.
     */
    public PdfBuilder withContentsPage() {
        this.contentsPage = true;
        return this;
    }

    /** Starts every heading on a new page, like the chapters of a book. */
    public PdfBuilder chaptersOnNewPages() {
        this.chaptersOnNewPages = true;
        return this;
    }

    public PdfBuilder withoutPageNumbers() {
        this.pageNumbers = false;
        return this;
    }

    /** Encrypts the document so it only opens with the given password. */
    public PdfBuilder password(String password) {
        this.password = password;
        return this;
    }

    public PdfBuilder heading(String text) {
        blocks.add(new Heading(text));
        return this;
    }

    public PdfBuilder paragraph(String text) {
        blocks.add(new Paragraph(text, REGULAR, bodySize));
        return this;
    }

    public PdfBuilder large(String text) {
        blocks.add(new Paragraph(text, BOLD, bodySize * 2));
        return this;
    }

    public PdfBuilder row(String left, String right) {
        blocks.add(new Row(left, right, false));
        return this;
    }

    public PdfBuilder boldRow(String left, String right) {
        blocks.add(new Row(left, right, true));
        return this;
    }

    /** Adds a row of bars that stands in for a barcode. */
    public PdfBuilder bars() {
        blocks.add(new Bars());
        return this;
    }

    public PdfBuilder pageBreak() {
        blocks.add(new PageBreak());
        return this;
    }

    /**
     * Renders the document.
     *
     * @return the PDF document as bytes
     */
    public byte[] build() {
        headingPages.clear();
        try (PDDocument document = new PDDocument()) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(title);
            document.setDocumentInformation(info);
            new Layout(document).render();
            if (pageNumbers && document.getNumberOfPages() > 1) {
                addPageNumbers(document);
            }
            if (password != null) {
                StandardProtectionPolicy policy = new StandardProtectionPolicy(
                        password + "-owner", password, new AccessPermission());
                policy.setEncryptionKeyLength(128);
                document.protect(policy);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not render " + title, e);
        }
    }

    /**
     * The page each heading ended up on, after {@link #build()}.
     *
     * @return the 1-based page of each heading, in document order
     */
    public Map<String, Integer> headingPages() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(headingPages));
    }

    /** Fills pages top to bottom, starting a new page when one is full. */
    private final class Layout {

        private final PDDocument document;
        private final @Nullable PDDocumentOutline documentOutline;
        private final Map<String, PDPageFitWidthDestination> destinations = new LinkedHashMap<>();
        private float contentsTop;
        private @Nullable PDPageContentStream content;
        private @Nullable PDPage page;
        private float y;

        Layout(PDDocument document) {
            this.document = document;
            if (outline) {
                documentOutline = new PDDocumentOutline();
                document.getDocumentCatalog()
                        .setDocumentOutline(documentOutline);
            } else {
                documentOutline = null;
            }
        }

        void render() throws IOException {
            newPage();
            writeLines(List.of(title), BOLD, bodySize * 1.8f);
            y -= bodySize;
            if (contentsPage) {
                contentsTop = y;
                newPage();
            }
            for (Block block : blocks) {
                switch (block) {
                case Heading heading -> heading(heading.text());
                case Paragraph paragraph -> {
                    writeLines(
                            wrap(paragraph.text(), paragraph.font(),
                                    paragraph.size()),
                            paragraph.font(), paragraph.size());
                    y -= paragraph.size() * 0.6f;
                }
                case Row row -> row(row);
                case Bars bars -> bars();
                case PageBreak pageBreak -> newPage();
                }
            }
            closePage();
            if (contentsPage) {
                writeContents();
            }
        }

        private void heading(String text) throws IOException {
            float size = bodySize * 1.3f;
            if (chaptersOnNewPages && !pageIsEmpty()) {
                newPage();
            }
            // Keep a heading together with the first lines below it.
            ensureSpace(size * 1.6f + bodySize * 4);
            y -= size * 0.6f;
            headingPages.put(text, document.getNumberOfPages());
            PDPageFitWidthDestination destination = new PDPageFitWidthDestination();
            destination.setPage(page);
            destination.setTop((int) (y + size * 1.2f));
            destinations.put(text, destination);
            if (documentOutline != null) {
                PDOutlineItem item = new PDOutlineItem();
                item.setTitle(text);
                item.setDestination(destination);
                documentOutline.addLast(item);
            }
            writeLines(List.of(text), BOLD, size);
            y -= size * 0.3f;
        }

        /**
         * Writes a line for each heading on the first page, with a link to the
         * heading over the whole line.
         */
        private void writeContents() throws IOException {
            PDPage first = document.getPage(0);
            float size = bodySize * 1.2f;
            float lineHeight = size * 2;
            try (PDPageContentStream stream = new PDPageContentStream(document,
                    first, PDPageContentStream.AppendMode.APPEND, true)) {
                content = stream;
                y = contentsTop - bodySize * 2;
                text("Contents", margin, BOLD, bodySize * 1.3f);
                for (Map.Entry<String, PDPageFitWidthDestination> entry : destinations
                        .entrySet()) {
                    y -= lineHeight;
                    String pageLabel = String
                            .valueOf(headingPages.get(entry.getKey()));
                    text(entry.getKey(), margin, REGULAR, size);
                    text(pageLabel,
                            pageSize.getWidth() - margin
                                    - width(pageLabel, REGULAR, size),
                            REGULAR, size);
                    PDAnnotationLink link = new PDAnnotationLink();
                    link.setRectangle(new PDRectangle(margin, y - size * 0.4f,
                            pageSize.getWidth() - 2 * margin, size * 1.6f));
                    link.setDestination(entry.getValue());
                    PDBorderStyleDictionary border = new PDBorderStyleDictionary();
                    border.setWidth(0);
                    link.setBorderStyle(border);
                    first.getAnnotations().add(link);
                }
            } finally {
                content = null;
            }
        }

        private void row(Row row) throws IOException {
            FontName font = row.bold() ? BOLD : REGULAR;
            ensureSpace(bodySize * 1.5f);
            y -= bodySize * 1.5f;
            text(row.left(), margin, font, bodySize);
            float width = width(row.right(), font, bodySize);
            text(row.right(), pageSize.getWidth() - margin - width, font,
                    bodySize);
        }

        private void bars() throws IOException {
            float height = bodySize * 4;
            ensureSpace(height + bodySize);
            y -= height + bodySize * 0.5f;
            float x = margin;
            float right = pageSize.getWidth() - margin;
            int i = 0;
            while (x < right) {
                // A fixed pattern of widths, so the bars look like a code.
                float barWidth = 1 + (i * 7 % 4);
                current().addRect(x, y, barWidth, height);
                x += barWidth + 1 + (i * 5 % 3);
                i++;
            }
            current().fill();
        }

        private void writeLines(List<String> lines, FontName font, float size)
                throws IOException {
            for (String line : lines) {
                ensureSpace(size * 1.45f);
                y -= size * 1.45f;
                text(line, margin, font, size);
            }
        }

        private void ensureSpace(float height) throws IOException {
            float bottom = margin + (pageNumbers ? bodySize * 2 : 0);
            if (y - height < bottom) {
                newPage();
            }
        }

        private boolean pageIsEmpty() {
            return y >= pageSize.getHeight() - margin;
        }

        private void newPage() throws IOException {
            closePage();
            page = new PDPage(pageSize);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = pageSize.getHeight() - margin;
        }

        private void closePage() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }

        private PDPageContentStream current() {
            if (content == null) {
                throw new IllegalStateException("No page is open");
            }
            return content;
        }

        private void text(String text, float x, FontName font, float size)
                throws IOException {
            PDPageContentStream stream = current();
            stream.beginText();
            stream.setFont(new PDType1Font(font), size);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
        }

        private List<String> wrap(String text, FontName font, float size)
                throws IOException {
            float maxWidth = pageSize.getWidth() - 2 * margin;
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (width(candidate, font, size) > maxWidth
                        && !line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            return lines;
        }
    }

    private void addPageNumbers(PDDocument document) throws IOException {
        int count = document.getNumberOfPages();
        for (int i = 0; i < count; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream content = new PDPageContentStream(document,
                    page, PDPageContentStream.AppendMode.APPEND, true)) {
                String label = "Page " + (i + 1) + " of " + count;
                float size = bodySize * 0.8f;
                content.beginText();
                content.setFont(new PDType1Font(REGULAR), size);
                content.newLineAtOffset(pageSize.getWidth() - margin
                        - width(label, REGULAR, size), margin);
                content.showText(label);
                content.endText();
            }
        }
    }

    private static float width(String text, FontName font, float size)
            throws IOException {
        return new PDType1Font(font).getStringWidth(text) / 1000 * size;
    }
}
