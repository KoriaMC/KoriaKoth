package fr.koriamc.koth.gui;

import fr.koriamc.koth.KoriaKoth;
import fr.koriamc.koth.game.KothManager;
import fr.koriamc.koth.model.KothDefinition;
import fr.koriamc.koth.model.KothSession;
import fr.koriamc.koth.model.MetaState;
import fr.koriamc.koth.util.Items;
import fr.koriamc.koth.util.Text;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /koth menu : liste des KOTH avec leur état, leur planning et leur dernier
 * vainqueur. L'apparence de chaque état et l'effet des clics viennent de
 * menus.json (liste). Actions propres : TELEPORTER, DEMARRER, ARRETER, TOP.
 */
final class KothListMenu extends Menu {

    KothListMenu(KoriaKoth plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected ConfigurationSection section() {
        return plugin.menus().section("liste");
    }

    @Override
    protected void render() {
        ConfigurationSection s = section();
        List<KothDefinition> koths = new ArrayList<KothDefinition>(plugin.catalog().all());
        List<Integer> slots = s.getIntegerList("cases");
        int perPage = Math.max(1, slots.size());
        pages = Math.max(1, (koths.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < slots.size() && page * perPage + i < koths.size(); i++) {
            item(slots.get(i), koths.get(page * perPage + i));
        }
        Object[] vars = vars();
        ConfigurationSection items = s.getConfigurationSection("objets");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                setConfigured(items.getConfigurationSection(key), vars);
            }
        }
        if (page > 0) {
            setConfigured(s.getConfigurationSection("precedent"), vars);
        }
        if (page < pages - 1) {
            setConfigured(s.getConfigurationSection("suivant"), vars);
        }
    }

    /** Un KOTH : modèle en-cours, planifie, inactif ou invalide selon son état. */
    private void item(int slot, final KothDefinition def) {
        final KothSession session = plugin.koths().get(def.getId());
        String state = session != null ? "en-cours"
                : !def.isPlayable() ? "invalide"
                : def.getSchedule().isEnabled() ? "planifie" : "inactif";
        ConfigurationSection template = section().getConfigurationSection("koths." + state);
        if (template == null) {
            return;
        }
        MetaState.Winner last = plugin.data().meta().getLast(def.getId());
        Location center = def.getZone() == null ? null : def.getZone().center(true);
        Object[] kv = {
                "koth", Text.color(def.getName()),
                "id", def.getId(),
                "icone", def.getIcon(),
                "capture", plugin.display().time(def.getCaptureSeconds()),
                "duree", def.getMaxDurationMinutes() == 0
                        ? plugin.msg().text("values.unlimited") : def.getMaxDurationMinutes() + " min",
                "monde", def.getZone() == null ? "?" : def.getZone().getWorldName(),
                "x", center == null ? 0 : center.getBlockX(),
                "y", center == null ? 0 : center.getBlockY(),
                "z", center == null ? 0 : center.getBlockZ(),
                "planning", plugin.display().schedule(def.getSchedule()),
                "prochain", plugin.msg().duration(def.getSchedule()
                        .millisUntilNext(java.time.ZonedDateTime.now(plugin.settings().zone))),
                "temps", plugin.display().time(session == null ? def.getCaptureSeconds() : session.getRemainingSeconds()),
                "barre", plugin.display().bar(session == null ? 0 : session.getProgress()),
                "pourcentage", plugin.display().percent(session == null ? 0 : session.getProgress()),
                "controle", session == null || session.getControllerName() == null
                        ? plugin.msg().text("values.nobody") : session.getControllerName(),
                "etat", session == null ? plugin.msg().text("display.state-waiting") : plugin.display().state(session),
                "joueurs", session == null ? 0 : session.getInZone().size(),
                "dernier", last == null ? plugin.msg().text("values.none") : last.getTag(),
                "dernier_joueur", last == null ? plugin.msg().text("values.none") : last.getPlayer(),
                "dernier_temps", last == null ? plugin.msg().text("values.none") : plugin.msg().since(last.getTime()),
                "recompenses", plugin.rewards().describe(def.getWinnerRewards()),
                "points", def.getVictoryPoints(),
                "label", plugin.commands().getLabel()
        };
        set(slot, Items.fromSection(template, kv), clickHandler(template, new CustomAction() {
            @Override
            public boolean run(String type, String value) {
                return action(type, def);
            }
        }, kv));
    }

    /** Actions propres aux KOTH de la liste. */
    private boolean action(String type, KothDefinition def) {
        if (type.equals("TELEPORTER")) {
            teleport(def);
            return true;
        }
        if (type.equals("DEMARRER")) {
            if (!isAdmin()) {
                return true;
            }
            KothManager.Result result = plugin.koths().start(def.getId(), false);
            plugin.msg().send(player, "admin.start-" + result.name().toLowerCase(java.util.Locale.ROOT),
                    "koth", Text.color(def.getName()));
            refreshLater();
            return true;
        }
        if (type.equals("ARRETER")) {
            if (!isAdmin()) {
                return true;
            }
            if (plugin.koths().stopKoth(def.getId())) {
                plugin.msg().send(player, "admin.stopped", "koth", Text.color(def.getName()));
            } else {
                plugin.msg().send(player, "admin.not-running", "koth", Text.color(def.getName()));
            }
            refreshLater();
            return true;
        }
        if (type.equals("TOP")) {
            plugin.menus().openTop(player, def.getId());
            return true;
        }
        return false;
    }

    /** Téléportation au centre de la zone (permission de la commande tp). */
    private void teleport(KothDefinition def) {
        fr.koriamc.koth.command.SubCommand command = plugin.commands().get("tp");
        if (command != null && !command.hasPermission(player)) {
            plugin.msg().send(player, "general.no-permission");
            return;
        }
        Location center = def.getZone() == null ? null : def.getZone().center(true);
        if (center == null) {
            plugin.msg().send(player, "koth.no-zone", "koth", Text.color(def.getName()));
            return;
        }
        player.closeInventory();
        player.teleport(center);
        plugin.msg().send(player, "koth.teleported", "koth", Text.color(def.getName()));
    }

    private boolean isAdmin() {
        fr.koriamc.koth.command.SubCommand command = plugin.commands().get("start");
        if (command == null || command.hasPermission(player)) {
            return true;
        }
        plugin.msg().send(player, "general.no-permission");
        return false;
    }
}
