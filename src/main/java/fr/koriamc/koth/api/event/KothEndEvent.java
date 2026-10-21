package fr.koriamc.koth.api.event;

import fr.koriamc.koth.model.KothDefinition;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Appelé à la fin d'un KOTH, qu'il ait été capturé, arrêté par un administrateur
 * ou terminé par sa durée maximale (non annulable).
 */
public final class KothEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final KothDefinition koth;
    private final boolean captured;

    public KothEndEvent(KothDefinition koth, boolean captured) {
        this.koth = koth;
        this.captured = captured;
    }

    public KothDefinition getKoth() {
        return koth;
    }

    /** true si le KOTH a été capturé, false s'il a été arrêté ou a expiré. */
    public boolean isCaptured() {
        return captured;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
