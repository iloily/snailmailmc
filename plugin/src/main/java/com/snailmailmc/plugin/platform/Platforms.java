package com.snailmailmc.plugin.platform;

import java.util.UUID;
import org.bukkit.Server;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Tells Java and Bedrock players apart, so each feature can show a chest menu
 * to Java players and a native form to Bedrock players.
 */
public final class Platforms {

    private final boolean floodgate;

    public Platforms(Server server) {
        this.floodgate = server.getPluginManager().getPlugin("floodgate") != null;
    }

    public boolean floodgateAvailable() {
        return floodgate;
    }

    public boolean isBedrock(UUID playerId) {
        return floodgate && FloodgateApi.getInstance().isFloodgatePlayer(playerId);
    }
}
