package fr.koriamc.koth.game;

import fr.koriamc.koth.KoriaKoth;
import fr.koriamc.koth.api.event.KothCaptureEvent;
import fr.koriamc.koth.api.event.KothControlEvent;
import fr.koriamc.koth.api.event.KothEndEvent;
import fr.koriamc.koth.api.event.KothStartEvent;
import fr.koriamc.koth.model.Interruption;
import fr.koriamc.koth.model.KothDefinition;
import fr.koriamc.koth.model.KothSession;
import fr.koriamc.koth.model.MetaState;
import fr.koriamc.koth.model.PlayerStats;
import fr.koriamc.koth.model.Target;
import fr.koriamc.koth.model.Zone;
import fr.koriamc.koth.util.Sounds;
import fr.koriamc.koth.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Déroulement des KOTH : démarrage manuel ou planifié, compte à rebours de
 * capture seconde par seconde, contestation, victoire et récompenses.
 */
public final class KothManager {

    /** Résultat d'une demande de démarrage. */
    public enum Result {
        OK,
        /** Aucun KOTH ne porte cet identifiant. */
        INCONNU,
        /** Ce KOTH est déjà en cours. */
        DEJA_EN_COURS,
        /** Un autre KOTH est en cours et evenement.un-seul-a-la-fois est actif. */
        AUTRE_EN_COURS,
        /** Zone incomplète ou monde non chargé. */
        ZONE_INVALIDE,
        /** Un autre plugin a annulé le démarrage. */
        ANNULE
    }

    private final KoriaKoth plugin;
    private final Map<String, KothSession> sessions = new LinkedHashMap<String, KothSession>();
    private BukkitTask task;
    private String lastMinute = "";

    public KothManager(KoriaKoth plugin) {
        this.plugin = plugin;
    }

    /** Démarre la boucle de jeu (une fois par seconde). */
    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Termine tous les évènements en cours sans vainqueur (arrêt du serveur, rechargement). */
    public void stopAll(boolean announce) {
        for (KothSession session : new ArrayList<KothSession>(sessions.values())) {
            end(session, announce ? "koth.stopped" : null, false);
        }
        plugin.display().updateSidebar();
    }

    // ============================================================== Accès

    public Collection<KothSession> sessions() {
        return sessions.values();
    }

    /** @return le KOTH en cours portant cet identifiant, ou null */
    public KothSession get(String id) {
        return id == null ? null : sessions.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    /** @return le premier évènement en cours, ou null */
    public KothSession getActive() {
        for (KothSession session : sessions.values()) {
            return session;
        }
        return null;
    }

    public boolean isRunning() {
        return !sessions.isEmpty();
    }

    /** @return le prochain KOTH prévu au planning, ou null */
    public KothDefinition nextScheduled() {
        ZonedDateTime now = ZonedDateTime.now(plugin.settings().zone);
        KothDefinition best = null;
        long bestMillis = Long.MAX_VALUE;
        if (!plugin.settings().scheduleEnabled) {
            return null;
        }
        for (KothDefinition def : plugin.catalog().all()) {
            long millis = def.getSchedule().millisUntilNext(now);
            if (millis >= 0 && millis < bestMillis) {
                bestMillis = millis;
                best = def;
            }
        }
        return best;
    }

    /** @return les millisecondes avant le prochain KOTH prévu, ou -1 */
    public long nextScheduledMillis() {
        KothDefinition next = nextScheduled();
        return next == null ? -1L : next.getSchedule().millisUntilNext(ZonedDateTime.now(plugin.settings().zone));
    }

    // =========================================================== Démarrage

    /**
     * Démarre un KOTH.
     *
     * @param force ignorer la limite d'un seul évènement à la fois
     */
    public Result start(String id, boolean force) {
        KothDefinition def = plugin.catalog().get(id);
        if (def == null) {
            return Result.INCONNU;
        }
        if (sessions.containsKey(def.getId())) {
            return Result.DEJA_EN_COURS;
        }
        if (!force && plugin.settings().onlyOneAtATime && !sessions.isEmpty()) {
            return Result.AUTRE_EN_COURS;
        }
        if (!def.isPlayable()) {
            return Result.ZONE_INVALIDE;
        }
        KothStartEvent event = new KothStartEvent(def);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return Result.ANNULE;
        }
        KothSession session = new KothSession(def);
        sessions.put(def.getId(), session);
        Zone zone = def.getZone();
        announce(session, "koth.start", "koth", Text.color(def.getName()), "id", def.getId(),
                "monde", zone.getWorldName(), "x", zone.center(true) == null ? 0 : zone.center(true).getBlockX(),
                "y", zone.center(true) == null ? 0 : zone.center(true).getBlockY(),
                "z", zone.center(true) == null ? 0 : zone.center(true).getBlockZ(),
                "temps", plugin.display().time(def.getCaptureSeconds()),
                "recompenses", plugin.rewards().describe(def.getWinnerRewards()));
        for (Player player : Bukkit.getOnlinePlayers()) {
            Sounds.play(player, plugin.settings().sound("demarrage"));
        }
        plugin.guiHook().update(session);
        return Result.OK;
    }

    /** Arrête un KOTH sans vainqueur. @return false s'il n'était pas en cours */
    public boolean stopKoth(String id) {
        KothSession session = get(id);
        if (session == null) {
            return false;
        }
        end(session, "koth.stopped", false);
        return true;
    }

    // ================================================================ Boucle

    private void tick() {
        ZonedDateTime now = ZonedDateTime.now(plugin.settings().zone);
        String minute = now.getYear() + "-" + now.getDayOfYear() + "-" + now.getHour() + "-" + now.getMinute();
        if (!minute.equals(lastMinute)) {
            lastMinute = minute;
            onMinute(now);
        }
        for (KothSession session : new ArrayList<KothSession>(sessions.values())) {
            try {
                update(session);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Erreur pendant le KOTH " + session.getDefinition().getId() + " : " + e);
            }
        }
        plugin.display().updateSidebar();
    }

    /** Démarrages planifiés et annonces avant le début. */
    private void onMinute(ZonedDateTime now) {
        if (!plugin.settings().scheduleEnabled) {
            return;
        }
        for (KothDefinition def : plugin.catalog().all()) {
            if (!def.getSchedule().isEnabled()) {
                continue;
            }
            if (def.getSchedule().matches(now)) {
                Result result = start(def.getId(), false);
                if (result != Result.OK) {
                    plugin.getLogger().info("KOTH planifié " + def.getId() + " non démarré : " + result.name());
                }
                continue;
            }
            long millis = def.getSchedule().millisUntilNext(now);
            if (millis < 0) {
                continue;
            }
            int minutes = (int) Math.round(millis / 60000.0);
            if (plugin.settings().warnMinutes.contains(minutes)) {
                plugin.msg().broadcast("koth.warn", "koth", Text.color(def.getName()), "id", def.getId(),
                        "minutes", minutes, "temps", plugin.msg().duration(millis));
            }
        }
    }

    /** Une seconde de jeu sur un évènement. */
    private void update(KothSession session) {
        KothDefinition def = session.getDefinition();
        World world = def.getZone().getWorld();
        if (world == null) {
            end(session, "koth.stopped", false);
            return;
        }
        Map<String, List<Player>> groups = new LinkedHashMap<String, List<Player>>();
        Set<UUID> inZone = new LinkedHashSet<UUID>();
        Set<String> addresses = new HashSet<String>();
        for (Player player : world.getPlayers()) {
            if (!def.getZone().contains(player.getLocation())) {
                continue;
            }
            inZone.add(player.getUniqueId());
            if (plugin.settings().isIgnored(player)) {
                continue;
            }
            if (plugin.settings().ignoreSameIp && !addresses.add(address(player))) {
                continue;
            }
            String factionId = plugin.factionHook().getFactionId(player);
            if (factionId == null && !plugin.settings().soloAllowed) {
                continue;
            }
            String key = factionId == null ? KothSession.SOLO + player.getUniqueId() : factionId;
            session.addSecond(player.getUniqueId(), player.getName());
            List<Player> group = groups.get(key);
            if (group == null) {
                group = new ArrayList<Player>();
                groups.put(key, group);
            }
            group.add(player);
        }
        session.refreshCaptureStep();
        session.getInZone().clear();
        session.getInZone().addAll(inZone);
        session.setContested(groups.size() > 1);

        if (groups.isEmpty()) {
            interrupt(session, def.getWhenEmpty());
        } else if (groups.size() > 1) {
            interrupt(session, def.getWhenContested());
        } else {
            control(session, groups);
        }
        plugin.display().update(session);
        plugin.guiHook().update(session);

        if (session.getRemainingSeconds() <= 0 && session.getController() != null) {
            capture(session);
        } else if (session.isExpired()) {
            end(session, "koth.timeout", false);
        }
    }

    /** Zone vide ou contestée : le compte à rebours est gelé, remis à zéro ou continue. */
    private void interrupt(KothSession session, Interruption mode) {
        if (mode == Interruption.REMISE_A_ZERO) {
            if (session.getController() != null || session.getRemaining() < session.getCaptureTotal()) {
                session.setController(null, null, null);
                session.resetCountdown();
            }
            return;
        }
        if (mode == Interruption.CONTINUER && session.getController() != null) {
            session.decrease(1);
            countdown(session);
        }
    }

    /** Un seul groupe dans la zone : il capture. */
    private void control(KothSession session, Map<String, List<Player>> groups) {
        Map.Entry<String, List<Player>> entry = groups.entrySet().iterator().next();
        String key = entry.getKey();
        boolean changed = !key.equals(session.getController());
        // Le capteur affiché reste le même tant qu'il est dans la zone, sinon le premier membre présent.
        Player first = entry.getValue().get(0);
        for (Player player : entry.getValue()) {
            if (!changed && player.getUniqueId().equals(session.getCapper())) {
                first = player;
            }
            session.addCaptureSecond(player.getUniqueId(), player.getName());
        }
        boolean solo = key.startsWith(KothSession.SOLO);
        String name = solo ? first.getName() : plugin.factionHook().getTag(key);
        session.setController(key, name, first.getUniqueId());
        if (changed) {
            if (session.getDefinition().getWhenControlChanges() == Interruption.REMISE_A_ZERO) {
                session.resetCountdown();
            }
            announce(session, "koth.control", "koth", Text.color(session.getDefinition().getName()),
                    "controle", name, "joueur", first.getName(),
                    "temps", plugin.display().time(session.getRemainingSeconds()));
            for (Player player : plugin.display().audience(session)) {
                Sounds.play(player, plugin.settings().sound("controle"));
            }
            Bukkit.getPluginManager().callEvent(new KothControlEvent(session.getDefinition(),
                    solo ? null : key, name, first));
        }
        session.decrease(1);
        String factionId = session.getControllerFaction();
        if (factionId != null) {
            plugin.data().faction(factionId, name).addControlSeconds(1);
        }
        countdown(session);
    }

    /** Annonce des paliers du compte à rebours (une seule fois chacun). */
    private void countdown(KothSession session) {
        int remaining = session.getRemainingSeconds();
        if (remaining <= 0 || !plugin.settings().announceSeconds.contains(remaining)
                || !session.announceOnce(remaining)) {
            return;
        }
        announce(session, "koth.countdown", plugin.display().vars(session));
        for (Player player : Bukkit.getOnlinePlayers()) {
            Sounds.play(player, plugin.settings().sound("palier"));
        }
    }

    // =============================================================== Victoire

    private void capture(KothSession session) {
        KothDefinition def = session.getDefinition();
        String factionId = session.getControllerFaction();
        String name = session.getControllerName();
        UUID capper = session.getCapper();
        String capperName = capper == null ? "?" : session.getName(capper);
        List<UUID> winners = recipients(session);

        if (factionId != null) {
            plugin.data().faction(factionId, name).addWin(def.getId());
        }
        if (capper != null) {
            plugin.data().player(capper, capperName).addCapture();
        }
        int min = plugin.settings().participationSeconds;
        plugin.data().meta().setLast(def.getId(), new MetaState.Winner(factionId, name, capperName));

        UUID best = session.getBestCapturer();
        Object[] vars = {"koth", Text.color(def.getName()), "id", def.getId(), "controle", name, "faction", name,
                "joueur", capperName, "capteur", capperName, "duree", plugin.msg().duration(session.elapsedMillis()),
                "participants", session.getSeconds().size(),
                "meilleur", best == null ? plugin.msg().text("values.nobody") : session.getName(best),
                "meilleur_temps", plugin.display().time(best == null ? 0 : session.getCaptureSeconds(best))};
        long points = factionId == null ? 0 : plugin.factionHook().addPoints(factionId, def.getVictoryPoints());
        points += plugin.rewards().grant(factionId, winners, def.getWinnerRewards(), vars);
        if (!def.getParticipantRewards().isEmpty()) {
            List<UUID> others = new ArrayList<UUID>();
            for (UUID uuid : session.participants(min)) {
                if (!winners.contains(uuid)) {
                    others.add(uuid);
                }
            }
            plugin.rewards().grant(null, others, def.getParticipantRewards(), vars);
        }
        announce(session, "koth.win", append(vars, "points", points,
                "recompenses", plugin.rewards().describe(def.getWinnerRewards())));
        for (Player player : Bukkit.getOnlinePlayers()) {
            Sounds.play(player, plugin.settings().sound("victoire"));
        }
        fireworks(def);
        plugin.questHook().onWin(def.getId(), winners);
        Bukkit.getPluginManager().callEvent(new KothCaptureEvent(def, factionId, name, capper, points));
        end(session, null, true);
    }

    /** Joueurs qui reçoivent les récompenses du vainqueur, selon la cible configurée. */
    private List<UUID> recipients(KothSession session) {
        List<UUID> out = new ArrayList<UUID>();
        Target target = session.getDefinition().getTarget();
        String factionId = session.getControllerFaction();
        if (target == Target.AUCUNE) {
            return out;
        }
        if (factionId == null || target == Target.CAPTEUR) {
            if (session.getCapper() != null) {
                out.add(session.getCapper());
            }
            return out;
        }
        if (target == Target.MEMBRES_EN_ZONE) {
            for (UUID uuid : session.getInZone()) {
                if (factionId.equals(plugin.factionHook().getFactionId(uuid))) {
                    out.add(uuid);
                }
            }
        } else if (target == Target.MEMBRES_EN_LIGNE) {
            for (Player player : plugin.factionHook().getOnlineMembers(factionId)) {
                out.add(player.getUniqueId());
            }
        } else if (target == Target.MEMBRES) {
            out.addAll(plugin.factionHook().getMembers(factionId));
        }
        if (out.isEmpty() && session.getCapper() != null) {
            out.add(session.getCapper());
        }
        return out;
    }

    /**
     * Termine un évènement.
     *
     * @param messageKey message diffusé (null après une victoire, déjà annoncée)
     * @param captured   true si le KOTH a été capturé par un groupe
     */
    private void end(KothSession session, String messageKey, boolean captured) {
        sessions.remove(session.getDefinition().getId());
        int min = plugin.settings().participationSeconds;
        for (Map.Entry<UUID, Integer> entry : session.getSeconds().entrySet()) {
            PlayerStats stats = plugin.data().player(entry.getKey(), session.getName(entry.getKey()));
            stats.addSecondsInZone(entry.getValue());
            stats.addCaptureSeconds(session.getCaptureSeconds(entry.getKey()));
            if (entry.getValue() >= min) {
                stats.addParticipation();
            }
        }
        if (messageKey != null) {
            announce(session, messageKey, plugin.display().vars(session));
            for (Player player : Bukkit.getOnlinePlayers()) {
                Sounds.play(player, plugin.settings().sound("fin"));
            }
        }
        plugin.guiHook().clear(session.getDefinition());
        Bukkit.getPluginManager().callEvent(new KothEndEvent(session.getDefinition(), captured));
        plugin.data().saveAsync();
    }

    /**
     * Annonce un message : à tout le serveur si evenement.annonce-globale est actif,
     * sinon seulement aux joueurs qui suivent l'évènement.
     */
    private void announce(KothSession session, String key, Object... kv) {
        if (plugin.settings().broadcast) {
            plugin.msg().broadcast(key, kv);
            return;
        }
        for (Player player : plugin.display().audience(session)) {
            plugin.msg().send(player, key, kv);
        }
    }

    private void fireworks(KothDefinition def) {
        if (!plugin.settings().fireworks) {
            return;
        }
        Location center = def.getZone().center(true);
        if (center == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            Firework firework = center.getWorld().spawn(center, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().withColor(Color.YELLOW, Color.ORANGE)
                    .with(FireworkEffect.Type.BALL_LARGE).flicker(true).trail(true).build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        }
    }

    private static String address(Player player) {
        return player.getAddress() == null || player.getAddress().getAddress() == null
                ? player.getName() : player.getAddress().getAddress().getHostAddress();
    }

    private static Object[] append(Object[] kv, Object... extra) {
        Object[] all = new Object[kv.length + extra.length];
        System.arraycopy(kv, 0, all, 0, kv.length);
        System.arraycopy(extra, 0, all, kv.length, extra.length);
        return all;
    }
}
