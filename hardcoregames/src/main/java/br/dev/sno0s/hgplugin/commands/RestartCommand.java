package br.dev.sno0s.hgplugin.commands;

import br.dev.sno0s.hgplugin.utils.CraftyAPI;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RestartCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("check")) {
            CraftyAPI.checkConnection(sender);
            return true;
        }
        if (args.length != 0) return false;
        Messages.broadcast("Reinício solicitado por " + Messages.hl(sender.getName()) + "!");
        CraftyAPI.scheduleRestart();
        return true;
    }
}
