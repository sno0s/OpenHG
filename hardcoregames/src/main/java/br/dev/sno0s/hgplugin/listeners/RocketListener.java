package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.items.Rocket;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RocketListener implements Listener {

    // -1 = nunca usou neste voo (pode usar 1x no ar)
    //  0 = exausto no ar (sem usos restantes)
    //  1 = tem 1 carga aérea (veio do chão)
    private final Map<UUID, Integer> airCharges = new HashMap<>();

    // Timestamp (ms) em que o cooldown de hit expira
    private final Map<UUID, Long> hitCooldownExpiry = new HashMap<>();

    // -------------------------
    // Boost do foguete
    // -------------------------

    @EventHandler
    public void onRocketUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!Rocket.DISPLAY_NAME.equals(item.getItemMeta().getDisplayName())) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Verifica cooldown de hit
        Long expiry = hitCooldownExpiry.get(uuid);
        if (expiry != null && System.currentTimeMillis() < expiry) {
            long remainingSeconds = (expiry - System.currentTimeMillis()) / 1000 + 1;
            Messages.error(player, "Foguete bloqueado por " + Messages.hl(remainingSeconds + "s") + " (levou hit).");
            return;
        }

        if (player.isOnGround()) {
            // do chão: boost + concede 1 carga aérea
            airCharges.put(uuid, 1);
            applyBoost(player);
        } else {
            int charges = airCharges.getOrDefault(uuid, -1);
            if (charges == 0) return; // exausto — bloqueado

            // charges == 1 (veio do chão) ou -1 (primeiro uso no ar): permite 1 boost
            airCharges.put(uuid, 0);
            applyBoost(player);
        }
    }

    private void applyBoost(Player player) {
        if (player.isSneaking()) {
            Vector direction = player.getLocation().getDirection();
            direction.setY(0);
            direction.normalize().multiply(1.8);
            direction.setY(0.25);
            player.setVelocity(direction);
        } else {
            Vector velocity = player.getVelocity();
            velocity.setY(1.0);
            player.setVelocity(velocity);
        }
    }

    // -------------------------
    // Reset de cargas ao pousar
    // -------------------------

    @EventHandler
    public void onPlayerLand(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!airCharges.containsKey(uuid)) return; // só processa quem tem carga registrada
        if (event.getPlayer().isOnGround()) {
            airCharges.remove(uuid); // volta ao estado padrão (-1) ao tocar o chão
        }
    }

    // -------------------------
    // Cooldown ao receber hit de player
    // -------------------------

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player)) return;

        GameState.PlayerData data = GameState.getInstance().getPlayer(victim.getUniqueId());
        if (data == null || !"Kangaroo".equals(data.getSelectedKit())) return;

        int cooldownSeconds = Hgplugin.getConfigManager().getKangarooHitCooldown();
        hitCooldownExpiry.put(victim.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
    }

    // -------------------------
    // Redução de dano de queda (kit Kangaroo)
    // -------------------------

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;

        GameState.PlayerData data = GameState.getInstance().getPlayer(player.getUniqueId());
        if (data == null || !"Kangaroo".equals(data.getSelectedKit())) return;

        event.setDamage(event.getDamage() * 0.5);
    }
}
