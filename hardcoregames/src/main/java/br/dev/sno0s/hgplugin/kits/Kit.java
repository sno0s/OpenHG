package br.dev.sno0s.hgplugin.kits;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public abstract class Kit {

    // -------------------------
    // Identidade do kit
    // -------------------------

    public abstract String getName();

    public abstract String getDescription();

    // -------------------------
    // Ícone para o seletor de kits (GUI futura)
    // -------------------------

    public abstract ItemStack getIconMaterial();

    public ItemStack getIcon() {
        ItemStack icon = getIconMaterial();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e" + getName());
            meta.setLore(List.of("§7" + getDescription()));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    // -------------------------
    // Aplicação do kit ao jogador
    // -------------------------

    /**
     * Dá os itens e aplica os efeitos do kit ao jogador.
     * Chamado no início da partida após o inventário ser limpo.
     */
    public abstract void apply(Player player);
}
