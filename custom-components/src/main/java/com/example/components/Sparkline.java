package com.example.components;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.shared.HasThemeVariant;
import com.vaadin.flow.dom.Element;

/**
 * A small line chart drawn as SVG on the server (UC4).
 * <p>
 * Built with the Element API alone: no JavaScript, no client-side dependency.
 * Setting new values rewrites the polyline's points, and Flow sends just that
 * attribute to the browser. The line takes its color from {@code currentColor},
 * so the theme variants and the surrounding text color decide it.
 */
@Tag("svg")
@StyleSheet("sparkline.css")
public class Sparkline extends Component
        implements HasSize, HasThemeVariant<SparklineVariant> {

    static final int WIDTH = 100;
    static final int HEIGHT = 30;

    private final Element line = new Element("polyline");
    private final Element area = new Element("polygon");
    private List<Double> values = List.of();

    public Sparkline() {
        getElement().setAttribute("viewBox", "0 0 " + WIDTH + " " + HEIGHT)
                .setAttribute("preserveAspectRatio", "none")
                .setAttribute("class", "sparkline").setAttribute("role", "img");
        area.setAttribute("class", "area");
        line.setAttribute("class", "line");
        getElement().appendChild(area, line);
        setWidth("8rem");
        setHeight("2rem");
    }

    public Sparkline(List<Double> values, String description) {
        this();
        setValues(values);
        setDescription(description);
    }

    public void setValues(List<Double> values) {
        this.values = List.copyOf(values);
        String points = points(this.values);
        line.setAttribute("points", points);
        area.setAttribute("points", values.isEmpty() ? ""
                : "0," + HEIGHT + " " + points + " " + WIDTH + "," + HEIGHT);
    }

    public List<Double> getValues() {
        return values;
    }

    /** What the chart shows, for screen readers. */
    public void setDescription(String description) {
        getElement().setAttribute("aria-label", description);
    }

    /** The polyline points for the values, scaled to the view box. */
    static String points(List<Double> values) {
        if (values.isEmpty()) {
            return "";
        }
        double min = values.stream().mapToDouble(Double::doubleValue).min()
                .orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max()
                .orElse(0);
        double range = max == min ? 1 : max - min;
        double step = values.size() == 1 ? 0
                : (double) WIDTH / (values.size() - 1);
        return IntStream.range(0, values.size())
                .mapToObj(i -> String.format(Locale.ROOT, "%.1f,%.1f", i * step,
                        HEIGHT - 2
                                - (values.get(i) - min) / range * (HEIGHT - 4)))
                .collect(Collectors.joining(" "));
    }
}
