package org.movsim.utilities;

import java.util.Random;

public class Colors {

    private static final Random random = new Random();

    public static final int color(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | (b << 0);
    }

    public static final int color(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | (b << 0);
    }

    public static final int color(int gray, int a) {
        return (a << 24) | (gray << 16) | (gray << 8) | (gray << 0);
    }

    public static final int color(int gray) {
        return 0xFF000000 | (gray << 16) | (gray << 8) | (gray << 0);
    }

    public static final int alpha(int rgba) {
        return (rgba >> 24) & 0xFF;
    }

    public static final int red(int rgba) {
        return (rgba >> 16) & 0xFF;
    }

    public static final int green(int rgba) {
        return (rgba >> 8) & 0xFF;
    }

    public static final int blue(int rgba) {
        return (rgba >> 0) & 0xFF;
    }

    public static int randomColor() {
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return color(r, g, b);
    }

}
