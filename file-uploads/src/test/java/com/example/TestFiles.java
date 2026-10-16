package com.example;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Files the tests feed into the upload components.
 */
public final class TestFiles {

    private TestFiles() {
    }

    /** A PNG photo of a single colour. */
    public static byte[] png(int width, int height) {
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.ORANGE);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /**
     * The start of an MP4 video: the same {@code ftyp} box as a HEIC photo, but
     * with a video brand.
     */
    public static byte[] mp4() {
        return new byte[] { 0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o',
                'm', 0, 0, 2, 0 };
    }

    /** Writes the content to a temporary file with the given name suffix. */
    public static File file(String name, byte[] content) {
        try {
            File file = Files.createTempFile("upload-", "-" + name).toFile();
            file.deleteOnExit();
            Files.write(file.toPath(), content);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A text file, whatever name it is uploaded under. */
    public static byte[] text(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
