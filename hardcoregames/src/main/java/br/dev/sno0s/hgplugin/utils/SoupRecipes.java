package br.dev.sno0s.hgplugin.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class SoupRecipes {

    private SoupRecipes() {}

    public static void register(JavaPlugin plugin) {
        // Cocoa beans é o próprio corante marrom desde a flattening da 1.13 (não existe
        // BROWN_DYE separado). O vanilla registra "minecraft:brown_dye" (1 cocoa bean vira
        // "corante marrom", ou seja, o mesmo item) só para aparecer no livro de receitas —
        // isso conflita visualmente com o craft da sopa, que também usa cocoa bean.
        // Removida para o craft da sopa não parecer craftar corante marrom junto.
        plugin.getServer().removeRecipe(NamespacedKey.minecraft("brown_dye"));

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
