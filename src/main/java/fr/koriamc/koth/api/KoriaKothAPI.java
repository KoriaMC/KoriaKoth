package fr.koriamc.koth.api;

import fr.koriamc.koth.KoriaKoth;
import fr.koriamc.koth.game.KothManager;
import fr.koriamc.koth.model.FactionStats;
import fr.koriamc.koth.model.KothDefinition;
import fr.koriamc.koth.model.KothSession;
import fr.koriamc.koth.model.MetaState;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * Accès pour les autres plugins KoriaMC (quêtes, boutique, panneaux...).
 * <pre>
 * if (KoriaKothAPI.isRunning()) {
 *     KothSession session = KoriaKothAPI.getActiveSession();
 *     int reste = session.getRemainingSeconds();
 * }
 * </pre>
 * Évènements disponibles dans fr.koriamc.koth.api.event.
 */
public final class KoriaKothAPI {

    private KoriaKothAPI() {
    }

    private static KoriaKoth plugin() {
        KoriaKoth plugin = KoriaKoth.get();
        if (plugin == null) {
            throw new IllegalStateException("KoriaKoth n'est pas activé");
        }
        return plugin;
    }

    public static boolean isAvailable() {
        return KoriaKoth.get() != null;
    }

    /** Identifiants des KOTH configurés. */
    public static List<String> getKothIds() {
        return plugin().catalog().ids();
    }

    /** @return la configuration d'un KOTH, ou null */
    public static KothDefinition getKoth(String id) {
        return plugin().catalog().get(id);
    }

    public static boolean isRunning() {
        return plugin().koths().isRunning();
    }

    /** @return le premier évènement en cours, ou null */
    public static KothSession getActiveSession() {
        return plugin().koths().getActive();
    }

    /** @return l'évènement en cours portant cet identifiant, ou null */
    public static KothSession getSession(String id) {
        return plugin().koths().get(id);
    }

    public static Collection<KothSession> getSessions() {
        return plugin().koths().sessions();
    }

    /**
     * Démarre un KOTH.
     *
     * @param force ignorer la limite d'un seul évènement à la fois
     */
    public static KothManager.Result start(String id, boolean force) {
        return plugin().koths().start(id, force);
    }

    /** Arrête un KOTH sans vainqueur. @return false s'il n'était pas en cours */
    public static boolean stop(String id) {
        return plugin().koths().stopKoth(id);
    }

    /** @return les millisecondes avant le prochain KOTH planifié, ou -1 */
    public static long getNextMillis() {
        return plugin().koths().nextScheduledMillis();
    }

    /** @return le prochain KOTH planifié, ou null */
    public static KothDefinition getNextKoth() {
        return plugin().koths().nextScheduled();
    }

    /** Le joueur est-il dans la zone d'un KOTH en cours ? */
    public static boolean isInZone(Player player) {
        for (KothSession session : plugin().koths().sessions()) {
            if (session.getDefinition().getZone().contains(player.getLocation())) {
                return true;
            }
        }
        return false;
    }

    /** Victoires d'une faction (identifiant KoriaFactions). */
    public static int getWins(String factionId) {
        FactionStats stats = plugin().data().findFaction(factionId);
        return stats == null ? 0 : stats.getWins();
    }

    /** Victoires d'une faction sur un KOTH précis. */
    public static int getWins(String factionId, String kothId) {
        FactionStats stats = plugin().data().findFaction(factionId);
        return stats == null ? 0 : stats.getWins(kothId);
    }

    /**
     * Classement des factions.
     *
     * @param kothId KOTH concerné, ou null pour le total
     */
    public static List<FactionStats> getTop(String kothId, int limit) {
        return plugin().data().top(kothId, limit);
    }

    /** @return le dernier vainqueur d'un KOTH, ou null */
    public static MetaState.Winner getLastWinner(String kothId) {
        return plugin().data().meta().getLast(kothId);
    }
}
