package com.example;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadEvent;

/**
 * Image helpers shared by the use cases that receive photos: recognising an
 * image by its leading bytes rather than by its name or claimed MIME type, and
 * scaling it down for thumbnails and avatars.
 */
public final class Images {

    /**
     * How many leading bytes
     * {@link #rejectUnlessImage(UploadEvent, ByteBuffer)} needs to recognise
     * every format in {@link #isImage(ByteBuffer)}.
     */
    public static final int HEADER_SIZE = 12;

    private Images() {
    }

    /**
     * Header validator for
     * {@code UploadHandler.inMemory(…).validateHeader(Images.HEADER_SIZE, Images::rejectUnlessImage)}:
     * refuses the upload before it is stored when the bytes are not a JPEG,
     * PNG, GIF, WebP or HEIC image, whatever the file name or the browser's
     * MIME type say.
     */
    public static void rejectUnlessImage(UploadEvent event, ByteBuffer header) {
        if (!isImage(header)) {
            event.reject(event.getFileName() + " is not a photo");
        }
    }

    /**
     * Whether the given leading bytes start a JPEG, PNG, GIF, WebP or HEIC
     * image.
     */
    public static boolean isImage(ByteBuffer header) {
        byte[] bytes = new byte[Math.min(header.remaining(), HEADER_SIZE)];
        header.duplicate().get(bytes);
        return startsWith(bytes, 0, 0xFF, 0xD8, 0xFF) // JPEG
                || startsWith(bytes, 0, 0x89, 'P', 'N', 'G') // PNG
                || startsWith(bytes, 0, 'G', 'I', 'F', '8') // GIF
                || startsWith(bytes, 0, 'R', 'I', 'F', 'F')
                        && startsWith(bytes, 8, 'W', 'E', 'B', 'P') // WebP
                || startsWith(bytes, 4, 'f', 't', 'y', 'p'); // HEIC/HEIF
    }

    private static boolean startsWith(byte[] bytes, int offset,
            int... expected) {
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Decodes the image, or returns {@code null} when the JDK cannot read the
     * format (WebP and HEIC, for example) or the bytes are damaged.
     */
    public static @Nullable BufferedImage read(byte[] image) {
        try {
            return ImageIO.read(new ByteArrayInputStream(image));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Scales the image down so that neither side exceeds {@code maxSize}
     * pixels, keeping the aspect ratio, and encodes it as PNG. Smaller images
     * are only re-encoded.
     */
    public static byte[] scaleToFit(BufferedImage source, int maxSize) {
        double scale = Math.min(1.0, (double) maxSize
                / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        return drawPng(source, 0, 0, source.getWidth(), source.getHeight(),
                width, height);
    }

    /**
     * Crops the largest centred square out of the image and scales it to
     * {@code size} × {@code size} pixels, encoded as PNG — the usual shape of a
     * profile picture.
     */
    public static byte[] squareCrop(BufferedImage source, int size) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        return drawPng(source, x, y, side, side, size, size);
    }

    private static byte[] drawPng(BufferedImage source, int x, int y, int width,
            int height, int targetWidth, int targetHeight) {
        BufferedImage target = new BufferedImage(targetWidth, targetHeight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, x, y,
                    x + width, y + height, null);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(target, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /**
     * Serves image bytes held in memory, for {@code Image} and {@code Avatar}
     * sources that need a URL rather than an inline data URL.
     */
    public static DownloadHandler download(byte[] image, String fileName,
            String contentType) {
        return DownloadHandler.fromInputStream(
                event -> new DownloadResponse(new ByteArrayInputStream(image),
                        fileName, contentType, image.length));
    }
}
