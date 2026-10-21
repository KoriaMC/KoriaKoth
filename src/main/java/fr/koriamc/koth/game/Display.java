package fr.koriamc.koth.game;

import fr.koriamc.koth.KoriaKoth;
import fr.koriamc.koth.config.Settings;
import fr.koriamc.koth.model.KothSession;
import fr.koriamc.koth.model.Schedule;
import fr.koriamc.koth.model.Zone;
import fr.koriamc.koth.util.Items;
import fr.koriamc.koth.util.Sidebar;
import fr.koriamc.koth.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Affichage de l'évènement en cours : barre de progression, temps restant et état
 * (capture, contestation, zone vide) envoyés aux joueurs concernés.
 */
public final class Display {

    private final KoriaKoth plugin;
    private final Sidebar sidebar = new Sidebar("koriakoth");

    public Display(KoriaKoth plugin) {
        this.plugin = plugin;
    }

    /** Barre de progression de la capture ("|||||-----"). */
    public String bar(double progress) {
        Settings s = plugin.settings();
        int length = s.barLength;
        int filled = (int) Math.round(Math.max(0, Math.min(1, progress)) * length);
        StringBuilder sb = new StringBuilder(s.barFull);
        for (int i = 0; i < length; i++) {
            if (i == filled) {
                sb.append(s.barEmpty);
            }
            sb.append(s.barSymbol);
        }
        return Text.color(sb.toString());
    }

    /** Temps au format m:ss. */
    public String time(int seconds) {
        int value = Math.max(0, seconds);
        return (value / 60) + ":" + (value % 60 < 10 ? "0" : "") + (value % 60);
    }

    public String percent(double progress) {
        return Text.number(Math.max(0, Math.min(1, progress)) * 100, plugin.settings().percentDecimals);
    }

    /** Planning lisible : "Samedi, Dimanche à 18:00, 21:00". */
    public String schedule(Schedule schedule) {
        if (!schedule.isEnabled()) {
            return plugin.msg().text("values.none");
        }
        List<String> days = new ArrayList<String>();
        for (DayOfWeek day : schedule.getDays()) {
            days.add(Items.capitalize(day.getDisplayName(TextStyle.FULL, Locale.FRENCH)));
        }
        List<String> hours = new ArrayList<String>();
        for (LocalTime time : schedule.getTimes()) {
            hours.add((time.getHour() < 10 ? "0" : "") + time.getHour()
                    + ":" + (time.getMinute() < 10 ? "0" : "") + time.getMinute());
        }
        return Text.join(days, ", ") + " " + plugin.msg().text("planning.at") + " " + Text.join(hours, ", ");
    }

    /** Variables communes d'un évènement, utilisables dans tous les messages. */
    public Object[] vars(KothSession session) {
        String nobody = plugin.msg().text("values.nobody");
        String controller = session.getControllerName() == null ? nobody : session.getControllerName();
        UUID capper = session.getController() == null ? null : session.getCapper();
        UUID best = session.getBestCapturer();
        Location center = session.getDefinition().getZone().center(true);
        return new Object[]{
                "x", center == null ? 0 : center.getBlockX(),
                "y", center == null ? 0 : center.getBlockY(),
                "z", center == null ? 0 : center.getBlockZ(),
                "capteur", capper == null ? nobody : session.getName(capper),
                "meilleur", best == null ? nobody : session.getName(best),
                "meilleur_temps", time(best == null ? 0 : session.getCaptureSeconds(best)),
                "points", session.getDefinition().getVictoryPoints(),
                "contestee", session.isContested() ? plugin.msg().text("display.contested-tag") : "",
                "koth", Text.color(session.getDefinition().getName()),
                "id", session.getDefinition().getId(),
                "temps", time(session.getRemainingSeconds()),
                "secondes", session.getRemainingSeconds(),
                "total", session.getDefinition().getCaptureSeconds(),
                "barre", bar(session.getProgress()),
                "pourcentage", percent(session.getProgress()),
                "controle", controller,
                "faction", controller,
                "etat", state(session),
                "joueurs", session.getInZone().size(),
                "monde", session.getDefinition().getZone().getWorldName()
        };
    }

    /** Texte d'état : capture en cours, zone contestée ou zone vide. */
    public String state(KothSession session) {
        if (session.isContested()) {
            return plugin.msg().text("display.state-contested");
        }
        if (session.getInZone().isEmpty() || session.getController() == null) {
            return plugin.msg().text("display.state-empty");
        }
        return plugin.msg().text("display.state-capturing", "controle", session.getControllerName());
    }

    /**
     * Scoreboard latéral de l'évènement en cours (messages.json : scoreboard), affiché à tous les
     * joueurs connectés ; retiré quand aucun KOTH n'est en cours.
     */
    public void updateSidebar() {
        KothSession session = plugin.koths().getActive();
        if (session == null || !plugin.settings().scoreboard) {
            if (sidebar.isShown()) {
                sidebar.hide();
            }
            return;
        }
        Object[] vars = vars(session);
        sidebar.show(Bukkit.getOnlinePlayers(), plugin.msg().text("scoreboard.title", vars),
                plugin.msg().lines("scoreboard.lines", vars));
    }

    /** Retire le scoreboard (arrêt du plugin). */
    public void hideSidebar() {
        sidebar.hide();
    }

    /** Envoie la barre de progression aux destinataires configurés. */
    public void update(KothSession session) {
        if (plugin.settings().audience == Settings.Audience.AUCUN) {
            return;
        }
        Object[] vars = vars(session);
        for (Player player : audience(session)) {
            plugin.msg().send(player, "display.actionbar", vars);
        }
    }

    /** Joueurs qui reçoivent l'affichage, selon affichage.destinataires. */
    public List<Player> audience(KothSession session) {
        List<Player> list = new ArrayList<Player>();
        Settings s = plugin.settings();
        Zone zone = session.getDefinition().getZone();
        if (s.audience == Settings.Audience.TOUS) {
            list.addAll(Bukkit.getOnlinePlayers());
            return list;
        }
        World world = zone.getWorld();
        if (world == null) {
            return list;
        }
        if (s.audience == Settings.Audience.ZONE) {
            for (Player player : world.getPlayers()) {
                if (zone.contains(player.getLocation())) {
                    list.add(player);
                }
            }
            return list;
        }
        Location center = zone.center(false);
        double radius = s.nearRadius;
        for (Player player : world.getPlayers()) {
            if (center != null && player.getLocation().distanceSquared(center) <= radius * radius) {
                list.add(player);
            }
        }
        return list;
    }
}
