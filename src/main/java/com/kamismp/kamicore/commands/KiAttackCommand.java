package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.ki.KiStyle;
import com.kamismp.kamicore.util.Msg;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

/**
 * One small class for all ki attacks: /kamehameha, /galickgun, /finalflash,
 * /specialbeamcannon and /spiritbomb. The real work happens in KiAttacks.
 */
public final class KiAttackCommand implements BasicCommand {

    private final KamiCore plugin;
    private final KiStyle style;

    public KiAttackCommand(KamiCore plugin, KiStyle style) {
        this.plugin = plugin;
        this.style = style;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }
        plugin.kiAttacks().use(player, style);
    }

    @Override
    public String permission() {
        return style.permission();
    }
}
