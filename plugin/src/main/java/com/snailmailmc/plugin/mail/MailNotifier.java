package com.snailmailmc.plugin.mail;

import static com.snailmailmc.plugin.text.Palette.good;
import static com.snailmailmc.plugin.text.Palette.info;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/** Tells players when the morning post arrives, and about waiting letters when they join. */
public final class MailNotifier implements Listener {

    private final Plugin plugin;
    private final MailStore store;

    public MailNotifier(Plugin plugin, MailStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void start() {
        // Check once a minute for letters whose delivery time has passed.
        Bukkit.getScheduler().runTaskTimer(plugin, this::announceDeliveries, 20L * 10, 20L * 60);
    }

    private void announceDeliveries() {
        for (UUID recipient : store.takeNewlyDelivered(Instant.now())) {
            Player player = Bukkit.getPlayer(recipient);
            if (player != null) {
                player.sendMessage(good("The morning post is here! ").append(info("Type /mail to open your mailbox.")));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            int waiting = store.unopenedCount(player.getUniqueId(), Instant.now());
            if (waiting > 0) {
                player.sendMessage(good("You have " + waiting + " new letter" + (waiting == 1 ? "" : "s") + "! ")
                        .append(info("Type /mail to read " + (waiting == 1 ? "it." : "them."))));
            }
        }, 40L);
    }
}
