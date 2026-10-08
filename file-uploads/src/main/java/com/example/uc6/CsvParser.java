package com.example.uc6;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Just enough CSV for files saved by a spreadsheet: comma- or
 * semicolon-separated (whichever the header line uses), quoted fields with
 * {@code ""} escapes and line breaks, an optional byte-order mark, and UTF-8
 * with a fallback to Windows-1252 for files that are not valid UTF-8.
 */
final class CsvParser {

    /** One record and the line in the file where it starts. */
    record Row(int line, List<String> fields) {
    }

    private CsvParser() {
    }

    static List<Row> parse(byte[] bytes) {
        String text = decode(bytes);
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        char separator = detectSeparator(text);

        List<Row> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int line = 1;
        int rowStart = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length()
                        && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == separator) {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length()
                        && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(field.toString());
                field.setLength(0);
                addUnlessBlank(rows, rowStart, fields);
                fields = new ArrayList<>();
                line++;
                rowStart = line;
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        addUnlessBlank(rows, rowStart, fields);
        return rows;
    }

    private static void addUnlessBlank(List<Row> rows, int line,
            List<String> fields) {
        if (fields.stream().anyMatch(f -> !f.isBlank())) {
            rows.add(new Row(line, List.copyOf(fields)));
        }
    }

    private static char detectSeparator(String text) {
        int end = text.indexOf('\n');
        String header = end < 0 ? text : text.substring(0, end);
        return header.chars().filter(c -> c == ';').count() > header.chars()
                .filter(c -> c == ',').count() ? ';' : ',';
    }

    private static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }
}
