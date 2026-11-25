package com.example.uc5;

import java.util.List;

import com.example.ai.DailyQuota;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.elevenlabs.ElevenLabsTextToSpeechOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Turns text into speech with ElevenLabs, through Spring AI's
 * {@link TextToSpeechModel}. Generated clips are not stored: they live only in
 * the view that asked for them.
 */
@Service
public class VoiceOverService {

    /**
     * A voice to read the text with: ElevenLabs' id and a description for the
     * reader.
     */
    public record Voice(String id, String label) {
    }

    /**
     * Some of the voices every ElevenLabs account has.
     */
    public static final List<Voice> VOICES = List.of(
            new Voice("JBFqnCBsd6RMkjVDRZzb", "George — warm, storyteller"),
            new Voice("EXAVITQu4vr4xnSDxMaL", "Sarah — calm, confident"),
            new Voice("IKne3meq5aSn9XLyUdCD", "Charlie — energetic"));

    public static final int MAX_CHARACTERS = 600;

    private final ObjectProvider<TextToSpeechModel> speechModel;
    private final DailyQuota quota;

    public VoiceOverService(ObjectProvider<TextToSpeechModel> speechModel,
            @Value("${app.elevenlabs.clips-per-day}") int clipsPerDay) {
        this.speechModel = speechModel;
        this.quota = new DailyQuota(clipsPerDay);
    }

    public DailyQuota quota() {
        return quota;
    }

    /**
     * Reads the text aloud with the given voice and returns the speech as MP3.
     *
     * @throws IllegalStateException
     *             if today's quota is used up
     */
    public byte[] speak(String text, Voice voice) {
        if (text.isBlank() || text.length() > MAX_CHARACTERS) {
            throw new IllegalArgumentException("The text must have 1 to "
                    + MAX_CHARACTERS + " characters");
        }
        if (!quota.tryAcquire()) {
            throw new IllegalStateException(
                    "Today's voice-overs are used up. Try again tomorrow.");
        }
        try {
            return speechModel.getObject()
                    .call(new TextToSpeechPrompt(text,
                            ElevenLabsTextToSpeechOptions.builder()
                                    .voiceId(voice.id()).build()))
                    .getResult().getOutput();
        } catch (RuntimeException e) {
            quota.release();
            throw e;
        }
    }
}
