package com.example.print;

/**
 * Paper size, orientation and margins — everything the CSS page box can be
 * told, and the numbers a preview needs in order to show the same thing on
 * screen.
 *
 * @param paper
 *            the paper size
 * @param landscape
 *            whether the paper is rotated
 * @param marginMm
 *            the margin on all four sides, in millimetres
 */
public record PageSetup(Paper paper, boolean landscape, int marginMm) {

    /** A4 portrait with 15 mm margins. */
    public static final PageSetup DEFAULT = new PageSetup(Paper.A4, false, 15);

    /**
     * The rule to hand to
     * {@link com.example.MissingAPI#setPageRule(com.vaadin.flow.component.UI, String)}.
     *
     * @return the {@code @page} rule for this setup
     */
    public String toPageRule() {
        return "@page { size: %s %s; margin: %dmm; }".formatted(paper.label(),
                landscape ? "landscape" : "portrait", marginMm);
    }

    /** The width of the sheet as the printer sees it, in millimetres. */
    public int sheetWidthMm() {
        return landscape ? paper.heightMm() : paper.widthMm();
    }

    /** The height of the sheet as the printer sees it, in millimetres. */
    public int sheetHeightMm() {
        return landscape ? paper.widthMm() : paper.heightMm();
    }

    /** The width left for content once the margins are taken off. */
    public int contentWidthMm() {
        return sheetWidthMm() - 2 * marginMm;
    }

    /** The height left for content once the margins are taken off. */
    public int contentHeightMm() {
        return sheetHeightMm() - 2 * marginMm;
    }
}
