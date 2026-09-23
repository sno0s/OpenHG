package br.dev.sno0s.hgplugin.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Leitura, cópia de segurança e gravação dos YAMLs da pasta de dados. */
public final class YamlFiles {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private YamlFiles() {}

    /**
     * Carrega o arquivo do servidor, ou um YAML vazio quando ele ainda não existe.
     * Um arquivo inválido é reportado e nunca substituído por padrões.
     */
    public static YamlConfiguration read(File file) {
        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);
        if (file == null || !file.exists()) return config;
        try {
            config.load(file);
        } catch (Exception e) {
            throw new IllegalStateException(Messages.text("errors.messages-load", "file", file), e);
        }
        return config;
    }

    /** Padrões embutidos no JAR. */
    public static YamlConfiguration bundled(String resource) {
        try (InputStream stream = YamlFiles.class.getResourceAsStream("/" + resource)) {
            if (stream == null) throw new IllegalStateException("Recurso ausente: " + resource);
            YamlConfiguration config = new YamlConfiguration();
            config.options().parseComments(true);
            config.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return config;
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            throw new IllegalStateException("YAML embutido inválido: " + resource, e);
        }
    }

    /** Copia o arquivo antes de uma migração reescrevê-lo; nunca sobrescreve um backup. */
    public static File backup(File file) {
        if (file == null || !file.exists()) return null;
        try {
            Path copy = Files.createTempFile(file.toPath().getParent(),
                    file.getName() + "." + STAMP.format(LocalDateTime.now()) + "-", ".bak");
            Files.copy(file.toPath(), copy, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return copy.toFile();
        } catch (IOException e) {
            throw new IllegalStateException(Messages.text("errors.backup-failed", "file", file), e);
        }
    }

    public static void save(YamlConfiguration config, File file) {
        Path temporary = null;
        try {
            Path target = file.toPath();
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), file.getName(), ".tmp");
            Files.writeString(temporary, config.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException(Messages.text("errors.messages-save", "file", file), e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* preserva a falha original */ }
            }
        }
    }

    /** Não permite que inserir uma chave apague um valor escalar em um ancestral. */
    public static void ensureSectionParents(ConfigurationSection config, String key) {
        for (int dot = key.indexOf('.'); dot >= 0; dot = key.indexOf('.', dot + 1)) {
            String parent = key.substring(0, dot);
            if (config.isSet(parent) && !config.isConfigurationSection(parent)) {
                throw new IllegalStateException("Seção YAML esperada em " + parent);
            }
        }
    }

    public static void validateTextTypes(ConfigurationSection config, ConfigurationSection defaults, String file) {
        for (String key : defaults.getKeys(true)) {
            ensureSectionParents(config, key);
            if (!config.isSet(key)) continue;
            Object expected = defaults.get(key);
            Object value = config.get(key);
            boolean valid = expected instanceof ConfigurationSection ? value instanceof ConfigurationSection
                    : expected instanceof String ? value instanceof String
                    : expected instanceof List<?> ? value instanceof List<?> list && list.stream().allMatch(String.class::isInstance)
                    : true;
            if (!valid) throw new IllegalStateException("Tipo de valor inválido em " + file + ": " + key);
        }
    }

    /** Copia as chaves ausentes dos padrões, preservando tudo que o servidor já definiu. */
    public static boolean mergeDefaults(ConfigurationSection config, ConfigurationSection defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            Object value = defaults.get(key);
            if (value != null && !defaults.isConfigurationSection(key) && !config.isSet(key)) {
                ensureSectionParents(config, key);
                config.set(key, value);
                changed = true;
            }
        }
        return changed;
    }

    /** Remove seções que ficaram vazias depois de uma migração. */
    public static void pruneEmptySections(ConfigurationSection section) {
        for (String key : List.copyOf(section.getKeys(false))) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null) continue;
            pruneEmptySections(child);
            if (child.getKeys(false).isEmpty()) section.set(key, null);
        }
    }
}
