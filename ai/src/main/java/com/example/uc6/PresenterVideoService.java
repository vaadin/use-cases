package com.example.uc6;

import java.util.List;

import com.example.ai.DailyQuota;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Starts presenter videos with HeyGen, within the app's daily quota.
 */
@Service
public class PresenterVideoService {

    /**
     * A presenter: HeyGen's id of the avatar look and a name for the reader.
     */
    public record Presenter(String avatarId, String label) {
    }

    /**
     * Some of HeyGen's public stock avatars. Each speaks with its own default
     * voice.
     */
    public static final List<Presenter> PRESENTERS = List.of(
            new Presenter("Bryce_public_1", "Bryce, blue blazer"),
            new Presenter("Diora_public_1", "Diora, green blazer"),
            new Presenter("Albert_public_1", "Albert, blue suit"));

    public static final int MAX_CHARACTERS = 300;

    private final HeyGenClient heyGen;
    private final DailyQuota quota;

    public PresenterVideoService(HeyGenClient heyGen,
            @Value("${app.heygen.videos-per-day}") int videosPerDay) {
        this.heyGen = heyGen;
        this.quota = new DailyQuota(videosPerDay);
    }

    public DailyQuota quota() {
        return quota;
    }

    /**
     * Starts rendering the video.
     *
     * @return the id of the video, to ask for its status
     * @throws IllegalStateException
     *             if today's quota is used up
     */
    public String start(Presenter presenter, String script) {
        if (script.isBlank() || script.length() > MAX_CHARACTERS) {
            throw new IllegalArgumentException("The script must have 1 to "
                    + MAX_CHARACTERS + " characters");
        }
        if (!quota.tryAcquire()) {
            throw new IllegalStateException(
                    "Today's videos are used up. Try again tomorrow.");
        }
        try {
            return heyGen.createVideo(presenter.avatarId(), script,
                    "AI use cases — UC6");
        } catch (HeyGenClient.HeyGenException e) {
            // HeyGen refused the video. Any other failure, such as a reply
            // that cannot be read, may come after the video was created and
            // paid for, so it keeps using the quota.
            quota.release();
            throw e;
        }
    }

    public HeyGenClient.VideoStatus status(String videoId) {
        return heyGen.status(videoId);
    }
}
