package fr.koriamc.koth.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Données générales sauvegardées : dernier vainqueur de chaque KOTH.
 */
public final class MetaState {

    /** Vainqueur d'un KOTH. */
    public static final class Winner {

        private String factionId;
        private String tag;
        private String player;
        private long time;

        public Winner(String factionId, String tag, String player) {
            this.factionId = factionId;
            this.tag = tag;
            this.player = player;
            this.time = System.currentTimeMillis();
        }

        /** Identifiant de la faction gagnante, ou null pour un joueur seul. */
        public String getFactionId() {
            return factionId;
        }

        /** Tag de la faction, ou le pseudo du joueur s'il n'avait pas de faction. */
        public String getTag() {
            return tag == null ? "?" : tag;
        }

        public String getPlayer() {
            return player == null ? "?" : player;
        }

        public long getTime() {
            return time;
        }
    }

    private Map<String, Winner> last;

    public void ensureInit() {
        if (last == null) {
            last = new LinkedHashMap<String, Winner>();
        }
    }

    /** @return le dernier vainqueur de ce KOTH, ou null */
    public Winner getLast(String kothId) {
        return last.get(kothId);
    }

    public void setLast(String kothId, Winner winner) {
        last.put(kothId, winner);
    }

    public Map<String, Winner> getLast() {
        return last;
    }
}
