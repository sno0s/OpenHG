package br.dev.sno0s.hgplugin.utils;

import br.dev.sno0s.hgplugin.ConfigManager;
import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utilitário central de mensagens.
 * Todas as mensagens do plugin passam por aqui para garantir
 * formato e cores consistentes.
 *
 * Formato: [Prefixo] <cor><mensagem>
 */
public class Messages {

    private Messages() {}

    // -------------------------
    // Métodos públicos
    // -------------------------

    /** Mensagem informativa com prefixo (para um jogador ou console). */
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(prefix() + cfg().getMsgColor() + message);
    }

    /** Mensagem de erro com prefixo. */
    public static void error(CommandSender sender, String message) {
        sender.sendMessage(prefix() + cfg().getErrorColor() + message);
    }

    /** Mensagem de sucesso com prefixo. */
    public static void success(CommandSender sender, String message) {
        sender.sendMessage(prefix() + cfg().getSuccessColor() + message);
    }

    /** Broadcast global com cor de destaque (anúncios importantes). */
    public static void broadcast(String message) {
        Bukkit.broadcastMessage(prefix() + cfg().getBroadcastColor() + message);
    }

    /** Broadcast global com cor informativa. */
    public static void broadcastInfo(String message) {
        Bukkit.broadcastMessage(prefix() + cfg().getMsgColor() + message);
    }

    /**
     * Destaque inline para nomes de jogadores e valores numéricos.
     * Uso: Messages.send(p, "Você eliminou " + Messages.hl(name) + "!");
     */
    public static String hl(String text) {
        return cfg().getHighlightColor() + text;
    }

    // -------------------------
    // Helpers privados
    // -------------------------

    private static String prefix() {
        return cfg().getServerName() + " ";
    }

    private static ConfigManager cfg() {
        return Hgplugin.getConfigManager();
    }
}
