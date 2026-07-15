package fr.merci.randomfind.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TeleportUtil {

    /**
     * Cherche un point aleatoire entre minRadius et maxRadius blocs d'un centre
     * (comme un vrai RTP), pose au sol, pas dans l'eau/la lave, avec assez de
     * place pour respirer, et si possible a au moins minDistance blocs (en X/Z)
     * des points deja utilises cette manche (pour eviter que les joueurs spawn
     * les uns sur les autres).
     */
    public static Location findSafeRandomLocation(World world, Location center, double minRadius, double maxRadius,
                                                   List<Location> avoid, double minDistance) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Location fallback = null;

        for (int i = 0; i < 80; i++) {
            double angle = rnd.nextDouble() * 2 * Math.PI;
            double dist = minRadius + rnd.nextDouble() * (maxRadius - minRadius);
            int x = (int) (center.getX() + Math.cos(angle) * dist);
            int z = (int) (center.getZ() + Math.sin(angle) * dist);

            Block ground = world.getHighestBlockAt(x, z);
            Material type = ground.getType();

            if (type == Material.WATER || type == Material.LAVA) continue;
            if (ground.getY() <= world.getMinHeight() + 2) continue;

            Location loc = ground.getLocation().add(0.5, 1, 0.5);
            Block feet = loc.getBlock();
            Block head = loc.clone().add(0, 1, 0).getBlock();

            if (feet.getType().isSolid() || head.getType().isSolid()) continue;

            if (fallback == null) fallback = loc;

            boolean farEnough = true;
            for (Location other : avoid) {
                double dx = other.getX() - loc.getX();
                double dz = other.getZ() - loc.getZ();
                if (Math.sqrt(dx * dx + dz * dz) < minDistance) {
                    farEnough = false;
                    break;
                }
            }
            if (farEnough) return loc;
        }

        return fallback != null ? fallback : center.clone().add(0, 1, 0);
    }
}
