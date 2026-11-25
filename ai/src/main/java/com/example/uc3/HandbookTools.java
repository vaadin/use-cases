package com.example.uc3;

import java.util.List;
import java.util.stream.Collectors;

import com.example.ai.ToolCallLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * The handbook search the assistant may call. The model decides when to search
 * and with which words; the application decides what it gets back.
 */
public class HandbookTools {

    static final int MAX_SECTIONS = 3;

    private final Handbook handbook;
    private final ToolCallLog log;

    public HandbookTools(Handbook handbook, ToolCallLog log) {
        this.handbook = handbook;
        this.log = log;
    }

    @Tool(description = "Searches the employee handbook and returns the "
            + "sections most related to the query, each with its title.")
    public String searchHandbook(@ToolParam(description = "What to look "
            + "for, in a few words") String query) {
        List<Handbook.Section> found = handbook.search(query, MAX_SECTIONS);
        log.record("searchHandbook", query,
                found.stream().map(Handbook.Section::title)
                        .collect(Collectors.joining(", ")));
        return found.stream()
                .map(section -> "## " + section.title() + "\n" + section.text())
                .collect(Collectors.joining("\n\n"));
    }
}
