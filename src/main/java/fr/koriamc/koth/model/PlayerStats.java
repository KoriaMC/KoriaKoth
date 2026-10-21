package fr.koriamc.koth.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Statistiques d'un joueur (sérialisées en JSON) : captures, temps passé dans les
 * zones et récompenses en attente s'il était hors ligne.
 */
public final class PlayerStats {

    private String uuid;
    private String name;
    private int captures;
    private int participations;
    private long secondsInZone;
    private long captureSeconds;
    private List<String> pending;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid.toString();
        this.name = name == null ? "?" : name;
        this.pending = new ArrayList<String>();
    }

    /** Recrée les collections absentes après une lecture JSON. */
    public void ensureInit() {
        if (pending == null) {
            pending = new ArrayList<String>();
        }
        if (name == null) {
            name = "?";
        }
    }

    /** Clé de stockage (UUID sous forme de texte). */
    public String getId() {
        return uuid;
    }

    public UUID getUniqueId() {
        return UUID.fromString(uuid);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isEmpty()) {
            this.name = name;
        }
    }

    /** Nombre de fois où ce joueur a été le capteur d'un KOTH. */
    public int getCaptures() {
        return captures;
    }

    public void addCapture() {
        captures++;
    }

    /** Nombre d'évènements auxquels le joueur a participé. */
    public int getParticipations() {
        return participations;
    }

    public void addParticipation() {
        participations++;
    }

    public long getSecondsInZone() {
        return secondsInZone;
    }

    public void addSecondsInZone(long seconds) {
        secondsInZone += Math.max(0, seconds);
    }

    /** Temps cumulé passé à capturer (dans la zone pendant que son groupe la contrôlait). */
    public long getCaptureSeconds() {
        return captureSeconds;
    }

    public void addCaptureSeconds(long seconds) {
        captureSeconds += Math.max(0, seconds);
    }

    /** Récompenses à donner à la prochaine connexion. */
    public List<String> getPending() {
        return pending;
    }

    /** Un joueur sans statistique ni récompense en attente n'est pas sauvegardé. */
    public boolean isEmpty() {
        return captures == 0 && participations == 0 && secondsInZone == 0 && pending.isEmpty();
    }
}
