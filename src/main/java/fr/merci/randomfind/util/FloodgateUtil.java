package fr.merci.randomfind.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Petit pont vers Floodgate, entierement optionnel : si Floodgate n'est pas
 * installe sur le serveur, tout le monde est simplement considere comme un
 * joueur Java et le plugin fonctionne normalement.
 */
public class FloodgateUtil {

    private static Boolean available;

    public static boolean isFloodgateAvailable() {
        if (available == null) {
            available = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        }
        return available;
    }

    public static boolean isBedrockPlayer(Player player) {
        if (!isFloodgateAvailable()) return false;
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            // Floodgate absent ou API indisponible : on retombe sur "joueur Java"
            return false;
        }
    }
}
