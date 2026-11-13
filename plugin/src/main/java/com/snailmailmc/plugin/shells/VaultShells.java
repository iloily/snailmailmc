package com.snailmailmc.plugin.shells;

import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Lets other plugins (shops, jobs and so on) use Shells through Vault/VaultUnlocked.
 * Shells are whole numbers, so fractions are rounded down. No banks.
 */
public final class VaultShells extends AbstractEconomy {

    private final ShellsLedger ledger;
    private final ShellsFormat format;

    public VaultShells(ShellsLedger ledger, ShellsFormat format) {
        this.ledger = ledger;
        this.format = format;
    }

    @Override public boolean isEnabled() { return true; }
    @Override public String getName() { return "SnailMail Shells"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return 0; }
    @Override public String format(double amount) { return format.format((long) Math.floor(amount)); }
    @Override public String currencyNamePlural() { return format.plural(); }
    @Override public String currencyNameSingular() { return format.singular(); }

    // Player methods by UUID.

    @Override public boolean hasAccount(OfflinePlayer player) { return ledger.hasAccount(player.getUniqueId()); }
    @Override public boolean hasAccount(OfflinePlayer player, String world) { return hasAccount(player); }
    @Override public double getBalance(OfflinePlayer player) { return ledger.balance(player.getUniqueId()); }
    @Override public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }
    @Override public boolean has(OfflinePlayer player, double amount) { return getBalance(player) >= amount; }
    @Override public boolean has(OfflinePlayer player, String world, double amount) { return has(player, amount); }
    @Override public boolean createPlayerAccount(OfflinePlayer player) { return ledger.ensureAccount(player.getUniqueId()); }
    @Override public boolean createPlayerAccount(OfflinePlayer player, String world) { return createPlayerAccount(player); }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return withdraw(player.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return deposit(player.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount) {
        return depositPlayer(player, amount);
    }

    private EconomyResponse withdraw(UUID id, double amount) {
        long whole = (long) Math.floor(amount);
        if (whole <= 0) {
            return new EconomyResponse(0, ledger.balance(id), ResponseType.FAILURE, "Amount must be at least 1");
        }
        boolean ok = ledger.withdraw(id, whole);
        return new EconomyResponse(ok ? whole : 0, ledger.balance(id),
                ok ? ResponseType.SUCCESS : ResponseType.FAILURE, ok ? null : "Not enough " + format.plural());
    }

    private EconomyResponse deposit(UUID id, double amount) {
        long whole = (long) Math.floor(amount);
        if (whole <= 0) {
            return new EconomyResponse(0, ledger.balance(id), ResponseType.FAILURE, "Amount must be at least 1");
        }
        ledger.deposit(id, whole);
        return new EconomyResponse(whole, ledger.balance(id), ResponseType.SUCCESS, null);
    }

    // Old name-based methods: look the player up and use the UUID versions.

    @SuppressWarnings("deprecation")
    private static OfflinePlayer byName(String name) {
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        return cached != null ? cached : Bukkit.getOfflinePlayer(name);
    }

    @Override public boolean hasAccount(String name) { return hasAccount(byName(name)); }
    @Override public boolean hasAccount(String name, String world) { return hasAccount(name); }
    @Override public double getBalance(String name) { return getBalance(byName(name)); }
    @Override public double getBalance(String name, String world) { return getBalance(name); }
    @Override public boolean has(String name, double amount) { return has(byName(name), amount); }
    @Override public boolean has(String name, String world, double amount) { return has(name, amount); }
    @Override public EconomyResponse withdrawPlayer(String name, double amount) { return withdrawPlayer(byName(name), amount); }
    @Override public EconomyResponse withdrawPlayer(String name, String world, double amount) { return withdrawPlayer(name, amount); }
    @Override public EconomyResponse depositPlayer(String name, double amount) { return depositPlayer(byName(name), amount); }
    @Override public EconomyResponse depositPlayer(String name, String world, double amount) { return depositPlayer(name, amount); }
    @Override public boolean createPlayerAccount(String name) { return createPlayerAccount(byName(name)); }
    @Override public boolean createPlayerAccount(String name, String world) { return createPlayerAccount(name); }

    // No banks.

    private static EconomyResponse noBanks() {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SnailMail has no banks");
    }

    @Override public EconomyResponse createBank(String name, String player) { return noBanks(); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return noBanks(); }
    @Override public EconomyResponse deleteBank(String name) { return noBanks(); }
    @Override public EconomyResponse bankBalance(String name) { return noBanks(); }
    @Override public EconomyResponse bankHas(String name, double amount) { return noBanks(); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return noBanks(); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return noBanks(); }
    @Override public EconomyResponse isBankOwner(String name, String player) { return noBanks(); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return noBanks(); }
    @Override public EconomyResponse isBankMember(String name, String player) { return noBanks(); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return noBanks(); }
    @Override public List<String> getBanks() { return List.of(); }
}
