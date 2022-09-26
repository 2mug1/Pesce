package net.iamtakagi.pesce;

import lombok.Getter;
import lombok.Setter;
import net.iamtakagi.iroha.Style;
import net.iamtakagi.kodaka.Kodaka;
import net.iamtakagi.medaka.Medaka;
import net.iamtakagi.sudachi.Sudachi;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * さかな～～～！！！🐟
 * ちなみに Pesce はイタリア語で "魚" という意味です
 * プラグイン構成の主役となるクラス
 */
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
     * プレイヤーにテキストを送信します
     * 
     * @param player
     * @param text
     */
    public static void send(Player player, String text) {
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
     * サーバーにいる全プレイヤーにテキストを送信します
     * 
     * @param text
     */
    public static void broadcast(String text) {
        instance.getServer().getOnlinePlayers().forEach(player -> send(player, text));
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