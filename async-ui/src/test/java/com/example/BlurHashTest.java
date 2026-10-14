package com.example;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlurHashTest {

    @Test
    void solidColourDecodesToThatColour() {
        String hash = BlurHash.encode(filled(64, 64, 0x3366cc), 4, 3);

        // The same hash the reference implementation produces.
        assertEquals("L25?~Ep3fQp3p3fkfQfkfQfQfQfQ", hash);
        assertEquals("#3366cc", BlurHash.averageColor(hash));
        BufferedImage decoded = BlurHash.decode(hash, 4, 4);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int rgb = decoded.getRGB(x, y);
                // Close, not exact: a hash is a coarse approximation.
                assertEquals(0x33, red(rgb), 10);
                assertEquals(0x66, (rgb >> 8) & 0xff, 10);
                assertEquals(0xcc, blue(rgb), 10);
            }
        }
    }

    @Test
    void coloursStayRoughlyWhereTheyWere() {
        BufferedImage image = filled(16, 8, 0xff0000);
        for (int y = 0; y < 8; y++) {
            for (int x = 8; x < 16; x++) {
                image.setRGB(x, y, 0x0000ff);
            }
        }

        String hash = BlurHash.encode(image, 4, 3);
        assertEquals("L~LjfL|T$0Js$Awun~WrfQfQfQfQ", hash);
        BufferedImage decoded = BlurHash.decode(hash, 16, 8);
        int left = decoded.getRGB(1, 4);
        int right = decoded.getRGB(14, 4);
        assertTrue(red(left) > blue(left), "left should stay red");
        assertTrue(blue(right) > red(right), "right should stay blue");
    }

    @Test
    void rejectsSomethingThatIsNotAHash() {
        assertThrows(IllegalArgumentException.class,
                () -> BlurHash.decode("abc", 4, 4));
        assertThrows(IllegalArgumentException.class,
                () -> BlurHash.encode(filled(2, 2, 0), 10, 1));
    }

    private static BufferedImage filled(int width, int height, int rgb) {
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    private static int red(int rgb) {
        return (rgb >> 16) & 0xff;
    }

    private static int blue(int rgb) {
        return rgb & 0xff;
    }
}
