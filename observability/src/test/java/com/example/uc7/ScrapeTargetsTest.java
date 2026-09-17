package com.example.uc7;

import java.util.List;

import com.example.uc7.MonitoringStackView.Health;
import com.example.uc7.MonitoringStackView.Scrape;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The local compose config lists two ports, so Prometheus always has one
 * target that is not this app (whatever else listens there, or nothing). The
 * view must judge itself by its own target only and report the rest apart,
 * which is what went wrong when a stranger's 302 page on port 8080 was
 * summarised as this app being down.
 */
class ScrapeTargetsTest {

    private static final String TWO_TARGETS = """
            {"status":"success","data":{"activeTargets":[
              {"labels":{"instance":"host.docker.internal:8080","job":"vaadin"},
               "scrapeUrl":"http://host.docker.internal:8080/actuator/prometheus",
               "lastError":"received unsupported Content-Type \\"text/html\\" and no fallback_scrape_protocol specified for target",
               "lastScrape":"2026-09-16T09:00:00.000Z","health":"down"},
              {"labels":{"instance":"host.docker.internal:8082","job":"vaadin"},
               "scrapeUrl":"http://host.docker.internal:8082/actuator/prometheus",
               "lastError":"","lastScrape":"2026-09-16T09:00:00.000Z","health":"up"}
            ]}}
            """;

    private static final String FLY_TARGET = """
            {"status":"success","data":{"activeTargets":[
              {"labels":{"instance":"[fdaa:2:1b2c:a7b:1f2:3c4d:5e6f:2]:8080","job":"vaadin"},
               "scrapeUrl":"http://[fdaa:2:1b2c:a7b:1f2:3c4d:5e6f:2]:8080/actuator/prometheus",
               "lastError":"","lastScrape":"2026-09-16T09:00:00.000Z","health":"up"}
            ]}}
            """;

    private static JsonNode json(String text) {
        return new ObjectMapper().readTree(text);
    }

    @Test
    void judgesThisAppByItsOwnPortOnly() {
        Scrape scrape = MonitoringStackView.readTargets(json(TWO_TARGETS), 8082);

        assertEquals(Health.UP, scrape.health());
        assertTrue(scrape.detail().startsWith("up"), scrape.detail());
        assertFalse(scrape.detail().contains("text/html"),
                "the other target's error must not leak into this app's state: "
                        + scrape.detail());
        assertFalse(scrape.detail().contains("8082"),
                "the address is an implementation detail: " + scrape.detail());
    }

    @Test
    void reportsTheOtherTargetApartWithItsError() {
        Scrape scrape = MonitoringStackView.readTargets(json(TWO_TARGETS), 8082);

        assertEquals(1, scrape.others().size());
        String other = scrape.others().get(0);
        assertTrue(other.startsWith("host.docker.internal:8080 is down"), other);
        assertTrue(other.contains("text/html"), other);
    }

    @Test
    void ownTargetDownIsDownWithItsError() {
        Scrape scrape = MonitoringStackView.readTargets(json(TWO_TARGETS), 8080);

        assertEquals(Health.DOWN, scrape.health());
        assertTrue(scrape.detail().startsWith("down: received unsupported"),
                scrape.detail());
        assertEquals(List.of("host.docker.internal:8082 is up"),
                scrape.others());
    }

    @Test
    void noTargetOnThisPortIsItsOwnVerdict() {
        Scrape scrape = MonitoringStackView.readTargets(json(TWO_TARGETS), 9999);

        assertEquals(Health.NO_TARGET, scrape.health());
        assertTrue(scrape.detail().contains("9999"), scrape.detail());
        assertEquals(2, scrape.others().size());
    }

    @Test
    void singleIpv6TargetOnFlyIsUpWithoutItsAddress() {
        Scrape scrape = MonitoringStackView.readTargets(json(FLY_TARGET), 8080);

        assertEquals(Health.UP, scrape.health());
        assertTrue(scrape.detail().startsWith("up, scraped "), scrape.detail());
        assertFalse(scrape.detail().contains("fdaa"), scrape.detail());
        assertTrue(scrape.others().isEmpty());
    }
}
