package fr.koriamc.koth.hook;

import fr.koriamc.koth.KoriaKoth;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Pont facultatif vers KoriaQuest, par réflexion : KoriaKoth n'a pas besoin de
 * KoriaQuest pour fonctionner, mais déclare l'objectif GAGNER_KOTH et signale les
 * victoires si le plugin est présent.
 * <p>
 * Une quête peut alors utiliser :
 * <pre>"objectif": "GAGNER_KOTH", "cibles": ["spawn"]</pre>
 * ("cibles" vide accepte n'importe quel KOTH).
 */
public final class QuestHook {

    private final KoriaKoth plugin;
    private boolean available;
    private Method of;
    private Method key;
    private Method progress;

    public QuestHook(KoriaKoth plugin) {
        this.plugin = plugin;
    }

    /** Déclare l'objectif auprès de KoriaQuest s'il est installé. */
    public void register() {
        available = false;
        if (!plugin.settings().questIntegration || Bukkit.getPluginManager().getPlugin("KoriaQuest") == null) {
            return;
        }
        try {
            Class<?> api = Class.forName("fr.koriamc.quest.api.KoriaQuestAPI");
            Class<?> targets = Class.forName("fr.koriamc.quest.objective.ObjectiveType$Targets");
            Class<?> context = Class.forName("fr.koriamc.quest.objective.ProgressContext");
            api.getMethod("registerObjectiveType", String.class, targets)
                    .invoke(null, plugin.settings().questObjective, constant(targets, "NAMES"));
            of = context.getMethod("of", Player.class, String.class, long.class);
            key = context.getMethod("key", String.class);
            progress = api.getMethod("progress", context);
            available = true;
            plugin.getLogger().info("KoriaQuest détecté : objectif " + plugin.settings().questObjective
                    + " disponible dans les quêtes.");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Impossible de s'accrocher à KoriaQuest", t);
        }
    }

    /** Signale la victoire aux quêtes des joueurs concernés. */
    public void onWin(String kothId, Collection<UUID> winners) {
        if (!available) {
            return;
        }
        for (UUID uuid : winners) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            try {
                Object context = of.invoke(null, player, plugin.settings().questObjective, 1L);
                progress.invoke(null, key.invoke(context, kothId));
            } catch (Throwable t) {
                available = false;
                plugin.getLogger().log(Level.WARNING, "Progression KoriaQuest impossible, intégration désactivée", t);
                return;
            }
        }
    }

    public boolean isAvailable() {
        return available;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object constant(Class<?> type, String name) {
        return Enum.valueOf((Class<Enum>) type, name);
    }
}
