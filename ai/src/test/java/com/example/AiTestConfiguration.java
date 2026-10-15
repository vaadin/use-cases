package com.example;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.example.ai.LlmProviderFactory;
import com.example.uc4.DocsMcpClient;
import com.example.uc6.HeyGenClient;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import com.vaadin.flow.component.ai.provider.LLMProvider;

/**
 * Replaces every AI service with an in-process fake, so the tests run without
 * network access or API keys and can script what the "AI" does.
 */
@TestConfiguration
public class AiTestConfiguration {

    @Bean
    FakeLlm fakeLlm() {
        return new FakeLlm();
    }

    @Bean
    @Primary
    LlmProviderFactory fakeLlmProviderFactory(FakeLlm fakeLlm) {
        return new LlmProviderFactory(null) {
            @Override
            public LLMProvider create() {
                return fakeLlm.newProvider();
            }
        };
    }

    /**
     * Embeds a text as the counts of a few topic words, so that "time off"
     * questions land near the vacation section without a real model.
     */
    @Bean
    EmbeddingModel fakeEmbeddingModel() {
        return new EmbeddingModel() {
            private final List<Set<String>> topics = List.of(
                    Set.of("vacation", "holiday", "off", "days"),
                    Set.of("remote", "country", "abroad", "spain"),
                    Set.of("sick", "ill", "flu", "child", "kid"),
                    Set.of("expense", "expenses", "receipt", "chair",
                            "equipment", "office"),
                    Set.of("travel", "flight", "hotel", "train"),
                    Set.of("salary", "pay", "ceo"));

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<String> texts = request.getInstructions();
                return new EmbeddingResponse(java.util.stream.IntStream
                        .range(0, texts.size())
                        .mapToObj(i -> new Embedding(embed(texts.get(i)), i))
                        .toList());
            }

            @Override
            public float[] embed(Document document) {
                return embed(document.getText());
            }

            @Override
            public float[] embed(String text) {
                List<String> words = Arrays
                        .asList(text.toLowerCase(Locale.ROOT).split("[^a-z]+"));
                float[] vector = new float[topics.size() + 1];
                for (int i = 0; i < topics.size(); i++) {
                    for (String word : words) {
                        if (topics.get(i).contains(word)) {
                            vector[i]++;
                        }
                    }
                }
                // Keeps the vector non-zero for texts without topic words.
                vector[topics.size()] = 0.1f;
                return vector;
            }
        };
    }

    @Bean
    @Primary
    DocsMcpClient fakeDocsMcpClient() {
        return new DocsMcpClient("https://docs.example.com/mcp",
                Set.of("search_docs")) {
            @Override
            public synchronized Connection connect() {
                return new Connection("example-docs", "1.0", 5,
                        List.of(new DocsTool("search_docs",
                                "Searches the documentation",
                                "{\"type\":\"object\",\"properties\":"
                                        + "{\"question\":{\"type\":\"string\"}}}")));
            }

            @Override
            public String call(String tool, Map<String, Object> arguments) {
                return "Use a Dialog with a confirm button (docs/dialog.adoc)";
            }
        };
    }

    @Bean
    TextToSpeechModel fakeSpeechModel() {
        return new TextToSpeechModel() {
            @Override
            public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
                return new TextToSpeechResponse(
                        List.of(new Speech(("ID3 fake mp3 of: "
                                + prompt.getInstructions().getText())
                                .getBytes())));
            }

            @Override
            public reactor.core.publisher.Flux<TextToSpeechResponse> stream(
                    TextToSpeechPrompt prompt) {
                return reactor.core.publisher.Flux.just(call(prompt));
            }
        };
    }

    @Bean
    @Primary
    FakeHeyGen fakeHeyGen() {
        return new FakeHeyGen(RestClient.builder());
    }

    /**
     * A HeyGen whose jobs move from pending to processing to completed on
     * successive status calls.
     */
    public static class FakeHeyGen extends HeyGenClient {

        private int statusCalls;

        FakeHeyGen(RestClient.Builder builder) {
            super(builder, "https://api.example.com", "test");
        }

        public synchronized void reset() {
            statusCalls = 0;
        }

        public synchronized int statusCalls() {
            return statusCalls;
        }

        @Override
        public String createVideo(String avatarId, String script,
                String title) {
            return "video-1";
        }

        @Override
        public synchronized VideoStatus status(String videoId) {
            statusCalls++;
            return switch (statusCalls) {
            case 1 -> new VideoStatus("pending", null, null, null);
            case 2 -> new VideoStatus("processing", null, null, null);
            default -> new VideoStatus("completed",
                    "https://files.example.com/video-1.mp4",
                    "https://files.example.com/video-1.jpg", null);
            };
        }
    }
}
