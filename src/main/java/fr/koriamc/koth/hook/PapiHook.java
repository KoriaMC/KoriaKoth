package fr.koriamc.koth.hook;

import fr.koriamc.koth.KoriaKoth;

/**
 * Pont vers PlaceholderAPI, chargé uniquement si le plugin est installé.
 */
public final class PapiHook {

    private final KothExpansion expansion;

    public PapiHook(KoriaKoth plugin) {
        this.expansion = new KothExpansion(plugin);
        this.expansion.register();
    }

    public void unregister() {
        try {
            expansion.unregister();
        } catch (Throwable ignored) {
            // PlaceholderAPI déjà arrêté
        }
    }
}
