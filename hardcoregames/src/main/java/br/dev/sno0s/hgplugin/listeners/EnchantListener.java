package br.dev.sno0s.hgplugin.listeners;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entrega lapis emprestado nas mesas de encantamento do hg_world.
 *
 * O lapis pertence a mesa, nunca ao jogador: enquanto a mesa esta aberta o slot
 * do lapis fica travado, e no fechamento/desconexao ele e removido antes do
 * servidor devolver o conteudo da mesa. Mesas fora do hg_world e o inventario
 * pessoal nao sao tocados.
 */
public class EnchantListener implements Listener {

    /** Slot do lapis na mesa de encantamento. */
    private static final int LAPIS_SLOT = 1;

    private static final String HG_WORLD = "hg_world";

    /** Jogadores com uma mesa gerenciada pelo plugin aberta no momento. */
    private final Set<UUID> managedViews = ConcurrentHashMap.newKeySet();

    @EventHandler(ignoreCancelled = true)
    public void onEnchantOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.ENCHANTING) return;

        HumanEntity player = event.getPlayer();
        if (!player.getWorld().getName().equals(HG_WORLD)) return;

        // Slot ocupado por outra coisa: nao mexe pra nao engolir item do jogador.
        ItemStack current = inventory.getItem(LAPIS_SLOT);
        if (current != null && !current.getType().isAir()) return;

        inventory.setItem(LAPIS_SLOT, new ItemStack(Material.LAPIS_LAZULI, 64));
        managedViews.add(player.getUniqueId());
    }

    @EventHandler
    public void onEnchantClose(InventoryCloseEvent event) {
        if (!managedViews.remove(event.getPlayer().getUniqueId())) return;
        clearLapis(event.getInventory());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!managedViews.remove(player.getUniqueId())) return;
        // Se o fechamento ja tiver acontecido a mesa some do view; o remove acima basta.
        clearLapis(player.getOpenInventory().getTopInventory());
    }

    @EventHandler
    public void onEnchantClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ENCHANTING) return;
        if (!managedViews.contains(event.getWhoClicked().getUniqueId())) return;

        // Clique, shift, tecla de hotbar ou troca de offhand direto no slot travado.
        if (event.getRawSlot() == LAPIS_SLOT) {
            event.setCancelled(true);
            return;
        }

        InventoryAction action = event.getAction();

        // Shift-click de lapis do inventario pessoal empilharia no slot travado.
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && isLapis(event.getCurrentItem())) {
            event.setCancelled(true);
            return;
        }

        // Duplo clique com lapis no cursor sugaria o lapis da mesa.
        if (action == InventoryAction.COLLECT_TO_CURSOR && isLapis(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnchantDrag(InventoryDragEvent event) {
        if (event.getInventory().getType() != InventoryType.ENCHANTING) return;
        if (!managedViews.contains(event.getWhoClicked().getUniqueId())) return;

        if (event.getRawSlots().contains(LAPIS_SLOT)) event.setCancelled(true);
    }

    private void clearLapis(Inventory inventory) {
        if (inventory == null || inventory.getType() != InventoryType.ENCHANTING) return;
        if (isLapis(inventory.getItem(LAPIS_SLOT))) inventory.setItem(LAPIS_SLOT, null);
    }

    private static boolean isLapis(ItemStack item) {
        return item != null && item.getType() == Material.LAPIS_LAZULI;
    }
}
