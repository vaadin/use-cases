package com.example.uc3;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * The signed-in user's profile picture, kept for the session so that it is
 * still there when the user navigates away and comes back. Stands in for a user
 * table in a real application.
 */
@Component
@VaadinSessionScope
public class ProfilePictureStore implements Serializable {

    private byte @Nullable [] picture;

    /** The current picture as a square PNG, or {@code null} if none is set. */
    public byte @Nullable [] get() {
        return picture;
    }

    public void set(byte @Nullable [] picture) {
        this.picture = picture;
    }
}
