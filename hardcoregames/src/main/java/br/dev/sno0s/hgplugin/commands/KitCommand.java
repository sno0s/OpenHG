package br.dev.sno0s.hgplugin.commands;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.kits.Kit;
import br.dev.sno0s.hgplugin.kits.KitRegistry;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KitCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, "Apenas jogadores podem usar este comando.");
            return true;
        }

        MatchPhase phase = GameState.getInstance().getPhase();
        if (phase == MatchPhase.IN_PROGRESS || phase == MatchPhase.ENDED) {
            Messages.error(player, "Não é possível trocar de kit durante a partida.");
            return true;
        }

        if (args.length == 0) {
            sendKitList(player);
            return true;
        }

        String kitName = args[0];
        Kit kit = KitRegistry.getByName(kitName);

        if (kit == null) {
            Messages.error(player, "Kit " + Messages.hl(kitName) + " não encontrado.");
            sendKitList(player);
            return true;
        }

        GameState.PlayerData data = GameState.getInstance().getPlayer(player.getUniqueId());
        if (data == null) {
            Messages.error(player, "Você não está registrado na partida.");
            return true;
        }

        data.setSelectedKit(kit.getName());
        Messages.success(player, "Kit " + Messages.hl(kit.getName()) + " selecionado!");
        return true;
    }

    private void sendKitList(Player player) {
        String list = KitRegistry.getAll().stream()
                .map(Kit::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("nenhum");
        Messages.send(player, "Kits disponíveis: " + Messages.hl(list));
    }
}
