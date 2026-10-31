package com.kamismp.kamicore.saiyan;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Super Saiyan transformations.
 *  - every second the form drains XP points; no XP left = you power down
 *  - buffs (Strength, Speed, Resistance...), extra hearts and flight while transformed
 *  - the "color change": a colored aura plus a colored glow outline and name color
 */
public final class SuperSaiyan {

    private static final String COOLDOWN_KEY = "supersaiyan";
    private static final String TEAM_PREFIX = "kc_";

    /** Who is transformed, into what, and which team they were on before. */
    private static final class State {
        SaiyanForm form;
        String previousTeam;
        boolean wasGlowing;
    }

    private final KamiCore plugin;
    private final NamespacedKey healthKey;
    private final Map<UUID, State> transformed = new HashMap<>();
    private BukkitTask task;
    private int tickCounter;

    public SuperSaiyan(KamiCore plugin) {
        this.plugin = plugin;
        this.healthKey = new NamespacedKey(plugin, "super_saiyan");
    }

    // =================================================================== forms

    /** All forms from config.yml, weakest first. */
    public List<SaiyanForm> forms() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("supersaiyan.forms");
        List<SaiyanForm> forms = new ArrayList<>();
        if (section == null) {
            return forms;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s != null) {
                forms.add(SaiyanForm.fromConfig(id, s));
            }
        }
        return forms;
    }

    public SaiyanForm form(String id) {
        for (SaiyanForm form : forms()) {
            if (form.id().equalsIgnoreCase(id)) {
                return form;
            }
        }
        return null;
    }

    public boolean isTransformed(Player player) {
        return transformed.containsKey(player.getUniqueId());
    }

    /** The current form, or null. */
    public SaiyanForm current(Player player) {
        State state = transformed.get(player.getUniqueId());
        return state == null ? null : state.form;
    }

    public double kiMultiplier(Player player) {
        SaiyanForm form = current(player);
        return form == null ? 1.0 : form.kiMultiplier();
    }

    public double powerMultiplier(Player player) {
        SaiyanForm form = current(player);
        return form == null ? 1.0 : form.powerMultiplier();
    }

    // ============================================================ transforming

    public void transform(Player player, SaiyanForm form) {
        SaiyanForm currentForm = current(player);
        if (currentForm != null && currentForm.id().equals(form.id())) {
            Msg.send(player, "<gray>You're already in " + form.chatColor() + form.name() + "</gray>.");
            return;
        }
        if (currentForm == null && !plugin.cooldowns().checkReady(player, COOLDOWN_KEY, "Transforming")) {
            return;
        }
        if (player.getLevel() < form.minLevel()) {
            Msg.send(player, "<red>You need at least <yellow>" + form.minLevel() + " XP levels</yellow> of ki to reach "
                    + form.chatColor() + form.name() + "</red>.");
            return;
        }
        if (player.calculateTotalExperiencePoints() < form.xpDrain() * 5) {
            Msg.send(player, "<red>You don't have enough ki (XP) to hold this form.");
            return;
        }

        // Switching forms: undo the old buffs first, but keep the team/glow memory
        State state = transformed.get(player.getUniqueId());
        if (state == null) {
            state = new State();
            Team oldTeam = scoreboard().getEntryTeam(player.getName());
            state.previousTeam = oldTeam != null && !oldTeam.getName().startsWith(TEAM_PREFIX) ? oldTeam.getName() : null;
            state.wasGlowing = player.isGlowing();
            transformed.put(player.getUniqueId(), state);
            plugin.nimbus().dismount(player, false);
        } else {
            removeHealthBonus(player);
        }
        state.form = form;

        applyHealthBonus(player, form);
        applyEffects(player, form);
        if (form.flight()) {
            player.setAllowFlight(true);
        } else if (!canFlyAnyway(player)) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        if (plugin.getConfig().getBoolean("supersaiyan.glow", true)) {
            teamFor(form).addEntry(player.getName());
            player.setGlowing(true);
        }

        // The big moment
        Location loc = player.getLocation();
        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.6f);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 150, 0.8, 1.2, 0.8, 0,
                new Particle.DustOptions(form.auraColor(), 2.0f));

        String message = "<yellow>" + player.getName() + "</yellow> <gray>has transformed into</gray> " + form.chatColor() + "<b>" + form.name() + "</b>!";
        for (Player nearby : loc.getWorld().getNearbyPlayers(loc, 64)) {
            if (!nearby.equals(player)) {
                Msg.send(nearby, message);
            }
        }
        Msg.send(player, form.chatColor() + "<b>" + form.name().toUpperCase(java.util.Locale.ROOT) + "!</b><reset> <gray>Draining <yellow>"
                + form.xpDrain() + " XP/s</yellow>. <yellow>/ssj off</yellow> to power down.");
    }

    /** Powers down. reason = message to show, or null for none. */
    public void revert(Player player, String reason) {
        State state = transformed.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        removeHealthBonus(player);
        removeEffects(player);
        if (!canFlyAnyway(player)) {
            player.setFlying(false);
            player.setAllowFlight(false);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 8, 0, false, false, true));
        }

        Team team = scoreboard().getEntryTeam(player.getName());
        if (team != null && team.getName().startsWith(TEAM_PREFIX)) {
            team.removeEntry(player.getName());
        }
        if (state.previousTeam != null) {
            Team previous = scoreboard().getTeam(state.previousTeam);
            if (previous != null) {
                previous.addEntry(player.getName());
            }
        }
        player.setGlowing(state.wasGlowing);

        plugin.cooldowns().start(player, COOLDOWN_KEY, plugin.getConfig().getLong("supersaiyan.cooldown-after", 30));
        if (reason != null && player.isOnline()) {
            Msg.send(player, reason);
        }
    }

    private boolean canFlyAnyway(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR
                || plugin.nimbus().isRiding(player);
    }

    public void revertAll() {
        for (UUID id : List.copyOf(transformed.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                revert(player, null);
            }
        }
        transformed.clear();
    }

    /** If the server crashed while someone was transformed, clean up leftovers when they join. */
    public void cleanupOnJoin(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getModifier(healthKey) != null) {
            maxHealth.removeModifier(healthKey);
            player.setGlowing(false);
            if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                player.setAllowFlight(false);
            }
        }
        Team team = scoreboard().getEntryTeam(player.getName());
        if (team != null && team.getName().startsWith(TEAM_PREFIX)) {
            team.removeEntry(player.getName());
        }
    }

    // ================================================================ the loop

    /** One repeating task: aura every 4 ticks, XP drain + buffs every second. */
    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tickCounter++;
            boolean everySecond = tickCounter % 5 == 0;
            for (UUID id : List.copyOf(transformed.keySet())) {
                Player player = Bukkit.getPlayer(id);
                State state = transformed.get(id);
                if (player == null || state == null) {
                    transformed.remove(id);
                    continue;
                }
                drawAura(player, state.form);
                if (everySecond) {
                    drain(player, state.form);
                }
            }
        }, 4L, 4L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void drain(Player player, SaiyanForm form) {
        int total = player.calculateTotalExperiencePoints();
        if (total < form.xpDrain()) {
            revert(player, "<red>You're out of ki!</red> <gray>Your " + form.name() + " fades away.");
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.8f);
            return;
        }
        player.setExperienceLevelAndProgress(total - form.xpDrain());
        applyEffects(player, form);
        Msg.actionBar(player, form.chatColor() + "<b>" + form.name() + "</b><reset>"
                + " <dark_gray>|</dark_gray> <gray>Ki: <green>" + player.getLevel() + " lvl</green> <red>(-" + form.xpDrain() + " XP/s)");
    }

    private void applyEffects(Player player, SaiyanForm form) {
        int ticks = 50; // a bit more than the 1 s refresh, so buffs vanish fast after powering down
        give(player, PotionEffectType.STRENGTH, form.strength(), ticks);
        give(player, PotionEffectType.SPEED, form.speed(), ticks);
        give(player, PotionEffectType.RESISTANCE, form.resistance(), ticks);
        give(player, PotionEffectType.JUMP_BOOST, form.jump(), ticks);
        give(player, PotionEffectType.REGENERATION, form.regeneration(), ticks);
    }

    private static void give(Player player, PotionEffectType type, int level, int ticks) {
        if (level > 0) {
            player.addPotionEffect(new PotionEffect(type, ticks, level - 1, false, false, false));
        }
    }

    private static void removeEffects(Player player) {
        for (PotionEffectType type : List.of(PotionEffectType.STRENGTH, PotionEffectType.SPEED, PotionEffectType.RESISTANCE,
                PotionEffectType.JUMP_BOOST, PotionEffectType.REGENERATION)) {
            PotionEffect effect = player.getPotionEffect(type);
            if (effect != null && effect.getDuration() <= 60) {
                player.removePotionEffect(type); // only remove our short buffs, not potions they drank
            }
        }
    }

    private void applyHealthBonus(Player player, SaiyanForm form) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null || form.extraHearts() <= 0) {
            return;
        }
        maxHealth.addModifier(new AttributeModifier(healthKey, form.extraHearts() * 2, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        player.setHealth(Math.min(maxHealth.getValue(), player.getHealth() + form.extraHearts() * 2));
    }

    private void removeHealthBonus(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null || maxHealth.getModifier(healthKey) == null) {
            return;
        }
        maxHealth.removeModifier(healthKey);
        if (player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    private void drawAura(Player player, SaiyanForm form) {
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.35, 0.9, 0.35, 0,
                new Particle.DustOptions(form.auraColor(), 1.4f));
        if (form.lightning() && ThreadLocalRandom.current().nextInt(3) == 0) {
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 6, 0.4, 0.9, 0.4, 0.1);
        }
        if (form.dodgeChance() > 0) {
            player.getWorld().spawnParticle(Particle.END_ROD, loc, 2, 0.4, 0.9, 0.4, 0.01);
        }
    }

    /** Ultra Instinct: maybe dodge an attack. Returns true if dodged. */
    public boolean tryDodge(Player player) {
        SaiyanForm form = current(player);
        if (form == null || form.dodgeChance() <= 0 || ThreadLocalRandom.current().nextDouble() >= form.dodgeChance()) {
            return false;
        }
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 15, 0.4, 0.8, 0.4, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 2.0f);
        Msg.actionBar(player, "<white><b>DODGED!</b></white> <gray>Your body moved on its own.");
        return true;
    }

    // ================================================================== teams

    private Scoreboard scoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    private Team teamFor(SaiyanForm form) {
        String name = TEAM_PREFIX + form.id();
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        Team team = scoreboard().getTeam(name);
        if (team == null) {
            team = scoreboard().registerNewTeam(name);
        }
        team.color(form.glowColor());
        return team;
    }
}
