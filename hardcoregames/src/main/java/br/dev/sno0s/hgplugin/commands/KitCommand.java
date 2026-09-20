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
            Messages.error(sender, "common.players-only");
            return true;
        }

        MatchPhase phase = GameState.getInstance().getPhase();
        if (phase == MatchPhase.IN_PROGRESS || phase == MatchPhase.ENDED) {
            Messages.error(player, "kits.match-locked");
            return true;
        }

        if (args.length == 0) {
            sendKitList(player);
            return true;
        }

        String kitName = String.join(" ", args);
        Kit kit = KitRegistry.getByName(kitName);

        if (kit == null) {
            Messages.error(player, "kits.not-found", "kit", kitName);
            sendKitList(player);
            return true;
        }

        GameState.PlayerData data = GameState.getInstance().getPlayer(player.getUniqueId());
        if (data == null) {
            Messages.error(player, "kits.not-registered");
            return true;
        }

        data.setSelectedKit(kit.getName());
        Messages.success(player, "kits.selected", "kit", kit.getDisplayName());
        return true;
    }

    private void sendKitList(Player player) {
        String list = KitRegistry.getAll().stream()
                .map(Kit::getDisplayName)
                .collect(java.util.stream.Collectors.joining(Messages.text("kits.list-separator")));
        if (list.isEmpty()) list = Messages.text("kits.none");
        Messages.send(player, "kits.available", "kits", list);
    }
}
