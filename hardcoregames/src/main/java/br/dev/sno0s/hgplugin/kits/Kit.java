package br.dev.sno0s.hgplugin.kits;

import br.dev.sno0s.hgplugin.items.PluginItems;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;


public abstract class Kit {

    // -------------------------
    // Identidade do kit
    // -------------------------

    public abstract String getName();

    /**
     * Bloco do items.yml com o nome, a lore e a descrição do kit. É a fonte única
     * usada pelo ícone do menu, pelo item do kit e pelas mensagens de chat.
     * Um kit novo só precisa de um bloco items.&lt;nome em minúsculas&gt;.
     */
    public String getCatalogKey() {
        return "items." + getName().toLowerCase(Locale.ROOT);
    }

    public String getDisplayName() {
        return Messages.text(getCatalogKey() + ".name");
    }

    public String getDescription() {
        String key = getCatalogKey() + ".description";
        return Messages.contains(key) ? Messages.text(key)
                : String.join(" ", Messages.lines(getCatalogKey() + ".lore"));
    }

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
            List<String> lore = new ArrayList<>();
            for (String line : Messages.lines("menus.kits.icon-lore", "description", getDescription())) {
                if (line.equals("{lore}")) lore.addAll(Messages.lines(getCatalogKey() + ".lore"));
                else lore.add(line);
            }
            meta.setLore(lore);
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
