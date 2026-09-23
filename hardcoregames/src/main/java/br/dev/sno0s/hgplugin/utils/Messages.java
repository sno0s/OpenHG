package br.dev.sno0s.hgplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Catálogo de textos com parâmetros nomeados e cores configuráveis. */
public final class Messages {
    private static final Pattern PARAMETER = Pattern.compile("\\{([a-zA-Z0-9_-]+)}");

    /** Arquivo do catálogo e as raízes que ele guarda; messages.yml fica com o resto. */
    private record CatalogFile(String name, List<String> roots) {
        boolean owns(String key) {
            return roots.stream().anyMatch(root -> key.equals(root) || key.startsWith(root + "."));
        }
    }

    private static final List<CatalogFile> FILES = List.of(
            new CatalogFile("items.yml", List.of("items")),
            new CatalogFile("menus.yml", List.of("menus")),
            new CatalogFile("messages.yml", List.of()));

    private static final CatalogFile MESSAGES = FILES.getLast();

    private record Rename(String from, String to) {}

    /**
     * Chaves que mudaram de lugar, na ordem de prioridade: a primeira origem
     * encontrada define o valor da nova chave e as demais são descartadas.
     */
    private static final List<Rename> RENAMED = List.of(
            new Rename("items.cannot-drop", "common.cannot-drop"),
            // O nome do kit vale mais que o nome antigo do foguete do Kangaroo.
            new Rename("kits.kangaroo.name", "items.kangaroo.name"),
            new Rename("kits.kangaroo.description", "items.kangaroo.description"),
            new Rename("kits.lumberjack.name", "items.lumberjack.name"),
            new Rename("kits.lumberjack.description", "items.lumberjack.description"),
            new Rename("items.rocket.name", "items.kangaroo.name"),
            new Rename("items.rocket.lore", "items.kangaroo.lore"));

    private static volatile Map<String, Object> catalog = snapshot(defaults().values());

    private Messages() {}

    public static void load(JavaPlugin plugin) {
        File folder = plugin.getDataFolder();
        Map<String, YamlConfiguration> files = new LinkedHashMap<>();
        Map<String, String> originals = new HashMap<>();
        // YAML inválido interrompe a carga antes de qualquer gravação.
        for (CatalogFile file : FILES) files.put(file.name(), YamlFiles.read(new File(folder, file.name())));

        Map<String, YamlConfiguration> bundled = defaults();
        for (CatalogFile file : FILES) {
            YamlConfiguration config = files.get(file.name());
            YamlFiles.validateTextTypes(config, bundled.get(file.name()), file.name());
            originals.put(file.name(), config.saveToString());
        }

        if (!new File(folder, MESSAGES.name()).exists()) {
            adoptLegacyConfigTexts(plugin, files.get(MESSAGES.name()));
        }

        List<String> migrated = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        splitIntoOwnFiles(files, migrated, conflicts);
        renameLegacyKeys(files, migrated, conflicts);
        // Kits sem lore de item anterior preservam a descrição como lore compartilhada.
        YamlConfiguration items = files.get("items.yml");
        var itemSection = items.getConfigurationSection("items");
        if (itemSection != null) {
            for (String id : itemSection.getKeys(false)) {
                String key = "items." + id;
                if (!items.isSet(key + ".lore") && items.isString(key + ".description")) {
                    String description = items.getString(key + ".description");
                    items.set(key + ".lore", description.isEmpty() ? List.of() : List.of("&7" + description));
                }
            }
        }
        // O formato antigo mostrava apenas description; o padrão novo usa a lore inteira.
        YamlConfiguration menus = files.get("menus.yml");
        if (!migrated.isEmpty() && menus.getStringList("menus.kits.icon-lore")
                .equals(List.of("&7{description}"))) {
            menus.set("menus.kits.icon-lore", List.of("{lore}"));
        }
        for (CatalogFile file : FILES) {
            YamlFiles.mergeDefaults(files.get(file.name()), bundled.get(file.name()));
            YamlFiles.validateTextTypes(files.get(file.name()), bundled.get(file.name()), file.name());
        }
        // Só há o que preservar quando a migração de fato move textos do servidor.
        if (!migrated.isEmpty()) {
            for (CatalogFile file : FILES) YamlFiles.backup(new File(folder, file.name()));
        }
        for (String key : conflicts) {
            plugin.getLogger().warning(Messages.log("console.messages.conflicting-key", "key", key));
        }

        for (CatalogFile file : FILES) {
            YamlConfiguration config = files.get(file.name());
            File destination = new File(folder, file.name());
            if (!destination.exists() || !config.saveToString().equals(originals.get(file.name()))) {
                config.options().header(bundled.get(file.name()).options().header());
                YamlFiles.save(config, destination);
            }
        }
        catalog = snapshot(files.values());
    }

    private static Map<String, YamlConfiguration> defaults() {
        Map<String, YamlConfiguration> bundled = new LinkedHashMap<>();
        for (CatalogFile file : FILES) bundled.put(file.name(), YamlFiles.bundled(file.name()));
        return bundled;
    }

    private static CatalogFile owner(String key) {
        return FILES.stream().filter(file -> file.owns(key)).findFirst().orElse(MESSAGES);
    }

    /** Primeira instalação do catálogo: preserva a tag completa e as cores do config.yml. */
    private static void adoptLegacyConfigTexts(JavaPlugin plugin, YamlConfiguration messages) {
        if (plugin.getConfig().isSet("HGconfigs.server-name")) {
            messages.set("server-name", plugin.getConfig().getString("HGconfigs.server-name"));
            messages.set("prefix", "{server-name} ");
        }
        for (String color : List.of("msg", "highlight", "error", "success", "broadcast")) {
            String oldPath = "HGconfigs.colors." + color;
            if (plugin.getConfig().isSet(oldPath)) {
                messages.set("colors." + color, plugin.getConfig().getString(oldPath));
            }
        }
    }

    /**
     * Move para items.yml e menus.yml os textos que ficaram no messages.yml.
     * Idempotente: depois da primeira carga não sobra nada para mover.
     */
    private static void splitIntoOwnFiles(Map<String, YamlConfiguration> files,
                                          List<String> migrated, List<String> conflicts) {
        YamlConfiguration messages = files.get(MESSAGES.name());
        List<String> moved = new ArrayList<>();
        for (String key : List.copyOf(messages.getKeys(true))) {
            if (messages.isConfigurationSection(key)) continue;
            CatalogFile owner = owner(key);
            if (owner == MESSAGES) continue;
            moved.add(key);
            adopt(files.get(owner.name()), key, messages.get(key), conflicts);
        }
        for (String key : moved) messages.set(key, null);
        YamlFiles.pruneEmptySections(messages);
        migrated.addAll(moved);
    }

    /** Aplica os renomes antigos uma única vez; a primeira origem da lista vence. */
    private static void renameLegacyKeys(Map<String, YamlConfiguration> files,
                                         List<String> migrated, List<String> conflicts) {
        List<String> renamed = new ArrayList<>();
        for (Rename rename : RENAMED) {
            YamlConfiguration source = files.values().stream()
                    .filter(config -> config.isSet(rename.from()) && !config.isConfigurationSection(rename.from()))
                    .findFirst().orElse(null);
            if (source == null) continue;
            // Uma origem anterior da mesma chave nova já decidiu o valor.
            if (!renamed.contains(rename.to())) {
                adopt(files.get(owner(rename.to()).name()), rename.to(), source.get(rename.from()), conflicts);
                renamed.add(rename.to());
            } else if (!Objects.equals(files.get(owner(rename.to()).name()).get(rename.to()), source.get(rename.from()))) {
                conflicts.add(rename.from());
            }
            source.set(rename.from(), null);
            migrated.add(rename.from());
        }
        if (!renamed.isEmpty()) files.values().forEach(YamlFiles::pruneEmptySections);
    }

    /**
     * O arquivo novo prevalece; divergências ficam no backup e geram aviso.
     */
    private static void adopt(YamlConfiguration target, String key, Object value, List<String> conflicts) {
        if (target.isSet(key)) {
            if (!Objects.equals(target.get(key), value)) conflicts.add(key);
        } else {
            YamlFiles.ensureSectionParents(target, key);
            target.set(key, value);
        }
    }

    private static Map<String, Object> snapshot(Collection<YamlConfiguration> configs) {
        Map<String, Object> values = new HashMap<>();
        for (YamlConfiguration config : configs) {
            config.getValues(true).forEach((key, value) -> {
                if (value instanceof String) values.put(key, value);
                // Somente listas de texto entram no catálogo.
                if (value instanceof List<?> list && list.stream().allMatch(String.class::isInstance)) {
                    values.put(key, List.copyOf(list));
                }
            });
        }
        return Map.copyOf(values);
    }

    public static boolean contains(String key) {
        return catalog.containsKey(key);
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
