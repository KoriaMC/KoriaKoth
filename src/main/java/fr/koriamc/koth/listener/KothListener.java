package fr.koriamc.koth.listener;

import fr.koriamc.koth.KoriaKoth;
import fr.koriamc.koth.model.KothDefinition;
import fr.koriamc.koth.model.KothSession;
import fr.koriamc.koth.model.PlayerStats;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Protection des zones de KOTH et remise des récompenses gardées pour les joueurs
 * qui étaient hors ligne.
 */
public final class KothListener implements Listener {

    /** Délai minimal entre deux messages de protection pour un même joueur. */
    private static final long MESSAGE_DELAY_MS = 3000L;

    private final KoriaKoth plugin;
    private final Map<UUID, Long> lastMessage = new HashMap<UUID, Long>();

    public KothListener(KoriaKoth plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerStats stats = plugin.data().findPlayer(player.getUniqueId());
        if (stats == null) {
            return;
        }
        stats.setName(player.getName());
        plugin.rewards().deliverPending(player, stats);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (plugin.settings().protectBreak && isProtected(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            warn(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.settings().protectPlace && isProtected(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            warn(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (!plugin.settings().protectContainers || block == null
                || !(block.getState() instanceof InventoryHolder)) {
            return;
        }
        if (isProtected(event.getPlayer(), block.getLocation())) {
            event.setCancelled(true);
            warn(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!plugin.settings().protectPearl || event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }
        if (isProtected(event.getPlayer(), event.getTo())) {
            event.setCancelled(true);
            plugin.msg().send(event.getPlayer(), "protection.no-pearl");
        }
    }

    /**
     * La position est-elle protégée pour ce joueur ?
     * Seules les zones des évènements en cours sont protégées si
     * protection.pendant-evenement-seulement est actif.
     */
    private boolean isProtected(Player player, Location location) {
        if (player.hasPermission("koriakoth.bypass")) {
            return false;
        }
        if (plugin.settings().protectDuringEventOnly) {
            for (KothSession session : plugin.koths().sessions()) {
                if (session.getDefinition().getZone().contains(location)) {
                    return true;
                }
            }
            return false;
        }
        for (KothDefinition def : plugin.catalog().all()) {
            if (def.getZone() != null && def.getZone().contains(location)) {
                return true;
            }
        }
        return false;
    }

    /** Message de protection, au maximum une fois toutes les trois secondes. */
    private void warn(Player player) {
        Long last = lastMessage.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (last != null && now - last < MESSAGE_DELAY_MS) {
            return;
        }
        lastMessage.put(player.getUniqueId(), now);
        plugin.msg().send(player, "protection.protected-zone");
    }
}
