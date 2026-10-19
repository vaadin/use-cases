package com.example.uc5;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VoiceOverServiceTest {

    private static final VoiceOverService.Voice VOICE = VoiceOverService.VOICES
            .getFirst();

    @Test
    void callsBeyondTheDailyQuotaAreRefused() {
        VoiceOverService service = service(prompt -> speech("mp3"), 1);

        service.speak("Hello", VOICE);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.speak("Hello again", VOICE));
        assertEquals("Today's voice-overs are used up. Try again tomorrow.",
                error.getMessage());
    }

    @Test
    void failedCallGivesItsQuotaBack() {
        VoiceOverService service = service(prompt -> {
            throw new IllegalStateException("ElevenLabs is down");
        }, 1);

        assertThrows(IllegalStateException.class,
                () -> service.speak("Hello", VOICE));

        assertEquals(1, service.quota().remaining());
    }

    @Test
    void blankOrTooLongTextIsRejectedWithoutUsingQuota() {
        VoiceOverService service = service(prompt -> speech("mp3"), 1);

        assertThrows(IllegalArgumentException.class,
                () -> service.speak(" ", VOICE));
        assertThrows(IllegalArgumentException.class, () -> service
                .speak("a".repeat(VoiceOverService.MAX_CHARACTERS + 1), VOICE));

        assertEquals(1, service.quota().remaining());
    }

    private static TextToSpeechResponse speech(String bytes) {
        return new TextToSpeechResponse(List.of(new Speech(bytes.getBytes())));
    }

    private static VoiceOverService service(
            Function<TextToSpeechPrompt, TextToSpeechResponse> model,
            int perDay) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("speech", new TextToSpeechModel() {
            @Override
            public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
                return model.apply(prompt);
            }

            @Override
            public Flux<TextToSpeechResponse> stream(
                    TextToSpeechPrompt prompt) {
                return Flux.just(call(prompt));
            }
        });
        return new VoiceOverService(
                beans.getBeanProvider(TextToSpeechModel.class), perDay);
    }
}
