package com.example.uc6;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvParserTest {

    @Test
    void byteOrderMarkIsNotPartOfTheFirstHeader() {
        List<CsvParser.Row> rows = parse("﻿SKU,Name\nA-1,Pen\n");

        assertEquals(List.of("SKU", "Name"), rows.get(0).fields());
    }

    @Test
    void blankLinesAreSkipped_butStillCountTowardsTheLineNumbers() {
        List<CsvParser.Row> rows = parse("SKU,Name\n\nA-1,Pen\n , \nB-2,Ink\n");

        assertEquals(List.of(1, 3, 5),
                rows.stream().map(CsvParser.Row::line).toList());
    }

    @Test
    void loneCarriageReturnsEndLinesLikeLineFeeds() {
        List<CsvParser.Row> rows = parse("SKU,Name\rA-1,Pen\r\rB-2,Ink");

        assertEquals(List.of(1, 2, 4),
                rows.stream().map(CsvParser.Row::line).toList());
        assertEquals(List.of("B-2", "Ink"), rows.get(2).fields());
    }

    @Test
    void lineBreakInsideAQuotedFieldMovesTheNextRowDown() {
        List<CsvParser.Row> rows = parse("SKU,Name\nA-1,\"Pen\nblue\"\nB-2,Ink\n");

        assertEquals("Pen\nblue", rows.get(1).fields().get(1));
        assertEquals(4, rows.get(2).line());
    }

    private static List<CsvParser.Row> parse(String text) {
        return CsvParser.parse(text.getBytes(StandardCharsets.UTF_8));
    }
}
