package com.example;

/**
 * The three states of a value that is loaded in the background: still loading,
 * loaded, or failed. Held in a signal, it lets a view bind its skeleton, its
 * content and its error message to the same source.
 * <p>
 * Flow has no such type; see {@link MissingAPI#load} and API-GAPS.md.
 */
public sealed interface AsyncState<T> {

    record Loading<T>() implements AsyncState<T> {
    }

    record Loaded<T>(T value) implements AsyncState<T> {
    }

    record Failed<T>(Throwable error) implements AsyncState<T> {
    }

    static <T> AsyncState<T> loading() {
        return new Loading<>();
    }

    default boolean isLoading() {
        return this instanceof Loading;
    }

    default boolean isLoaded() {
        return this instanceof Loaded;
    }

    default boolean isFailed() {
        return this instanceof Failed;
    }
}
