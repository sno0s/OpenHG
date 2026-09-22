package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.items.PluginMenu;
import br.dev.sno0s.hgplugin.items.PluginItems;
import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.kits.Kit;
import br.dev.sno0s.hgplugin.kits.KitRegistry;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class KitSelectorListener implements Listener {


    @EventHandler
    public void onKitSelectorUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!PluginItems.is(item, "kit-selector")) return;

        event.setCancelled(true);

        MatchPhase phase = GameState.getInstance().getPhase();
        if (phase == MatchPhase.IN_PROGRESS || phase == MatchPhase.ENDED) {
            Messages.error(event.getPlayer(), "kits.match-locked");
            return;
        }

        openGui(event.getPlayer());
    }

    private void openGui(Player player) {
        List<Kit> kits = KitRegistry.getAll();
        Inventory gui = new PluginMenu(PluginMenu.Type.KITS, 54, Messages.text("menus.kits.title")).getInventory();
        for (int i = 0; i < kits.size(); i++) {
            int slot = 10 + (i / 7) * 9 + (i % 7);
            gui.setItem(slot, kits.get(i).getIcon());
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onKitClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PluginMenu menu)
                || menu.getType() != PluginMenu.Type.KITS) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        String rawName = PluginItems.id(clicked);
        if (rawName == null) return;
        Kit kit = KitRegistry.getByName(rawName);
        if (kit == null) return;

        Player player = (Player) event.getWhoClicked();
        GameState.PlayerData data = GameState.getInstance().getPlayer(player.getUniqueId());
        if (data == null) return;

        data.setSelectedKit(kit.getName());
        Messages.success(player, "kits.selected", "kit", kit.getDisplayName());
        player.closeInventory();
    }
}
