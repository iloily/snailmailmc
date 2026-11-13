package com.snailmailmc.plugin.mail;

import static com.snailmailmc.plugin.text.Palette.bad;
import static com.snailmailmc.plugin.text.Palette.good;
import static com.snailmailmc.plugin.text.Palette.info;

import com.snailmailmc.plugin.SnailMailPlugin;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Sending, reading and unwrapping letters. Shared by the Java and Bedrock mailboxes. */
public final class MailService {

    public static final int MAX_LETTER_LENGTH = 500;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final SnailMailPlugin plugin;
    private final MailStore store;

    public MailService(SnailMailPlugin plugin, MailStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public MailStore store() {
        return store;
    }

    public DeliveryClock clock() {
        return new DeliveryClock(plugin.townZone(), plugin.getConfig().getInt("mail.delivery-hour", 6));
    }

    /** Posts a letter, optionally with the item in the sender's main hand. Tells the sender how it went. */
    public void send(Player sender, OfflinePlayer recipient, String body, boolean withHeldItem) {
        if (recipient.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(bad("You can't mail yourself, but it's a lovely thought."));
            return;
        }
        if (body.length() > MAX_LETTER_LENGTH) {
            sender.sendMessage(bad("Letters can be up to " + MAX_LETTER_LENGTH + " characters."));
            return;
        }
        Instant now = Instant.now();
        int limit = plugin.getConfig().getInt("mail.max-in-transit", 10);
        if (store.inTransitFrom(sender.getUniqueId(), now) >= limit) {
            sender.sendMessage(bad("You already have " + limit + " letters on the way. Wait for the morning post."));
            return;
        }

        ItemStack held = sender.getInventory().getItemInMainHand();
        if (withHeldItem && held.isEmpty()) {
            sender.sendMessage(bad("Hold the item you want to send."));
            return;
        }

        long postage = plugin.getConfig().getLong("mail.postage", 0);
        if (postage > 0 && !plugin.shells().withdraw(sender.getUniqueId(), postage)) {
            sender.sendMessage(bad("Postage is " + plugin.shellsFormat().format(postage) + ", and you don't have enough."));
            return;
        }

        byte[] item = null;
        if (withHeldItem) {
            item = held.serializeAsBytes();
            sender.getInventory().setItemInMainHand(null);
        }

        Instant deliverAt = clock().deliveryTime(now);
        store.send(sender.getUniqueId(), sender.getName(), recipient.getUniqueId(), body, item, now, deliverAt);

        var when = deliverAt.atZone(plugin.townZone());
        sender.sendMessage(good("Your letter to " + recipient.getName() + " is in the post")
                .append(info(" and arrives " + DAY.format(when) + " at " + TIME.format(when) + ".")));
    }

    /** Opens a letter as a book, unwraps any gift, and marks it read. Must run on the main thread. */
    public void read(Player player, Letter letter) {
        store.markOpened(letter.id());
        giveGift(player, letter);
        player.openBook(asBook(letter));
    }

    /** Gives the letter's item to the player. Keeps it on the letter if they have no free slot. */
    public void giveGift(Player player, Letter letter) {
        if (!letter.hasGift()) {
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(bad("Your inventory is full. Make room, then open the letter again to take the gift."));
            return;
        }
        store.takeItem(letter.id(), player.getUniqueId()).ifPresent(bytes -> {
            ItemStack gift = ItemStack.deserializeBytes(bytes);
            // There is a free slot, so this always fits; drop anything odd at their feet just in case.
            player.getInventory().addItem(gift).values()
                    .forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
            player.sendMessage(good("You unwrapped the gift from " + letter.senderName() + "."));
        });
    }

    public Book asBook(Letter letter) {
        var delivered = letter.deliverAt().atZone(plugin.townZone());
        List<Component> pages = new ArrayList<>();
        String header = "From " + letter.senderName() + "\n" + DAY.format(delivered) + "\n\n";
        String text = header + letter.body();
        // A book page fits roughly 250 characters.
        for (int i = 0; i < text.length(); i += 250) {
            pages.add(Component.text(text.substring(i, Math.min(text.length(), i + 250))));
        }
        if (pages.isEmpty()) {
            pages.add(Component.text(header));
        }
        return Book.book(Component.text("Letter"), Component.text(letter.senderName()), pages);
    }

    public String deliveredLabel(Letter letter) {
        return DAY.format(letter.deliverAt().atZone(plugin.townZone()));
    }
}
