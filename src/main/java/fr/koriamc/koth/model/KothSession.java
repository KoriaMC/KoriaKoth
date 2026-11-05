package fr.koriamc.koth.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Un KOTH en cours : compte à rebours, groupe qui capture et temps passé dans la
 * zone par chaque joueur (pour les récompenses de participation).
 */
public final class KothSession {

    /** Préfixe de la clé d'un joueur sans faction, qui joue alors pour lui seul. */
    public static final String SOLO = "@";

    private final KothDefinition definition;
    private final long startedAt = System.currentTimeMillis();
    private final Map<UUID, Integer> seconds = new LinkedHashMap<UUID, Integer>();
    private final Map<UUID, Integer> capture = new LinkedHashMap<UUID, Integer>();
    private final Map<UUID, String> names = new HashMap<UUID, String>();
    private final Set<UUID> inZone = new LinkedHashSet<UUID>();
    private final Set<Integer> announced = new HashSet<Integer>();

    private double remaining;
    /** Temps de capture du palier en cours (diminue avec les paliers-capture). */
    private int total;
    private String controller;
    private String controllerName;
    private UUID capper;
    private boolean contested;

    public KothSession(KothDefinition definition) {
        this.definition = definition;
        this.total = definition.getCaptureSecondsAt(0);
        this.remaining = total;
    }

    /** Temps de capture du palier en cours. */
    public int getCaptureTotal() {
        return total;
    }

    /**
     * Applique le palier de capture correspondant au temps écoulé : si le temps
     * demandé baisse, le compte à rebours en cours est raccourci d'autant.
     */
    public void refreshCaptureStep() {
        int now = definition.getCaptureSecondsAt(elapsedMillis());
        if (now < total) {
            remaining = Math.max(0, remaining - (total - now));
        }
        total = now;
    }

    public KothDefinition getDefinition() {
        return definition;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long elapsedMillis() {
        return System.currentTimeMillis() - startedAt;
    }

    /** L'évènement a-t-il dépassé sa durée maximale ? */
    public boolean isExpired() {
        int max = definition.getMaxDurationMinutes();
        return max > 0 && elapsedMillis() >= max * 60000L;
    }

    // ==================================================== Compte à rebours

    /** Secondes restantes, arrondies au supérieur pour l'affichage. */
    public int getRemainingSeconds() {
        return (int) Math.ceil(remaining);
    }

    public double getRemaining() {
        return remaining;
    }

    public void decrease(double seconds) {
        remaining = Math.max(0, remaining - seconds);
    }

    public void resetCountdown() {
        total = definition.getCaptureSecondsAt(elapsedMillis());
        remaining = total;
        announced.clear();
    }

    /** Progression de la capture, de 0 à 1. */
    public double getProgress() {
        return total <= 0 ? 1 : Math.max(0, Math.min(1, (total - remaining) / total));
    }

    /** Palier d'annonce déjà passé ? (true la première fois seulement) */
    public boolean announceOnce(int seconds) {
        return announced.add(seconds);
    }

    // ========================================================= Contrôle

    /** Clé du groupe qui capture : identifiant de faction, ou {@link #SOLO} + UUID. */
    public String getController() {
        return controller;
    }

    /** Nom affiché du groupe qui capture (tag de faction ou pseudo). */
    public String getControllerName() {
        return controllerName;
    }

    /** Joueur du groupe présent dans la zone (celui qui recevra la récompense CAPTEUR). */
    public UUID getCapper() {
        return capper;
    }

    public void setController(String controller, String controllerName, UUID capper) {
        this.controller = controller;
        this.controllerName = controllerName;
        this.capper = capper;
    }

    public boolean isContested() {
        return contested;
    }

    public void setContested(boolean contested) {
        this.contested = contested;
    }

    public boolean isSolo() {
        return controller != null && controller.startsWith(SOLO);
    }

    /** @return l'identifiant de faction qui capture, ou null pour un joueur seul */
    public String getControllerFaction() {
        return controller == null || isSolo() ? null : controller;
    }

    // ======================================================= Participation

    /** Ajoute une seconde de présence dans la zone. */
    public void addSecond(UUID uuid, String name) {
        Integer current = seconds.get(uuid);
        seconds.put(uuid, current == null ? 1 : current + 1);
        names.put(uuid, name);
    }

    public int getSeconds(UUID uuid) {
        Integer value = seconds.get(uuid);
        return value == null ? 0 : value;
    }

    public Map<UUID, Integer> getSeconds() {
        return seconds;
    }

    public String getName(UUID uuid) {
        return names.get(uuid);
    }

    /** Joueurs restés au moins {@code minSeconds} dans la zone. */
    public List<UUID> participants(int minSeconds) {
        List<UUID> list = new ArrayList<UUID>();
        for (Map.Entry<UUID, Integer> entry : seconds.entrySet()) {
            if (entry.getValue() >= minSeconds) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    /** Joueurs relevés dans la zone au dernier tick. */
    public Set<UUID> getInZone() {
        return inZone;
    }

    // =========================================================== Capture

    /** Ajoute une seconde de capture : le joueur était dans la zone pendant que son groupe la contrôlait. */
    public void addCaptureSecond(UUID uuid, String name) {
        Integer current = capture.get(uuid);
        capture.put(uuid, current == null ? 1 : current + 1);
        names.put(uuid, name);
    }

    public int getCaptureSeconds(UUID uuid) {
        Integer value = capture.get(uuid);
        return value == null ? 0 : value;
    }

    public Map<UUID, Integer> getCaptureSeconds() {
        return capture;
    }

    /** @return le joueur qui a capturé le plus longtemps pendant cet évènement, ou null */
    public UUID getBestCapturer() {
        UUID best = null;
        int bestSeconds = 0;
        for (Map.Entry<UUID, Integer> entry : capture.entrySet()) {
            if (entry.getValue() > bestSeconds) {
                best = entry.getKey();
                bestSeconds = entry.getValue();
            }
        }
        return best;
    }
}
