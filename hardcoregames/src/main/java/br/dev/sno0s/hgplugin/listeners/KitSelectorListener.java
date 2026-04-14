package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.kits.Kit;
import br.dev.sno0s.hgplugin.kits.KitRegistry;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Bukkit;
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

    private static final String GUI_TITLE = "§8Selecione seu kit";

    @EventHandler
    public void onKitSelectorUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!"§eSeletor de Kits".equals(item.getItemMeta().getDisplayName())) return;

        event.setCancelled(true);

        MatchPhase phase = GameState.getInstance().getPhase();
        if (phase == MatchPhase.IN_PROGRESS || phase == MatchPhase.ENDED) {
            Messages.error(event.getPlayer(), "Não é possível trocar de kit durante a partida.");
            return;
        }

        openGui(event.getPlayer());
    }

    private void openGui(Player player) {
        List<Kit> kits = KitRegistry.getAll();
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        for (Kit kit : kits) gui.addItem(kit.getIcon());
        player.openInventory(gui);
    }

    @EventHandler
    public void onKitClick(InventoryClickEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String rawName = meta.getDisplayName().replaceAll("§.", "").trim();
        Kit kit = KitRegistry.getByName(rawName);
        if (kit == null) return;

        Player player = (Player) event.getWhoClicked();
        GameState.PlayerData data = GameState.getInstance().getPlayer(player.getUniqueId());
        if (data == null) return;

        data.setSelectedKit(kit.getName());
        Messages.success(player, "Kit " + Messages.hl(kit.getName()) + " selecionado!");
        player.closeInventory();
    }
}
