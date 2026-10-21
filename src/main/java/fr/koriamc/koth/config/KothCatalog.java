package fr.koriamc.koth.config;

import fr.koriamc.koth.model.KothDefinition;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Les KOTH de koths.json, dans l'ordre du fichier.
 */
public final class KothCatalog {

    private final Map<String, KothDefinition> koths = new LinkedHashMap<String, KothDefinition>();
    private final List<String> problems = new ArrayList<String>();

    public void load(Configuration file, Settings settings) {
        koths.clear();
        problems.clear();
        for (String key : file.getKeys(false)) {
            ConfigurationSection s = file.getConfigurationSection(key);
            if (s == null) {
                problems.add("koths.json : " + key + " doit être un bloc { ... }");
                continue;
            }
            String id = key.toLowerCase(Locale.ROOT);
            if (koths.containsKey(id)) {
                problems.add("koths.json : deux KOTH portent le nom " + id);
                continue;
            }
            koths.put(id, KothDefinition.read(id, s, settings, problems));
        }
    }

    /** @return le KOTH, ou null s'il n'existe pas */
    public KothDefinition get(String id) {
        return id == null ? null : koths.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<KothDefinition> all() {
        return koths.values();
    }

    public List<String> ids() {
        return new ArrayList<String>(koths.keySet());
    }

    public int count() {
        return koths.size();
    }

    public List<String> getProblems() {
        return problems;
    }
}
