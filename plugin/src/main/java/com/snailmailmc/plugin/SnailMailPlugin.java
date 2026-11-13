package com.snailmailmc.plugin;

import com.snailmailmc.plugin.command.SnailMailCommand;
import com.snailmailmc.plugin.platform.Platforms;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the SnailMail plugin. Each town feature (mail, currency,
 * fishing and bugs, museum, seasons, news board) is added as its own module.
 */
public final class SnailMailPlugin extends JavaPlugin {

    private Platforms platforms;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        platforms = new Platforms(getServer());

        SnailMailCommand command = new SnailMailCommand(this);
        var snailmail = getCommand("snailmail");
        if (snailmail != null) {
            snailmail.setExecutor(command);
            snailmail.setTabCompleter(command);
        }

        getLogger().info("SnailMail " + getPluginMeta().getVersion() + " enabled"
                + (platforms.floodgateAvailable() ? " with Bedrock forms." : " (Floodgate not found, Bedrock forms off)."));
    }

    @Override
    public void onDisable() {
        getLogger().info("SnailMail disabled.");
    }

    public Platforms platforms() {
        return platforms;
    }
}
