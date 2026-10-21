package fr.koriamc.koth.model;

import java.util.Locale;

/**
 * Effet d'une interruption de la capture (zone vide ou zone contestée).
 */
public enum Interruption {

    /** Le compte à rebours s'arrête sur sa valeur actuelle. */
    PAUSE,
    /** Le compte à rebours repart du temps total. */
    REMISE_A_ZERO,
    /** Le compte à rebours continue de descendre. */
    CONTINUER;

    /** Lit "PAUSE", "REMISE-A-ZERO" ou "CONTINUER". @return null si la valeur est inconnue */
    public static Interruption parse(String value, Interruption def) {
        if (value == null) {
            return def;
        }
        String v = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (Interruption mode : values()) {
            if (mode.name().equals(v)) {
                return mode;
            }
        }
        return null;
    }
}
