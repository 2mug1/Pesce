package net.iamtakagi.pesce;

import lombok.*;
import net.iamtakagi.iroha.*;
import net.iamtakagi.medaka.Button;
import net.iamtakagi.medaka.Menu;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.*;

import static net.iamtakagi.pesce.Pesce.broadcast;
import static net.iamtakagi.pesce.PesceConfig.CONFIG_YAML_DEST;
import static net.iamtakagi.pesce.PesceConfig.loadYamlConfig;

/**
 * ロビーのコンフィグ管理を行うクラス
 */
final class PesceLobbyConfig {

    // ロビーのスポーン座標
    @Getter
    private static Location spawn;

    // 開始人数
    @Getter
    private static int startPlayersSize;

    // カウントダウン秒数
    @Getter
    private static int countdownSeconds;

    /**
     * 初期化
     */
    public static void init() {
        // コンフィグファイルを読み込む
        YamlConfiguration yaml = loadYamlConfig();
        // ロビーのスポーン座標を読み込む
        String spawnRaw = yaml.getString("lobby.spawn");
        spawn = LocationUtil.deserialize(spawnRaw);
        Pesce.getInstance().getLogger().info("lobby.spawn を読み込みました: " + spawnRaw);
        // 開始人数を読み込む
        startPlayersSize = yaml.getInt("lobby.start_players_size");
        Pesce.getInstance().getLogger().info("lobby.start_players_size を読み込みました: " + startPlayersSize);
        // カウントダウン秒数を読み込む
        countdownSeconds = yaml.getInt("lobby.countdown_seconds");
        Pesce.getInstance().getLogger().info("lobby.countdown_seconds を読み込みました: " + countdownSeconds);
    }

    /**
     * ロビーのスポーン座標を設定します
     * 
     * @param location
     * @return
     * @throws IOException
     */
    public static boolean setSpawn(Location location) throws IOException {
        // コンフィグファイルを読み込む
        YamlConfiguration yaml = loadYamlConfig();
        // 座標を書き込む
        yaml.set("lobby.spawn", LocationUtil.serialize(location));
        // 保存
        yaml.save(CONFIG_YAML_DEST);
        return true;
    }
}

/**
 * ロビーの処理を行うクラス
 */
class PesceLobby {

    // ロビープレイヤーたち
    @Getter
    private List<LobbyPlayer> players;

    // プロセス
    @Getter
    private LobbyProcess process;

    // 状態
    @Getter
    @Setter
    private LobbyState state;

    // ステージ投票管理
    @Getter
    private StageVoteManger stageVoteManger;

    public PesceLobby() {
        // 何もしない
    }

    /**
     * 初期化処理
     * コンストラクタ内で行うような処理ではないため、初期化関数を用意した
     * 
     * @return
     */
    public PesceLobby init() {
        // イベントリスナー登録
        Bukkit.getPluginManager().registerEvents(new LobbyListener(), Pesce.getInstance());
        // 初期状態は待機中
        state = LobbyState.WAITING;
        // プロセス初期化
        process = new LobbyProcess();
        // ステージ投票管理 初期化
        stageVoteManger = new StageVoteManger();
        // プレイヤーリスト初期化
        players = new LinkedList<>();
        // インスタンスを返す
        return this;
    }

    /**
     * ロビー状態の定義
     */
    enum LobbyState {
        INACTIVE,
        WAITING,
        COUNTDOWN,
    }

    /**
     * ロビープレイヤー
     */
    @Data
    static
    class LobbyPlayer {
        // Player UUID
        private UUID uuid;

        // ステージ投票済みか
        boolean isVoted = false;

        LobbyPlayer(UUID uuid) {
            this.uuid = uuid;
        }
    }

    /**
     * ロビープロセス
     * 実際にロビーを動かすためのタスク処理など
     */
    @Getter
    class LobbyProcess {
        private BukkitTask task; // インスタンス

        private Runnable runnable = new Runnable() {
            @Override
            public void run() {
                onTicks();
            }
        };

        private Cooldown cooldown;

        @Setter
        // 残り秒数
        private int seconds = 0;

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
         * ロビータスクを開始します
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
         * ロビータスクを停止します
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
            // カウントダウン開始時の処理
            if (state == LobbyState.COUNTDOWN) {
                // 秒数セット
                seconds = PesceLobbyConfig.getCountdownSeconds();
                // 全プレイヤーに知らせる
                broadcast("カウントダウンを開始しました");
            }
        }

        /**
         * 途中経過の処理
         */
        public void onSeconds() {
            // カウントダウン途中の処理
            if (state == LobbyState.COUNTDOWN) {
                broadcast(String.valueOf(seconds));
                // 残り秒数が5の倍数でサウンドを鳴らす
                if (seconds % 5 == 0) {
                    Bukkit.getOnlinePlayers()
                            .forEach(player -> player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f));
                }
                // 残り10秒以下でピアノのサウンド鳴らす
                if (seconds <= 10) {
                    Bukkit.getOnlinePlayers().forEach(
                            player -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1f));
                }
            }
        }

        /**
         * 状態終了時の処理
         */
        public void onEndState() {
            // カウントダウン終了時の処理
            if (state == LobbyState.COUNTDOWN) {
                // ステージ決定
                final StageVote mostStageVote = stageVoteManger.getMostStageVote();
                final PesceStage decidedStage = mostStageVote.getStage();
                // 全プレイヤーに知らせる
                broadcast("ステージが決定しました: " + decidedStage.getName() + " (投票数: " + mostStageVote.getPolls() + ")");
                // ゲーム取得
                PesceGame game = Pesce.getInstance().getGame();
                // ステージ設定
                game.setStage(decidedStage);
                Bukkit.getOnlinePlayers().forEach(player -> {
                    // 全プレイヤーをステージに転送する
                    player.teleport(decidedStage.getSpawn());
                    // 音を鳴らす
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
                    // ゲームプレイヤー生成
                    PesceGame.GamePlayer gamePlayer = new PesceGame.GamePlayer(player.getUniqueId());
                    // フリーズさせる
                    gamePlayer.setAllowMovement(false);
                    // プレイヤーリセット
                    PlayerUtil.reset(player);
                    // ゲームプレイヤー追加
                    game.getPlayers().add(gamePlayer);
                });
                // ロビープレイヤーリストを空にする
                players.clear();
                // ロビーはインアクティブ状態にする
                state = LobbyState.INACTIVE;
                // 現在のタスクを止める
                handleStop();
                // ゲームプロセスに引き渡す
                game.setState(PesceGame.GameState.STARTING);
                game.getProcess().handleStart();
            }
        }
    }

    /**
     * ステージ投票箱
     */
    @Data
    class StageVote {
        // 投票対象のステージ
        private PesceStage stage;

        // 投票数
        private int polls = 0;

        StageVote(PesceStage stage) {
            this.stage = stage;
        }

        /**
         * 投票数を1つ増やす
         */
        void incrementPolls() {
            this.polls += 1;
        }
    }

    /**
     * ステージ投票管理クラス
     */
    final class StageVoteManger {
        /**
         * ステージ投票のインスタンスリスト
         */
        @Getter
        private List<StageVote> votes = new ArrayList<>();

        /**
         * ステージ投票の候補を選びます
         */
        public void handleCollectRandomStages() {
            votes.clear();
            List<PesceStage> stages = PesceStageConfig.getStages();
            Collections.shuffle(stages);
            // 5つ以上のステージが登録されていたらランダムに5つ投票ステージリストに追加する そうでない場合は登録されているステージを全て投票ステージリストに追加する
            for (int i = 0; i < (Math.min(5, stages.size())); i++) {
                votes.add(new StageVote(stages.get(i)));
            }
        }

        /**
         * 投票数が最も多かったステージを返す
         * 
         * @return
         */
        public StageVote getMostStageVote() {
            // 昇順ソート
            votes.sort((o1, o2) -> o1.getPolls() > o2.getPolls() ? -1 : 1);
            return votes.get(0);
        }
    }

    /**
     * ステージ投票メニュー
     */
    class StageVoteMenu extends Menu {

        public StageVoteMenu(Plugin instance) {
            super(instance);
        }

        @Override
        public String getTitle(Player player) {
            return "ステージ投票";
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            for (int i = 0; i < stageVoteManger.votes.size(); i++) {
                buttons.put(i, new StageVoteButton(instance, stageVoteManger.getVotes().get(i)));
            }
            return buttons;
        }

        /**
         * ステージ投票ボタン
         */
        class StageVoteButton extends Button {
            // ステージ投票箱
            private StageVote vote;

            public StageVoteButton(Plugin instance, StageVote vote) {
                super(instance);
                this.vote = vote;
            }

            /**
             * ボタンアイテム
             * 
             * @param player
             * @return
             */
            @Override
            public ItemStack getButtonItem(Player player) {
                // 無難に地図
                return new ItemBuilder(Material.MAP).name(Style.AQUA + vote.getStage().getName())
                        .lore(Style.GRAY + "投票数: " + Style.WHITE + vote.getPolls()).build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                // ロビープレイヤーを取得
                LobbyPlayer lobbyPlayer = players.stream().filter(other -> other.getUuid() == player.getUniqueId())
                        .findFirst().orElse(null);
                // ロビープレイヤーが存在しない場合は何もしない
                if (lobbyPlayer == null)
                    return;
                // 既に投票済みなら何もしない
                if (lobbyPlayer.isVoted()) {
                    Pesce.send(player, Style.RED + "既にステージ投票済みです");
                    return;
                }
                // ロビープレイヤーの投票フラグを true にする
                lobbyPlayer.setVoted(true);
                // １票入れる
                vote.incrementPolls();
                // 投票したことを知らせる
                Pesce.send(player,
                        Style.GRAY + "ステージ: " + Style.WHITE + vote.getStage().getName() + Style.GRAY + " に投票しました");
            }
        }
    }

    /**
     * ロビーでのプレイヤー挙動を処理するイベントリスナー実装クラス
     */
    class LobbyListener implements Listener {

        /**
         * ロビーでプレイヤーがサーバーに参加したとき
         * 
         * @param event
         */
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            if (state != LobbyState.INACTIVE) {
                // プレイヤー取得
                final Player player = event.getPlayer();
                // ロビープレイヤー追加
                players.add(new LobbyPlayer(player.getUniqueId()));
                // ロビー待機状態の場合
                if (state == LobbyState.WAITING) {
                    // ロビーのスポーン座標に転送する
                    player.teleport(PesceLobbyConfig.getSpawn());
                    // プレイヤーリセット
                    PlayerUtil.reset(player);
                    // プレイヤーインベントリ取得
                    PlayerInventory inventory = player.getInventory();
                    // クリックアイテムを渡す
                    inventory.setItem(4, LobbyClickItem.STAGE_VOTE_ITEM);
                    // 開始人数より多くなったら
                    if (PesceLobbyConfig.getStartPlayersSize() <= Bukkit.getOnlinePlayers().size()) {
                        // 投票管理の処理 ランダムにステージ候補を選ぶ
                        stageVoteManger.handleCollectRandomStages();
                        // ロビープロセスを起動
                        state = LobbyState.COUNTDOWN;
                        process.handleStart();
                    }
                }
            }
        }

        /**
         * ロビーでプレイヤーがサーバーから退出したとき
         * 
         * @param event
         */
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            if (state != LobbyState.INACTIVE) {
                final Player player = event.getPlayer();
                final UUID uuid = player.getUniqueId();
                // ロビープレイヤー削除
                players.stream().filter(other -> other.uuid == uuid).findFirst()
                        .ifPresent(lobbyPlayer -> players.remove(lobbyPlayer));
                // ロビーカウントダウン状態の場合
                if (state == LobbyState.COUNTDOWN) {
                    // 開始人数より少なくなったらカウントダウンを停止する
                    if (players.size() < PesceLobbyConfig.getStartPlayersSize()) {
                        state = LobbyState.WAITING;
                        process.handleStop();
                    }
                }
            }
        }

        /**
         * ロビーでアクションアイテムをクリックしたとき
         * 
         * @param event
         */
        @EventHandler
        public void onClickItem(PlayerInteractEvent event) {
            if (state != LobbyState.INACTIVE) {
                // アクションを取得
                final Action action = event.getAction();
                // クリックしたとき
                if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR ||
                        action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR
                        || action == Action.PHYSICAL) {
                    final ItemStack item = event.getItem();
                    if (item == null)
                        return; // null だったら return
                    // アイテムメタかつディスプレイネームを保有しているアイテムスタックの場合
                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        // 特定のアイテムだったら処理する
                        if (item.equals(LobbyClickItem.STAGE_VOTE_ITEM)) {
                            // プレイヤーを取得
                            final Player player = event.getPlayer();
                            // ステージ投票メニューを開く
                            new StageVoteMenu(Pesce.getInstance()).openMenu(player);
                        }
                    }
                }
            }
        }

        /**
         * ロビーでアイテムをドロップしたとき
         * 
         * @param event
         */
        @EventHandler
        public void onDrop(PlayerDropItemEvent event) {
            if (state != LobbyState.INACTIVE) {
                // クリエイティブ以外の時はキャンセル
                if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    event.setCancelled(true);
                }
            }
        }

        /**
         * ロビーでインベントリをクリックしたとき
         * 
         * @param event
         */
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (state != LobbyState.INACTIVE) {
                if (event.getWhoClicked() instanceof Player) {
                    final Player player = (Player) event.getWhoClicked();

                    final Inventory clicked = event.getClickedInventory();
                    if (clicked == null)
                        return;

                    // 自分のインベントリ
                    if (clicked.equals(player.getInventory())) {
                        event.setCancelled(true);
                    }

                    // 作業台
                    if (event.getClickedInventory() instanceof CraftingInventory) {
                        if (player.getGameMode() != GameMode.CREATIVE) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }

        /**
         * ロビーでダメージを受けたとき
         * 
         * @param event
         */
        @EventHandler
        public void onDamage(EntityDamageEvent event) {
            if (state != LobbyState.INACTIVE) {
                event.setCancelled(true);
            }
        }
    }

    class LobbyClickItem {
        static ItemStack STAGE_VOTE_ITEM = new ItemBuilder(Material.DIAMOND).name(Style.AQUA + "ステージ投票")
                .lore(Style.GRAY + "アイテムをクリックして投票メニューを開く").build();
    }
}
