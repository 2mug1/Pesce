package net.iamtakagi.pesce;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.iamtakagi.iroha.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.iamtakagi.pesce.PesceConfig.*;

/**
 * ステージの定義
 */
@Data
@AllArgsConstructor
class PesceStage {
    private String name;
    private Location spawn;
}

/**
 * ステージのコンフィグ管理を行うクラス
 */
final class PesceStageConfig {

    @Getter
    private static List<PesceStage> stages = new ArrayList<>();

    public static void init() {
        // 全てのステージデータを読み込んでリストに格納する
        YamlConfiguration yaml = loadYamlConfig();
        // 各キー (ステージ名) を取り出す
        for (String stageName : Objects.requireNonNull(yaml.getConfigurationSection("stages")).getKeys(false)) { 
            String spawnRaw = yaml.getString("stages." + stageName + ".spawn");
            PesceStage stage = new PesceStage(stageName, LocationUtil.deserialize(spawnRaw)); // インスタンス生成
            stages.add(stage); // 格納
            Pesce.getInstance().getLogger().info("ステージデータを読み込みました: " + stageName + " (スポーン座標: " + spawnRaw + ")"); // ログ
        }
    }

    /**
     * ステージデータを作成 (ベースを書き込みます)
     * 
     * @param stageName
     * @return 作成に成功したか
     * @throws IOException
     */
    public static boolean create(String stageName) throws IOException {
        YamlConfiguration yaml = loadYamlConfig();
        if (yaml.contains("stages." + stageName))
            return false; // 既に存在するステージ名の場合 false を返却する
        // 初期値を打ち込んでおく
        yaml.set("stages." + stageName + ".spawn", "world:0:0:0:0:0"); // world:x:y:z:yaw:pitch
        // 保存
        yaml.save(CONFIG_YAML_DEST);
        // リストに追加
        stages.add(new PesceStage(stageName, new Location(Bukkit.getWorld("world"), 0, 0, 0, 0, 0)));
        return true;
    }

    /**
     * ステージデータを削除します
     * 
     * @param stageName
     * @return 削除に成功したか
     * @throws IOException
     */
    public static boolean delete(String stageName) throws IOException {
        YamlConfiguration yaml = loadYamlConfig();
        if (!yaml.contains("stages." + stageName))
            return false; // 存在しないステージ名の場合 false を返却する
        yaml.set("stages." + stageName, null);
        yaml.save(CONFIG_YAML_DEST);
        // リストから削除
        stages.stream().filter(other -> Objects.equals(other.getName(), stageName)).findFirst()
                .ifPresent(stage -> stages.remove(stage));
        return true;
    }

    /**
     * ステージデータを更新します
     * 
     * @param stage
     */
    public static boolean update(PesceStage stage) throws IOException {
        YamlConfiguration yaml = loadYamlConfig();
        final String name = stage.getName();
        final Location spawn = stage.getSpawn();
        yaml.set("stages." + name + ".spawn", LocationUtil.serialize(spawn));
        yaml.save(CONFIG_YAML_DEST);
        return true;
    }
}