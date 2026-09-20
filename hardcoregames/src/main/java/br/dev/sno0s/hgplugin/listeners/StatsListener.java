package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.items.PluginMenu;
import br.dev.sno0s.hgplugin.items.PluginItems;
import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.database.PlayerStats;
import br.dev.sno0s.hgplugin.database.PlayerStatsDAO;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Material;
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


public class StatsListener implements Listener {

    @EventHandler
    public void onStatsItemUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!PluginItems.is(item, "stats")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        PlayerStatsDAO dao = Hgplugin.getStatsDAO();
        if (dao == null) {
            Messages.error(player, "stats.unavailable");
            return;
        }

        PlayerStats stats = dao.load(player.getUniqueId());
        if (stats == null) {
            Messages.error(player, "stats.empty");
            return;
        }

        openGui(player, stats);
    }

    public static void openGui(Player player, PlayerStats stats) {
        String title = Messages.text("menus.stats.title", "player", stats.name());
        Inventory gui = new PluginMenu(PluginMenu.Type.STATS, 27, title).getInventory();

        gui.setItem(11, buildStatItem(Material.DIAMOND_SWORD, "menus.stats.kills", stats.kills()));

        gui.setItem(13, buildStatItem(Material.SKELETON_SKULL, "menus.stats.deaths", stats.deaths()));

        gui.setItem(15, buildStatItem(Material.NETHER_STAR, "menus.stats.wins", stats.wins()));

        var kit = stats.lastKit() == null ? null
                : br.dev.sno0s.hgplugin.kits.KitRegistry.getByName(stats.lastKit());
        String kitName = kit != null ? kit.getDisplayName()
                : stats.lastKit() == null || stats.lastKit().isBlank() ? Messages.text("kits.none") : stats.lastKit();
        gui.setItem(22, buildStatItem(Material.LEATHER_CHESTPLATE, "menus.stats.last-kit", kitName));

        player.openInventory(gui);
    }

    private static ItemStack buildStatItem(Material material, String key, Object value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.text(key + ".name"));
            meta.setLore(Messages.lines(key + ".lore", "value", value));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PluginMenu menu
                && menu.getType() == PluginMenu.Type.STATS) {
            event.setCancelled(true);
        }
    }
}
