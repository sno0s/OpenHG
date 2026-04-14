package br.dev.sno0s.hgplugin.listeners;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Ajusta o dano dos machados para os valores da 1.8,
 * onde machados eram mais fracos que espadas.
 */
public class AxeDamageListener implements Listener {

    // Dano base dos machados na 1.8 (sem cooldown, sem crítico)
    private static final Map<Material, Double> AXE_DAMAGE = Map.of(
            Material.WOODEN_AXE,    3.0,
            Material.STONE_AXE,     4.0,
            Material.IRON_AXE,      5.0,
            Material.GOLDEN_AXE,    3.0,
            Material.DIAMOND_AXE,   6.0,
            Material.NETHERITE_AXE, 7.0
    );

    @EventHandler(priority = EventPriority.HIGH)
    public void onAxeHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        ItemStack item = attacker.getInventory().getItemInMainHand();
        Double damage = AXE_DAMAGE.get(item.getType());
        if (damage != null) {
            event.setDamage(damage);
        }
    }
}
