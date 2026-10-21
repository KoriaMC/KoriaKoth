package fr.koriamc.koth.hook;

import fr.koriamc.koth.KoriaKoth;
import fr.koriamc.koth.model.KothDefinition;
import fr.koriamc.koth.model.KothSession;
import fr.koriamc.koth.util.Text;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Pont facultatif vers le système de GUI serveur de KoriaMC (fr.koriamc.gui.KoriaGuiAPI,
 * fourni par le serveur lui-même), par réflexion.
 * <p>
 * Les valeurs sont publiées en variables globales, utilisables dans les layouts du
 * client avec %gvar_koth_nom%, %gvar_koth_temps%, %gvar_koth_controle%...
 */
public final class GuiHook {

    private final KoriaKoth plugin;
    private final Map<String, String> pushed = new HashMap<String, String>();
    private boolean checked;
    private Method setGlobalVar;

    public GuiHook(KoriaKoth plugin) {
        this.plugin = plugin;
    }

    private boolean available() {
        if (!plugin.settings().guiIntegration) {
            return false;
        }
        if (!checked) {
            checked = true;
            try {
                setGlobalVar = Class.forName("fr.koriamc.gui.KoriaGuiAPI")
                        .getMethod("setGlobalVar", String.class, String.class);
                plugin.getLogger().info("Interface KoriaMC détectée : variables %gvar_" + plugin.settings().guiPrefix
                        + "...% publiées pendant les évènements.");
            } catch (Throwable t) {
                setGlobalVar = null;
            }
        }
        return setGlobalVar != null;
    }

    /** Publie l'état de l'évènement en cours. */
    public void update(KothSession session) {
        if (!available()) {
            return;
        }
        set("actif", "true");
        set("nom", Text.strip(session.getDefinition().getName()));
        set("id", session.getDefinition().getId());
        set("temps", plugin.display().time(session.getRemainingSeconds()));
        set("secondes", String.valueOf(session.getRemainingSeconds()));
        set("pourcentage", plugin.display().percent(session.getProgress()));
        set("controle", session.getControllerName() == null ? "" : Text.strip(session.getControllerName()));
        set("joueurs", String.valueOf(session.getInZone().size()));
        Object[] vars = plugin.display().vars(session);
        for (int i = 0; i + 1 < vars.length; i += 2) {
            if ("capteur".equals(vars[i]) || "meilleur".equals(vars[i]) || "meilleur_temps".equals(vars[i])
                    || "x".equals(vars[i]) || "y".equals(vars[i]) || "z".equals(vars[i])) {
                set(String.valueOf(vars[i]), Text.strip(String.valueOf(vars[i + 1])));
            }
        }
    }

    /** Remet les variables à vide à la fin de l'évènement. */
    public void clear(KothDefinition def) {
        if (!available() || plugin.koths().isRunning()) {
            return;
        }
        set("actif", "false");
        set("nom", "");
        set("id", "");
        set("temps", "");
        set("secondes", "0");
        set("pourcentage", "0");
        set("controle", "");
        set("joueurs", "0");
        set("capteur", "");
        set("meilleur", "");
        set("meilleur_temps", "");
    }

    /** N'envoie que les valeurs qui ont changé (chaque écriture rafraîchit les clients). */
    private void set(String key, String value) {
        String full = plugin.settings().guiPrefix + key;
        if (value.equals(pushed.get(full))) {
            return;
        }
        pushed.put(full, value);
        try {
            setGlobalVar.invoke(null, full, value);
        } catch (Throwable t) {
            setGlobalVar = null;
            plugin.getLogger().warning("Interface KoriaMC : publication impossible (" + t + ")");
        }
    }
}
