package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.items.PluginItems;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Derruba somente o conjunto de troncos ligado a uma árvore, sem afetar folhas ou construções. */
public final class LumberjackListener implements Listener {
    private static final int MAX_LOGS = 256;
    private static final Set<Material> LOGS = Set.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD,
            Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD,
            Material.MANGROVE_WOOD, Material.CHERRY_WOOD);

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isLumberjack(player) || !LOGS.contains(event.getBlock().getType())) return;
        event.setCancelled(true);
        Set<Block> tree = collectTree(event.getBlock());
        if (tree.isEmpty()) return;
        for (Block block : tree) {
            Material type = block.getType();
            // Remoção sem física evita que folhas, terra ou blocos vizinhos sejam alterados.
            block.setType(Material.AIR, false);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(type, 1));
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (PluginItems.is(event.getItemInHand(), "lumberjack-axe")) event.setCancelled(true);
    }

    private static boolean isLumberjack(Player player) {
        GameState.PlayerData data = GameState.getInstance().getPlayer(player.getUniqueId());
        ItemStack held = player.getInventory().getItemInMainHand();
        return data != null && "Lumberjack".equals(data.getSelectedKit())
                && PluginItems.is(held, "lumberjack-axe");
    }

    private static Set<Block> collectTree(Block root) {
        Set<Block> found = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty() && found.size() < MAX_LOGS) {
            Block block = queue.removeFirst();
            if (!LOGS.contains(block.getType()) || !found.add(block)) continue;
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dy != 0 || dz != 0) queue.add(block.getRelative(dx, dy, dz));
            }
        }
        return found;
    }

}
