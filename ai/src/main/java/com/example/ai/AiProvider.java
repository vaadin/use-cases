package com.example.ai;

/**
 * The AI services the use cases call, with the environment variable that holds
 * each one's API key.
 */
public enum AiProvider {

    OPENAI("OpenAI", "OPENAI_API_KEY"),

    ELEVENLABS("ElevenLabs", "ELEVENLABS_API_KEY"),

    HEYGEN("HeyGen", "HEYGEN_API_KEY");

    private final String displayName;
    private final String keyVariable;

    AiProvider(String displayName, String keyVariable) {
        this.displayName = displayName;
        this.keyVariable = keyVariable;
    }

    public String displayName() {
        return displayName;
    }

    public String keyVariable() {
        return keyVariable;
    }
}
