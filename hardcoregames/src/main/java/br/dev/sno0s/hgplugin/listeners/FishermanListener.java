package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.items.PluginItems;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlot;

/** Substitui o puxão vanilla por teleporte ao recolher um jogador com a vara do kit. */
public final class FishermanListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY
                && event.getState() != PlayerFishEvent.State.REEL_IN) return;

        Player fisherman = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        var rod = hand == EquipmentSlot.OFF_HAND
                ? fisherman.getInventory().getItemInOffHand()
                : fisherman.getInventory().getItemInMainHand();
        if (hand == null && !PluginItems.is(rod, "fisherman-rod")) {
            rod = fisherman.getInventory().getItemInOffHand();
        }
        if (rod.getType() != Material.FISHING_ROD || !PluginItems.is(rod, "fisherman-rod")) return;

        Player target = event.getCaught() instanceof Player player
                ? player
                : event.getHook().getHookedEntity() instanceof Player player ? player : null;
        if (target == null) return;

        // Usos negados também não podem aplicar o puxão vanilla. Não fica estado pendente.
        event.setCancelled(true);
        event.getHook().remove();

        GameState state = GameState.getInstance();
        if (state == null || state.getPhase() != MatchPhase.IN_PROGRESS) return;
        if (!isParticipant(state, fisherman) || !isParticipant(state, target)) return;
        if (!"Fisherman".equals(state.getPlayer(fisherman.getUniqueId()).getSelectedKit())) return;
        if (fisherman.equals(target) || !fisherman.getWorld().equals(target.getWorld())) return;
        if (!"hg_world".equals(fisherman.getWorld().getName())) return;

        var destination = fisherman.getLocation();
        var border = fisherman.getWorld().getWorldBorder();
        if (!border.isInside(destination) || !border.isInside(target.getLocation())) return;

        // A API mantém PlayerTeleportEvent cancelável por outros plugins.
        target.teleport(destination, TeleportCause.PLUGIN);
    }

    private static boolean isParticipant(GameState state, Player player) {
        var data = state.getPlayer(player.getUniqueId());
        return data != null && data.isAlive() && player.isOnline() && !player.isDead()
                && player.getGameMode() == GameMode.SURVIVAL && !player.isInvulnerable();
    }
}
