package com.snailmailmc.plugin.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/** SnailMail brand colours (from the logo palette) and small message helpers. */
public final class Palette {

    public static final TextColor COCOA = TextColor.color(0x3B2820);
    public static final TextColor CREAM = TextColor.color(0xF9CDAD);
    public static final TextColor PETAL_PINK = TextColor.color(0xF6ACAE);
    public static final TextColor LAVENDER = TextColor.color(0xD5B5DC);
    public static final TextColor SHELL_PURPLE = TextColor.color(0xB986E1);
    public static final TextColor POSTAL_NAVY = TextColor.color(0x5D5891);
    public static final TextColor STAMP_GOLD = TextColor.color(0xFBCC71);
    public static final TextColor MINT = TextColor.color(0xC2F2D0);

    private Palette() {
    }

    public static Component info(String text) {
        return Component.text(text, LAVENDER);
    }

    public static Component good(String text) {
        return Component.text(text, MINT);
    }

    public static Component bad(String text) {
        return Component.text(text, PETAL_PINK);
    }

    public static Component plain(String text, TextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
