package fr.koriamc.koth.api.event;

import fr.koriamc.koth.model.KothDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Appelé quand un nouveau groupe prend le contrôle de la zone (non annulable).
 */
public final class KothControlEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final KothDefinition koth;
    private final String factionId;
    private final String name;
    private final Player player;

    public KothControlEvent(KothDefinition koth, String factionId, String name, Player player) {
        this.koth = koth;
        this.factionId = factionId;
        this.name = name;
        this.player = player;
    }

    public KothDefinition getKoth() {
        return koth;
    }

    /** Faction qui prend le contrôle, ou null pour un joueur sans faction. */
    public String getFactionId() {
        return factionId;
    }

    /** Tag de la faction, ou pseudo du joueur seul. */
    public String getName() {
        return name;
    }

    /** Joueur présent dans la zone à ce moment. */
    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
