package com.ghostchu.quickshop.addon.reremakemigrator.migratecomponent;

import com.dumptruckman.bukkit.configuration.json.JsonConfiguration;
import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.addon.reremakemigrator.Main;
import com.ghostchu.quickshop.util.ProgressMonitor;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class TranslationMigrateComponent extends AbstractMigrateComponent {

    public TranslationMigrateComponent(Main main, QuickShop hikari, org.maxgamer.quickshop.QuickShop reremake, CommandSender sender) {
        super(main, hikari, reremake, sender);
    }

    @Override
    public boolean migrate() {
        text("modules.translation.start-migrate").send();
        File reremakeOverrideFolder = new File(getReremake().getDataFolder(), "overrides");
        if (!reremakeOverrideFolder.exists()) return true;
        File[] fileTree = reremakeOverrideFolder.listFiles();
        if (fileTree == null) {
            getHikari().logger().warn("Skipping translation migrate (no file tree)");
            return true;
        }

        for (File langFolder : new ProgressMonitor<>(fileTree, (triple) -> text("modules.translation.migrate-entry", triple.getRight().getName(), triple.getLeft(), triple.getMiddle()))) {
            File langJsonFile = new File(langFolder, "messages.json");
            if (!langJsonFile.exists()) continue;
            try {
                migrateFile(langFolder, langJsonFile);
            } catch (Exception exception) {
                getHikari().logger().warn("Failed migrate lang file " + langJsonFile.getName() + ".", exception);
            }
        }
        return false;
    }

    private void migrateFile(File langFolder, File langJsonFile) throws IOException {
        File hikariOverrideFolder = new File(getHikari().getDataFolder(), "overrides");
        File targetLangFolder = new File(hikariOverrideFolder, langFolder.getName());
        if (!targetLangFolder.exists()) {
            if (!targetLangFolder.mkdirs()) {
                throw new IOException("Failed to create lang " + langFolder.getName() + " directory.");
            }
        }
        JsonConfiguration jsonConfiguration = JsonConfiguration.loadConfiguration(langJsonFile);
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        File targetLangFile = new File(hikariOverrideFolder, langJsonFile.getName().replace(".json", ".yml"));
        if (targetLangFile.exists()) {
            yamlConfiguration = YamlConfiguration.loadConfiguration(targetLangFile);
        }
        getHikari().logger().info("Migrating the Reremake language file: " + langJsonFile.getName() + "...");
        for (String key : new ProgressMonitor<>(jsonConfiguration.getKeys(true), (triple) -> text("modules.translation.copying-value", triple.getRight(), triple.getLeft(), triple.getMiddle()))) {
            if (key.equals("_comment")) continue;
            if (jsonConfiguration.isConfigurationSection(key)) continue;
            if (jsonConfiguration.isString(key)) {
                String originalValue = jsonConfiguration.getString(key);
                Component component = MineDown.parse(originalValue);
                String convertedValue = MiniMessage.miniMessage().serialize(component);
                yamlConfiguration.set(key, convertedValue);
            } else {
                yamlConfiguration.set(key, jsonConfiguration.get(key));
            }
        }
        if (!targetLangFile.exists()) {
            if (!targetLangFile.createNewFile()) {
                getHikari().logger().warn("Couldn't save converted data to file: file creation failed");
                return;
            }
        }
        yamlConfiguration.save(targetLangFile);
    }
}
