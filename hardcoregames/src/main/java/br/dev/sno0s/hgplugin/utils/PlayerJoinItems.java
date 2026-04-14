package br.dev.sno0s.hgplugin.utils;

import br.dev.sno0s.hgplugin.items.KitSelector;
import br.dev.sno0s.hgplugin.items.StatsItem;
import org.bukkit.entity.Player;

public class PlayerJoinItems {

    /*
        this method is responsible to set the joining items to the player:
        chest: kit selector
        emerald: kits shop
     */

    public static void give(Player player) {

        player.getInventory().setItem(0, KitSelector.create()); // slot 1
        player.getInventory().setItem(8, StatsItem.create()); // slot 9

    }

    private PlayerJoinItems() {}
}
