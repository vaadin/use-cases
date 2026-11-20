package com.example.uc6;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Calls HeyGen's video API (v3): starts rendering an avatar video from a script
 * and reports how the rendering is going. Spring AI has no video-generation
 * model, so this is a plain REST client.
 */
@Component
public class HeyGenClient {

    /**
     * Where a rendering job is: {@code pending}, {@code processing},
     * {@code completed} or {@code failed}; with the video's URL once it is
     * completed and HeyGen's reason once it failed.
     */
    public record VideoStatus(String status, @Nullable String videoUrl,
            @Nullable String thumbnailUrl, @Nullable String failureMessage) {

        public boolean isFinished() {
            return "completed".equals(status) || "failed".equals(status);
        }
    }

    /**
     * HeyGen answered a request with an error status, so it did not do what was
     * asked.
     */
    public static class HeyGenException extends RuntimeException {

        public HeyGenException(String message) {
            super(message);
        }
    }

    private final RestClient restClient;

    public HeyGenClient(RestClient.Builder restClientBuilder,
            @Value("${app.heygen.base-url}") String baseUrl,
            @Value("${app.heygen.api-key}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl)
                .defaultHeader("X-Api-Key", apiKey).defaultStatusHandler(
                        HttpStatusCode::isError, (request, response) -> {
                            throw new HeyGenException("HeyGen: " + errorMessage(
                                    response.getBody().readAllBytes()));
                        })
                .build();
    }

    /**
     * Starts rendering a video in which the avatar speaks the script with its
     * own voice.
     *
     * @return the id of the video, to ask for its status
     */
    public String createVideo(String avatarId, String script, String title) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("type", "avatar");
        request.put("avatar_id", avatarId);
        request.put("script", script);
        request.put("title", title);
        request.put("resolution", "720p");
        request.put("aspect_ratio", "16:9");
        JsonNode response = restClient.post().uri("/v3/videos")
                .contentType(MediaType.APPLICATION_JSON).body(request)
                .retrieve().body(JsonNode.class);
        return data(response).path("video_id").asString();
    }

    public VideoStatus status(String videoId) {
        JsonNode data = data(restClient.get().uri("/v3/videos/{id}", videoId)
                .retrieve().body(JsonNode.class));
        return new VideoStatus(data.path("status").asString(),
                text(data, "video_url"), text(data, "thumbnail_url"),
                text(data, "failure_message"));
    }

    /**
     * Returns the message of a HeyGen error response, such as "Insufficient
     * credits", or the raw body when it has none.
     */
    static String errorMessage(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        try {
            JsonNode message = JsonMapper.shared().readTree(text).path("error")
                    .path("message");
            return message.isString() ? message.asString() : text;
        } catch (JacksonException e) {
            return text;
        }
    }

    private static JsonNode data(@Nullable JsonNode response) {
        if (response == null || !response.path("data").isObject()) {
            throw new IllegalStateException(
                    "Unexpected response from HeyGen: " + response);
        }
        return response.path("data");
    }

    private static @Nullable String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isString() ? value.asString() : null;
    }
}
