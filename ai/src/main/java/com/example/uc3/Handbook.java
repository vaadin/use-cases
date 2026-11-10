package com.example.uc3;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * The employee handbook, split into one section per heading and indexed by
 * meaning: each section is turned into an embedding vector, so a search for
 * "time off" finds the "Vacation" section even though the words differ.
 * <p>
 * The index lives in memory ({@link SimpleVectorStore}) and is built on the
 * first search, so starting the app calls no AI service. A real application
 * would index its documents into a persistent vector store once, not on
 * startup.
 */
@Component
public class Handbook {

    static final String RESOURCE = "/uc3/employee-handbook.md";

    /**
     * One section of the handbook: its heading and its text.
     */
    public record Section(String title, String text) {
    }

    private final ObjectProvider<EmbeddingModel> embeddingModel;
    private final List<Section> sections;
    private @Nullable SimpleVectorStore index;

    public Handbook(ObjectProvider<EmbeddingModel> embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.sections = parse(read());
    }

    public List<Section> sections() {
        return sections;
    }

    /**
     * Returns the sections closest in meaning to the question, closest first.
     */
    public List<Section> search(String question, int maxResults) {
        return index()
                .similaritySearch(SearchRequest
                        .builder().query(question).topK(maxResults).build())
                .stream()
                .map(document -> section(
                        String.valueOf(document.getMetadata().get("title"))))
                .toList();
    }

    private Section section(String title) {
        return sections.stream().filter(s -> s.title().equals(title))
                .findFirst().orElseThrow();
    }

    private synchronized SimpleVectorStore index() {
        if (index == null) {
            SimpleVectorStore store = SimpleVectorStore
                    .builder(embeddingModel.getObject()).build();
            store.add(
                    sections.stream()
                            .map(section -> new Document(section.text(),
                                    Map.of("title", section.title())))
                            .toList());
            index = store;
        }
        return index;
    }

    /**
     * Splits the Markdown text at its {@code ##} headings. The text before the
     * first heading (the document title) is not a section.
     */
    static List<Section> parse(String markdown) {
        List<Section> result = new ArrayList<>();
        for (String chunk : markdown.split("(?m)^## ")) {
            int end = chunk.indexOf('\n');
            if (chunk.startsWith("# ") || end < 0) {
                continue;
            }
            result.add(new Section(chunk.substring(0, end).trim(),
                    chunk.substring(end + 1).trim()));
        }
        return List.copyOf(result);
    }

    private static String read() {
        try (InputStream in = Handbook.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(RESOURCE + " is missing");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
