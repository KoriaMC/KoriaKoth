package fr.koriamc.koth.command.impl;

import fr.koriamc.koth.KoriaKoth;
import fr.koriamc.koth.command.CommandContext;
import fr.koriamc.koth.command.SubCommand;
import fr.koriamc.koth.config.JsonFile;
import fr.koriamc.koth.game.KothManager;
import fr.koriamc.koth.model.KothDefinition;
import fr.koriamc.koth.model.KothSession;
import fr.koriamc.koth.util.Items;
import fr.koriamc.koth.util.Text;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Commandes d'administration : téléportation, démarrage, arrêt, création et
 * suppression de KOTH, éditeur, sauvegarde et rechargement.
 */
public final class AdminCommands {

    /** Suppression en attente de confirmation : joueur -> identifiant et instant. */
    private static final Map<UUID, String> CONFIRM_TARGET = new HashMap<UUID, String>();
    private static final Map<UUID, Long> CONFIRM_TIME = new HashMap<UUID, Long>();
    private static final long CONFIRM_MS = 10000L;

    private AdminCommands() {
    }

    /** Complète avec les identifiants des KOTH. */
    private static List<String> completeIds(KoriaKoth plugin, CommandContext ctx) {
        return ctx.size() == 1 ? Text.complete(ctx.arg(0), plugin.catalog().ids()) : Collections.<String>emptyList();
    }

    /** /koth tp &lt;koth&gt; */
    public static final class Teleport extends SubCommand {
        public Teleport(KoriaKoth plugin) {
            super(plugin, "tp", true, 1, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            KothDefinition def = plugin.catalog().get(ctx.arg(0));
            if (def == null) {
                ctx.msg("koth.unknown", "koth", ctx.arg(0));
                return;
            }
            Location center = def.getZone() == null ? null : def.getZone().center(true);
            if (center == null) {
                ctx.msg("koth.no-zone", "koth", Text.color(def.getName()));
                return;
            }
            ctx.player.teleport(center);
            ctx.msg("koth.teleported", "koth", Text.color(def.getName()));
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return completeIds(plugin, ctx);
        }
    }

    /** /koth start &lt;koth&gt; [force] */
    public static final class Start extends SubCommand {
        public Start(KoriaKoth plugin) {
            super(plugin, "start", false, 1, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            boolean force = ctx.size() > 1 && Text.parseBoolean(ctx.arg(1), true);
            KothManager.Result result = plugin.koths().start(ctx.arg(0), force);
            KothDefinition def = plugin.catalog().get(ctx.arg(0));
            ctx.msg("admin.start-" + result.name().toLowerCase(Locale.ROOT),
                    "koth", def == null ? ctx.arg(0) : Text.color(def.getName()), "id", ctx.arg(0));
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 2) {
                return Text.complete(ctx.arg(1), Arrays.asList("force"));
            }
            return completeIds(plugin, ctx);
        }
    }

    /** /koth stop [koth] : sans argument, arrête tous les évènements en cours */
    public static final class Stop extends SubCommand {
        public Stop(KoriaKoth plugin) {
            super(plugin, "stop", false, 0, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (ctx.size() == 0) {
                if (!plugin.koths().isRunning()) {
                    ctx.msg("admin.nothing-running");
                    return;
                }
                plugin.koths().stopAll(true);
                ctx.msg("admin.stopped-all");
                return;
            }
            KothDefinition def = plugin.catalog().get(ctx.arg(0));
            String name = def == null ? ctx.arg(0) : Text.color(def.getName());
            if (plugin.koths().stopKoth(ctx.arg(0))) {
                ctx.msg("admin.stopped", "koth", name);
            } else {
                ctx.msg("admin.not-running", "koth", name);
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() != 1) {
                return Collections.<String>emptyList();
            }
            List<String> running = new ArrayList<String>();
            for (KothSession session : plugin.koths().sessions()) {
                running.add(session.getDefinition().getId());
            }
            return Text.complete(ctx.arg(0), running);
        }
    }

    /** /koth create &lt;nom&gt; : ajoute un KOTH dans koths.json autour du joueur */
    public static final class Create extends SubCommand {
        public Create(KoriaKoth plugin) {
            super(plugin, "create", true, 1, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            String id = ctx.arg(0).toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+")) {
                ctx.msg("admin.invalid-id");
                return;
            }
            if (plugin.catalog().get(id) != null) {
                ctx.msg("admin.already-exists", "id", id);
                return;
            }
            Player player = ctx.player;
            Location at = player.getLocation();
            Map<String, Object> zone = new LinkedHashMap<String, Object>();
            zone.put("x1", at.getBlockX() - 5);
            zone.put("y1", at.getBlockY());
            zone.put("z1", at.getBlockZ() - 5);
            zone.put("x2", at.getBlockX() + 5);
            zone.put("y2", at.getBlockY() + 4);
            zone.put("z2", at.getBlockZ() + 5);
            Map<String, Object> schedule = new LinkedHashMap<String, Object>();
            schedule.put("actif", Boolean.FALSE);
            schedule.put("jours", new ArrayList<Object>());
            schedule.put("heures", new ArrayList<Object>());
            Map<String, Object> rewards = new LinkedHashMap<String, Object>();
            rewards.put("cible", plugin.settings().target.name());
            rewards.put("vainqueur", new ArrayList<Object>());
            rewards.put("participants", new ArrayList<Object>());
            Map<String, Object> koth = new LinkedHashMap<String, Object>();
            koth.put("nom", "&6KOTH " + Items.capitalize(id));
            koth.put("icone", "BEACON");
            koth.put("monde", at.getWorld().getName());
            koth.put("zone", zone);
            koth.put("capture-secondes", plugin.settings().captureSeconds);
            koth.put("zone-vide", name(plugin.settings().whenEmpty.name()));
            koth.put("contestation", name(plugin.settings().whenContested.name()));
            koth.put("changement-controle", name(plugin.settings().whenControlChanges.name()));
            koth.put("duree-max-minutes", plugin.settings().maxDurationMinutes);
            koth.put("planning", schedule);
            koth.put("points-victoire", plugin.settings().victoryPoints);
            koth.put("recompenses", rewards);
            plugin.kothsFile().set(Collections.singletonList(id), koth);
            if (!save(ctx)) {
                return;
            }
            ctx.msg("admin.created", "id", id, "monde", at.getWorld().getName());
        }

        private static String name(String value) {
            return value.replace('_', '-');
        }
    }

    /** /koth delete &lt;koth&gt; */
    public static final class Delete extends SubCommand {
        public Delete(KoriaKoth plugin) {
            super(plugin, "delete", false, 1, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            KothDefinition def = plugin.catalog().get(ctx.arg(0));
            if (def == null) {
                ctx.msg("koth.unknown", "koth", ctx.arg(0));
                return;
            }
            UUID uuid = ctx.player == null ? new UUID(0, 0) : ctx.player.getUniqueId();
            Long time = CONFIRM_TIME.get(uuid);
            if (!def.getId().equals(CONFIRM_TARGET.get(uuid)) || time == null
                    || System.currentTimeMillis() - time > CONFIRM_MS) {
                CONFIRM_TARGET.put(uuid, def.getId());
                CONFIRM_TIME.put(uuid, System.currentTimeMillis());
                ctx.msg("admin.confirm-delete", "id", def.getId(), "koth", Text.color(def.getName()));
                return;
            }
            CONFIRM_TARGET.remove(uuid);
            CONFIRM_TIME.remove(uuid);
            plugin.koths().stopKoth(def.getId());
            plugin.kothsFile().remove(Collections.singletonList(def.getId()));
            if (!save(ctx)) {
                return;
            }
            ctx.msg("admin.deleted", "id", def.getId());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return completeIds(plugin, ctx);
        }
    }

    /** /koth zone &lt;koth&gt; &lt;1|2&gt; : place un coin de la zone à la position du joueur */
    public static final class SetZone extends SubCommand {
        public SetZone(KoriaKoth plugin) {
            super(plugin, "zone", true, 2, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            KothDefinition def = plugin.catalog().get(ctx.arg(0));
            if (def == null) {
                ctx.msg("koth.unknown", "koth", ctx.arg(0));
                return;
            }
            Integer corner = Text.parseInt(ctx.arg(1));
            if (corner == null || (corner != 1 && corner != 2)) {
                ctx.msg("general.usage", "command", ctx.subLabel, "usage", getUsage());
                return;
            }
            Location at = ctx.player.getLocation();
            JsonFile file = plugin.kothsFile();
            String id = def.getId();
            file.set(Arrays.asList(id, "monde"), at.getWorld().getName());
            file.set(Arrays.asList(id, "zone", "x" + corner), at.getBlockX());
            file.set(Arrays.asList(id, "zone", "y" + corner), at.getBlockY());
            file.set(Arrays.asList(id, "zone", "z" + corner), at.getBlockZ());
            if (!save(ctx)) {
                return;
            }
            KothDefinition updated = plugin.catalog().get(id);
            ctx.msg("admin.zone-set", "id", id, "coin", corner, "x", at.getBlockX(), "y", at.getBlockY(),
                    "z", at.getBlockZ(), "monde", at.getWorld().getName(),
                    "zone", updated == null || updated.getZone() == null ? "?" : updated.getZone().toString());
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 2) {
                return Text.complete(ctx.arg(1), Arrays.asList("1", "2"));
            }
            return completeIds(plugin, ctx);
        }
    }

    /** /koth config [fichier] */
    public static final class Config extends SubCommand {
        public Config(KoriaKoth plugin) {
            super(plugin, "config", true, 0, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (!plugin.menus().openConfig(ctx.player, ctx.arg(0))) {
                ctx.msg("config-editor.unknown-file", "file", ctx.arg(0));
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            List<String> names = new ArrayList<String>();
            for (JsonFile file : plugin.configFiles()) {
                names.add(file.getName().replace(".json", ""));
            }
            return ctx.size() == 1 ? Text.complete(ctx.arg(0), names) : Collections.<String>emptyList();
        }
    }

    /** /koth save */
    public static final class Save extends SubCommand {
        public Save(KoriaKoth plugin) {
            super(plugin, "save", false, 0, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            plugin.data().saveAsync();
            ctx.msg("general.saved");
        }
    }

    /** /koth reload */
    public static final class Reload extends SubCommand {
        public Reload(KoriaKoth plugin) {
            super(plugin, "reload", false, 0, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            List<String> problems = plugin.reload();
            ctx.msg("general.reload-done", "koths", plugin.catalog().count());
            if (!problems.isEmpty()) {
                ctx.msg("general.reload-problems", "count", problems.size());
                for (String problem : problems) {
                    ctx.msg("general.reload-problem", "problem", problem);
                }
            }
        }
    }

    /** Écrit koths.json puis recharge la configuration. @return false en cas d'erreur */
    private static boolean save(CommandContext ctx) {
        try {
            ctx.plugin.kothsFile().save();
        } catch (IOException e) {
            ctx.msg("config-editor.save-error", "file", "koths.json", "error", e.getMessage());
            return false;
        }
        List<String> problems = ctx.plugin.reload();
        if (!problems.isEmpty()) {
            ctx.msg("general.reload-problems", "count", problems.size());
            for (String problem : problems) {
                ctx.msg("general.reload-problem", "problem", problem);
            }
        }
        return true;
    }
}
