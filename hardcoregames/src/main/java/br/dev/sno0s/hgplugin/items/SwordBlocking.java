package br.dev.sno0s.hgplugin.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import io.papermc.paper.datacomponent.item.blocksattacks.ItemDamageFunction;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.DamageTypeKeys;
import io.papermc.paper.registry.set.RegistrySet;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

/** Bloqueio nativo: a defesa termina ao soltar o botão ou trocar de item. */
public final class SwordBlocking {
    private static final NamespacedKey OWNER = new NamespacedKey("hardcoregames", "sword-blocking");
    private static final Set<Material> SWORDS = Set.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.COPPER_SWORD,
            Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD);

    private final boolean enabled;
    private final BlocksAttacks blocking;

    public SwordBlocking(boolean enabled, float reduction) {
        if (!Float.isFinite(reduction) || reduction < 0 || reduction > 20) {
            throw new IllegalArgumentException("Sword blocking reduction must be between 0 and 20");
        }
        this.enabled = enabled;
        this.blocking = BlocksAttacks.blocksAttacks()
                .blockDelaySeconds(0f)
                .disableCooldownScale(0f)
                .damageReductions(java.util.List.of(DamageReduction.damageReduction()
                        .type(RegistrySet.keySet(RegistryKey.DAMAGE_TYPE,
                                DamageTypeKeys.PLAYER_ATTACK, DamageTypeKeys.MOB_ATTACK,
                                DamageTypeKeys.MOB_ATTACK_NO_AGGRO, DamageTypeKeys.ARROW,
                                DamageTypeKeys.MOB_PROJECTILE, DamageTypeKeys.TRIDENT,
                                DamageTypeKeys.THROWN, DamageTypeKeys.EXPLOSION,
                                DamageTypeKeys.PLAYER_EXPLOSION))
                        .horizontalBlockingAngle(180f).base(reduction).factor(0f).build()))
                .itemDamage(ItemDamageFunction.itemDamageFunction()
                        .threshold(0f).base(0f).factor(0f).build())
                .build();
    }

    public static boolean isSword(ItemStack item) {
        return item != null && SWORDS.contains(item.getType());
    }

    public static boolean isManaged(ItemStack item) {
        return isSword(item) && item.getPersistentDataContainer().has(OWNER, PersistentDataType.BYTE);
    }

    /** Retorna true somente quando o item foi alterado; preserva encantamentos e demais componentes. */
    public boolean apply(ItemStack item) {
        if (!isSword(item)) return false;
        boolean owned = item.getPersistentDataContainer().has(OWNER, PersistentDataType.BYTE);
        if (!enabled) {
            if (!owned) return false;
            item.unsetData(DataComponentTypes.BLOCKS_ATTACKS);
            item.editPersistentDataContainer(pdc -> pdc.remove(OWNER));
            return true;
        }
        BlocksAttacks existing = item.getData(DataComponentTypes.BLOCKS_ATTACKS);
        // Não substitui uma habilidade de outro plugin.
        if (existing != null && !owned) return false;
        if (blocking.equals(existing) && owned) return false;
        item.setData(DataComponentTypes.BLOCKS_ATTACKS, blocking);
        item.editPersistentDataContainer(pdc -> pdc.set(OWNER, PersistentDataType.BYTE, (byte) 1));
        return true;
    }
}
