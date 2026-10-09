package com.example.backend;

import javax.imageio.ImageIO;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

import com.example.BlurHash;
import org.springframework.stereotype.Component;

/**
 * The photos UC13 shows, each with the {@link BlurHash} an application would
 * store next to the image when it is uploaded. The hashes are computed once,
 * when the library is created.
 */
@Component
public class PhotoLibrary {

    /** One stored photo and what is known about it without loading it. */
    public record Photo(String title, int width, int height, String blurHash,
            byte[] bytes) {
    }

    private static final List<String[]> FILES = List.of(
            new String[] { "Sunset over the bay", "sunset.jpg" },
            new String[] { "Lavender field", "lavender.jpg" },
            new String[] { "Cabin by the lake", "forest.jpg" },
            new String[] { "Fruit stall", "market.jpg" });

    private final List<Photo> photos;

    public PhotoLibrary() {
        photos = FILES.stream().map(entry -> load(entry[0], entry[1])).toList();
    }

    public List<Photo> photos() {
        return photos;
    }

    private static Photo load(String title, String file) {
        try (InputStream in = Objects.requireNonNull(
                PhotoLibrary.class.getResourceAsStream("/uc13/" + file),
                file)) {
            byte[] bytes = in.readAllBytes();
            BufferedImage image = Objects.requireNonNull(
                    ImageIO.read(new ByteArrayInputStream(bytes)), file);
            return new Photo(title, image.getWidth(), image.getHeight(),
                    BlurHash.encode(thumbnail(image, 64), 4, 3), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The hash keeps only coarse structure, so a small copy is enough. */
    private static BufferedImage thumbnail(BufferedImage image, int width) {
        int height = Math.max(1, image.getHeight() * width / image.getWidth());
        BufferedImage small = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        small.getGraphics().drawImage(
                image.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0,
                0, null);
        return small;
    }
}
