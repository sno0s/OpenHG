package br.dev.sno0s.hgplugin.kits;

import java.util.List;

public class KitRegistry {

    private static final List<Kit> KITS = List.of(
            new KangarooKit()
    );

    public static List<Kit> getAll() {
        return KITS;
    }

    public static Kit getByName(String name) {
        return KITS.stream()
                .filter(k -> k.getName().equalsIgnoreCase(name)
                        || org.bukkit.ChatColor.stripColor(k.getDisplayName()).equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private KitRegistry() {}
}
