package com.snailmailmc.plugin.command;

import com.snailmailmc.plugin.SnailMailPlugin;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /snailmail: shows plugin info, and lets admins reload the config. */
public final class SnailMailCommand implements TabExecutor {

    private static final TextColor LAVENDER = TextColor.color(0xD5B5DC);
    private static final TextColor CREAM = TextColor.color(0xF9CDAD);

    private final SnailMailPlugin plugin;

    public SnailMailCommand(SnailMailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("snailmail.admin")) {
                sender.sendMessage(Component.text("You don't have permission to do that.", CREAM));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(Component.text("SnailMail config reloaded.", LAVENDER));
            return true;
        }

        String edition = sender instanceof Player p && plugin.platforms().isBedrock(p.getUniqueId())
                ? "Bedrock" : "Java";
        sender.sendMessage(Component.text("SnailMail " + plugin.getPluginMeta().getVersion(), LAVENDER)
                .append(Component.text(" · playing on " + edition, CREAM)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("snailmail.admin")) {
            return List.of("reload");
        }
        return List.of();
    }
}
