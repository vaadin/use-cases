package com.example.usecase29;

import java.io.Serializable;

record UserProfile(String name, String email,
        String bio) implements Serializable {
    UserProfile withName(String name) {
        return new UserProfile(name, email, bio);
    }

    UserProfile withEmail(String email) {
        return new UserProfile(name, email, bio);
    }

    UserProfile withBio(String bio) {
        return new UserProfile(name, email, bio);
    }
}
