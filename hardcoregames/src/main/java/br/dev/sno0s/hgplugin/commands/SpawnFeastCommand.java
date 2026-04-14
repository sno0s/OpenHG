package br.dev.sno0s.hgplugin.commands;

import br.dev.sno0s.hgplugin.utils.Messages;
import br.dev.sno0s.hgplugin.worldgeneration.Feast;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnFeastCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, "Apenas jogadores podem usar este comando.");
            return true;
        }

        Feast.spawnFeast(player.getLocation());
        Messages.success(player, "Feast spawnado na sua localização!");
        return true;
    }
}
