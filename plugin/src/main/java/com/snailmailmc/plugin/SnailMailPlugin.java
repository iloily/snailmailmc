package com.snailmailmc.plugin;

import com.snailmailmc.plugin.command.SnailMailCommand;
import com.snailmailmc.plugin.mail.BedrockMailbox;
import com.snailmailmc.plugin.mail.JavaMailbox;
import com.snailmailmc.plugin.mail.MailCommand;
import com.snailmailmc.plugin.mail.MailNotifier;
import com.snailmailmc.plugin.mail.MailService;
import com.snailmailmc.plugin.mail.MailStore;
import com.snailmailmc.plugin.news.NewsBoard;
import com.snailmailmc.plugin.news.NewsStore;
import com.snailmailmc.plugin.platform.Platforms;
import com.snailmailmc.plugin.shells.ShellsCommand;
import com.snailmailmc.plugin.shells.ShellsFormat;
import com.snailmailmc.plugin.shells.ShellsLedger;
import com.snailmailmc.plugin.shells.VaultShells;
import com.snailmailmc.plugin.storage.Database;
import java.sql.SQLException;
import java.time.ZoneId;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the SnailMail plugin. Each town feature (mail, Shells,
 * fishing and bugs, museum, seasons, news board) is its own package.
 */
public final class SnailMailPlugin extends JavaPlugin implements Listener {

    private Platforms platforms;
    private Database database;
    private ShellsLedger shells;
    private ShellsFormat shellsFormat;
    private MailService mail;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        platforms = new Platforms(getServer());

        try {
            getDataFolder().mkdirs();
            database = new Database(getDataFolder().toPath().resolve("snailmail.db"));
        } catch (SQLException e) {
            getLogger().severe("Couldn't open snailmail.db, disabling: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        shellsFormat = ShellsFormat.from(getConfig().getConfigurationSection("currency"));
        shells = new ShellsLedger(database, getConfig().getLong("currency.starting-balance", 0));
        hookVault();

        MailStore mailStore = new MailStore(database);
        mail = new MailService(this, mailStore);
        JavaMailbox javaMailbox = new JavaMailbox(this, mail);
        MailNotifier notifier = new MailNotifier(this, mailStore);

        register("snailmail", new SnailMailCommand(this));
        ShellsCommand shellsCommand = new ShellsCommand(this);
        register("shells", shellsCommand);
        register("pay", shellsCommand);
        register("mail", new MailCommand(this, mail, javaMailbox,
                platforms.floodgateAvailable() ? new BedrockMailbox(this, mail) : null));

        NewsBoard news = new NewsBoard(this, new NewsStore(database));
        register("news", news);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(news, this);
        getServer().getPluginManager().registerEvents(javaMailbox, this);
        getServer().getPluginManager().registerEvents(notifier, this);
        notifier.start();

        getLogger().info("SnailMail " + getPluginMeta().getVersion() + " enabled"
                + (platforms.floodgateAvailable() ? " with Bedrock forms." : " (Floodgate not found, Bedrock forms off)."));
    }

    private void hookVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().info("Vault not found, so other plugins can't see Shells.");
            return;
        }
        getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class,
                new VaultShells(shells, shellsFormat), this, ServicePriority.Highest);
        getLogger().info("Shells registered with Vault.");
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        shells.ensureAccount(event.getPlayer().getUniqueId());
    }

    @Override
    public void onDisable() {
        if (database != null) {
            try {
                database.close();
            } catch (SQLException e) {
                getLogger().warning("Couldn't close snailmail.db cleanly: " + e.getMessage());
            }
        }
        getLogger().info("SnailMail disabled.");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (getConfig().getConfigurationSection("currency") != null) {
            shellsFormat = ShellsFormat.from(getConfig().getConfigurationSection("currency"));
        }
    }

    public Platforms platforms() {
        return platforms;
    }

    public ShellsLedger shells() {
        return shells;
    }

    public ShellsFormat shellsFormat() {
        return shellsFormat;
    }

    public ZoneId townZone() {
        return ZoneId.of(getConfig().getString("seasons.timezone", "America/New_York"));
    }
}
