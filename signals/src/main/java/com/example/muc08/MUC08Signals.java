package com.example.muc08;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * Application-scoped signals for MUC08: Broadcast Announcement.
 * <p>
 * A single {@link SharedValueSignal} holds the current announcement (or
 * {@code null} when there is none). Every open session binds to it, so posting
 * or clearing an announcement reaches all users without a hand-written
 * broadcaster, listener registry or {@code UI.access()} calls.
 */
@Component
public class MUC08Signals {

    public record Announcement(String id, String author, String text,
            LocalDateTime postedAt) {
        public Announcement(String author, String text) {
            this(UUID.randomUUID().toString(), author, text,
                    LocalDateTime.now());
        }

        public String getFormattedTimestamp() {
            return postedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }

    private final SharedValueSignal<@Nullable Announcement> announcementSignal = new SharedValueSignal<>(
            Announcement.class);

    public SharedValueSignal<@Nullable Announcement> getAnnouncementSignal() {
        return announcementSignal;
    }

    public void post(Announcement announcement) {
        announcementSignal.set(announcement);
    }

    public void clear() {
        announcementSignal.set(null);
    }
}
