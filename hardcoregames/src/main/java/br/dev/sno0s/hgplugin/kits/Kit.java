package br.dev.sno0s.hgplugin.kits;

import br.dev.sno0s.hgplugin.items.PluginItems;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;


public abstract class Kit {

    // -------------------------
    // Identidade do kit
    // -------------------------

    public abstract String getName();

    public String getDisplayName() {
        return Messages.text("kits." + getName().toLowerCase(java.util.Locale.ROOT) + ".name");
    }

    public abstract String getDescription();

    // -------------------------
    // Ícone para o seletor de kits (GUI futura)
    // -------------------------

    public abstract ItemStack getIconMaterial();

    public ItemStack getIcon() {
        ItemStack icon = getIconMaterial();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.text("menus.kits.icon-name", "kit", getDisplayName()));
            PluginItems.mark(meta, getName());
            meta.setLore(Messages.lines("menus.kits.icon-lore", "description", getDescription()));
            // O ícone representa o kit; não exibir dano/velocidade do item vanilla.
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
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
