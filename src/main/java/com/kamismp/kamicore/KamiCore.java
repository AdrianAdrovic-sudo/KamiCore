package com.kamismp.kamicore;

import com.kamismp.kamicore.commands.GravityCommand;
import com.kamismp.kamicore.commands.KaiokenCommand;
import com.kamismp.kamicore.commands.KamiCoreCommand;
import com.kamismp.kamicore.commands.KiMarkCommand;
import com.kamismp.kamicore.commands.KiResponseCommand;
import com.kamismp.kamicore.commands.KiSenseCommand;
import com.kamismp.kamicore.commands.NimbusCommand;
import com.kamismp.kamicore.commands.SenzuCommand;
import com.kamismp.kamicore.commands.ShunkanCommand;
import com.kamismp.kamicore.data.DataStore;
import com.kamismp.kamicore.listeners.PlayerListener;
import com.kamismp.kamicore.util.Cooldowns;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.TeleportRequests;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of the plugin. Paper calls onEnable() when the server starts
 * and onDisable() when it stops.
 */
public final class KamiCore extends JavaPlugin {

    private DataStore dataStore;
    private Cooldowns cooldowns;
    private TeleportRequests teleportRequests;
    private NimbusCommand nimbus;
    private GravityCommand gravity;
    private KaiokenCommand kaioken;

    @Override
    public void onEnable() {
        // Creates plugins/KamiCore/config.yml the first time
        saveDefaultConfig();
        Msg.setPrefix(getConfig().getString("prefix", ""));

        // Saved player data (ki marks + cooldowns) in plugins/KamiCore/data.yml
        dataStore = new DataStore(this);
        dataStore.load();

        cooldowns = new Cooldowns(this);
        teleportRequests = new TeleportRequests(this);

        nimbus = new NimbusCommand(this);
        gravity = new GravityCommand(this);
        kaioken = new KaiokenCommand(this);

        // ---- Register commands: name, description, aliases, handler ----
        registerCommand("kimark", "Save, delete or list your ki marks", List.of("marks"), new KiMarkCommand(this));
        registerCommand("shunkan", "Instant Transmission to a ki mark or coordinates", List.of("it"), new ShunkanCommand(this));
        registerCommand("kisense", "Ask to Instant Transmission to a player", List.of("tpa"), new KiSenseCommand(this));
        registerCommand("kiaccept", "Accept a ki sense request", List.of("tpaccept"), new KiResponseCommand(this, true));
        registerCommand("kideny", "Deny a ki sense request", List.of("tpdeny"), new KiResponseCommand(this, false));
        registerCommand("nimbus", "Call the Flying Nimbus", List.of(), nimbus);
        registerCommand("senzu", "Eat a Senzu Bean", List.of(), new SenzuCommand(this));
        registerCommand("gravity", "Gravity chamber training", List.of("gravitytraining"), gravity);
        registerCommand("kaioken", "Use the Kaioken technique", List.of(), kaioken);
        registerCommand("kamicore", "KamiCore help and admin", List.of("kami"), new KamiCoreCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        nimbus.startParticleTask();

        getLogger().info("KamiCore enabled - power level is over 9000!");
    }

    @Override
    public void onDisable() {
        // Clean up so nobody keeps flying / keeps effects after a restart
        if (nimbus != null) nimbus.dismountAll();
        if (gravity != null) gravity.cancelAll();
        if (kaioken != null) kaioken.cancelAll();
        if (dataStore != null) dataStore.save();
    }

    /** Reloads config.yml (used by /kamicore reload). */
    public void reloadEverything() {
        reloadConfig();
        Msg.setPrefix(getConfig().getString("prefix", ""));
    }

    public DataStore data() { return dataStore; }
    public Cooldowns cooldowns() { return cooldowns; }
    public TeleportRequests teleportRequests() { return teleportRequests; }
    public NimbusCommand nimbus() { return nimbus; }
    public GravityCommand gravity() { return gravity; }
    public KaiokenCommand kaioken() { return kaioken; }
}
