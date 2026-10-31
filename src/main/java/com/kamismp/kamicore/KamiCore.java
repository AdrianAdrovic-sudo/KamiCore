package com.kamismp.kamicore;

import com.kamismp.kamicore.commands.DragonBallsCommand;
import com.kamismp.kamicore.commands.GravityCommand;
import com.kamismp.kamicore.commands.KaiokenCommand;
import com.kamismp.kamicore.commands.KamiCoreCommand;
import com.kamismp.kamicore.commands.KiAttackCommand;
import com.kamismp.kamicore.commands.KiMarkCommand;
import com.kamismp.kamicore.commands.KiResponseCommand;
import com.kamismp.kamicore.commands.KiSenseCommand;
import com.kamismp.kamicore.commands.NimbusCommand;
import com.kamismp.kamicore.commands.ScouterCommand;
import com.kamismp.kamicore.commands.SenzuCommand;
import com.kamismp.kamicore.commands.ShenronCommand;
import com.kamismp.kamicore.commands.ShunkanCommand;
import com.kamismp.kamicore.commands.SolarFlareCommand;
import com.kamismp.kamicore.commands.SuperSaiyanCommand;
import com.kamismp.kamicore.data.DataStore;
import com.kamismp.kamicore.dragonballs.DragonBallListener;
import com.kamismp.kamicore.dragonballs.DragonBalls;
import com.kamismp.kamicore.dragonballs.Wishes;
import com.kamismp.kamicore.ki.KiAttacks;
import com.kamismp.kamicore.ki.KiStyle;
import com.kamismp.kamicore.listeners.PlayerListener;
import com.kamismp.kamicore.saiyan.SuperSaiyan;
import com.kamismp.kamicore.util.Cooldowns;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.TeleportRequests;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
    private KiAttacks kiAttacks;
    private SuperSaiyan superSaiyan;
    private DragonBalls dragonBalls;
    private Wishes wishes;

    @Override
    public void onEnable() {
        // Creates plugins/KamiCore/config.yml the first time
        saveDefaultConfig();
        // Adds settings for new features to an older config.yml without touching your changes
        getConfig().options().copyDefaults(true);
        saveConfig();
        reloadConfig(); // load the updated file, so new sections are really there in memory
        Msg.setPrefix(getConfig().getString("prefix", ""));

        // Saved player data (ki marks + cooldowns) in plugins/KamiCore/data.yml
        dataStore = new DataStore(this);
        dataStore.load();

        cooldowns = new Cooldowns(this);
        teleportRequests = new TeleportRequests(this);

        nimbus = new NimbusCommand(this);
        gravity = new GravityCommand(this);
        kaioken = new KaiokenCommand(this);
        kiAttacks = new KiAttacks(this);
        superSaiyan = new SuperSaiyan(this);
        wishes = new Wishes(this);
        dragonBalls = new DragonBalls(this);
        dragonBalls.load();

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
        // Ki attacks: one command per attack, all handled by KiAttacks
        registerCommand("kamehameha", "Charge and fire a Kamehameha", List.of("kame"), new KiAttackCommand(this, KiStyle.KAMEHAMEHA));
        registerCommand("galickgun", "Charge and fire a Galick Gun", List.of("galick"), new KiAttackCommand(this, KiStyle.GALICK_GUN));
        registerCommand("finalflash", "Charge and fire a Final Flash", List.of(), new KiAttackCommand(this, KiStyle.FINAL_FLASH));
        registerCommand("specialbeamcannon", "Charge and fire a Special Beam Cannon", List.of("sbc", "makankosappo"),
                new KiAttackCommand(this, KiStyle.SPECIAL_BEAM_CANNON));
        registerCommand("spiritbomb", "Gather energy for a Spirit Bomb", List.of("genkidama"), new KiAttackCommand(this, KiStyle.SPIRIT_BOMB));
        registerCommand("solarflare", "Blind everyone who can see you", List.of("taiyoken"), new SolarFlareCommand(this));
        registerCommand("scouter", "Read power levels, locations and track Dragon Balls", List.of(), new ScouterCommand(this));
        registerCommand("ssj", "Transform into a Super Saiyan", List.of("supersaiyan", "transform"), new SuperSaiyanCommand(this));
        registerCommand("shenron", "Summon Shenron with all 7 Dragon Balls", List.of(), new ShenronCommand(this));
        registerCommand("dragonballs", "Dragon Ball admin tools", List.of(), new DragonBallsCommand(this));
        registerCommand("kamicore", "KamiCore help and admin", List.of("kami"), new KamiCoreCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new DragonBallListener(this), this);
        nimbus.startParticleTask();
        superSaiyan.start();
        dragonBalls.start();

        // Every minute: end expired wishes (Immortal Body, Unlimited Power)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                wishes.refresh(player);
            }
        }, 1200L, 1200L);

        getLogger().info("KamiCore enabled - power level is over 9000!");
    }

    @Override
    public void onDisable() {
        // Clean up so nobody keeps flying / keeps effects after a restart
        if (nimbus != null) nimbus.dismountAll();
        if (gravity != null) gravity.cancelAll();
        if (kaioken != null) kaioken.cancelAll();
        if (kiAttacks != null) kiAttacks.cancelAll();
        if (superSaiyan != null) {
            superSaiyan.stop();
            superSaiyan.revertAll();
        }
        if (dragonBalls != null) dragonBalls.stop();
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
    public KiAttacks kiAttacks() { return kiAttacks; }
    public SuperSaiyan superSaiyan() { return superSaiyan; }
    public DragonBalls dragonBalls() { return dragonBalls; }
    public Wishes wishes() { return wishes; }
}