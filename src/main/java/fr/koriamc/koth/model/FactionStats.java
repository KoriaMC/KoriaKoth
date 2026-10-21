package fr.koriamc.koth.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Statistiques d'une faction (sérialisées en JSON) : victoires, temps de contrôle
 * cumulé et détail par KOTH.
 */
public final class FactionStats {

    private String factionId;
    private String tag;
    private int wins;
    private long controlSeconds;
    private long lastWin;
    private Map<String, Integer> winsPerKoth;

    public FactionStats(String factionId, String tag) {
        this.factionId = factionId;
        this.tag = tag;
        this.winsPerKoth = new LinkedHashMap<String, Integer>();
    }

    /** Recrée les collections absentes après une lecture JSON. */
    public void ensureInit() {
        if (winsPerKoth == null) {
            winsPerKoth = new LinkedHashMap<String, Integer>();
        }
    }

    public String getFactionId() {
        return factionId;
    }

    /** Tag connu lors de la dernière victoire (la faction peut avoir été dissoute). */
    public String getTag() {
        return tag == null ? "?" : tag;
    }

    public void setTag(String tag) {
        if (tag != null && !tag.isEmpty()) {
            this.tag = tag;
        }
    }

    public int getWins() {
        return wins;
    }

    public long getControlSeconds() {
        return controlSeconds;
    }

    public void addControlSeconds(long seconds) {
        controlSeconds += Math.max(0, seconds);
    }

    public long getLastWin() {
        return lastWin;
    }

    public int getWins(String kothId) {
        Integer value = winsPerKoth.get(kothId);
        return value == null ? 0 : value;
    }

    /** Enregistre une victoire sur un KOTH. */
    public void addWin(String kothId) {
        wins++;
        lastWin = System.currentTimeMillis();
        Integer current = winsPerKoth.get(kothId);
        winsPerKoth.put(kothId, current == null ? 1 : current + 1);
    }

    public Map<String, Integer> getWinsPerKoth() {
        return winsPerKoth;
    }

    /** Une faction sans victoire ni temps de contrôle n'est pas sauvegardée. */
    public boolean isEmpty() {
        return wins == 0 && controlSeconds == 0;
    }
}
