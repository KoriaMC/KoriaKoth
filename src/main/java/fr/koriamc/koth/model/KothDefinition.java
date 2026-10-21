package fr.koriamc.koth.model;

import fr.koriamc.koth.config.Settings;
import fr.koriamc.koth.util.Items;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

/**
 * Un KOTH décrit dans koths.json. Les options absentes prennent la valeur par
 * défaut de config.json (section evenement).
 */
public final class KothDefinition {

    private final String id;
    private final String name;
    private final String icon;
    private final Zone zone;
    private final int captureSeconds;
    private final Interruption whenEmpty;
    private final Interruption whenContested;
    private final Interruption whenControlChanges;
    private final int maxDurationMinutes;
    private final Schedule schedule;
    private final Target target;
    private final long victoryPoints;
    private final List<String> winnerRewards;
    private final List<String> participantRewards;

    private KothDefinition(String id, String name, String icon, Zone zone, int captureSeconds,
                           Interruption whenEmpty, Interruption whenContested, Interruption whenControlChanges,
                           int maxDurationMinutes,
                           Schedule schedule, Target target, long victoryPoints, List<String> winnerRewards,
                           List<String> participantRewards) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.zone = zone;
        this.captureSeconds = captureSeconds;
        this.whenEmpty = whenEmpty;
        this.whenContested = whenContested;
        this.whenControlChanges = whenControlChanges;
        this.maxDurationMinutes = maxDurationMinutes;
        this.schedule = schedule;
        this.target = target;
        this.victoryPoints = victoryPoints;
        this.winnerRewards = Collections.unmodifiableList(winnerRewards);
        this.participantRewards = Collections.unmodifiableList(participantRewards);
    }

    /**
     * Lit un KOTH de koths.json.
     *
     * @param problems liste alimentée avec les erreurs de configuration
     */
    public static KothDefinition read(String id, ConfigurationSection s, Settings settings, List<String> problems) {
        String where = "koths.json : " + id;
        String name = s.getString("nom", id);
        String icon = s.getString("icone", "BEACON");
        String world = s.getString("monde", "");
        Zone zone = Zone.read(s.getConfigurationSection("zone"), world);
        if (zone == null || !zone.isDefined()) {
            problems.add(where + " : zone incomplète (monde, zone.x1/y1/z1 et zone.x2/y2/z2)");
        }
        int capture = s.getInt("capture-secondes", settings.captureSeconds);
        if (capture <= 0) {
            problems.add(where + ".capture-secondes doit être supérieur à 0");
            capture = settings.captureSeconds;
        }
        Interruption empty = Interruption.parse(s.getString("zone-vide"), settings.whenEmpty);
        if (empty == null) {
            problems.add(where + ".zone-vide inconnu (PAUSE, REMISE-A-ZERO ou CONTINUER)");
            empty = settings.whenEmpty;
        }
        Interruption contested = Interruption.parse(s.getString("contestation"), settings.whenContested);
        if (contested == null) {
            problems.add(where + ".contestation inconnu (PAUSE, REMISE-A-ZERO ou CONTINUER)");
            contested = settings.whenContested;
        }
        Interruption control = Interruption.parse(s.getString("changement-controle"), settings.whenControlChanges);
        if (control == null) {
            problems.add(where + ".changement-controle inconnu (REMISE-A-ZERO ou CONTINUER)");
            control = settings.whenControlChanges;
        }
        int maxMinutes = s.getInt("duree-max-minutes", settings.maxDurationMinutes);
        Schedule schedule = Schedule.read(s.getConfigurationSection("planning"), where + ".planning", problems);
        ConfigurationSection rewards = s.getConfigurationSection("recompenses");
        Target target = Target.parse(rewards == null ? null : rewards.getString("cible"), settings.target);
        if (target == null) {
            problems.add(where + ".recompenses.cible inconnue (CAPTEUR, MEMBRES_EN_ZONE, MEMBRES_EN_LIGNE, MEMBRES, AUCUNE)");
            target = settings.target;
        }
        List<String> winner = Items.stringList(rewards, "vainqueur");
        List<String> participants = Items.stringList(rewards, "participants");
        long points = Math.max(0, s.getLong("points-victoire", settings.victoryPoints));
        return new KothDefinition(id, name, icon, zone, capture, empty, contested, control, Math.max(0, maxMinutes),
                schedule, target, points, winner, participants);
    }

    public String getId() {
        return id;
    }

    /** Nom affiché (codes couleur non traduits). */
    public String getName() {
        return name;
    }

    /** Matériau de l'icône dans les menus. */
    public String getIcon() {
        return icon;
    }

    public Zone getZone() {
        return zone;
    }

    /** Secondes à passer dans la zone pour gagner. */
    public int getCaptureSeconds() {
        return captureSeconds;
    }

    public Interruption getWhenEmpty() {
        return whenEmpty;
    }

    public Interruption getWhenContested() {
        return whenContested;
    }

    /** Effet d'une prise de contrôle par un autre groupe. */
    public Interruption getWhenControlChanges() {
        return whenControlChanges;
    }

    /** Durée maximale de l'évènement en minutes (0 = illimitée). */
    public int getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public Target getTarget() {
        return target;
    }

    /** Points de faction crédités à la faction gagnante (en plus des actions POINTS des récompenses). */
    public long getVictoryPoints() {
        return victoryPoints;
    }

    public List<String> getWinnerRewards() {
        return winnerRewards;
    }

    public List<String> getParticipantRewards() {
        return participantRewards;
    }

    /** Un KOTH est jouable si sa zone est complète et son monde chargé. */
    public boolean isPlayable() {
        return zone != null && zone.isDefined() && zone.getWorld() != null;
    }
}
