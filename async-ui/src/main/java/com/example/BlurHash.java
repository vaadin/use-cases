package com.example;

import java.awt.image.BufferedImage;

/**
 * Encoder and decoder for <a href="https://blurha.sh">BlurHash</a>: a photo
 * described in a few dozen characters — a handful of cosine components of its
 * colours — that decodes into a blurry preview with the colours roughly in the
 * right places.
 * <p>
 * An application computes the hash once, when the image is stored, and keeps it
 * next to the image's URL. A page can then paint the preview at once and load
 * the real image afterwards.
 */
public final class BlurHash {

    private static final String DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~";

    private BlurHash() {
    }

    /**
     * Encodes {@code image}. Downscale large images first: the hash only keeps
     * the coarsest structure, and every pixel is visited once per component.
     *
     * @param componentsX
     *            horizontal components, 1 to 9
     * @param componentsY
     *            vertical components, 1 to 9
     */
    public static String encode(BufferedImage image, int componentsX,
            int componentsY) {
        if (componentsX < 1 || componentsX > 9 || componentsY < 1
                || componentsY > 9) {
            throw new IllegalArgumentException(
                    "BlurHash needs 1 to 9 components in each direction");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] factors = new double[componentsX * componentsY][];
        for (int j = 0; j < componentsY; j++) {
            for (int i = 0; i < componentsX; i++) {
                double normalisation = i == 0 && j == 0 ? 1 : 2;
                double r = 0;
                double g = 0;
                double b = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        double basis = normalisation
                                * Math.cos(Math.PI * i * x / width)
                                * Math.cos(Math.PI * j * y / height);
                        int rgb = image.getRGB(x, y);
                        r += basis * srgbToLinear((rgb >> 16) & 0xff);
                        g += basis * srgbToLinear((rgb >> 8) & 0xff);
                        b += basis * srgbToLinear(rgb & 0xff);
                    }
                }
                double scale = 1.0 / (width * height);
                factors[j * componentsX + i] = new double[] { r * scale,
                        g * scale, b * scale };
            }
        }

        StringBuilder hash = new StringBuilder();
        encode83((componentsX - 1) + (componentsY - 1) * 9, 1, hash);
        double maximum = 1;
        if (factors.length > 1) {
            double actualMaximum = 0;
            for (int k = 1; k < factors.length; k++) {
                for (double component : factors[k]) {
                    actualMaximum = Math.max(actualMaximum,
                            Math.abs(component));
                }
            }
            int quantisedMaximum = (int) Math.max(0,
                    Math.min(82, Math.floor(actualMaximum * 166 - 0.5)));
            maximum = (quantisedMaximum + 1) / 166.0;
            encode83(quantisedMaximum, 1, hash);
        } else {
            encode83(0, 1, hash);
        }
        double[] dc = factors[0];
        encode83((linearToSrgb(dc[0]) << 16) + (linearToSrgb(dc[1]) << 8)
                + linearToSrgb(dc[2]), 4, hash);
        for (int k = 1; k < factors.length; k++) {
            double[] ac = factors[k];
            encode83(quantiseAc(ac[0], maximum) * 19 * 19
                    + quantiseAc(ac[1], maximum) * 19
                    + quantiseAc(ac[2], maximum), 2, hash);
        }
        return hash.toString();
    }

    /**
     * Decodes {@code hash} into a {@code width} × {@code height} image. A few
     * dozen pixels across are enough: the browser scales the preview up, and
     * the result is blurry by design.
     */
    public static BufferedImage decode(String hash, int width, int height) {
        int sizeFlag = decode83(hash, 0, 1);
        int componentsX = sizeFlag % 9 + 1;
        int componentsY = sizeFlag / 9 + 1;
        if (hash.length() != 4 + 2 * componentsX * componentsY) {
            throw new IllegalArgumentException("Not a BlurHash: " + hash);
        }
        double maximum = (decode83(hash, 1, 2) + 1) / 166.0;
        double[][] colours = new double[componentsX * componentsY][];
        int dc = decode83(hash, 2, 6);
        colours[0] = new double[] { srgbToLinear(dc >> 16),
                srgbToLinear((dc >> 8) & 0xff), srgbToLinear(dc & 0xff) };
        for (int k = 1; k < colours.length; k++) {
            int ac = decode83(hash, 4 + k * 2, 6 + k * 2);
            colours[k] = new double[] {
                    signPow((ac / (19 * 19) - 9) / 9.0, 2) * maximum,
                    signPow((ac / 19 % 19 - 9) / 9.0, 2) * maximum,
                    signPow((ac % 19 - 9) / 9.0, 2) * maximum };
        }

        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double r = 0;
                double g = 0;
                double b = 0;
                for (int j = 0; j < componentsY; j++) {
                    for (int i = 0; i < componentsX; i++) {
                        double basis = Math.cos(Math.PI * x * i / width)
                                * Math.cos(Math.PI * y * j / height);
                        double[] colour = colours[i + j * componentsX];
                        r += colour[0] * basis;
                        g += colour[1] * basis;
                        b += colour[2] * basis;
                    }
                }
                image.setRGB(x, y, (linearToSrgb(r) << 16)
                        | (linearToSrgb(g) << 8) | linearToSrgb(b));
            }
        }
        return image;
    }

    /**
     * The average colour of the image {@code hash} describes, as a CSS colour
     * such as {@code #d2691e}: the cheapest placeholder there is.
     */
    public static String averageColor(String hash) {
        return "#%06x".formatted(decode83(hash, 2, 6));
    }

    private static int quantiseAc(double value, double maximum) {
        return (int) Math.max(0, Math.min(18,
                Math.floor(signPow(value / maximum, 0.5) * 9 + 9.5)));
    }

    private static double signPow(double value, double exponent) {
        return Math.copySign(Math.pow(Math.abs(value), exponent), value);
    }

    private static double srgbToLinear(int value) {
        double v = value / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static int linearToSrgb(double value) {
        double v = Math.max(0, Math.min(1, value));
        return v <= 0.0031308 ? (int) (v * 12.92 * 255 + 0.5)
                : (int) ((1.055 * Math.pow(v, 1 / 2.4) - 0.055) * 255 + 0.5);
    }

    private static void encode83(int value, int length, StringBuilder target) {
        for (int i = 1; i <= length; i++) {
            int digit = value / (int) Math.pow(83, length - i) % 83;
            target.append(DIGITS.charAt(digit));
        }
    }

    private static int decode83(String hash, int from, int to) {
        int value = 0;
        for (int i = from; i < to; i++) {
            int digit = DIGITS.indexOf(hash.charAt(i));
            if (digit < 0) {
                throw new IllegalArgumentException("Not a BlurHash: " + hash);
            }
            value = value * 83 + digit;
        }
        return value;
    }
}
