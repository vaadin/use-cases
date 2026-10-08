package com.example.uc4;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.Images;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * The user's photo album, kept for the session. Every photo gets its own id, so
 * two photos called {@code IMG_0001.jpg} from different phones are two
 * different photos; the second one is shown as {@code IMG_0001 (2).jpg}.
 */
@Component
@VaadinSessionScope
public class PhotoAlbum implements Serializable {

    static final int THUMBNAIL_SIZE = 240;

    /**
     * A stored photo. {@code thumbnail} is {@code null} when the server cannot
     * decode the format (HEIC or WebP, for example); the browser may still be
     * able to show the original.
     */
    public record Photo(UUID id, String name, String contentType,
            byte[] original, byte @Nullable [] thumbnail) {
    }

    private final List<Photo> photos = new ArrayList<>();

    public List<Photo> photos() {
        return List.copyOf(photos);
    }

    public Photo add(String fileName, String contentType, byte[] bytes) {
        BufferedImage image = Images.read(bytes);
        Photo photo = new Photo(UUID.randomUUID(), uniqueName(fileName),
                contentType, bytes, image == null ? null
                        : Images.scaleToFit(image, THUMBNAIL_SIZE));
        photos.add(photo);
        return photo;
    }

    public void remove(UUID id) {
        photos.removeIf(p -> p.id().equals(id));
    }

    private String uniqueName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        String candidate = fileName;
        for (int n = 2; isTaken(candidate); n++) {
            candidate = base + " (" + n + ")" + extension;
        }
        return candidate;
    }

    private boolean isTaken(String name) {
        return photos.stream().anyMatch(p -> p.name().equals(name));
    }
}
