package br.dev.sno0s.hgplugin.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SoupRecipesTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void registersBothShapelessRecipesWithoutDuplicateEntries() {
        var plugin = MockBukkit.createMockPlugin();
        int originalRecipes = server.getRecipesFor(new ItemStack(Material.MUSHROOM_STEW)).size();
        SoupRecipes.register(plugin);
        SoupRecipes.register(plugin);
        String[] keys = {"cocoa_soup", "cactus_soup"};
        Material[] ingredients = {Material.COCOA_BEANS, Material.CACTUS};
        for (int i = 0; i < keys.length; i++) {
            var recipe = assertInstanceOf(ShapelessRecipe.class, server.getRecipe(new NamespacedKey(plugin, keys[i])));
            assertEquals(Material.MUSHROOM_STEW, recipe.getResult().getType());
            assertEquals(1, recipe.getResult().getAmount());
            assertEquals(List.of(Material.BOWL, ingredients[i]),
                    recipe.getIngredientList().stream().map(ItemStack::getType).toList());
        }
        assertEquals(originalRecipes + 2, server.getRecipesFor(new ItemStack(Material.MUSHROOM_STEW)).size());
    }
}
