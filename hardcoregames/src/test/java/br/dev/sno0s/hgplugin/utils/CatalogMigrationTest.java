package br.dev.sno0s.hgplugin.utils;

import br.dev.sno0s.hgplugin.items.KitSelector;
import br.dev.sno0s.hgplugin.kits.KangarooKit;
import br.dev.sno0s.hgplugin.kits.LumberjackKit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Separação de messages.yml em items.yml e menus.yml. */
class CatalogMigrationTest {
    private static final List<String> CATALOG = List.of("messages.yml", "items.yml", "menus.yml");

    private JavaPlugin plugin;
    private File folder;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        folder = plugin.getDataFolder();
        Messages.load(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Evita que os textos deste teste afetem os outros testes do plugin.
        for (String name : CATALOG) Files.writeString(new File(folder, name).toPath(), "{}\n");
        Messages.load(plugin);
        MockBukkit.unmock();
    }

    /** Estado de quem atualiza o plugin: só existe o messages.yml antigo. */
    private void writeLegacyCatalog(String content) throws Exception {
        for (String name : CATALOG) Files.deleteIfExists(new File(folder, name).toPath());
        for (File backup : backups()) Files.delete(backup.toPath());
        Files.writeString(new File(folder, "messages.yml").toPath(), content);
    }

    private YamlConfiguration read(String name) {
        return YamlConfiguration.loadConfiguration(new File(folder, name));
    }

    private List<File> backups() {
        File[] found = folder.listFiles((dir, name) -> name.endsWith(".bak"));
        return found == null ? List.of() : List.of(found);
    }

    @Test
    void movesCustomTextsToTheirOwnFilesAndKeepsTheRestInMessages() throws Exception {
        String legacy = """
                server-name: "&bMeu HG"
                items:
                  cannot-drop: "Solte não!"
                  kit-selector:
                    name: "&dEscolha sua classe"
                    lore:
                      - "&7Descrição própria"
                  rocket:
                    name: "&6Canguru saltador"
                kits:
                  kangaroo:
                    description: "Salte alto!"
                    cooldown: "Espere {seconds}s."
                  lumberjack:
                    name: "&2Lenhador"
                menus:
                  kits:
                    title: "Meu menu de kits"
                feast:
                  countdown: "Feast em {seconds}s!"
                console:
                  feast:
                    spawned: "Feast pronto"
                  plugin:
                    enabled: "Subiu!"
                """;
        writeLegacyCatalog(legacy);
        Messages.load(plugin);

        YamlConfiguration items = read("items.yml");
        assertEquals("&dEscolha sua classe", items.getString("items.kit-selector.name"));
        assertEquals(List.of("&7Descrição própria"), items.getStringList("items.kit-selector.lore"));
        // O nome do foguete e a descrição do kit passam a ser o bloco do Kangaroo.
        assertEquals("&6Canguru saltador", items.getString("items.kangaroo.name"));
        assertEquals("Salte alto!", items.getString("items.kangaroo.description"));
        assertEquals("&2Lenhador", items.getString("items.lumberjack.name"));

        assertEquals("Meu menu de kits", read("menus.yml").getString("menus.kits.title"));
        YamlConfiguration feast = read("messages.yml");
        assertEquals("Feast em {seconds}s!", feast.getString("feast.countdown"));
        assertEquals("Feast pronto", feast.getString("console.feast.spawned"));

        YamlConfiguration messages = read("messages.yml");
        assertEquals("&bMeu HG", messages.getString("server-name"));
        assertEquals("Solte não!", messages.getString("common.cannot-drop"));
        assertEquals("Espere {seconds}s.", messages.getString("kits.kangaroo.cooldown"));
        assertEquals("Subiu!", messages.getString("console.plugin.enabled"));
        // As seções migradas saem do messages.yml, inclusive as que ficaram vazias.
        assertFalse(messages.isSet("items"));
        assertFalse(messages.isSet("menus"));
        assertTrue(messages.isSet("feast"));
        assertTrue(messages.isSet("console.feast"));
        assertFalse(messages.isSet("kits.lumberjack"));

        // Chaves ausentes do arquivo antigo continuam vindo dos padrões.
        assertEquals("&aEstatísticas", items.getString("items.stats.name"));
        assertFalse(feast.isSet("loot"), "a migração de textos não mexe no loot");
    }

    @Test
    void migrationRunsOnceAndPreservesTheOriginalFile() throws Exception {
        String legacy = """
                menus:
                  kits:
                    title: "Meu menu de kits"
                items:
                  rocket:
                    name: "&6Canguru saltador"
                """;
        writeLegacyCatalog(legacy);
        Messages.load(plugin);

        List<File> afterFirst = backups();
        assertEquals(1, afterFirst.size(), "só o messages.yml existia para copiar");
        assertEquals(legacy, Files.readString(afterFirst.getFirst().toPath()));
        String items = Files.readString(new File(folder, "items.yml").toPath());

        Messages.load(plugin);
        assertEquals(afterFirst.size(), backups().size(), "nada a migrar, nada a copiar");
        assertEquals(items, Files.readString(new File(folder, "items.yml").toPath()));
        assertEquals("&6Canguru saltador", read("items.yml").getString("items.kangaroo.name"));
        assertEquals("Meu menu de kits", read("menus.yml").getString("menus.kits.title"));
    }

    @Test
    void kitNameWinsOverTheOldRocketNameAndReachesMenuAndItem() throws Exception {
        writeLegacyCatalog("""
                items:
                  rocket:
                    name: "&6Nome do foguete"
                    lore:
                      - "&7Solta faísca"
                kits:
                  kangaroo:
                    name: "&dMestre do salto"
                  lumberjack:
                    description: "Derruba tudo"
                """);
        Messages.load(plugin);

        assertEquals("&dMestre do salto", read("items.yml").getString("items.kangaroo.name"));
        assertEquals(List.of("&7Solta faísca"), read("items.yml").getStringList("items.kangaroo.lore"));
        // Kit, ícone do menu e item entregue leem o mesmo bloco.
        var kangaroo = new KangarooKit();
        assertEquals("§dMestre do salto", kangaroo.getDisplayName());
        assertEquals("§dMestre do salto", kangaroo.getIcon().getItemMeta().getDisplayName());
        assertEquals("§dMestre do salto",
                br.dev.sno0s.hgplugin.items.Rocket.create().getItemMeta().getDisplayName());
        assertEquals(List.of("§7Solta faísca"),
                br.dev.sno0s.hgplugin.items.Rocket.create().getItemMeta().getLore());
        assertEquals("Derruba tudo", new LumberjackKit().getDescription());
        assertEquals(List.of("§7Derruba tudo"), new LumberjackKit().getIcon().getItemMeta().getLore());
    }

    @Test
    void theNewFileWinsConflictsAndTheOriginalIsBackedUp() throws Exception {
        File items = new File(folder, "items.yml");
        YamlConfiguration custom = read("items.yml");
        custom.set("items.kit-selector.name", "&aNome novo");
        custom.save(items);
        // O operador voltou a editar o arquivo antigo depois da separação.
        YamlConfiguration messages = read("messages.yml");
        messages.set("items.kit-selector.name", "&cNome antigo");
        messages.save(new File(folder, "messages.yml"));

        Messages.load(plugin);

        assertEquals("&aNome novo", read("items.yml").getString("items.kit-selector.name"));
        assertFalse(read("messages.yml").isSet("items"), "a duplicata é removida");
        assertEquals("§aNome novo", KitSelector.create().getItemMeta().getDisplayName());
    }

    @Test
    void invalidFileIsPreservedAndDoesNotReplaceTheWorkingCatalog() throws Exception {
        String title = Messages.text("menus.kits.title");
        String invalid = "menus: [sem fechar\n";
        File menus = new File(folder, "menus.yml");
        Files.writeString(menus.toPath(), invalid);

        assertThrows(IllegalStateException.class, () -> Messages.load(plugin));
        assertEquals(invalid, Files.readString(menus.toPath()));
        assertEquals(title, Messages.text("menus.kits.title"));
    }
    @Test
    void customLoreIsSharedByKitMenuAndItemsIncludingAnEmptyLore() throws Exception {
        var custom = read("items.yml");
        custom.set("items.kangaroo.name", "&dSalto");
        custom.set("items.kangaroo.lore", List.of("&aPrimeira", "&bSegunda"));
        custom.set("items.lumberjack.name", "&2Machado");
        custom.set("items.lumberjack.lore", List.of());
        custom.save(new File(folder, "items.yml"));
        Messages.load(plugin);
        var rocket = br.dev.sno0s.hgplugin.items.Rocket.create();
        var axe = br.dev.sno0s.hgplugin.items.LumberjackAxe.create();
        assertEquals(rocket.getItemMeta().getLore(), new KangarooKit().getIcon().getItemMeta().getLore());
        assertFalse(new LumberjackKit().getIcon().getItemMeta().hasLore());
        assertFalse(axe.getItemMeta().hasLore());
        assertEquals("§2Machado", axe.getItemMeta().getDisplayName());
        assertTrue(axe.getItemMeta().isUnbreakable());
        assertTrue(br.dev.sno0s.hgplugin.items.PluginItems.is(axe, "lumberjack-axe"));
        assertTrue(br.dev.sno0s.hgplugin.items.PluginItems.is(rocket, "rocket"));
    }

    @Test
    void invalidTextTypeDoesNotOverwriteAnyFileOrWorkingCatalog() throws Exception {
        String previous = Messages.text("items.kangaroo.name");
        String invalid = "items: quebrado\\n";
        File items = new File(folder, "items.yml");
        String messages = Files.readString(new File(folder, "messages.yml").toPath());
        Files.writeString(items.toPath(), invalid);
        assertThrows(IllegalStateException.class, () -> Messages.load(plugin));
        assertEquals(invalid, Files.readString(items.toPath()));
        assertEquals(messages, Files.readString(new File(folder, "messages.yml").toPath()));
        assertEquals(previous, Messages.text("items.kangaroo.name"));
    }

    @Test
    void unchangedCatalogIsNotRewrittenOnReload() throws Exception {
        File items = new File(folder, "items.yml");
        var previous = Files.getLastModifiedTime(items.toPath());
        Messages.load(plugin);
        assertEquals(previous, Files.getLastModifiedTime(items.toPath()));
    }

    @Test
    void everyRegisteredKitHasAConfigurableNameAndLore() {
        for (var kit : br.dev.sno0s.hgplugin.kits.KitRegistry.getAll()) {
            assertTrue(Messages.contains(kit.getCatalogKey() + ".name"), kit.getName());
            assertTrue(Messages.contains(kit.getCatalogKey() + ".lore"), kit.getName());
            assertFalse(kit.getDisplayName().startsWith("items."));
            assertEquals(Messages.lines(kit.getCatalogKey() + ".lore"), kit.getIcon().getItemMeta().getLore());
        }
    }

}
