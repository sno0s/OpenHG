package br.dev.sno0s.hgplugin.items;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class PluginMenu implements InventoryHolder {
    public enum Type { KITS, STATS }

    private final Type type;
    private final Inventory inventory;

    public PluginMenu(Type type, int size, String title) {
        this.type = type;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public Type getType() {
        return type;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
