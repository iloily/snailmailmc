package com.snailmailmc.plugin.mail;

import static com.snailmailmc.plugin.text.Palette.plain;

import com.snailmailmc.plugin.text.Palette;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/** The chest-style mailbox Java players see. Click a letter to read it. */
public final class JavaMailbox implements Listener {

    private static final int SIZE = 54;

    private final Plugin plugin;
    private final MailService mail;

    public JavaMailbox(Plugin plugin, MailService mail) {
        this.plugin = plugin;
        this.mail = mail;
    }

    public void open(Player player) {
        List<Letter> letters = mail.store().mailbox(player.getUniqueId(), Instant.now());
        Holder holder = new Holder(letters.subList(0, Math.min(SIZE, letters.size())));
        Inventory inventory = Bukkit.createInventory(holder, SIZE, Component.text("Your mailbox", Palette.POSTAL_NAVY));
        holder.inventory = inventory;
        for (int slot = 0; slot < holder.letters.size(); slot++) {
            inventory.setItem(slot, icon(holder.letters.get(slot)));
        }
        if (letters.isEmpty()) {
            ItemStack empty = new ItemStack(Material.FEATHER);
            empty.editMeta(meta -> {
                meta.displayName(plain("No letters yet", Palette.LAVENDER));
                meta.lore(List.of(plain("Mail arrives every morning.", Palette.CREAM)));
            });
            inventory.setItem(22, empty);
        }
        player.openInventory(inventory);
    }

    private ItemStack icon(Letter letter) {
        ItemStack item = new ItemStack(letter.hasGift() ? Material.BUNDLE : letter.opened() ? Material.PAPER : Material.WRITABLE_BOOK);
        item.editMeta(meta -> {
            meta.displayName(plain((letter.opened() ? "" : "New! ") + "From " + letter.senderName(),
                    letter.opened() ? Palette.LAVENDER : Palette.STAMP_GOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(plain("Delivered " + mail.deliveredLabel(letter), Palette.CREAM));
            String preview = letter.body().length() > 40 ? letter.body().substring(0, 40) + "..." : letter.body();
            if (!preview.isBlank()) {
                lore.add(plain("\"" + preview + "\"", Palette.CREAM));
            }
            if (letter.hasGift()) {
                lore.add(plain("Has a gift inside!", Palette.PETAL_PINK));
            }
            lore.add(plain("Click to read", Palette.MINT));
            meta.lore(lore);
        });
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != holder.inventory) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= holder.letters.size()) {
            return;
        }
        long id = holder.letters.get(slot).id();
        player.closeInventory();
        // Re-read the letter so a gift can't be taken twice from a stale menu.
        Bukkit.getScheduler().runTask(plugin, () -> mail.store().find(id).ifPresent(letter -> mail.read(player, letter)));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static final class Holder implements InventoryHolder {
        private final List<Letter> letters;
        private Inventory inventory;

        Holder(List<Letter> letters) {
            this.letters = letters;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
