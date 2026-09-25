package br.dev.sno0s.hgplugin.commands;

import br.dev.sno0s.hgplugin.utils.Messages;
import br.dev.sno0s.hgplugin.utils.StartMatch;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** Comando de teste para encerrar a invencibilidade após /startmatch. */
public final class TimeSkipCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0) return false;
        if (!sender.hasPermission("hg.plugin.timeskip")) {
            Messages.send(sender, "common.no-permission");
            return true;
        }
        if (!StartMatch.skipInvincibility()) {
            Messages.send(sender, "match.timeskip-inactive");
            return true;
        }
        Messages.send(sender, "match.timeskip-success");
        return true;
    }
}
