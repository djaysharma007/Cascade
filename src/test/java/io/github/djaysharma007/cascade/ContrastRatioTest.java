package io.github.djaysharma007.cascade;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates WCAG contrast ratio for all text and background token pairs.
 * Minimum contrast ratio is 4.5:1.
 */
class ContrastRatioTest {

    private record Rgb(double r, double g, double b) {
        static Rgb fromHex(String hex) {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);
            return new Rgb(r / 255.0, g / 255.0, b / 255.0);
        }

        double luminance() {
            return 0.2126 * adjust(r) + 0.7152 * adjust(g) + 0.0722 * adjust(b);
        }

        private static double adjust(double val) {
            return val <= 0.04045 ? val / 12.92 : Math.pow((val + 0.055) / 1.055, 2.4);
        }
    }

    private static double contrastRatio(String hex1, String hex2) {
        double l1 = Rgb.fromHex(hex1).luminance();
        double l2 = Rgb.fromHex(hex2).luminance();
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    @Test
    void allTextAndBackgroundTokenPairsMeetWcagThreshold() {
        Map<String, String> backgrounds = Map.of(
                "bg-base", "#001011",
                "bg-surface", "#061E21",
                "bg-raised", "#093A3E"
        );

        Map<String, String> texts = Map.of(
                "text-primary", "#E6F2F4",
                "text-secondary", "#97C8EB",
                "text-muted", "#86B0B7"
        );

        for (var bg : backgrounds.entrySet()) {
            for (var text : texts.entrySet()) {
                double ratio = contrastRatio(bg.getValue(), text.getValue());
                assertThat(ratio)
                        .withFailMessage("Contrast ratio for %s on %s is %.2f:1, below required 4.5:1",
                                text.getKey(), bg.getKey(), ratio)
                        .isGreaterThanOrEqualTo(4.5);
            }
        }
    }
}
