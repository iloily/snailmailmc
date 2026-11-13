package com.snailmailmc.plugin.shells;

import org.bukkit.configuration.ConfigurationSection;

/** "1 Shell", "12 Shells". Names come from config so the currency can be renamed. */
public record ShellsFormat(String singular, String plural) {

    public static ShellsFormat from(ConfigurationSection currency) {
        return new ShellsFormat(
                currency.getString("singular", "Shell"),
                currency.getString("plural", "Shells"));
    }

    public String format(long amount) {
        return amount + " " + (amount == 1 ? singular : plural);
    }

    /** Parses a whole, positive amount. Returns -1 if it isn't one. */
    public static long parseAmount(String raw) {
        try {
            long value = Long.parseLong(raw.replace(",", "").replace("_", ""));
            return value > 0 ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
