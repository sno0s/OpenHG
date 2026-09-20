package br.dev.sno0s.hgplugin.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class SoupRecipes {

    private SoupRecipes() {}

    public static void register(JavaPlugin plugin) {
        registerRecipe(plugin, "cocoa_soup", Material.COCOA_BEANS);
        registerRecipe(plugin, "cactus_soup", Material.CACTUS);
    }

    private static void registerRecipe(JavaPlugin plugin, String name, Material ingredient) {
        NamespacedKey key = new NamespacedKey(plugin, name);
        ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(Material.MUSHROOM_STEW));
        recipe.addIngredient(Material.BOWL);
        recipe.addIngredient(ingredient);

        // Permite registrar novamente sem duplicar a receita ao recarregar o plugin.
        plugin.getServer().removeRecipe(key);
        plugin.getServer().addRecipe(recipe);
    }
}
