package br.dev.sno0s.hgplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Catálogo de textos com parâmetros nomeados e cores configuráveis. */
public final class Messages {
    private static final Pattern PARAMETER = Pattern.compile("\\{([a-zA-Z0-9_-]+)}");
    private static volatile Map<String, Object> catalog = snapshot(defaults());

    private Messages() {}

    public static void load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration config = defaults();
        try {
            if (file.exists()) {
                config = new YamlConfiguration();
                config.load(file);
            } else {
                // Preserva a tag completa e as cores de instalações anteriores.
                if (plugin.getConfig().isSet("HGconfigs.server-name")) {
                    config.set("server-name", plugin.getConfig().getString("HGconfigs.server-name"));
                    config.set("prefix", "{server-name} ");
                }
                for (String color : List.of("msg", "highlight", "error", "success", "broadcast")) {
                    String oldPath = "HGconfigs.colors." + color;
                    if (plugin.getConfig().isSet(oldPath)) {
                        config.set("colors." + color, plugin.getConfig().getString(oldPath));
                    }
                }
            }
            YamlConfiguration bundled = defaults();
            for (String key : bundled.getKeys(true)) {
                if (!bundled.isConfigurationSection(key) && !config.isSet(key)) {
                    config.set(key, bundled.get(key));
                }
            }
            config.options().header(bundled.options().header());
            config.save(file);
            catalog = snapshot(config);
        } catch (Exception e) {
            // Um YAML inválido nunca deve ser sobrescrito por padrões.
            throw new IllegalStateException(text("errors.messages-load", "file", file), e);
        }
    }

    private static YamlConfiguration defaults() {
        try (var stream = Objects.requireNonNull(Messages.class.getResourceAsStream("/messages.yml"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> snapshot(YamlConfiguration config) {
        Map<String, Object> values = new HashMap<>();
        config.getValues(true).forEach((key, value) -> {
            if (value instanceof String) values.put(key, value);
            if (value instanceof List<?> list) values.put(key, List.copyOf(list));
        });
        return Map.copyOf(values);
    }

    public static String text(String key, Object... parameters) {
        Map<String, Object> values = catalog;
        return render(Objects.toString(values.get(key), key), values, parameters);
    }

    public static List<String> lines(String key, Object... parameters) {
        Map<String, Object> values = catalog;
        Object value = values.get(key);
        if (!(value instanceof List<?> list)) return List.of(text(key, parameters));
        return list.stream().map(line -> render(line.toString(), values, parameters)).toList();
    }

    private static String render(String template, Map<String, Object> values, Object... parameters) {
        if (parameters.length % 2 != 0) throw new IllegalArgumentException("key/value pairs required");
        Map<String, String> replacements = new HashMap<>();
        replacements.put("server-name", color(Objects.toString(values.get("server-name"), "")));
        for (String name : List.of("msg", "highlight", "error", "success", "broadcast")) {
            replacements.put(name, color(Objects.toString(values.get("colors." + name), "")));
        }
        // Valores dos jogadores não são interpretados como cores ou novos parâmetros.
        for (int i = 0; i < parameters.length; i += 2) {
            replacements.put(parameters[i].toString(), Objects.toString(parameters[i + 1], ""));
        }
        Matcher matcher = PARAMETER.matcher(color(template));
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    replacements.getOrDefault(matcher.group(1), matcher.group())));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private static String formatted(String style, String key, Object... parameters) {
        String body = text(key, parameters);
        return body.isEmpty() ? "" : text("prefix") + text("colors." + style) + body;
    }

    public static void send(CommandSender sender, String key, Object... parameters) {
        deliver(sender, "msg", key, parameters);
    }

    public static void error(CommandSender sender, String key, Object... parameters) {
        deliver(sender, "error", key, parameters);
    }

    public static void success(CommandSender sender, String key, Object... parameters) {
        deliver(sender, "success", key, parameters);
    }

    private static void deliver(CommandSender sender, String style, String key, Object... parameters) {
        String message = formatted(style, key, parameters);
        if (!message.isEmpty()) sender.sendMessage(message);
    }

    public static void broadcast(String key, Object... parameters) {
        String message = formatted("broadcast", key, parameters);
        if (!message.isEmpty()) Bukkit.broadcastMessage(message);
    }

    public static void broadcastInfo(String key, Object... parameters) {
        String message = formatted("msg", key, parameters);
        if (!message.isEmpty()) Bukkit.broadcastMessage(message);
    }

    public static String log(String key, Object... parameters) {
        return ChatColor.stripColor(formatted("msg", key, parameters));
    }
}
