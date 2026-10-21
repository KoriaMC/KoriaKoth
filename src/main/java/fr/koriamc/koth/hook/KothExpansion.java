package fr.koriamc.koth.hook;

import fr.koriamc.koth.KoriaKoth;
import fr.koriamc.koth.model.FactionStats;
import fr.koriamc.koth.model.KothDefinition;
import fr.koriamc.koth.model.KothSession;
import fr.koriamc.koth.model.MetaState;
import fr.koriamc.koth.model.PlayerStats;
import fr.koriamc.koth.util.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Variables PlaceholderAPI :
 * <ul>
 *   <li>%koriakoth_actif%, %koriakoth_nom%, %koriakoth_temps%, %koriakoth_secondes%,
 *       %koriakoth_barre%, %koriakoth_pourcentage%, %koriakoth_controle%, %koriakoth_etat%,
 *       %koriakoth_joueurs%</li>
 *   <li>%koriakoth_prochain%, %koriakoth_prochain_temps%</li>
 *   <li>%koriakoth_&lt;id&gt;_actif%, %koriakoth_&lt;id&gt;_temps%, %koriakoth_&lt;id&gt;_prochain%,
 *       %koriakoth_&lt;id&gt;_dernier%, %koriakoth_&lt;id&gt;_victoires%</li>
 *   <li>%koriakoth_top_&lt;n&gt;_faction%, %koriakoth_top_&lt;n&gt;_victoires%</li>
 *   <li>%koriakoth_victoires%, %koriakoth_rang%, %koriakoth_captures%, %koriakoth_participations%,
 *       %koriakoth_temps_zone%, %koriakoth_dans_zone%</li>
 * </ul>
 */
final class KothExpansion extends PlaceholderExpansion {

    private final KoriaKoth plugin;

    KothExpansion(KoriaKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "koriakoth";
    }

    @Override
    public String getAuthor() {
        return "KoriaMC";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        String key = params.toLowerCase(Locale.ROOT);
        KothSession active = plugin.koths().getActive();
        String global = global(key, active);
        if (global != null) {
            return global;
        }
        if (key.startsWith("top_")) {
            return top(key.substring("top_".length()));
        }
        String perKoth = perKoth(key);
        if (perKoth != null) {
            return perKoth;
        }
        return player == null ? "" : forPlayer(player, key);
    }

    /** Variables de l'évènement en cours et du prochain. */
    private String global(String key, KothSession active) {
        if (key.equals("actif")) {
            return String.valueOf(active != null);
        }
        if (key.equals("prochain") || key.equals("prochain_temps")) {
            KothDefinition next = plugin.koths().nextScheduled();
            if (next == null) {
                return plugin.msg().text("values.none");
            }
            return key.equals("prochain") ? Text.color(next.getName())
                    : plugin.msg().duration(plugin.koths().nextScheduledMillis());
        }
        if (active == null) {
            if (key.equals("nom") || key.equals("controle") || key.equals("etat")) {
                return plugin.msg().text("values.none");
            }
            if (key.equals("temps")) {
                return plugin.display().time(0);
            }
            if (key.equals("secondes") || key.equals("pourcentage") || key.equals("joueurs")) {
                return "0";
            }
            if (key.equals("barre")) {
                return plugin.display().bar(0);
            }
            return null;
        }
        if (key.equals("nom")) {
            return Text.color(active.getDefinition().getName());
        }
        if (key.equals("id")) {
            return active.getDefinition().getId();
        }
        if (key.equals("temps")) {
            return plugin.display().time(active.getRemainingSeconds());
        }
        if (key.equals("secondes")) {
            return String.valueOf(active.getRemainingSeconds());
        }
        if (key.equals("barre")) {
            return plugin.display().bar(active.getProgress());
        }
        if (key.equals("pourcentage")) {
            return plugin.display().percent(active.getProgress());
        }
        if (key.equals("controle")) {
            return active.getControllerName() == null ? plugin.msg().text("values.nobody") : active.getControllerName();
        }
        if (key.equals("etat")) {
            return plugin.display().state(active);
        }
        if (key.equals("joueurs")) {
            return String.valueOf(active.getInZone().size());
        }
        Object[] vars = plugin.display().vars(active);
        for (int i = 0; i + 1 < vars.length; i += 2) {
            // capteur, meilleur, meilleur_temps, x, y, z, points
            if (key.equals(vars[i]) && (key.equals("capteur") || key.startsWith("meilleur") || key.length() == 1
                    || key.equals("points"))) {
                return String.valueOf(vars[i + 1]);
            }
        }
        return null;
    }

    /** "&lt;n&gt;_faction" ou "&lt;n&gt;_victoires" */
    private String top(String rest) {
        int underscore = rest.indexOf('_');
        Integer index = underscore < 0 ? null : Text.parseInt(rest.substring(0, underscore));
        if (index == null || index < 1) {
            return "";
        }
        List<FactionStats> top = plugin.data().top(null, index);
        if (top.size() < index) {
            return rest.endsWith("victoires") ? "0" : plugin.msg().text("values.none");
        }
        FactionStats stats = top.get(index - 1);
        return rest.substring(underscore + 1).equals("victoires") ? String.valueOf(stats.getWins()) : stats.getTag();
    }

    /** "&lt;id&gt;_actif", "&lt;id&gt;_temps", "&lt;id&gt;_prochain", "&lt;id&gt;_dernier" ou "&lt;id&gt;_victoires" */
    private String perKoth(String key) {
        int underscore = key.lastIndexOf('_');
        if (underscore <= 0) {
            return null;
        }
        KothDefinition def = plugin.catalog().get(key.substring(0, underscore));
        if (def == null) {
            return null;
        }
        String field = key.substring(underscore + 1);
        KothSession session = plugin.koths().get(def.getId());
        if (field.equals("actif")) {
            return String.valueOf(session != null);
        }
        if (field.equals("temps")) {
            return plugin.display().time(session == null ? 0 : session.getRemainingSeconds());
        }
        if (field.equals("prochain")) {
            long millis = def.getSchedule().millisUntilNext(java.time.ZonedDateTime.now(plugin.settings().zone));
            return millis < 0 ? plugin.msg().text("values.none") : plugin.msg().duration(millis);
        }
        if (field.equals("dernier")) {
            MetaState.Winner winner = plugin.data().meta().getLast(def.getId());
            return winner == null ? plugin.msg().text("values.none") : winner.getTag();
        }
        if (field.equals("victoires")) {
            List<FactionStats> top = plugin.data().top(def.getId(), 1);
            return top.isEmpty() ? "0" : String.valueOf(top.get(0).getWins(def.getId()));
        }
        return null;
    }

    /** Variables propres au joueur. */
    private String forPlayer(Player player, String key) {
        String factionId = plugin.factionHook().getFactionId(player);
        FactionStats faction = plugin.data().findFaction(factionId);
        PlayerStats stats = plugin.data().findPlayer(player.getUniqueId());
        if (key.equals("victoires")) {
            return faction == null ? "0" : String.valueOf(faction.getWins());
        }
        if (key.equals("rang")) {
            return factionId == null ? "0" : String.valueOf(plugin.data().rank(factionId));
        }
        if (key.equals("captures")) {
            return stats == null ? "0" : String.valueOf(stats.getCaptures());
        }
        if (key.equals("participations")) {
            return stats == null ? "0" : String.valueOf(stats.getParticipations());
        }
        if (key.equals("temps_capture")) {
            return stats == null ? plugin.msg().duration(0) : plugin.msg().duration(stats.getCaptureSeconds() * 1000L);
        }
        if (key.equals("temps_zone")) {
            return stats == null ? plugin.msg().duration(0) : plugin.msg().duration(stats.getSecondsInZone() * 1000L);
        }
        if (key.equals("dans_zone")) {
            for (KothSession session : plugin.koths().sessions()) {
                if (session.getDefinition().getZone().contains(player.getLocation())) {
                    return "true";
                }
            }
            return "false";
        }
        return null;
    }
}
