package com.example.uc11;

/**
 * Pair of viewport-relative pixel coordinates. Plain record so Jackson can
 * decode the JSON object literal {@code {x, y}} that the custom
 * {@link PointInput} returns from the client.
 */
public record Point(int x, int y) {
}
