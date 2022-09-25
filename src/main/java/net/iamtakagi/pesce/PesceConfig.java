package net.iamtakagi.pesce;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * コンフィグ関連
 */
public final class PesceConfig {

    /**
     * config.yml 書き込み先のパス
     */
    public static String CONFIG_YAML_DEST = Pesce.getInstance().getDataFolder() + File.separator + "config.yml";

    /**
     * config.yml を読み込んで org.bukkit.configuration.file#YamlConfiguration を返却します
     * 
     * @return
     */
    public static YamlConfiguration loadYamlConfig() {
        return YamlConfiguration.loadConfiguration(new File(CONFIG_YAML_DEST));
    }
}
