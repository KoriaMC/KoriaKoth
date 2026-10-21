package fr.koriamc.koth.api.event;

import fr.koriamc.koth.model.KothDefinition;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Appelé juste avant le démarrage d'un KOTH (manuel ou planifié). L'annuler
 * empêche le démarrage.
 */
public final class KothStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final KothDefinition koth;
    private boolean cancelled;

    public KothStartEvent(KothDefinition koth) {
        this.koth = koth;
    }

    public KothDefinition getKoth() {
        return koth;
    }

    public String getKothId() {
        return koth.getId();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
