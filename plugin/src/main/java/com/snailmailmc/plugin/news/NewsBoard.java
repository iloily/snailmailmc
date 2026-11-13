package com.snailmailmc.plugin.news;

import static com.snailmailmc.plugin.text.Palette.bad;
import static com.snailmailmc.plugin.text.Palette.good;
import static com.snailmailmc.plugin.text.Palette.info;
import static com.snailmailmc.plugin.text.Palette.plain;

import com.snailmailmc.plugin.SnailMailPlugin;
import com.snailmailmc.plugin.text.Palette;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * The town news board.
 *
 * /news                          open the board
 * /news post <title> | <text>    post news (snailmail.news)
 * /news remove <id>              take a post down (snailmail.news)
 * /news bind | unbind            make the block you're looking at open the board when clicked (snailmail.news)
 */
public final class NewsBoard implements TabExecutor, Listener {

    private static final int SHOWN = 45;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);

    private final SnailMailPlugin plugin;
    private final NewsStore store;
    private final Set<String> boards = new HashSet<>();

    public NewsBoard(SnailMailPlugin plugin, NewsStore store) {
        this.plugin = plugin;
        this.store = store;
        boards.addAll(store.allBoards());
    }

    // Opening the board.

    public void open(Player player) {
        List<NewsPost> posts = store.recent(SHOWN);
        if (plugin.platforms().isBedrock(player.getUniqueId())) {
            openBedrock(player, posts);
        } else {
            openJava(player, posts);
        }
    }

    private void openJava(Player player, List<NewsPost> posts) {
        Holder holder = new Holder(posts);
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text("Town news", Palette.POSTAL_NAVY));
        holder.inventory = inventory;
        for (int i = 0; i < posts.size(); i++) {
            inventory.setItem(i, icon(posts.get(i), i == 0));
        }
        if (posts.isEmpty()) {
            ItemStack empty = new ItemStack(Material.FEATHER);
            empty.editMeta(meta -> meta.displayName(plain("No news yet", Palette.LAVENDER)));
            inventory.setItem(22, empty);
        }
        player.openInventory(inventory);
    }

    private ItemStack icon(NewsPost post, boolean newest) {
        ItemStack item = new ItemStack(newest ? Material.WRITTEN_BOOK : Material.PAPER);
        item.editMeta(meta -> {
            meta.displayName(plain(post.title(), newest ? Palette.STAMP_GOLD : Palette.LAVENDER));
            List<Component> lore = new ArrayList<>();
            lore.add(plain(day(post) + " - " + post.author(), Palette.CREAM));
            String preview = post.body().length() > 40 ? post.body().substring(0, 40) + "..." : post.body();
            lore.add(plain(preview, Palette.CREAM));
            lore.add(plain("Click to read", Palette.MINT));
            meta.lore(lore);
        });
        return item;
    }

    private void openBedrock(Player player, List<NewsPost> posts) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("Town news")
                .content(posts.isEmpty() ? "No news yet." : "Tap a post to read it.");
        posts.forEach(post -> form.button(post.title() + "\n" + day(post)));
        form.validResultHandler(response -> Bukkit.getScheduler().runTask(plugin, () -> {
            NewsPost post = posts.get(response.clickedButtonId());
            SimpleForm article = SimpleForm.builder()
                    .title(post.title())
                    .content(day(post) + " - " + post.author() + "\n\n" + post.body())
                    .button("Back to the news")
                    .validResultHandler(r -> Bukkit.getScheduler().runTask(plugin, () -> open(player)))
                    .build();
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), article);
        }));
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    private Book book(NewsPost post) {
        String text = post.title() + "\n" + day(post) + " - " + post.author() + "\n\n" + post.body();
        List<Component> pages = new ArrayList<>();
        for (int i = 0; i < text.length(); i += 250) {
            pages.add(Component.text(text.substring(i, Math.min(text.length(), i + 250))));
        }
        return Book.book(Component.text(post.title()), Component.text(post.author()), pages);
    }

    private String day(NewsPost post) {
        return DAY.format(post.postedAt().atZone(plugin.townZone()));
    }

    // Commands.

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                open(player);
            } else {
                store.recent(5).forEach(p -> sender.sendMessage(info("#" + p.id() + " " + p.title() + ": " + p.body())));
            }
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (!sender.hasPermission("snailmail.news")) {
            sender.sendMessage(bad("Only town staff can change the news board."));
            return true;
        }
        switch (action) {
            case "post" -> post(sender, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            case "remove" -> {
                long id = args.length == 2 ? parseId(args[1]) : -1;
                sender.sendMessage(id > 0 && store.remove(id) ? good("Took down post #" + id + ".") : bad("Usage: /news remove <id>"));
            }
            case "bind", "unbind" -> bind(sender, action.equals("bind"));
            default -> sender.sendMessage(info("/news post <title> | <text>, /news remove <id>, /news bind, /news unbind"));
        }
        return true;
    }

    private void post(CommandSender sender, String raw) {
        int bar = raw.indexOf('|');
        if (bar <= 0 || bar == raw.length() - 1) {
            sender.sendMessage(bad("Usage: /news post <title> | <text>"));
            return;
        }
        String title = raw.substring(0, bar).trim();
        String body = raw.substring(bar + 1).trim();
        if (title.length() > 40) {
            sender.sendMessage(bad("Keep the title to 40 characters."));
            return;
        }
        long id = store.post(title, body, sender.getName(), Instant.now());
        sender.sendMessage(good("Posted #" + id + "."));
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(
                good("New on the news board: ").append(info(title + " - type /news to read it."))));
    }

    private void bind(CommandSender sender, boolean bind) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(bad("Look at a block in game to do that."));
            return;
        }
        Block block = player.getTargetBlockExact(6);
        if (block == null || block.getType().isAir()) {
            sender.sendMessage(bad("Look at the board block (within 6 blocks) and try again."));
            return;
        }
        String world = block.getWorld().getName();
        boolean changed = bind
                ? store.bindBoard(world, block.getX(), block.getY(), block.getZ())
                : store.unbindBoard(world, block.getX(), block.getY(), block.getZ());
        String key = NewsStore.key(world, block.getX(), block.getY(), block.getZ());
        if (bind) {
            boards.add(key);
        } else {
            boards.remove(key);
        }
        sender.sendMessage(changed
                ? good(bind ? "Clicking that block now opens the news board." : "That block no longer opens the news board.")
                : info(bind ? "That block already opens the news board." : "That block wasn't a news board."));
    }

    private static long parseId(String raw) {
        try {
            return Long.parseLong(raw.replace("#", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("snailmail.news")) {
            return List.of("post", "remove", "bind", "unbind").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }

    // World and menu events.

    @EventHandler(ignoreCancelled = false)
    public void onBoardClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null || boards.isEmpty()) {
            return;
        }
        Block block = event.getClickedBlock();
        if (boards.contains(NewsStore.key(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()))) {
            event.setCancelled(true);
            open(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBoardBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (boards.contains(NewsStore.key(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()))) {
            if (event.getPlayer().hasPermission("snailmail.news") && event.getPlayer().isSneaking()) {
                bind(event.getPlayer(), false);
            } else {
                event.setCancelled(true);
                event.getPlayer().sendMessage(info("That's the news board. Staff can sneak and break it to remove it."));
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int fresh = store.countSince(Instant.now().minusSeconds(24 * 3600));
        if (fresh > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(info("There's fresh news on the board today. Type /news to read it."));
                }
            }, 60L);
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != holder.inventory) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < holder.posts.size()) {
            NewsPost post = holder.posts.get(slot);
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> player.openBook(book(post)));
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static final class Holder implements InventoryHolder {
        private final List<NewsPost> posts;
        private Inventory inventory;

        Holder(List<NewsPost> posts) {
            this.posts = posts;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
