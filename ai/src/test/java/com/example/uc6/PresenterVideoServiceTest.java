package com.example.uc6;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PresenterVideoServiceTest {

    private static final PresenterVideoService.Presenter PRESENTER = PresenterVideoService.PRESENTERS
            .getFirst();

    @Test
    void videosBeyondTheDailyQuotaAreRefused() {
        PresenterVideoService service = service(() -> "video-1", 1);

        service.start(PRESENTER, "Hello");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.start(PRESENTER, "Hello again"));
        assertEquals("Today's videos are used up. Try again tomorrow.",
                error.getMessage());
    }

    @Test
    void videoHeyGenRefusedGivesItsQuotaBack() {
        PresenterVideoService service = service(() -> {
            throw new HeyGenClient.HeyGenException("HeyGen: no credits");
        }, 1);

        assertThrows(HeyGenClient.HeyGenException.class,
                () -> service.start(PRESENTER, "Hello"));

        assertEquals(1, service.quota().remaining());
    }

    @Test
    void unreadableReplyKeepsUsingTheQuota() {
        // HeyGen may have created, and charged for, the video.
        PresenterVideoService service = service(() -> {
            throw new IllegalStateException("Unexpected response from HeyGen");
        }, 1);

        assertThrows(IllegalStateException.class,
                () -> service.start(PRESENTER, "Hello"));

        assertEquals(0, service.quota().remaining());
    }

    @Test
    void blankOrTooLongScriptIsRejectedWithoutUsingQuota() {
        PresenterVideoService service = service(() -> "video-1", 1);

        assertThrows(IllegalArgumentException.class,
                () -> service.start(PRESENTER, " "));
        assertThrows(IllegalArgumentException.class,
                () -> service.start(PRESENTER,
                        "a".repeat(PresenterVideoService.MAX_CHARACTERS + 1)));

        assertEquals(1, service.quota().remaining());
    }

    private static PresenterVideoService service(Supplier<String> create,
            int perDay) {
        HeyGenClient heyGen = new HeyGenClient(RestClient.builder(),
                "https://api.heygen.test", "test") {
            @Override
            public String createVideo(String avatarId, String script,
                    String title) {
                return create.get();
            }
        };
        return new PresenterVideoService(heyGen, perDay);
    }
}
