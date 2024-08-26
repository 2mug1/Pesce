package net.iamtakagi.pesce;

import lombok.Getter;
import lombok.Setter;
import net.iamtakagi.iroha.Style;
import net.iamtakagi.kodaka.Kodaka;
import net.iamtakagi.medaka.Medaka;
import net.iamtakagi.sudachi.Sudachi;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
public class Pesce extends JavaPlugin {

    // プラグインのインスタンス
    @Getter
    private static Pesce instance;

    // ロビー
    @Getter
    @Setter
    private PesceLobby lobby;

    // ゲーム
    @Getter
    @Setter
    private PesceGame game;

    /**
     * プラグインが有効化されたとき
     */
    @Override
    public void onEnable() {
        // インスタンス宣言
        instance = this;

        // コンフィグファイルを読み込む
        this.saveDefaultConfig();

        // 🐠 Medaka (メニュー) 初期化
        Medaka.init(this);

        // 🍋 Sudachi (スコアボード) 初期化
        Sudachi.init(this, new PesceBoard());

        // 🦅 Kodaka (コマンド) 初期化
        new Kodaka(this).registerCommand(new PesceCommand());

        // ステージコンフィグ初期化
        PesceStageConfig.init();

        // ロビーコンフィグ初期化
        PesceLobbyConfig.init();

        // ゲームコンフィグ初期化
        PesceGameConfig.init();

        // ロビー初期化
        lobby = new PesceLobby().init();

        // ゲーム初期化
        game = new PesceGame().init();
    }

    /**
     * ログを出力します
     * 
     * @param level
     * @param text
     */
    public static void log(Level level, String text) {
        instance.getLogger().log(level, text);
    }

    /**
     * プラグインが無効化されたとき
     */
    @Override
    public void onDisable() {
    }

    // プラグインのプレフィックス
    private static final String PLUGIN_PREFIX = Style.AQUA + "[Pesce]";

    /**
     * プレイヤーにプラグインのプレフィックス付きテキストを送信します
     * 
     * @param player
     * @param text
     */
    public static void sendWithPrefix(Player player, String text) {
        player.sendMessage(PLUGIN_PREFIX + " " + Style.RESET + text);
    }

    /**
     * プレイヤーにテキストを送信します
     *
     * @param player
     * @param text
     */
    public static void sendWithoutPrefix(Player player, String text) {
        player.sendMessage(Style.RESET + text);
    }

    /**
     * サーバーにいる全プレイヤーにプラグインのプレフィックス付きテキストを送信します
     * 
     * @param text
     */
    public static void broadcastWithPrefix(String text) {
        instance.getServer().getOnlinePlayers().forEach(player -> sendWithPrefix(player, text));
    }

    /**
     * サーバーにいる全プレイヤーにテキストを送信します
     *
     * @param text
     */
    public static void broadcastWithoutPrefix(String text) {
        instance.getServer().getOnlinePlayers().forEach(player -> sendWithoutPrefix(player, text));
    }
}
