package br.dev.sno0s.hgplugin.commands;

import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.database.PlayerStats;
import br.dev.sno0s.hgplugin.database.PlayerStatsDAO;
import br.dev.sno0s.hgplugin.listeners.StatsListener;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            Messages.error(sender, "Apenas jogadores podem usar este comando.");
            return true;
        }

        PlayerStatsDAO dao = Hgplugin.getStatsDAO();
        if (dao == null) {
            Messages.error(viewer, "Estatísticas indisponíveis.");
            return true;
        }

        PlayerStats stats;

        if (args.length == 0) {
            stats = dao.load(viewer.getUniqueId());
            if (stats == null) {
                Messages.error(viewer, "Você ainda não possui estatísticas.");
                return true;
            }
        } else {
            Player online = Bukkit.getPlayerExact(args[0]);
            if (online != null) {
                stats = dao.load(online.getUniqueId());
            } else {
                stats = dao.loadByName(args[0]);
            }
            if (stats == null) {
                Messages.error(viewer, "Jogador " + Messages.hl(args[0]) + " não encontrado.");
                return true;
            }
        }

        StatsListener.openGui(viewer, stats);
        return true;
    }
}
