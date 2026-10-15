package com.example.uc13;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * Turns a page of a PDF document on the server, by changing the page's
 * rotation. The content is not touched, so the result is as sharp as the scan.
 */
final class PageRotation {

    private PageRotation() {
    }

    /**
     * Turns one page.
     *
     * @param pdf
     *            the document
     * @param page
     *            the page, starting from 1
     * @param degrees
     *            90 to turn clockwise, -90 to turn counterclockwise
     * @return the document with the page turned
     */
    static byte[] rotate(byte[] pdf, int page, int degrees) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDPage target = document.getPage(page - 1);
            target.setRotation(
                    Math.floorMod(target.getRotation() + degrees, 360));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not turn page " + page, e);
        }
    }

    /**
     * Reads the rotation of a page.
     *
     * @param pdf
     *            the document
     * @param page
     *            the page, starting from 1
     * @return the rotation in degrees: 0, 90, 180 or 270
     */
    static int rotation(byte[] pdf, int page) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getPage(page - 1).getRotation();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read page " + page, e);
        }
    }
}
