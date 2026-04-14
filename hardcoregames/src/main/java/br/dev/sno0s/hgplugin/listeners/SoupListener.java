package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SoupListener implements Listener {

    @EventHandler
    public void onPlayerUseSoup(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // verifica se é sopa
        if (item.getType() == Material.MUSHROOM_STEW) {

            if(player.getHealth() < 20){
                // cancela uso normal
                event.setCancelled(true);
                double heal = Hgplugin.getConfigManager().getSoupHeal();
                int foodHeal = Hgplugin.getConfigManager().getSoupFood();

                // cura 3 corações (6 pontos de vida)
                double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getDefaultValue();
                double newHealth = Math.min(player.getHealth() + heal, maxHealth);
                player.setHealth(newHealth);

                // cura 4 barras de fome
                int maxHungry = 20;
                int newHungry = Math.min(player.getFoodLevel() + foodHeal, maxHungry);
                player.setFoodLevel(newHungry);

                // troca a sopa vazia por bowl
                item.setType(Material.BOWL);
                item.setAmount(1);
            }

        }
    }
}
