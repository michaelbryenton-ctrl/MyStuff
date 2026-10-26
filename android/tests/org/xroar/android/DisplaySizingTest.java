package org.xroar.android;

public final class DisplaySizingTest {
    private static int checks;

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(DisplaySizing.displayHeight(false, 800, 800, 96) == 400,
                "normal display stays in top half");
        check(DisplaySizing.displayHeight(true, 800, 800, 48) == 752,
                "expanded display reserves only compact strip");
        check(DisplaySizing.displayHeight(true, 800, 500, 48) == 452,
                "expanded display clears keyboard");
        check(DisplaySizing.displayHeight(false, 800, 420, 140) == 280,
                "normal rows clear keyboard");
        check(DisplaySizing.displayHeight(true, 30, 30, 48) == 1,
                "tiny windows remain measurable");
        check(DisplaySizing.displayHeight(true, 780, 760, 48) == 712,
                "safe area padding is excluded");
        int[][] windows = {{1080, 1872}, {1920, 1032}, {1280, 672},
                {800, 1232}, {600, 352}, {320, 192}, {2048, 1488}};
        for (int[] window : windows) {
            DisplaySizing picture = new DisplaySizing(window[0], window[1]);
            check(picture.width <= window[0] && picture.height <= window[1],
                    "picture fits phone/tablet/landscape");
            check(Math.abs(picture.width * 3 - picture.height * 4) <= 3,
                    "4:3 aspect within integer rounding");
            check(picture.left == (window[0] - picture.width) / 2
                    && picture.top == (window[1] - picture.height) / 2, "centred picture");
        }
        for (int w = 4; w <= 2400; w += 19) {
            for (int h = 3; h <= 2400; h += 23) {
                DisplaySizing picture = new DisplaySizing(w, h);
                check(picture.width > 0 && picture.width <= w
                        && picture.height > 0 && picture.height <= h, "no crop");
                check(Math.abs(picture.width * 3 - picture.height * 4) <= 3, "no stretch");
                check(picture.left >= 0 && picture.top >= 0, "no negative bounds");
            }
        }
        System.out.println("PASS: " + checks + " full-screen sizing checks");
    }
}
