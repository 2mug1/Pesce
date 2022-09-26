package net.iamtakagi.pesce;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.iamtakagi.iroha.*;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static net.iamtakagi.pesce.Pesce.broadcast;
import static net.iamtakagi.pesce.Pesce.log;
import static net.iamtakagi.pesce.PesceConfig.CONFIG_YAML_DEST;
import static net.iamtakagi.pesce.PesceConfig.loadYamlConfig;

/**
 * ゲームのコンフィグ管理を行うクラス
 */
final class PesceGameConfig {

    // スターティング秒数
    @Getter
    private static int startingSeconds;

    // ゲーム秒数
    @Getter
    private static int ingameSeconds;

    // エンドゲーム秒数
    @Getter
    private static int endgameSeconds;

    /**
     * 初期化
     */
    public static void init() {
        YamlConfiguration yaml = loadYamlConfig();
        // スターティング秒数を読み込む
        startingSeconds = yaml.getInt("game.starting_seconds");
        Pesce.getInstance().getLogger().info("game.starting_seconds を読み込みました: " + startingSeconds);
        // ゲーム秒数を読み込む
        ingameSeconds = yaml.getInt("game.ingame_seconds");
        Pesce.getInstance().getLogger().info("game.ingame_seconds を読み込みました: " + ingameSeconds);
        // エンドゲーム秒数を読み込む
        endgameSeconds = yaml.getInt("game.endgame_seconds");
        Pesce.getInstance().getLogger().info("game.endgame_seconds を読み込みました: " + endgameSeconds);
    }
}

/**
 * ゲーム関連の処理を行うクラス
 */
class PesceGame {

    @Getter
    private List<GamePlayer> players; // ゲームに参加しているプレイヤー

    @Getter
    @Setter
    private GameState state; // ゲーム状態

    @Getter
    private GameProcess process; // ゲームプロセス

    @Getter
    @Setter
    private PesceStage stage;

    public PesceGame() {
        // 状態初期化
        state = GameState.INACTIVE;
        // ゲームプロセス初期化
        process = new GameProcess();
        // プレイヤーリスト初期化
     players = new LinkedList<>();
    }

    public PesceGame init() {
        // イベントリスナー登録
        Bukkit.getPluginManager().registerEvents(new GameListener(), Pesce.getInstance());
        return this;
    }

    /**
     * ゲーム状態の定義
     */
    enum GameState {
        INACTIVE,
        STARTING,
        INGAME,
        ENDGAME
    }

    /**
     * ゲームプレイヤー
     */
    @Data
    static class GamePlayer {
        // プレイヤーの UUID
        private UUID uuid;

        // フリーズフラグ
        private boolean isAllowMovement = true;

        // 釣った魚の数
        private int catches = 0;

        GamePlayer(UUID uuid) {
            this.uuid = uuid;
        }

        void incrementCatches() {
            catches++;
        }
    }

    /**
     * ゲームプロセス
     * 実際にゲームを動かすためのタスク処理など
     */
    @Getter
    class GameProcess {
        private BukkitTask task; // インスタンス

        private Runnable runnable = new Runnable() {
            @Override
            public void run() {
                onTicks();
            }
        };

        private Cooldown cooldown;

        private int seconds = 0; // 残り秒数

        private long startedAt;

        /**
         * 20 ticks (1秒) ごとに呼び出される処理
         */
        public void onTicks() {
            // 終了時
            if (seconds <= 0) {
                onEndState();
                return;
            }
            // 秒数経過
            seconds--;
            // 途中経過の処理を実行
            onSeconds();
        }

        /**
         * ゲームタスクを開始します
         */
        public void handleStart() {
            // 状態開始時の処理を実行
            onStartState();
            // 開始時の時間をセット
            startedAt = System.currentTimeMillis();
            cooldown = new Cooldown(seconds * 1000L);
            // タスクを動かす
            this.task = Bukkit.getScheduler().runTaskTimer(Pesce.getInstance(), runnable, 0L, 20L);
        }

        /**
         * ゲームタスクを停止します
         */
        public void handleStop() {
            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
        }

        /**
         * 状態開始時の処理
         */
        public void onStartState() {
            if (state == GameState.STARTING) {
                // 秒数セット
                seconds = PesceGameConfig.getStartingSeconds();
                // 全プレイヤーに知らせる
                broadcast("ゲームを開始しています...");
                return;
            }
            if (state == GameState.INGAME) {
                seconds = PesceGameConfig.getIngameSeconds();
                broadcast("ゲーム開始！");
                // フリーズ解除
                players.forEach(gamePlayer -> gamePlayer.setAllowMovement(true));
                // プレイヤーリセット & 釣り竿配布
                Bukkit.getOnlinePlayers().forEach(player -> {
                    PlayerUtil.reset(player);
                    player.getInventory().setItem(0, PesceGameItems.UNBREAKABLE_FISHING_ROD);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 3F);
                });
                return;
            }
            if(state == GameState.ENDGAME) {
                seconds = PesceGameConfig.getEndgameSeconds();
                broadcast("ゲーム終了！");
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
                });
            }
        }

        /**
         * 途中経過の処理
         */
        public void onSeconds() {
            // スターティング途中の処理
            if (state == GameState.STARTING) {
                broadcast(String.valueOf(seconds));
                Bukkit.getOnlinePlayers()
                        .forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 2F));
            }
        }

        /**
         * 状態終了時の処理
         */
        public void onEndState() {
            // スターティング終了時の処理
            if (state == GameState.STARTING) {
                // プロセスを止める
                handleStop();
                // 状態変更
                state = GameState.INGAME;
                // プロセスを開始する
                handleStart();
                return;
            }
            if (state == GameState.INGAME) {
                // プロセスを止める
                handleStop();
                // 状態変更
                state = GameState.ENDGAME;
                // プロセスを開始する
                handleStart();
                return;
            }
            if (state == GameState.ENDGAME) {
                // 全ゲームプレイヤー削除
                players.clear();
                // プロセスを止める
                handleStop();
                // 状態変更
                state = GameState.INACTIVE;
                // ロビーに回す
                PesceLobby lobby =  Pesce.getInstance().getLobby();
                // 全ロビープレイヤー削除
                lobby.getPlayers().clear();
                Bukkit.getOnlinePlayers().forEach(player -> {
                    // ロビーのスポーン座標に転送する
                    player.teleport(PesceLobbyConfig.getSpawn());
                    // プレイヤーリセット
                    PlayerUtil.reset(player);
                    // プレイヤーインベントリ取得
                    PlayerInventory inventory = player.getInventory();
                    // クリックアイテムを渡す
                    inventory.setItem(4, PesceLobby.LobbyClickItem.STAGE_VOTE_ITEM);
                    // ロビープレイヤー追加
                    lobby.getPlayers().add(new PesceLobby.LobbyPlayer(player.getUniqueId()));
                });
                // とりあえず待機状態にする
                Pesce.getInstance().getLobby().setState(PesceLobby.LobbyState.WAITING);
                // 人数が揃っていたら投票を開始する
                if (PesceLobbyConfig.getStartPlayersSize() <= Bukkit.getOnlinePlayers().size()) {
                    // 投票管理の処理 ランダムにステージ候補を選ぶ
                    lobby.getStageVoteManger().handleCollectRandomStages();
                    // ロビープロセスを起動
                    lobby.setState(PesceLobby.LobbyState.COUNTDOWN);
                    lobby.getProcess().handleStart();
                }
            }
        }
    }

    /**
     * ゲームプレイヤーの挙動を処理するイベントリスナークラス
     */
    class GameListener implements Listener {

        /**
         * ゲーム進行中にプレイヤーがサーバーに参加したとき
         *
         * @param event
         */
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            if (state != GameState.INACTIVE) {
                final Player player = event.getPlayer();
                // ステージのスポーン座標に転送する
                player.teleport(stage.getSpawn());
                // プレイヤーリセット
                PlayerUtil.reset(player);
                // プレイヤーインベントリ取得
                PlayerInventory inventory = player.getInventory();
                // クリックアイテムを渡す
                inventory.setItem(0, PesceGameItems.UNBREAKABLE_FISHING_ROD);
                if(state != GameState.ENDGAME) {
                    // ゲームプレイヤー追加
                    players.add(new GamePlayer(player.getUniqueId()));
                }
            }
        }

        /**
         * ゲーム進行中にプレイヤーがサーバーから退出したとき
         *
         * @param event
         */
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            if (state != GameState.INACTIVE) {
                final Player player = event.getPlayer();
                final UUID uuid = player.getUniqueId();
                // ゲームプレイヤー削除
                players.stream().filter(other -> other.uuid == uuid).findFirst().ifPresent(gamePlayer -> players.remove(gamePlayer));
                // 開始人数より少なくなったらタスクを停止する
                if (players.size() < PesceLobbyConfig.getStartPlayersSize()) {
                    state = GameState.INACTIVE;
                    process.handleStop();
                    // ロビーに回す
                    Pesce.getInstance().getLobby().setState(PesceLobby.LobbyState.WAITING);
                }
            }
        }

        /**
         * 釣り竿で何かを釣り上げたとき
         *
         * @param event
         */
        @EventHandler
        public void onFishCaught(PlayerFishEvent event) {
            if (state != GameState.INACTIVE) {
                // インゲームの時
                if (state == GameState.INGAME) {
                    final Player player = event.getPlayer();
                    final UUID uuid = player.getUniqueId();
                    if (players.stream().anyMatch(other -> other.uuid == uuid)) {
                        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                            if (event.getCaught() != null) {
                                if (event.getCaught() instanceof Entity) {
                                    Entity entity = (Entity) event.getCaught();
                                    players.stream().filter(other -> other.getUuid() == uuid)
                                            .findFirst().ifPresent(gamePlayer -> {
                                                // 昇順ソート
                                                players.sort((o1, o2) -> o1.getCatches() > o2.getCatches() ? -1 : 1);
                                                // どのプレイヤーが何を釣ったかを全プレイヤーに教える
                                                broadcast(Style.WHITE + player.getName() + Style.GRAY + " が " + Style.WHITE + entity.getName() + Style.GRAY + " を釣り上げた！さかな～！");
                                                // プラス
                                                gamePlayer.incrementCatches();
                                                /* 釣り上げた時にサウンドやエフェクトを発生させることで快感を得ることによって中毒性が上がりそう！ */
                                                // レベルアップ音を鳴らしてみる
                                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
                                            });
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * ゲーム中にアイテムをドロップしたとき
         *
         * @param event
         */
        @EventHandler
        public void onDrop(PlayerDropItemEvent event) {
            if (state != GameState.INACTIVE) {
                // クリエイティブ以外の時はキャンセル
                if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    event.setCancelled(true);
                }
            }
        }

        /**
         * ゲーム中にインベントリをクリックしたとき
         *
         * @param event
         */
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (state != GameState.INACTIVE) {
                if (event.getWhoClicked() instanceof Player) {
                    final Player player = (Player) event.getWhoClicked();

                    final Inventory clicked = event.getClickedInventory();
                    if (clicked == null)
                        return;

                    // 作業台だけ禁止
                    if (event.getClickedInventory() instanceof CraftingInventory) {
                        if (player.getGameMode() != GameMode.CREATIVE) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }

        /**
         * ゲーム中にダメージを受けたとき
         *
         * @param event
         */
        @EventHandler
        public void onDamage(EntityDamageEvent event) {
            if (state != GameState.INACTIVE) {
                event.setCancelled(true);
            }
        }

        /**
         * ゲーム中にプレイヤーが動いたとき
         * 
         * @param event
         */
        @EventHandler
        public void onMove(PlayerMoveEvent event) {
            if (state != GameState.INACTIVE) {
                final Player player = event.getPlayer();
                final UUID uuid = player.getUniqueId();
                final GamePlayer gamePlayer = players.stream().filter(other -> other.uuid == uuid).findFirst()
                        .orElse(null);
                if (gamePlayer == null)
                    return;
                // フリーズ処理
                if (!gamePlayer.isAllowMovement()) {
                    Location from = event.getFrom();
                    double xfrom = event.getFrom().getX();
                    double yfrom = event.getFrom().getY();
                    double zfrom = event.getFrom().getZ();
                    double xto = event.getTo().getX();
                    double yto = event.getTo().getY();
                    double zto = event.getTo().getZ();
                    if (!(xfrom == xto && yfrom == yto && zfrom == zto)) {
                        player.teleport(from);
                    }
                }
            }
        }
    }

    class PesceGameItems {
        static final ItemStack UNBREAKABLE_FISHING_ROD;

        static {
            UNBREAKABLE_FISHING_ROD = new ItemBuilder(Material.FISHING_ROD).build();
            ItemMeta itemMeta = UNBREAKABLE_FISHING_ROD.getItemMeta();
            itemMeta.setUnbreakable(true);
            UNBREAKABLE_FISHING_ROD.setItemMeta(itemMeta);
        }
    }
}
