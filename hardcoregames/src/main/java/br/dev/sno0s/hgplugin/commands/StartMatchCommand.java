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
            Bukkit.getLogger().info("[HardcoreGames] Console forçou início da partida.");
        } else {
            Messages.send(player, "Você iniciou a partida.");
            Bukkit.getLogger().info("[HardcoreGames] " + player.getName() + " iniciou a partida.");
        }

        br.dev.sno0s.hgplugin.utils.StartMatch.execute();
        return true;
    }
}
