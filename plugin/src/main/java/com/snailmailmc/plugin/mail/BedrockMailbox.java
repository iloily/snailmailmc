package com.snailmailmc.plugin.mail;

import java.time.Instant;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

/** The mailbox Bedrock players see: native forms instead of a chest menu. */
public final class BedrockMailbox {

    private final Plugin plugin;
    private final MailService mail;

    public BedrockMailbox(Plugin plugin, MailService mail) {
        this.plugin = plugin;
        this.mail = mail;
    }

    public void open(Player player) {
        List<Letter> letters = mail.store().mailbox(player.getUniqueId(), Instant.now());
        List<Letter> shown = letters.subList(0, Math.min(50, letters.size()));
        SimpleForm.Builder form = SimpleForm.builder()
                .title("Your mailbox")
                .content(letters.isEmpty() ? "No letters yet. Mail arrives every morning." : "Tap a letter to read it.");
        for (Letter letter : shown) {
            String status = letter.hasGift() ? "Gift inside!" : letter.opened() ? mail.deliveredLabel(letter) : "New!";
            form.button("From " + letter.senderName() + "\n" + status);
        }
        form.validResultHandler(response -> onMain(() -> {
            Letter picked = shown.get(response.clickedButtonId());
            mail.store().find(picked.id()).ifPresent(letter -> showLetter(player, letter));
        }));
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void showLetter(Player player, Letter letter) {
        mail.store().markOpened(letter.id());
        SimpleForm.Builder form = SimpleForm.builder()
                .title("From " + letter.senderName())
                .content(mail.deliveredLabel(letter) + "\n\n" + letter.body());
        boolean gift = letter.hasGift();
        if (gift) {
            form.button("Unwrap the gift");
        }
        form.button("Back to mailbox");
        form.validResultHandler(response -> onMain(() -> {
            if (gift && response.clickedButtonId() == 0) {
                mail.giveGift(player, letter);
            } else {
                open(player);
            }
        }));
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private void onMain(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
