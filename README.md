# Misskey Sharer

<p align="center">
    <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/ame0011/MisskeySharer">
        <img alt="Obtaniumでインストール" src="./docs/images/badge_obtainium.jxl" height="80">
    </a>
</p>

Androidの「共有」機能で受け取った文字列を、確認画面をはさんでMisskeyに投稿するミニアプリケーションです。  
Kotlin + Jetpack Composeで実装しています。

## 機能

- 他アプリの共有シート → 「Misskeyに投稿」→ 確認画面 → ノート投稿
- MiAuthによるアカウント連携（初回のみブラウザで認証、以降は自動）
- 投稿前に内容を編集可能
- 文字数表示（Misskeyと同じくUnicodeコードポイントで計数）
- 公開範囲（公開 / ホーム / フォロワー）と「ローカルのみ」を投稿ごとに選択可能
- 設定画面でデフォルトの公開範囲を保存
- ブラウザ共有時は「タイトル + URL」を自動整形して受け取り

## 動作要件

| 項目 | 要件 |
|---|---|
| 端末 | Android 8.0（API 26）以上 |
| 開発環境 | Android Studio Koala Feature Drop (2024.1.2) 以降、Temurin JDK 25 |
| Misskey | v12.75以上（MiAuth対応インスタンス） |

## 🌏️対応言語

- [x] 日本語
- [x] 英語

翻訳追加・修正など歓迎です！一部分だけでも大丈夫です  
`app/src/main/res/` 以下にリソースがあります。

## ビルド手順

### Android Studio

1. Android Studioで `File > Open` からこのフォルダ（`MisskeySharer`）を開きます。
2. Gradle Syncが完了するまで待ちます（初回はGradleと依存関係をダウンロードします）。
3. 実機またはエミュレータを選んで `Run` でインストールします。

### コマンドライン

[mise](https://mise.jdx.dev) に対応しています。  
`mise install` でJDKをインストールし、`mise run <タスク名>` でタスクを実行できます。

`mise tasks` で実行可能なタスクを一覧できます。

#### デバッグビルド

```bash
mise build
```

`app/build/outputs/apk/debug/app-debug.apk` に生成されます。

#### リリースビルド

```bash
mise release
```

`app/build/outputs/apk/release/` 配下にAPKが生成されます。

- 署名なし → `app-release-unsigned.apk`
- 署名あり(`keystore.properties` 設定済み) → `app-release.apk`

## 使い方

1. **初回ログイン**: アプリを起動 → インスタンスのURLを入力（例: `https://misskey.io`）→「ブラウザで認証する」→ ブラウザでMisskeyにログインして「許可する」。
   承認後は自動でアプリに戻ります。戻らない場合は設定画面の「認証状況を確認」を押してください。
2. **共有から投稿**: 任意のアプリでテキストを選択 → 「共有」→「Misskeyに投稿」を選択。
3. **確認画面**: 内容を確認・編集し、公開範囲を選んで「投稿する」。「キャンセル」で破棄して元のアプリに戻ります。

## 開発メモ

### ファイル構成

| ファイル | 役割 |
|---|---|
| `ShareActivity.kt` | `ACTION_SEND`（`text/plain`）を受信。`EXTRA_SUBJECT`（タイトル）と `EXTRA_TEXT`（URL）を結合して共有確認画面へ渡す |
| `MainActivity.kt` | 設定画面のエントリポイント。`MainScreen` / `AboutScreen` の遷移をNavigation Composeで管理 |
| `MiAuthCallbackActivity.kt` | `misskeysharer://miauth` のリダイレクトを受け取る透過Activity。トークンを取得・保存して設定画面を開き終了する |
| `MisskeyClient.kt` | MiAuth URL生成、`/api/miauth/<session>/check`、`/api/notes/create` の呼び出し（OkHttp）とAPIエラー解析 |
| `Prefs.kt` | アカウント・認証保留セッション・デフォルト公開範囲・外観設定の保存（`MODE_PRIVATE` のSharedPreferences）。インスタンスURLの正規化／検証も担当 |
| `Errors.kt` | 通信例外を、ローカライズ済み文言またはサーバー由来のメッセージに変換して表示用に返す |
| `ui/ShareScreen.kt` | 共有確認画面。テキスト編集、Unicodeコードポイント数表示、公開範囲／ローカルのみの選択、投稿とエラー表示 |
| `ui/MainScreen.kt` | 設定画面。アカウント連携、認証状況確認、デフォルト公開範囲、ピュアブラック切り替え |
| `ui/AboutScreen.kt` | アプリ情報・バージョン・ライセンス・OSSライセンス一覧（AboutLibraries） |
| `ui/Theme.kt` | `AppTheme`。Android 12+ はダイナミックカラー、以下はMisskey風グリーン。ピュアブラック適用とWindow背景の同期 |
| `ui/Color.kt` | ライト／ダーク・各コントラストレベルの配色定義（`ui/Theme.kt` から参照） |
| `ui/Type.kt` | `Typography` 定義（現在はMaterial 3のデフォルト） |
| `AndroidManifest.xml` | ランチャー／共有ターゲット／MiAuthコールバックの3Activityとintent-filter、`INTERNET` 権限を宣言 |
| `app/src/main/res/` | 文字列（日英）・テーマ・アイコンなどのリソース。`values` / `values-ja` で言語切り替え |
| `app/src/test/` | 単体テスト（インスタンスURL検証、MiAuthレスポンス解析、APIエラー解析、リクエスト生成） |

### 認証とトークン

- **認証フロー**: UUIDセッションを生成 → `https://<instance>/miauth/<uuid>?name=...&permission=write:notes&callback=...` をブラウザで開く → 承認後にコールバックへリダイレクト → `POST /api/miauth/<session>/check` でトークン取得・保存。
- **トークンの保存先**: アプリ専用ストレージ（他アプリから参照不可）。

### カスタマイズ

- **httpのセルフホストインスタンスに対応させる場合**: `AndroidManifest.xml` の `<application>` に `android:usesCleartextTraffic="true"` を追加（通信が平文になる点に注意）。

### 制限と拡張アイデア

- 対応する共有はテキスト（`text/plain`）のみ。画像添付や `ACTION_SEND_MULTIPLE` は未対応。
- 共有シート上部に候補を出すDirectShare（Sharing Shortcuts）は未実装。
- 文字数上限はMisskeyの標準実装で3000文字（Unicodeコードポイント）に固定されています。超過した場合はAPIのエラーをToastで表示します。
- 拡張例: 複数アカウント切替、CW（注釈）入力、投稿履歴、投稿に失敗した下書きのキュー保存、DataStoreへの移行。

## トラブルシューティング

- **ブラウザから戻ってこない**: インストールされていない古いブラウザや、リダイレクトをブロックする環境が原因のことがあります。設定画面の「認証状況を確認」で手動チェックできます。
- **「まだ認証が完了していません」**: ブラウザ側で「許可する」を押す前に確認ボタンを押した場合に表示されます。
- **投稿エラー（HTTP 400等）**: 文字数上限の可能性があります。確認画面の文字数表示をチェックしてください。
- **トークンを失効させたい**: Misskeyの設定 → アプリ連携 から削除できます。アプリ側は「連携を解除」でクリアされます。
