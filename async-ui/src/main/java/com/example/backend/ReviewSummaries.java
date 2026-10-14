package com.example.backend;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.vaadin.flow.shared.Registration;

/**
 * A slow writer of review summaries, standing in for a language model asked for
 * an answer in a fixed structure: a headline, a score, then what reviewers like
 * and dislike. The answer arrives part by part, in that order, so a page knows
 * what it will look like long before it is complete.
 */
@Component
public class ReviewSummaries {

    /** One piece of a summary, delivered in the order the summary is read. */
    public sealed interface Part {
    }

    public record Headline(String title, String verdict) implements Part {
    }

    public record Score(double rating, int reviews, int positive, int mixed,
            int negative) implements Part {
    }

    public record Pro(String text) implements Part {
    }

    public record Con(String text) implements Part {
    }

    /** The summary is complete. */
    public record Done() implements Part {
    }

    /** The time until the first part: the model "thinking". */
    public static final Duration FIRST_PART = Duration.ofMillis(1500);
    /** The time between later parts. */
    public static final Duration NEXT_PART = Duration.ofMillis(600);

    private static final Map<String, List<Part>> SUMMARIES = Map.of(
            "Barista espresso grinder", List.of(
                    new Headline("Barista espresso grinder",
                            "A quiet, consistent grinder that rewards "
                                    + "dialling in, let down by its hopper."),
                    new Score(4.6, 1284, 81, 12, 7),
                    new Pro("Even grind from espresso to French press"),
                    new Pro("Noticeably quieter than its rivals"),
                    new Pro("Easy to take apart and clean"),
                    new Pro("Timer remembers two doses"),
                    new Con("Hopper lid rattles when grinding"),
                    new Con("Static makes a mess with dark roasts")),
            "Trail running shoes", List.of(
                    new Headline("Trail running shoes",
                            "Grippy and light, but wears out sooner "
                                    + "than the price suggests."),
                    new Score(3.9, 412, 58, 24, 18),
                    new Pro("Excellent grip on wet rock"),
                    new Pro("Light enough for long races"),
                    new Con("Sole wears through within 500 km"),
                    new Con("Runs half a size small"),
                    new Con("Laces come undone")));

    public static final List<String> PRODUCTS = SUMMARIES.keySet().stream()
            .sorted().toList();

    private final SimulatedLatency latency;

    public ReviewSummaries(SimulatedLatency latency) {
        this.latency = latency;
    }

    /**
     * Writes the summary of {@code product}, delivering each part to
     * {@code onPart} on a background thread, ending with {@link Done}. Removing
     * the registration stops the parts that have not been delivered yet.
     */
    public Registration summarize(String product, Consumer<Part> onPart) {
        List<Part> parts = new ArrayList<>(SUMMARIES.get(product));
        parts.add(new Done());
        Writer writer = new Writer(parts, onPart);
        writer.deliver(0);
        return writer;
    }

    private final class Writer implements Registration {

        private final List<Part> parts;
        private final Consumer<Part> onPart;
        private volatile boolean active = true;
        private volatile @Nullable CompletableFuture<Part> current;

        Writer(List<Part> parts, Consumer<Part> onPart) {
            this.parts = parts;
            this.onPart = onPart;
        }

        void deliver(int index) {
            CompletableFuture<Part> next = latency.after(
                    index == 0 ? FIRST_PART : NEXT_PART,
                    () -> parts.get(index));
            current = next;
            next.thenAccept(part -> {
                if (!active) {
                    return;
                }
                onPart.accept(part);
                if (index + 1 < parts.size()) {
                    deliver(index + 1);
                }
            });
        }

        @Override
        public void remove() {
            active = false;
            CompletableFuture<Part> next = current;
            if (next != null) {
                next.cancel(false);
            }
        }
    }
}
