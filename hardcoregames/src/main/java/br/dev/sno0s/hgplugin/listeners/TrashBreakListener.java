package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.worldgeneration.TrashCleanPopulator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Impede que flores, gramas e arbustos mortos dropem item ao serem quebrados.
 * O bloco é removido normalmente, mas nada cai no chão nem vai pro inventário.
 */
public class TrashBreakListener implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!event.getBlock().getWorld().getName().equals("hg_world")) return;

        if (TrashCleanPopulator.CLEAR_BLOCKS.contains(event.getBlock().getType())) {
            event.setDropItems(false);
        }
    }
}
