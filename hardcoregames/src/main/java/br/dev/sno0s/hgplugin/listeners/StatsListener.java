package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.database.PlayerStats;
import br.dev.sno0s.hgplugin.database.PlayerStatsDAO;
import br.dev.sno0s.hgplugin.items.StatsItem;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Bukkit;
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

import java.util.List;

public class StatsListener implements Listener {

    @EventHandler
    public void onStatsItemUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!StatsItem.DISPLAY_NAME.equals(item.getItemMeta().getDisplayName())) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        PlayerStatsDAO dao = Hgplugin.getStatsDAO();
        if (dao == null) {
            Messages.error(player, "Estatísticas indisponíveis.");
            return;
        }

        PlayerStats stats = dao.load(player.getUniqueId());
        if (stats == null) {
            Messages.error(player, "Nenhuma estatística encontrada.");
            return;
        }

        openGui(player, stats);
    }

    public static void openGui(Player player, PlayerStats stats) {
        String title = "§8Stats de §e" + stats.name();
        Inventory gui = Bukkit.createInventory(null, 27, title);

        gui.setItem(11, buildStatItem(Material.DIAMOND_SWORD, "§eKills",
                "§7Total de eliminações:", "§f" + stats.kills()));

        gui.setItem(13, buildStatItem(Material.SKELETON_SKULL, "§cMortes",
                "§7Total de mortes:", "§f" + stats.deaths()));

        gui.setItem(15, buildStatItem(Material.NETHER_STAR, "§6Vitórias",
                "§7Total de vitórias:", "§f" + stats.wins()));

        gui.setItem(22, buildStatItem(Material.LEATHER_CHESTPLATE, "§bÚltimo kit usado",
                "§7Kit da última partida:", "§f" + stats.lastKit()));

        player.openInventory(gui);
    }

    private static ItemStack buildStatItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(loreLines));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("§8Stats de ")) {
            event.setCancelled(true);
        }
    }
}
