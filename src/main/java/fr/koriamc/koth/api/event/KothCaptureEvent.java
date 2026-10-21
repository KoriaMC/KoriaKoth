package fr.koriamc.koth.api.event;

import fr.koriamc.koth.model.KothDefinition;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Appelé après la capture d'un KOTH, une fois les récompenses données (non annulable).
 */
public final class KothCaptureEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final KothDefinition koth;
    private final String factionId;
    private final String name;
    private final UUID capper;
    private final long points;

    public KothCaptureEvent(KothDefinition koth, String factionId, String name, UUID capper, long points) {
        this.koth = koth;
        this.factionId = factionId;
        this.name = name;
        this.capper = capper;
        this.points = points;
    }

    public KothDefinition getKoth() {
        return koth;
    }

    /** Faction gagnante, ou null si un joueur sans faction a gagné. */
    public String getFactionId() {
        return factionId;
    }

    /** Tag de la faction gagnante, ou pseudo du joueur seul. */
    public String getName() {
        return name;
    }

    /** Joueur présent dans la zone à la fin du compte à rebours. */
    public UUID getCapper() {
        return capper;
    }

    /** Points de faction crédités. */
    public long getPoints() {
        return points;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
