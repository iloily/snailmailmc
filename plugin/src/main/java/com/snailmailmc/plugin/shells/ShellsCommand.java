package com.snailmailmc.plugin.shells;

import static com.snailmailmc.plugin.text.Palette.bad;
import static com.snailmailmc.plugin.text.Palette.good;
import static com.snailmailmc.plugin.text.Palette.info;

import com.snailmailmc.plugin.SnailMailPlugin;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * /shells               your balance
 * /shells <player>      someone else's balance
 * /shells give|take|set <player> <amount>   admin tools
 * /pay <player> <amount>
 */
public final class ShellsCommand implements TabExecutor {

    private static final List<String> ADMIN_ACTIONS = List.of("give", "take", "set");

    private final SnailMailPlugin plugin;

    public ShellsCommand(SnailMailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("pay")) {
            return pay(sender, args);
        }
        ShellsLedger ledger = plugin.shells();
        ShellsFormat fmt = plugin.shellsFormat();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(bad("Usage: /shells <player>"));
                return true;
            }
            sender.sendMessage(info("You have ").append(good(fmt.format(ledger.balance(player.getUniqueId())))).append(info(".")));
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (ADMIN_ACTIONS.contains(action) && args.length == 3) {
            if (!sender.hasPermission("snailmail.admin")) {
                sender.sendMessage(bad("You don't have permission to do that."));
                return true;
            }
            OfflinePlayer target = known(sender, args[1]);
            long amount = action.equals("set") ? parseNonNegative(args[2]) : ShellsFormat.parseAmount(args[2]);
            if (target == null) {
                return true;
            }
            if (amount < 0) {
                sender.sendMessage(bad("Amount must be a whole number" + (action.equals("set") ? "." : " above 0.")));
                return true;
            }
            switch (action) {
                case "give" -> ledger.deposit(target.getUniqueId(), amount);
                case "take" -> {
                    if (!ledger.withdraw(target.getUniqueId(), amount)) {
                        sender.sendMessage(bad(target.getName() + " only has " + fmt.format(ledger.balance(target.getUniqueId())) + "."));
                        return true;
                    }
                }
                default -> ledger.set(target.getUniqueId(), amount);
            }
            sender.sendMessage(good(target.getName() + " now has " + fmt.format(ledger.balance(target.getUniqueId())) + "."));
            return true;
        }

        if (args.length == 1) {
            OfflinePlayer target = known(sender, args[0]);
            if (target != null) {
                sender.sendMessage(info(target.getName() + " has ").append(good(fmt.format(ledger.balance(target.getUniqueId())))).append(info(".")));
            }
            return true;
        }

        sender.sendMessage(bad("Usage: /shells [player]" + (sender.hasPermission("snailmail.admin") ? " or /shells give|take|set <player> <amount>" : "")));
        return true;
    }

    private boolean pay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(bad("Only players can pay."));
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(bad("Usage: /pay <player> <amount>"));
            return true;
        }
        OfflinePlayer target = known(sender, args[0]);
        if (target == null) {
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(bad("You can't pay yourself."));
            return true;
        }
        long amount = ShellsFormat.parseAmount(args[1]);
        if (amount < 0) {
            sender.sendMessage(bad("Amount must be a whole number above 0."));
            return true;
        }
        ShellsFormat fmt = plugin.shellsFormat();
        if (!plugin.shells().transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            sender.sendMessage(bad("You only have " + fmt.format(plugin.shells().balance(player.getUniqueId())) + "."));
            return true;
        }
        sender.sendMessage(good("You paid " + target.getName() + " " + fmt.format(amount) + "."));
        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage(good(player.getName() + " paid you " + fmt.format(amount) + "."));
        }
        return true;
    }

    private OfflinePlayer known(CommandSender sender, String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
        if (target == null || !plugin.shells().hasAccount(target.getUniqueId())) {
            sender.sendMessage(bad("No player called " + name + " has joined SnailMail yet."));
            return null;
        }
        return target;
    }

    private static long parseNonNegative(String raw) {
        if (raw.equals("0")) {
            return 0;
        }
        return ShellsFormat.parseAmount(raw);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean pay = command.getName().equalsIgnoreCase("pay");
        if (args.length == 1) {
            List<String> options = new java.util.ArrayList<>(onlineNames(args[0]));
            if (!pay && sender.hasPermission("snailmail.admin")) {
                ADMIN_ACTIONS.stream().filter(a -> a.startsWith(args[0].toLowerCase(Locale.ROOT))).forEach(options::add);
            }
            return options;
        }
        if (args.length == 2 && !pay && ADMIN_ACTIONS.contains(args[0].toLowerCase(Locale.ROOT))) {
            return onlineNames(args[1]);
        }
        return List.of();
    }

    static List<String> onlineNames(String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
