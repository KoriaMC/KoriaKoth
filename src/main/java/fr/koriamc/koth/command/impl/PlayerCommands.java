package fr.koriamc.koth.command.impl;

import fr.koriamc.koth.KoriaKoth;
import fr.koriamc.koth.command.CommandContext;
import fr.koriamc.koth.command.SubCommand;
import fr.koriamc.koth.model.KothDefinition;
import fr.koriamc.koth.model.KothSession;
import fr.koriamc.koth.model.MetaState;
import fr.koriamc.koth.util.Text;

import java.util.Collections;
import java.util.List;

/**
 * Commandes des joueurs : aide, menu, liste, classement et état de l'évènement.
 */
public final class PlayerCommands {

    private PlayerCommands() {
    }

    /** /koth help */
    public static final class Help extends SubCommand {
        public Help(KoriaKoth plugin) {
            super(plugin, "help", false, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            ctx.msg("help.header");
            for (SubCommand sub : plugin.commands().visible(ctx.sender)) {
                ctx.msg("help.line", "command", sub.getId(), "usage", sub.getUsage(), "description", sub.getDescription());
            }
            ctx.msg("help.footer");
        }
    }

    /** /koth menu */
    public static final class OpenMenu extends SubCommand {
        public OpenMenu(KoriaKoth plugin) {
            super(plugin, "menu", true, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            plugin.menus().openList(ctx.player);
        }
    }

    /** /koth list : les KOTH et leur planning dans le chat */
    public static final class ListKoths extends SubCommand {
        public ListKoths(KoriaKoth plugin) {
            super(plugin, "list", false, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (plugin.catalog().count() == 0) {
                ctx.msg("koth.none-configured");
                return;
            }
            ctx.msg("koth.list-header", "total", plugin.catalog().count());
            for (KothDefinition def : plugin.catalog().all()) {
                KothSession session = plugin.koths().get(def.getId());
                MetaState.Winner last = plugin.data().meta().getLast(def.getId());
                ctx.msg("koth.list-line",
                        "koth", Text.color(def.getName()),
                        "id", def.getId(),
                        "etat", session == null ? plugin.msg().text("display.state-waiting") : plugin.display().state(session),
                        "temps", plugin.display().time(session == null ? def.getCaptureSeconds() : session.getRemainingSeconds()),
                        "planning", plugin.display().schedule(def.getSchedule()),
                        "prochain", plugin.msg().duration(def.getSchedule()
                                .millisUntilNext(java.time.ZonedDateTime.now(plugin.settings().zone))),
                        "dernier", last == null ? plugin.msg().text("values.none") : last.getTag());
            }
            ctx.msg("koth.list-footer");
        }
    }

    /** /koth top [koth] */
    public static final class Top extends SubCommand {
        public Top(KoriaKoth plugin) {
            super(plugin, "top", true, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            plugin.menus().openTop(ctx.player, ctx.arg(0));
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? Text.complete(ctx.arg(0), plugin.catalog().ids()) : Collections.<String>emptyList();
        }
    }

    /** /koth info : évènement en cours ou prochain démarrage */
    public static final class Info extends SubCommand {
        public Info(KoriaKoth plugin) {
            super(plugin, "info", false, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (plugin.koths().isRunning()) {
                for (KothSession session : plugin.koths().sessions()) {
                    plugin.msg().send(ctx.sender, "koth.info-running", plugin.display().vars(session));
                }
                return;
            }
            KothDefinition next = plugin.koths().nextScheduled();
            if (next == null) {
                ctx.msg("koth.info-none");
                return;
            }
            ctx.msg("koth.info-next", "koth", Text.color(next.getName()), "id", next.getId(),
                    "temps", plugin.msg().duration(plugin.koths().nextScheduledMillis()),
                    "planning", plugin.display().schedule(next.getSchedule()));
        }
    }
}
