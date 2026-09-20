package br.dev.sno0s.hgplugin.commands;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartMatchCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info(Messages.log("console.start-match-command.console-start"));
        } else {
            Messages.send(player, "match.started-by-you");
            Bukkit.getLogger().info(Messages.log("console.start-match-command.player-start", "player", player.getName()));
        }

        br.dev.sno0s.hgplugin.utils.StartMatch.execute();
        return true;
    }
}
