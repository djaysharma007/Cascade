package io.github.djaysharma007.cascade;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates WCAG contrast ratio for all text and background token pairs.
 * Minimum contrast ratio is 4.5:1 for normal text (WCAG AA).
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
    void lightSurfacesTextTokensMeetWcagThreshold() {
        Map<String, String> lightBackgrounds = Map.of(
                "paper", "#F4EFE8",
                "cream", "#EEE7DB"
        );

        Map<String, String> lightSurfaceTexts = Map.of(
                "text-primary (ink)", "#102730",
                "text-secondary", "#445963",
                "text-muted", "#546870",
                "status-success", "#206E49",
                "status-warning", "#875500",
                "status-error", "#A12E24"
        );

        for (var bg : lightBackgrounds.entrySet()) {
            for (var text : lightSurfaceTexts.entrySet()) {
                double ratio = contrastRatio(bg.getValue(), text.getValue());
                assertThat(ratio)
                        .withFailMessage("Contrast ratio for %s on %s is %.2f:1, below required 4.5:1",
                                text.getKey(), bg.getKey(), ratio)
                        .isGreaterThanOrEqualTo(4.5);
            }
        }
    }

    @Test
    void darkSurfacesTextTokensMeetWcagThreshold() {
        Map<String, String> darkBackgrounds = Map.of(
                "ink (sidebar/topbar)", "#102730",
                "petrol (primary button)", "#17495A"
        );

        Map<String, String> darkSurfaceTexts = Map.of(
                "text-on-dark (paper)", "#F4EFE8",
                "text-on-dark-secondary (sand)", "#D9BC9A"
        );

        for (var bg : darkBackgrounds.entrySet()) {
            for (var text : darkSurfaceTexts.entrySet()) {
                double ratio = contrastRatio(bg.getValue(), text.getValue());
                assertThat(ratio)
                        .withFailMessage("Contrast ratio for %s on %s is %.2f:1, below required 4.5:1",
                                text.getKey(), bg.getKey(), ratio)
                        .isGreaterThanOrEqualTo(4.5);
            }
        }
    }

    @Test
    void statusChipsTintedPairsMeetWcagThreshold() {
        Map<String, String[]> chipPairs = Map.of(
                "success-chip", new String[]{"#E2EFE7", "#206E49"},
                "warning-chip", new String[]{"#F6EEDA", "#875500"},
                "error-chip", new String[]{"#F7E6E4", "#A12E24"},
                "neutral-chip", new String[]{"#EAE3D6", "#445963"}
        );

        for (var entry : chipPairs.entrySet()) {
            String bg = entry.getValue()[0];
            String text = entry.getValue()[1];
            double ratio = contrastRatio(bg, text);
            assertThat(ratio)
                    .withFailMessage("Contrast ratio for chip %s is %.2f:1, below required 4.5:1",
                            entry.getKey(), ratio)
                    .isGreaterThanOrEqualTo(4.5);
        }
    }
}
