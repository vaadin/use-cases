package com.example.uc6;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HeyGenClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer
            .bindTo(builder).build();
    private final HeyGenClient client = new HeyGenClient(builder,
            "https://api.heygen.test", "secret");

    @Test
    void createVideoSendsAnAvatarVideoRequestAndReturnsTheVideoId() {
        server.expect(requestTo("https://api.heygen.test/v3/videos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Api-Key", "secret"))
                .andExpect(jsonPath("$.type").value("avatar"))
                .andExpect(jsonPath("$.avatar_id").value("Bryce_public_1"))
                .andExpect(jsonPath("$.script").value("Hello"))
                .andRespond(withSuccess(
                        "{\"data\":{\"video_id\":\"v_123\",\"status\":\"waiting\"}}",
                        MediaType.APPLICATION_JSON));

        assertEquals("v_123",
                client.createVideo("Bryce_public_1", "Hello", "Title"));
        server.verify();
    }

    @Test
    void statusReadsTheVideoUrlOfACompletedVideo() {
        server.expect(requestTo("https://api.heygen.test/v3/videos/v_123"))
                .andRespond(withSuccess("""
                        {"data": {"id": "v_123", "status": "completed",
                          "video_url": "https://files.test/v.mp4",
                          "thumbnail_url": "https://files.test/v.jpg",
                          "failure_message": null}}""",
                        MediaType.APPLICATION_JSON));

        HeyGenClient.VideoStatus status = client.status("v_123");

        assertEquals("completed", status.status());
        assertEquals("https://files.test/v.mp4", status.videoUrl());
        assertNull(status.failureMessage());
    }

    @Test
    void errorResponsesSurfaceHeyGensMessage() {
        server.expect(requestTo("https://api.heygen.test/v3/videos"))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED).body("""
                        {"error": {"code": "insufficient_credit",
                          "message": "Insufficient credits."}}""")
                        .contentType(MediaType.APPLICATION_JSON));

        HeyGenClient.HeyGenException error = assertThrows(
                HeyGenClient.HeyGenException.class,
                () -> client.createVideo("a", "Hello", "Title"));
        assertEquals("HeyGen: Insufficient credits.", error.getMessage());
    }
}
