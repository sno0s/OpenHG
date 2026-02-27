package br.dev.sno0s.hgplugin.commands;

import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.utils.StartMatch;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static br.dev.sno0s.hgplugin.Hgplugin.getServerName;

public class StartMatchCommand implements CommandExecutor {

    /*
        CommandExecutor to force the match start
        verify if sender is not a player, so is the console
        execute match start and say it to the player
        author: sno0s
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player)) {
            Bukkit.getLogger().info(getServerName() + " Console forçou início da partida.");
            StartMatch.execute();
            return true;
        }

        Player player = (Player) sender;

        sender.sendMessage(getServerName() + " §eVocê iniciou a partida.");
        Bukkit.getLogger().info(player.getName() + " iniciou a partida.");
        StartMatch.execute();

        return true;
    }
}
