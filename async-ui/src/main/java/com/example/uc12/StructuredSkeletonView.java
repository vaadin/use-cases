package com.example.uc12;

import java.util.Locale;

import com.example.backend.ReviewSummaries;
import com.example.backend.ReviewSummaries.Con;
import com.example.backend.ReviewSummaries.Done;
import com.example.backend.ReviewSummaries.Headline;
import com.example.backend.ReviewSummaries.Part;
import com.example.backend.ReviewSummaries.Pro;
import com.example.backend.ReviewSummaries.Score;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC12 — A skeleton shaped like the answer, filled in part by part.
 * <p>
 * A review summary takes several seconds to write, but its structure is known
 * up front: a headline, a score, and two lists. The card therefore renders at
 * once as placeholder bars in that shape, and each bar is replaced by real
 * content the moment its part arrives through push. Nothing jumps around: the
 * placeholders take about the space of the text they stand for.
 * <p>
 * The lists do not know their length in advance. Each starts with as many
 * placeholder lines as a typical answer has; arriving items replace them one by
 * one, a single line stays while more may follow, and the rest disappear when
 * the next section starts.
 */
@Route(value = "uc12", layout = MainLayout.class)
@PageTitle("UC12 — Skeleton that fills in")
@UseCaseDescription("Showing the shape of a structured answer at once and filling it in part by part")
@Menu(order = 12, title = "UC12 — Skeleton that fills in")
@StyleSheet("uc12.css")
public class StructuredSkeletonView extends VerticalLayout {

    /** The parts of a summary, in the order they arrive. */
    enum Section {
        HEADLINE, SCORE, PROS, CONS, DONE
    }

    /** Placeholder lines per list before its length is known. */
    static final int EXPECTED_ITEMS = 3;

    private final ReviewSummaries summaries;

    private final Select<String> product = new Select<>();
    // The section being written; every earlier one is complete.
    private final ValueSignal<Section> section = new ValueSignal<>(
            Section.HEADLINE);
    private final ValueSignal<@Nullable Headline> headline = new ValueSignal<>(
            null);
    private final ValueSignal<@Nullable Score> score = new ValueSignal<>(null);
    private final ListSignal<String> pros = new ListSignal<>();
    private final ListSignal<String> cons = new ListSignal<>();
    private @Nullable Registration writing;
    private int generation;

    public StructuredSkeletonView(ReviewSummaries summaries) {
        this.summaries = summaries;
        addClassName("uc12-view");

        add(new H1("UC12 — Skeleton that fills in"));
        add(new Paragraph("A summary of a product's reviews takes a few "
                + "seconds to write, but its shape is known from the start. "
                + "The card shows that shape at once and fills in each part "
                + "as it arrives. Pick the other product: its lists are a "
                + "different length, and the placeholders adapt."));

        product.setLabel("Product");
        product.setWidth("16rem");
        product.setItems(ReviewSummaries.PRODUCTS);
        product.setValue(ReviewSummaries.PRODUCTS.getFirst());
        product.addValueChangeListener(event -> summarize());
        Button again = new Button("Summarise again", event -> summarize());

        Span status = new Span();
        status.addClassName("summary-status");
        status.bindText(section.map(
                s -> s == Section.DONE ? "Complete" : "Writing the summary…"));

        HorizontalLayout controls = new HorizontalLayout(product, again,
                status);
        controls.setAlignItems(FlexComponent.Alignment.BASELINE);

        Card card = new Card();
        card.addClassName("summary-card");
        card.addThemeVariants(CardVariant.OUTLINED);
        card.add(headlineSection(), scoreSection(),
                listSection("What reviewers like", Section.PROS, pros),
                listSection("What reviewers dislike", Section.CONS, cons));

        add(controls, card);
        summarize();
    }

    private Component headlineSection() {
        Div skeleton = new Div(bar("bar-title"), bar("bar-line"));
        skeleton.bindVisible(headline.map(h -> h == null));

        H2 title = new H2();
        title.bindText(headline.map(h -> h == null ? "" : h.title()));
        Paragraph verdict = new Paragraph();
        verdict.addClassName("verdict");
        verdict.bindText(headline.map(h -> h == null ? "" : h.verdict()));
        Div content = new Div(title, verdict);
        content.addClassName("streamed");
        content.bindVisible(headline.map(h -> h != null));

        Div section = new Div(skeleton, content);
        section.addClassName("summary-headline");
        return section;
    }

    private Component scoreSection() {
        Div skeleton = new Div(bar("bar-rating"), bar("bar-sentiment"));
        skeleton.addClassName("score-skeleton");
        skeleton.bindVisible(score.map(s -> s == null));

        Span rating = new Span();
        rating.addClassName("rating");
        rating.bindText(score.map(s -> s == null ? ""
                : String.format(Locale.ENGLISH, "%.1f ★", s.rating())));
        Span reviews = new Span();
        reviews.addClassName("reviews");
        reviews.bindText(score.map(s -> s == null ? ""
                : String.format(Locale.ENGLISH,
                        "from %,d reviews: %d%% positive, %d%% mixed, "
                                + "%d%% negative",
                        s.reviews(), s.positive(), s.mixed(), s.negative())));

        Div positive = segment("positive");
        Div mixed = segment("mixed");
        Div negative = segment("negative");
        Div sentiment = new Div(positive, mixed, negative);
        sentiment.addClassName("sentiment");
        Signal.effect(sentiment, () -> {
            Score s = score.get();
            if (s != null) {
                positive.setWidth(s.positive() + "%");
                mixed.setWidth(s.mixed() + "%");
                negative.setWidth(s.negative() + "%");
            }
        });

        Div content = new Div(rating, reviews, sentiment);
        content.addClassName("streamed");
        content.addClassName("score");
        content.bindVisible(score.map(s -> s != null));

        Div section = new Div(skeleton, content);
        section.addClassName("summary-score");
        return section;
    }

    private Component listSection(String title, Section own,
            ListSignal<String> items) {
        Signal<Boolean> complete = section.map(s -> s.compareTo(own) > 0);
        // Before the length is known: the typical length, then one line for
        // "more may follow".
        Signal<Integer> placeholders = Signal.computed(() -> complete.get() ? 0
                : Math.max(1, EXPECTED_ITEMS - items.get().size()));

        UnorderedList list = new UnorderedList();
        list.bindChildren(items, item -> {
            ListItem entry = new ListItem(item.peek());
            entry.addClassName("streamed");
            return entry;
        });

        Div skeleton = new Div();
        skeleton.addClassName("list-skeleton");
        for (int i = 0; i < EXPECTED_ITEMS; i++) {
            int line = i;
            Div placeholder = bar("bar-item");
            placeholder.bindVisible(placeholders.map(n -> line < n));
            skeleton.add(placeholder);
        }

        Span none = new Span("Nothing mentioned");
        none.addClassName("none-mentioned");
        none.bindVisible(
                Signal.computed(() -> complete.get() && items.get().isEmpty()));

        Div section = new Div(new H3(title), list, skeleton, none);
        section.addClassName("summary-list");
        return section;
    }

    private static Div bar(String kind) {
        Div bar = new Div();
        bar.addClassNames("skeleton", "bar", kind);
        return bar;
    }

    private static Div segment(String kind) {
        Div segment = new Div();
        segment.addClassName(kind);
        return segment;
    }

    private void summarize() {
        if (writing != null) {
            writing.remove();
        }
        section.set(Section.HEADLINE);
        headline.set(null);
        score.set(null);
        pros.clear();
        cons.clear();

        UI ui = UI.getCurrent();
        // Removing the registration stops later parts, but one may already be
        // queued in UI.access; the generation drops it there.
        int current = ++generation;
        writing = summaries.summarize(product.getValue(),
                part -> ui.accessLater(() -> {
                    if (current == generation) {
                        apply(part);
                    }
                }, null).run());
    }

    private void apply(Part part) {
        switch (part) {
        case Headline h -> {
            headline.set(h);
            section.set(Section.SCORE);
        }
        case Score s -> {
            score.set(s);
            section.set(Section.PROS);
        }
        case Pro p -> pros.insertLast(p.text());
        case Con c -> {
            section.set(Section.CONS);
            cons.insertLast(c.text());
        }
        case Done d -> section.set(Section.DONE);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (writing != null) {
            writing.remove();
            writing = null;
        }
        super.onDetach(detachEvent);
    }

    // Package-private test seam.
    Section section() {
        return section.peek();
    }
}
