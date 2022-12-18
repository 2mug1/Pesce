# Pesce

## これはなに
[PaperMC/Paper](https://github.com/PaperMC/Paper) 対応のサーバプラグイン\
体たらくで退屈な魚釣りを多人数かつゲーム形式でワイワイのんびり楽しく遊べるようにするゲームプラグインです\
初見の方からしてみれば「完成度すごい！これだけで十分！」となるかもしれませんが（？）今後も様々な機能を実装していく予定です。本音を言うと某アニメに影響されて作りたくなってしまった！！！！！（笑）というのはさておき... 完全に自己満足プラグインなので、身内で楽しんでもらう目的で開発しています

![2022-09-26_06 00 28](https://user-images.githubusercontent.com/46530214/192165770-782e9ad3-d48f-4cf2-8653-5364905a3a7a.png)
![2022-09-26_06 00 33](https://user-images.githubusercontent.com/46530214/192165776-d6737594-6d84-4411-af66-3f3d85853281.png)
![2022-09-26_06 00 42](https://user-images.githubusercontent.com/46530214/192165785-0cc99996-bc2a-4644-affb-e6ecd576ffef.png)
![2022-09-26_06 01 08](https://user-images.githubusercontent.com/46530214/192165745-d13d4afe-39e4-42a9-a931-b61028aba90b.png)

## Getting Started

### インストール
- [Releases](https://github.com/2mug1/Pesce/releases) から Jar ファイルをダウンロードし、 `plugins` フォルダに入れます
- サーバ起動時、コンソール出力に異常がないことを確認してください

### コマンド
| ラベル | 説明 |
| ---- | ---- |
| /pesce lobby setspawn | ロビーのスポーン座標を設定します |
| /pesce stage list | 登録済みのステージリストを表示します |
| /pesce stage create ＜ステージ名＞ | ステージを作成します |
| /pesce stage delete ＜ステージ名＞ | ステージを削除します |
| /pesce stage setspawn ＜ステージ名＞ | ステージのスポーン座標を設定します |

### 設定ファイル
`config.yml`
```yml
# ロビーに関する設定
lobby:
  # ロビーのスポーン座標
  spawn: world:0:0:0:0:0
  # ゲームの開始人数　(デフォルト: 1)
  start_players_size: 1
  # カウントダウンの秒数 (デフォルト: 60)
  countdown_seconds: 60

# ゲームに関する設定
game:
  # ゲーム開始までの秒数 (デフォルト: 5)
  starting_seconds: 5
  # ゲーム時間の秒数 (デフォルト: 600)
  ingame_seconds: 600
  # ゲーム終了時の秒数 (デフォルト: 5)
  endgame_seconds: 5

# 登録済みのステージデータ
stages:
  example:
    spawn: world:0:0:0:0:0
```

## 技術スタック
- Java 18
- paper-api (1.19.2-R0.1-SNAPSHOT)

### 依存関係
- [2mug1/kodaka](https://github.com/2mug1/kodaka): コマンドに関連するフレームワーク
- [2mug1/medaka](https://github.com/2mug1/medaka): メニューに関連するフレームワーク
- [2mug1/sudachi](https://github.com/2mug1/sudachi): スコアボードに関連するフレームワーク
- [2mug1/iroha](https://github.com/2mug1/iroha): 様々なユーティリティを提供するフレームワーク

## 動作確認
- `Paper 1.19.2 #166` 確認済みです

## 注意
現在は開発バージョンのみリリースしています\
全ての処理や動作が正しく行われるとは限りません

## LICENSE
MIT License (© 2022- iamtakagi)\
See [LICENSE](./LICENSE)

## LICENSE (PaperMC/Paper)
See [Paper/LICENSE.md](./Paper/LICENSE.md) or [PaperMC/Paper/blob/master/LICENSE.md](https://github.com/PaperMC/Paper/blob/master/LICENSE.md)
