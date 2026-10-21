package fr.koriamc.koth.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Zone de capture : un pavé droit défini par deux coins dans un monde.
 * Les coordonnées sont celles des blocs, les deux coins sont inclus.
 */
public final class Zone {

    private final String world;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public Zone(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world = world == null ? "" : world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    /**
     * Lit la section "zone" d'un KOTH.
     *
     * @param world nom du monde (option "monde" du KOTH)
     * @return la zone, ou null si la section est absente
     */
    public static Zone read(ConfigurationSection s, String world) {
        if (s == null) {
            return null;
        }
        return new Zone(world, s.getInt("x1"), s.getInt("y1"), s.getInt("z1"),
                s.getInt("x2"), s.getInt("y2"), s.getInt("z2"));
    }

    /** Une zone est utilisable si un monde est indiqué et si ses coins ne sont pas tous à zéro. */
    public boolean isDefined() {
        return !world.isEmpty() && (minX != 0 || maxX != 0 || minY != 0 || maxY != 0 || minZ != 0 || maxZ != 0);
    }

    /** @return le monde, ou null s'il n'est pas chargé */
    public World getWorld() {
        return world.isEmpty() ? null : Bukkit.getWorld(world);
    }

    public String getWorldName() {
        return world;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || !location.getWorld().getName().equals(world)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * Centre de la zone.
     *
     * @param onTop true pour se placer juste au-dessus du sommet (téléportation)
     * @return la position, ou null si le monde n'est pas chargé
     */
    public Location center(boolean onTop) {
        World w = getWorld();
        if (w == null) {
            return null;
        }
        double x = (minX + maxX) / 2.0 + 0.5;
        double z = (minZ + maxZ) / 2.0 + 0.5;
        double y = onTop ? maxY + 1 : (minY + maxY) / 2.0;
        return new Location(w, x, y, z);
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    @Override
    public String toString() {
        return world + " " + minX + "," + minY + "," + minZ + " / " + maxX + "," + maxY + "," + maxZ;
    }
}
