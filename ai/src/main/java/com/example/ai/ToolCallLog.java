package com.example.ai;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Lists the application methods the AI called during the conversation, with
 * what it passed and what it got back, so the reader can check an answer
 * against the data it was built from.
 * <p>
 * Tools run on the thread of the LLM provider, not on the UI thread, so
 * {@link #record} only writes to a signal; the list follows the signal.
 */
public class ToolCallLog extends VerticalLayout {

    /**
     * One call the AI made: the tool's name, its arguments and its result.
     */
    public record ToolCall(String tool, String input, String output) {
    }

    private static final int MAX_OUTPUT_LENGTH = 600;

    private final ValueSignal<List<ToolCall>> calls = new ValueSignal<>(
            List.of());

    public ToolCallLog(String heading, String emptyText) {
        addClassName("tool-call-log");
        setPadding(false);
        setSpacing(false);
        Div entries = new Div();
        entries.addClassName("tool-call-entries");
        add(new H2(heading), entries);

        Signal.effect(this, () -> {
            entries.removeAll();
            List<ToolCall> current = calls.get();
            if (current.isEmpty()) {
                entries.add(new Paragraph(emptyText));
            }
            current.forEach(call -> entries.add(entry(call)));
        });
    }

    /**
     * Adds a call to the log. Safe to call from any thread.
     */
    public void record(String tool, String input, String output) {
        ToolCall call = new ToolCall(tool, input, output);
        calls.update(current -> {
            List<ToolCall> next = new ArrayList<>(current);
            next.add(call);
            return List.copyOf(next);
        });
    }

    public List<ToolCall> getCalls() {
        return calls.peek();
    }

    private static Div entry(ToolCall call) {
        Span name = new Span(call.tool() + "(" + call.input() + ")");
        name.addClassName("tool-call-name");
        String output = call.output().length() > MAX_OUTPUT_LENGTH
                ? call.output().substring(0, MAX_OUTPUT_LENGTH) + " …"
                : call.output();
        Pre result = new Pre(output);
        result.addClassName("tool-call-output");
        Div entry = new Div(name, result);
        entry.addClassName("tool-call");
        return entry;
    }
}
