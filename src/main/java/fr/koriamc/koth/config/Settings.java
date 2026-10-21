package fr.koriamc.koth.config;

import fr.koriamc.koth.model.Interruption;
import fr.koriamc.koth.model.Target;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Réglages typés de config.json, relus à chaque rechargement.
 */
public final class Settings {

    /** Destinataires de la barre de progression pendant l'évènement. */
    public enum Audience {
        /** Les joueurs présents dans la zone. */
        ZONE,
        /** Les joueurs à portée de la zone (rayon-proches). */
        PROCHES,
        /** Tout le serveur. */
        TOUS,
        /** Personne. */
        AUCUN
    }

    public boolean debug;

    // Stockage
    public String storageType;
    public int autoSaveMinutes;
    public int jsonBackups;
    public boolean jsonPretty;
    public String sqliteFile;
    public String mysqlHost;
    public int mysqlPort;
    public String mysqlDatabase;
    public String mysqlUser;
    public String mysqlPassword;
    public String mysqlPrefix;
    public String mysqlParameters;

    // Planning
    public ZoneId zone;
    public boolean scheduleEnabled;
    /** Minutes avant le démarrage annoncées aux joueurs. */
    public final List<Integer> warnMinutes = new ArrayList<Integer>();

    // Évènement
    public boolean onlyOneAtATime;
    public int captureSeconds;
    public int maxDurationMinutes;
    public Interruption whenEmpty;
    public Interruption whenContested;
    /** Effet d'une prise de contrôle par un autre groupe (REMISE-A-ZERO ou CONTINUER). */
    public Interruption whenControlChanges;
    public Target target;
    /** Points de faction crédités à la faction gagnante. */
    public long victoryPoints;
    public boolean broadcast;
    /** Secondes restantes qui déclenchent une annonce globale. */
    public final List<Integer> announceSeconds = new ArrayList<Integer>();
    public int participationSeconds;
    public boolean fireworks;
    public boolean soloAllowed;

    // Anti-abus
    public boolean ignoreCreative;
    public boolean ignoreSpectator;
    public boolean ignoreSameIp;
    public final Set<String> ignoredPermissions = new HashSet<String>();

    // Protection de la zone
    public boolean protectDuringEventOnly;
    public boolean protectBreak;
    public boolean protectPlace;
    public boolean protectPearl;
    public boolean protectContainers;

    // Affichage
    public Audience audience;
    public int nearRadius;
    public int barLength;
    public String barSymbol;
    public String barFull;
    public String barEmpty;
    public int topShown;
    public int percentDecimals;
    public boolean scoreboard;

    // Titres
    public int titleFadeIn;
    public int titleStay;
    public int titleFadeOut;

    // Intégrations
    public boolean questIntegration;
    public String questObjective;
    public boolean guiIntegration;
    public String guiPrefix;

    private final Map<String, String> sounds = new HashMap<String, String>();

    public void load(ConfigurationSection c, Logger logger) {
        debug = c.getBoolean("debug");

        storageType = str(c, "stockage.type", "JSON").toUpperCase(Locale.ROOT);
        autoSaveMinutes = c.getInt("stockage.sauvegarde-auto-minutes");
        jsonBackups = c.getInt("stockage.copies-de-securite");
        jsonPretty = c.getBoolean("stockage.json-indente");
        sqliteFile = str(c, "stockage.sqlite-fichier", "donnees.db");
        mysqlHost = str(c, "stockage.mysql.hote", "localhost");
        mysqlPort = c.getInt("stockage.mysql.port", 3306);
        mysqlDatabase = str(c, "stockage.mysql.base", "koriakoth");
        mysqlUser = str(c, "stockage.mysql.utilisateur", "root");
        mysqlPassword = str(c, "stockage.mysql.mot-de-passe", "");
        mysqlPrefix = str(c, "stockage.mysql.prefixe-tables", "kk_");
        mysqlParameters = str(c, "stockage.mysql.parametres", "");

        try {
            zone = ZoneId.of(str(c, "planning.fuseau", "Europe/Paris"));
        } catch (DateTimeException e) {
            logger.warning("Fuseau horaire inconnu dans planning.fuseau : " + c.getString("planning.fuseau")
                    + " (Europe/Paris utilisé)");
            zone = ZoneId.of("Europe/Paris");
        }
        scheduleEnabled = !c.contains("planning.actif") || c.getBoolean("planning.actif");
        warnMinutes.clear();
        for (Integer minutes : c.getIntegerList("planning.annonce-avant-minutes")) {
            if (minutes != null && minutes > 0) {
                warnMinutes.add(minutes);
            }
        }

        onlyOneAtATime = c.getBoolean("evenement.un-seul-a-la-fois");
        captureSeconds = Math.max(1, c.getInt("evenement.capture-secondes", 300));
        maxDurationMinutes = Math.max(0, c.getInt("evenement.duree-max-minutes", 60));
        whenEmpty = interruption(c.getString("evenement.zone-vide"), Interruption.PAUSE, "evenement.zone-vide", logger);
        whenContested = interruption(c.getString("evenement.contestation"), Interruption.PAUSE,
                "evenement.contestation", logger);
        whenControlChanges = interruption(c.getString("evenement.changement-controle"), Interruption.REMISE_A_ZERO,
                "evenement.changement-controle", logger);
        Target parsedTarget = Target.parse(c.getString("evenement.cible-recompenses"), Target.MEMBRES_EN_ZONE);
        if (parsedTarget == null) {
            logger.warning("Cible inconnue dans evenement.cible-recompenses : "
                    + c.getString("evenement.cible-recompenses") + " (MEMBRES_EN_ZONE utilisée)");
            parsedTarget = Target.MEMBRES_EN_ZONE;
        }
        target = parsedTarget;
        victoryPoints = Math.max(0, c.getLong("evenement.points-victoire", 500));
        broadcast = c.getBoolean("evenement.annonce-globale");
        announceSeconds.clear();
        for (Integer seconds : c.getIntegerList("evenement.annonce-paliers-secondes")) {
            if (seconds != null && seconds > 0) {
                announceSeconds.add(seconds);
            }
        }
        participationSeconds = Math.max(0, c.getInt("evenement.participation-secondes-minimum"));
        fireworks = c.getBoolean("evenement.feux-artifice");
        soloAllowed = c.getBoolean("evenement.joueurs-sans-faction");

        ignoreCreative = c.getBoolean("anti-abus.ignorer-creatif");
        ignoreSpectator = c.getBoolean("anti-abus.ignorer-spectateur");
        ignoreSameIp = c.getBoolean("anti-abus.ignorer-meme-ip");
        ignoredPermissions.clear();
        for (String node : c.getStringList("anti-abus.permissions-ignorees")) {
            if (node != null && !node.trim().isEmpty()) {
                ignoredPermissions.add(node.trim());
            }
        }

        protectDuringEventOnly = c.getBoolean("protection.pendant-evenement-seulement");
        protectBreak = c.getBoolean("protection.casser-blocs");
        protectPlace = c.getBoolean("protection.poser-blocs");
        protectPearl = c.getBoolean("protection.enderpearl");
        protectContainers = c.getBoolean("protection.conteneurs");

        audience = audience(str(c, "affichage.destinataires", "ZONE"), logger);
        nearRadius = Math.max(1, c.getInt("affichage.rayon-proches", 50));
        barLength = Math.max(1, c.getInt("affichage.barre.longueur", 20));
        barSymbol = str(c, "affichage.barre.symbole", "|");
        barFull = str(c, "affichage.barre.couleur-pleine", "&a");
        barEmpty = str(c, "affichage.barre.couleur-vide", "&8");
        topShown = Math.max(1, c.getInt("affichage.top-affiches", 10));
        percentDecimals = Math.max(0, Math.min(2, c.getInt("affichage.decimales-pourcentage")));
        scoreboard = c.getBoolean("affichage.scoreboard");

        titleFadeIn = c.getInt("titres.apparition", 5);
        titleStay = c.getInt("titres.duree", 40);
        titleFadeOut = c.getInt("titres.disparition", 10);

        questIntegration = c.getBoolean("integrations.koriaquest");
        questObjective = str(c, "integrations.objectif-koriaquest", "GAGNER_KOTH").toUpperCase(Locale.ROOT);
        guiIntegration = c.getBoolean("integrations.koria-gui");
        guiPrefix = str(c, "integrations.koria-gui-prefixe", "koth_");

        sounds.clear();
        ConfigurationSection soundSection = c.getConfigurationSection("sons");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                sounds.put(key, soundSection.getString(key, ""));
            }
        }
    }

    /** Son de la section sons (chaîne vide si absent). */
    public String sound(String key) {
        String value = sounds.get(key);
        return value == null ? "" : value;
    }

    /** Un joueur ignoré ne capture pas et ne conteste pas (staff en modération). */
    public boolean isIgnored(Player player) {
        if (player == null) {
            return true;
        }
        if (ignoreCreative && player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return true;
        }
        if (ignoreSpectator && player.getGameMode().name().equals("SPECTATOR")) {
            return true;
        }
        for (String node : ignoredPermissions) {
            if (player.hasPermission(node)) {
                return true;
            }
        }
        return false;
    }

    private Audience audience(String raw, Logger logger) {
        String value = raw.trim().toUpperCase(Locale.ROOT);
        for (Audience candidate : Audience.values()) {
            if (candidate.name().equals(value)) {
                return candidate;
            }
        }
        logger.warning("Valeur inconnue dans affichage.destinataires : " + raw + " (ZONE utilisé)");
        return Audience.ZONE;
    }

    private Interruption interruption(String raw, Interruption def, String path, Logger logger) {
        Interruption parsed = Interruption.parse(raw, def);
        if (parsed == null) {
            logger.warning("Valeur inconnue dans " + path + " : " + raw + " (" + def.name() + " utilisé)");
            return def;
        }
        return parsed;
    }

    private static String str(ConfigurationSection c, String path, String def) {
        String value = c.getString(path);
        return value == null ? def : value;
    }
}
