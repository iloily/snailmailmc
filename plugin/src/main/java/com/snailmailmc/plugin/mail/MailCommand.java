package com.snailmailmc.plugin.mail;

import static com.snailmailmc.plugin.text.Palette.bad;
import static com.snailmailmc.plugin.text.Palette.info;

import com.snailmailmc.plugin.SnailMailPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * /mail                         open your mailbox
 * /mail send <player> <letter>  post a letter
 * /mail gift <player> [letter]  post the item in your hand, with an optional note
 */
public final class MailCommand implements TabExecutor {

    private final SnailMailPlugin plugin;
    private final MailService mail;
    private final JavaMailbox javaMailbox;
    private final BedrockMailbox bedrockMailbox; // null when Floodgate isn't installed

    public MailCommand(SnailMailPlugin plugin, MailService mail, JavaMailbox javaMailbox, BedrockMailbox bedrockMailbox) {
        this.plugin = plugin;
        this.mail = mail;
        this.javaMailbox = javaMailbox;
        this.bedrockMailbox = bedrockMailbox;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(bad("Only players have mailboxes."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            if (bedrockMailbox != null && plugin.platforms().isBedrock(player.getUniqueId())) {
                bedrockMailbox.open(player);
            } else {
                javaMailbox.open(player);
            }
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        boolean gift = action.equals("gift");
        if ((action.equals("send") && args.length >= 3) || (gift && args.length >= 2)) {
            OfflinePlayer recipient = Bukkit.getOfflinePlayerIfCached(args[1]);
            if (recipient == null || !plugin.shells().hasAccount(recipient.getUniqueId())) {
                player.sendMessage(bad("No player called " + args[1] + " has joined SnailMail yet."));
                return true;
            }
            String body = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            mail.send(player, recipient, body, gift);
            return true;
        }
        player.sendMessage(info("/mail - open your mailbox"));
        player.sendMessage(info("/mail send <player> <letter> - post a letter"));
        player.sendMessage(info("/mail gift <player> [note] - post the item in your hand"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("send", "gift", "open").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("send") || args[0].equalsIgnoreCase("gift"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }
        return List.of();
    }
}
