package fr.koriamc.koth;

import fr.koriamc.koth.command.CommandManager;
import fr.koriamc.koth.config.ConfigDocs;
import fr.koriamc.koth.config.JsonFile;
import fr.koriamc.koth.config.KothCatalog;
import fr.koriamc.koth.config.Messages;
import fr.koriamc.koth.config.Settings;
import fr.koriamc.koth.game.Display;
import fr.koriamc.koth.game.KothManager;
import fr.koriamc.koth.gui.Menu;
import fr.koriamc.koth.gui.MenuManager;
import fr.koriamc.koth.hook.FactionHook;
import fr.koriamc.koth.hook.GuiHook;
import fr.koriamc.koth.hook.PapiHook;
import fr.koriamc.koth.hook.QuestHook;
import fr.koriamc.koth.hook.VaultHook;
import fr.koriamc.koth.listener.KothListener;
import fr.koriamc.koth.reward.RewardService;
import fr.koriamc.koth.storage.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * KoriaKoth : évènement KOTH (King of the Hill) pour KoriaMC (Spigot 1.8.8).
 * Une faction capture une zone en y restant seule le temps demandé.
 */
public final class KoriaKoth extends JavaPlugin {

    private static KoriaKoth instance;

    // Fichiers
    private JsonFile configFile;
    private JsonFile kothsFile;
    private JsonFile messagesFile;
    private JsonFile menusFile;
    private Configuration config = new MemoryConfiguration();
    private Configuration messagesConfig = new MemoryConfiguration();
    private Configuration menusConfig = new MemoryConfiguration();

    // Réglages
    private final Settings settings = new Settings();
    private final KothCatalog catalog = new KothCatalog();
    private final Messages messages = new Messages();
    private final ConfigDocs configDocs = new ConfigDocs();

    // Services
    private DataManager data;
    private FactionHook factionHook;
    private KothManager koths;
    private Display display;
    private RewardService rewards;
    private MenuManager menus;
    private CommandManager commands;
    private QuestHook questHook;
    private GuiHook guiHook;
    private PapiHook papi;
    private VaultHook vault;

    public static KoriaKoth get() {
        return instance;
    }

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        instance = this;

        configFile = new JsonFile(this, "config.json");
        kothsFile = new JsonFile(this, "koths.json", "*");
        messagesFile = new JsonFile(this, "messages.json");
        menusFile = new JsonFile(this, "menus.json", "liste.objets", "top.objets", "editeur.fichiers");

        data = new DataManager(this);
        factionHook = new FactionHook();
        koths = new KothManager(this);
        display = new Display(this);
        rewards = new RewardService(this);
        menus = new MenuManager(this);
        commands = new CommandManager(this);
        questHook = new QuestHook(this);
        guiHook = new GuiHook(this);

        for (String problem : loadConfigs()) {
            getLogger().warning(problem);
        }
        configDocs.load(this);
        configDocs.writeReadme(this, configFiles());

        try {
            data.init();
            data.load();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Impossible de charger les données : le plugin est désactivé pour éviter"
                    + " toute perte.", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                papi = new PapiHook(this);
                getLogger().info("PlaceholderAPI détecté : variables %koriakoth_...% enregistrées.");
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Impossible de s'accrocher à PlaceholderAPI", t);
                papi = null;
            }
        }
        questHook.register();

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new KothListener(this), this);
        pm.registerEvents(menus, this);
        pm.registerEvents(menus.prompt(), this);

        commands.load(config, messagesConfig);
        commands.registerBukkitCommand();

        koths.start();
        data.startAutoSave();
        getLogger().info("KoriaKoth activé en " + (System.currentTimeMillis() - start) + " ms (stockage : "
                + data.getStorageName() + ", " + catalog.count() + " KOTH).");
    }

    @Override
    public void onDisable() {
        if (data == null) {
            return;
        }
        if (commands != null) {
            commands.unregisterBukkitCommand();
        }
        if (koths != null) {
            koths.stop();
            koths.stopAll(false);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                player.closeInventory();
            }
        }
        if (papi != null) {
            papi.unregister();
            papi = null;
        }
        data.shutdown();
        instance = null;
    }

    /** @return les problèmes à signaler (erreurs JSON, KOTH invalides) */
    private List<String> loadConfigs() {
        List<String> problems = new ArrayList<String>();
        for (JsonFile file : configFiles()) {
            String error = file.load();
            if (error != null) {
                problems.add(file.getName() + " : " + error);
            }
        }
        config = configFile.toConfiguration();
        messagesConfig = messagesFile.toConfiguration();
        menusConfig = menusFile.toConfiguration();

        settings.load(config, getLogger());
        messages.load(messagesConfig, settings);
        catalog.load(kothsFile.toConfiguration(), settings);
        problems.addAll(catalog.getProblems());
        return problems;
    }

    /**
     * Recharge toute la configuration (/koth reload, éditeur).
     *
     * @return les problèmes détectés, à afficher à l'auteur de la commande
     */
    public List<String> reload() {
        List<String> problems = loadConfigs();
        commands.load(config, messagesConfig);
        data.startAutoSave();
        questHook.register();
        return problems;
    }

    /** Dépose de l'argent via Vault. @return false si aucune économie n'est disponible */
    public boolean deposit(Player player, double amount) {
        if (vault == null && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vault = VaultHook.create();
        }
        if (vault == null) {
            return false;
        }
        vault.deposit(player, amount);
        return true;
    }

    // ============================================================ Accesseurs

    public Settings settings() {
        return settings;
    }

    public KothCatalog catalog() {
        return catalog;
    }

    public Messages msg() {
        return messages;
    }

    /** Explication des options (éditeur /koth config et LISEZMOI.txt). */
    public ConfigDocs configDocs() {
        return configDocs;
    }

    public Configuration config() {
        return config;
    }

    public Configuration menusConfig() {
        return menusConfig;
    }

    /** Fichiers de configuration, dans l'ordre de l'éditeur. */
    public List<JsonFile> configFiles() {
        return Arrays.asList(configFile, kothsFile, messagesFile, menusFile);
    }

    /** Fichier des KOTH, modifié par /koth create, zone et supprimer. */
    public JsonFile kothsFile() {
        return kothsFile;
    }

    public DataManager data() {
        return data;
    }

    public FactionHook factionHook() {
        return factionHook;
    }

    public KothManager koths() {
        return koths;
    }

    public Display display() {
        return display;
    }

    public RewardService rewards() {
        return rewards;
    }

    public MenuManager menus() {
        return menus;
    }

    public CommandManager commands() {
        return commands;
    }

    public QuestHook questHook() {
        return questHook;
    }

    public GuiHook guiHook() {
        return guiHook;
    }
}
