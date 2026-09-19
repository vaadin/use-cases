package com.example.print;

/**
 * The paper sizes the print-preview use case offers, with their dimensions in
 * millimetres so that a preview can be drawn at the right aspect ratio.
 */
public enum Paper {

    A4("A4", 210, 297), A5("A5", 148, 210), LETTER("Letter", 216, 279);

    private final String label;
    private final int widthMm;
    private final int heightMm;

    Paper(String label, int widthMm, int heightMm) {
        this.label = label;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
    }

    /** The name the CSS {@code size} descriptor and the UI both use. */
    public String label() {
        return label;
    }

    /** The short edge, in millimetres. */
    public int widthMm() {
        return widthMm;
    }

    /** The long edge, in millimetres. */
    public int heightMm() {
        return heightMm;
    }
}
