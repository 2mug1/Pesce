package net.iamtakagi.pesce;

import net.iamtakagi.iroha.LocationUtil;
import net.iamtakagi.iroha.Style;
import net.iamtakagi.kodaka.annotation.CommandMeta;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static net.iamtakagi.pesce.Pesce.send;
import static net.iamtakagi.pesce.Pesce.sendWithoutPrefix;

/**
 * セットアップに関するコマンド
 * /pesce lobby setspawn ロビーのスポーン座標を設定します
 * /pesce stage list 登録済みのステージリストを表示します
 * /pesce stage create <name> ステージを作成します
 * /pesce stage delete <name> ステージを削除します
 * /pesce stage setspawn <name> ステージのスポーン座標を設定します
 */
@CommandMeta(label = { "pesce" }, permission = "pesce.setup", subcommands = true)
public class PesceCommand {
    @CommandMeta(label = { "lobby" }, permission = "pesce.setup.lobby", subcommands = true)
    public class PesceLobbyCommand extends PesceCommand {

        @CommandMeta(label = { "setspawn" }, permission = "pesce.setup.lobby.spawn")
        public class PesceSetLobbyCommand extends PesceLobbyCommand {

            public void execute(Player player) throws IOException {
                Location location = player.getLocation();
                boolean isSuccess = PesceLobbyConfig.setSpawn(location);
                if (isSuccess) {
                    send(player, "ロビースポーンの座標設定が完了しました " + LocationUtil.serialize(location));
                } else {
                    send(player, "ロビースポーンの座標設定に失敗しました");
                }
            }
        }
    }

    @CommandMeta(label = { "stage" }, permission = "pesce.setup.stage", subcommands = true)
    public class PesceStageCommand extends PesceCommand {

        @CommandMeta(label = { "list" }, permission = "pesce.setup.stage.list", subcommands = true)
        public class PesceStageListCommand extends PesceStageCommand {

            public void execute(Player player) throws IOException {
                StringBuilder sb = new StringBuilder()
                        .append(Style.HORIZONTAL_SEPARATOR).append("\n")
                        .append(Style.AQUA + "ステージリスト").append("\n");
                PesceStageConfig.getStages().forEach(stage -> {
                    sb.append(Style.WHITE + stage.getName() + Style.GRAY + "(スポーン座標: " + LocationUtil.serialize(stage.getSpawn()) + ")").append("\n");
                });
                sb.append(Style.HORIZONTAL_SEPARATOR);
                sendWithoutPrefix(player, sb.toString());
            }
        }

        @CommandMeta(label = { "create" }, permission = "pesce.setup.stage.create", subcommands = true)
        public class PesceStageCreateCommand extends PesceStageCommand {

            public void execute(Player player, String stageName) throws IOException {
                boolean isSuccess = PesceStageConfig.create(stageName);
                if (isSuccess) {
                    send(player, "ステージ「" + stageName + "」を作成しました");
                } else {
                    send(player, "ステージ「" + stageName + "」の作成に失敗しました");
                }
            }
        }

        @CommandMeta(label = { "delete" }, permission = "pesce.setup.stage.delete")
        public class PesceStageDeleteCommand extends PesceStageCommand {

            public void execute(Player player, String stageName) throws IOException {
                boolean isSuccess = PesceStageConfig.delete(stageName);
                if (isSuccess) {
                    send(player, "ステージ「" + stageName + "」を削除しました");
                } else {
                    send(player, "ステージ「" + stageName + "」の削除に失敗しました");
                }
            }
        }

        @CommandMeta(label = { "setspawn" }, permission = "pesce.setup.stage.setspawn")
        public class PesceStageSetSpawnCommand extends PesceStageCommand {

            public void execute(Player player, String stageName) throws IOException {
                PesceStage stage = PesceStageConfig.getStages().stream()
                        .filter(other -> other.getName().equals(stageName)).findFirst().orElse(null);
                if (stage == null) {
                    send(player, "ステージ「" + stageName + "」は存在しません");
                    return;
                }
                Location location = player.getLocation();
                stage.setSpawn(location);
                boolean isSuccess = PesceStageConfig.update(stage);
                if (isSuccess) {
                    send(player, "ステージ「" + stageName + "」のスポーン座標設定が完了しました " + LocationUtil.serialize(location));
                } else {
                    send(player, "ステージ「" + stageName + "」のスポーン座標設定に失敗しました");
                }
            }
        }
    }
}
