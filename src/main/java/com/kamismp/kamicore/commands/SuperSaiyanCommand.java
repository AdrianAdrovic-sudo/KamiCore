package com.kamismp.kamicore.commands;

import com.kamismp.kamicore.KamiCore;
import com.kamismp.kamicore.saiyan.SaiyanForm;
import com.kamismp.kamicore.saiyan.SuperSaiyan;
import com.kamismp.kamicore.util.Msg;
import com.kamismp.kamicore.util.Suggest;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * /ssj          - list all forms (or power down if transformed)
 * /ssj <form>   - transform (or switch form)
 * /ssj off      - power down
 */
public final class SuperSaiyanCommand implements BasicCommand {

    private final KamiCore plugin;

    public SuperSaiyanCommand(KamiCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            Msg.playersOnly(source.getSender());
            return;
        }
        SuperSaiyan saiyan = plugin.superSaiyan();

        if (args.length == 0) {
            if (saiyan.isTransformed(player)) {
                saiyan.revert(player, "<gray>You power down and return to your base form.");
            } else {
                listForms(player);
            }
            return;
        }
        if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("base")) {
            if (saiyan.isTransformed(player)) {
                saiyan.revert(player, "<gray>You power down and return to your base form.");
            } else {
                Msg.send(player, "<gray>You're already in your base form.");
            }
            return;
        }

        SaiyanForm form = saiyan.form(args[0]);
        if (form == null) {
            Msg.send(player, "<red>Unknown form.</red> <gray>Type <yellow>/ssj</yellow> to see all forms.");
            return;
        }
        saiyan.transform(player, form);
    }

    private void listForms(Player player) {
        List<SaiyanForm> forms = plugin.superSaiyan().forms();
        if (forms.isEmpty()) {
            Msg.send(player, "<red>No forms are set up in config.yml.");
            return;
        }
        Msg.send(player, "<gold><b>Transformations</b></gold> <gray>(you have <green>" + player.getLevel() + " XP levels</green>)");
        for (SaiyanForm form : forms) {
            boolean unlocked = player.getLevel() >= form.minLevel();
            String status = unlocked ? "" : " <red>(needs " + form.minLevel() + " levels)";
            Msg.raw(player, " <dark_gray>-</dark_gray> <click:run_command:'/ssj " + form.id() + "'><hover:show_text:'Click to transform'>"
                    + form.chatColor() + form.name() + "</hover></click> <gray>/ssj " + form.id() + " · drains <yellow>"
                    + form.xpDrain() + " XP/s</yellow> · +" + formatHearts(form.extraHearts()) + " ❤" + (form.flight() ? " · flight" : "")
                    + (form.dodgeChance() > 0 ? " · " + Math.round(form.dodgeChance() * 100) + "% dodge" : "") + status);
        }
    }

    private static String formatHearts(double hearts) {
        return hearts == Math.floor(hearts) ? String.valueOf((int) hearts) : String.valueOf(hearts);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (Suggest.index(args) != 0) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        for (SaiyanForm form : plugin.superSaiyan().forms()) {
            options.add(form.id());
        }
        options.add("off");
        return Suggest.filter(options, Suggest.current(args));
    }

    @Override
    public String permission() {
        return "kamicore.supersaiyan";
    }
}
