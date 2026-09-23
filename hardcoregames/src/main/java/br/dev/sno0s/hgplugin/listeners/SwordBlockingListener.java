package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.items.SwordBlocking;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Aplica os componentes antes do uso; não cancela interações de baús ou outros itens. */
public final class SwordBlockingListener implements Listener {
    private final JavaPlugin plugin;
    private final SwordBlocking blocking;
    private final Set<UUID> pending = new HashSet<>();

    public SwordBlockingListener(JavaPlugin plugin, SwordBlocking blocking) {
        this.plugin = plugin;
        this.blocking = blocking;
    }

    public void updateInventory(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (blocking.apply(item)) player.getInventory().setItem(slot, item);
        }
    }

    private void schedule(Player player) {
        if (!pending.add(player.getUniqueId())) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pending.remove(player.getUniqueId());
            if (player.isOnline()) updateInventory(player);
        });
    }

    // O bloqueio de espada não deve empurrar o atacante como um escudo moderno.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShieldRecoil(io.papermc.paper.event.entity.EntityKnockbackEvent event) {
        if (event.getCause() != io.papermc.paper.event.entity.EntityKnockbackEvent.Cause.SHIELD_BLOCK) return;
        if (event instanceof io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent push
                && push.getPushedBy() instanceof Player defender
                && SwordBlocking.isManaged(defender.getActiveItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        schedule(event.getPlayer());
    }

    // Cobre baús, crafting, comandos, trocas e itens entregues por outros plugins.
    @EventHandler
    public void onSlotChanged(PlayerInventorySlotChangeEvent event) {
        if (SwordBlocking.isSword(event.getNewItemStack())) schedule(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (blocking.apply(item)) event.getPlayer().getInventory().setItem(event.getNewSlot(), item);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (blocking.apply(item)) event.getItem().setItemStack(item);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (blocking.apply(result)) event.getInventory().setResult(result);
    }
}
