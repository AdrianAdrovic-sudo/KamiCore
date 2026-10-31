package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.util.Msg;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

/**
 * /shenron - summon the Eternal Dragon with all 7 Dragon Balls and make a wish.
 */
public final class ShenronCommand implements BasicCommand {

    private final KamiCore plugin;

    public ShenronCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }
        plugin.wishes().summon(player);
    }

    @Override
    public String permission() {
        return "kamicore.shenron";
    }
}
