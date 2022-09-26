package net.iamtakagi.pesce;

import net.iamtakagi.iroha.Style;
import net.iamtakagi.iroha.TimeUtil;
import net.iamtakagi.sudachi.Board;
import net.iamtakagi.sudachi.BoardAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * スコアボード実装クラス
 */
public class PesceBoard implements BoardAdapter {

    @Override
    public String getTitle(Player player) {
        return Style.AQUA + "さかな釣りゲーム！";
    }

    @Override
    public List<String> getScoreboard(Player player, Board board) {
        List<String> contents = new ArrayList<>();

        // 線
        contents.add(0, Style.SCOREBAORD_SEPARATOR);

        /**
         * 基本表示
         */
        contents.add(Style.GRAY + "あなた: " + Style.WHITE + player.getName());
        contents.add(Style.GRAY + "現在のプレイヤー数: " + Style.WHITE
                + Pesce.getInstance().getServer().getOnlinePlayers().size() + "名");

        /**
         * ロビーのスコアボード
         */
        final PesceLobby lobby = Pesce.getInstance().getLobby();
        final PesceLobby.LobbyState lobbyState = lobby.getState();
        final PesceLobby.LobbyProcess lobbyProcess = lobby.getProcess();
        if (lobbyState != PesceLobby.LobbyState.INACTIVE) {
            if (lobbyState == PesceLobby.LobbyState.WAITING) {
                contents.add(Style.GRAY + "ゲーム開始に必要なプレイヤー数: " + Style.WHITE + "2名");
            }
            if (lobbyState == PesceLobby.LobbyState.COUNTDOWN) {
                contents.add(
                        Style.GRAY + "カウントダウン終了まで: " + Style.WHITE + lobbyProcess.getCooldown().getTimeLeft() + "s");
                contents.add("");
                contents.add(Style.AQUA + "ステージ投票受付中...");
                lobby.getStageVoteManger().getVotes().forEach(vote -> contents
                        .add(Style.GRAY + vote.getStage().getName() + ": " + Style.WHITE + vote.getPolls()));
            }
        }

        /**
         * ゲームのスコアボード
         */
        final PesceGame game = Pesce.getInstance().getGame();
        final PesceGame.GameState gameState = game.getState();
        final PesceGame.GameProcess gameProcess = game.getProcess();
        if (gameState != PesceGame.GameState.INACTIVE) {
            if (gameState == PesceGame.GameState.STARTING) {
                contents.add(Style.GRAY + "ゲーム開始まで: " + Style.WHITE + gameProcess.getCooldown().getTimeLeft() + "s");
            }
            if (gameState == PesceGame.GameState.INGAME) {
                contents.add(Style.GRAY + "残り時間: " + Style.WHITE
                        + TimeUtil.millisToTimer(gameProcess.getCooldown().getRemaining()));
                contents.add(Style.GRAY + "ステージ: " + Style.WHITE + game.getStage().getName());
                // 自分が釣った数を表示する
                game.getPlayers().stream().filter(other -> other.getUuid() == player.getUniqueId())
                        .findFirst().ifPresent(mineGamePlayer -> contents.add(Style.GRAY + "自分が釣った数: " + Style.WHITE + mineGamePlayer.getCatches() + "匹"));
                contents.add("");
                contents.add(Style.AQUA + "現在のランキング");
                // トップ5名を表示する
                final List<PesceGame.GamePlayer> gamePlayers = game.getPlayers();
                for (int i = 0; i < (Math.min(5, gamePlayers.size())); i++) {
                    final PesceGame.GamePlayer gamePlayer = gamePlayers.get(i);
                    final Player bukkitPlayer = Bukkit.getPlayer(gamePlayer.getUuid());
                    if (bukkitPlayer != null) {
                        contents.add(Style.GRAY + bukkitPlayer.getName() + ": " + Style.WHITE + gamePlayer.getCatches()
                                + "匹");
                    }
                }
            }
        }

        // リンクを貼っておく
        contents.add("");
        contents.add(Style.WHITE + "github.com/2mug1/Pesce");
        // 線
        contents.add(Style.SCOREBAORD_SEPARATOR);
        return contents;
    }

    @Override
    public long getInterval() {
        return 2L;
    }

    @Override
    public void onScoreboardCreate(Player player, Scoreboard board) {

    }

    @Override
    public void preLoop() {

    }
}
