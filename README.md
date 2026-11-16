# Misskey Sharer

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
| 開発環境 | Android Studio Koala Feature Drop (2024.1.2) 以降、JDK 17 |
| Misskey | v12.75以上（MiAuth対応インスタンス） |

## ビルド手順

### Android Studio

1. Android Studioで `File > Open` からこのフォルダ（`MisskeySharer`）を開きます。
2. Gradle Syncが完了するまで待ちます（初回はGradleと依存関係をダウンロードします）。
3. 実機またはエミュレータを選んで `Run` でインストールします。

### コマンドライン

#### デバッグビルド

```bash
./gradlew assembleDebug
```

`app/build/outputs/apk/debug/app-debug.apk` に生成されます。

#### リリースビルド

```bash
./gradlew assembleRelease
```

`app/build/outputs/apk/release/app-release-unsigned.apk` に生成されます。

## 使い方

1. **初回ログイン**: アプリを起動 → インスタンスのURLを入力（例: `https://misskey.io`）→「ブラウザで認証する」→ ブラウザでMisskeyにログインして「許可する」。
   承認後は自動でアプリに戻ります。戻らない場合は設定画面の「認証状況を確認」を押してください。
2. **共有から投稿**: 任意のアプリでテキストを選択 → 「共有」→「Misskeyに投稿」を選択。
3. **確認画面**: 内容を確認・編集し、公開範囲を選んで「投稿する」。「キャンセル」で破棄して元のアプリに戻ります。

## しくみ（実装メモ）

| ファイル | 役割 |
|---|---|
| `ShareActivity.kt` | `ACTION_SEND`（text/plain）の受信と投稿確認画面（Compose） |
| `MainActivity.kt` | インスタンス連携・デフォルト公開範囲などの設定画面 |
| `MiAuthCallbackActivity.kt` | `misskeysharer://miauth` ディープリンクで認証結果を受信 |
| `MisskeyClient.kt` | MiAuth URL生成・`/api/miauth/<session>/check`・`/api/notes/create` 呼び出し（OkHttp） |
| `Prefs.kt` | トークンや設定の保存（アプリ専用領域のSharedPreferences） |
| `ui/Theme.kt` | テーマ定義。Android 12+ ではMaterial You（壁紙由来のダイナミックカラー）、それ以下ではMisskey風グリーンのカスタムテーマを適用 |

- **認証フロー**: UUIDセッションを生成 → `https://<instance>/miauth/<uuid>?name=...&permission=write:notes&callback=...` をブラウザで開く → 承認後にコールバックへリダイレクト → `POST /api/miauth/<session>/check` でトークン取得・保存。
- **投稿**: `POST /api/notes/create` に `{"i": "<token>", "text": "...", "visibility": "home", "localOnly": true}` を送信。
- **トークンの保存先**: アプリ専用ストレージ（他アプリから参照不可）。

## カスタマイズ

- **httpのセルフホストインスタンスに対応させる場合**: `AndroidManifest.xml` の `<application>` に `android:usesCleartextTraffic="true"` を追加（通信が平文になる点に注意）。

## 制限と拡張アイデア

- 対応する共有はテキスト（`text/plain`）のみ。画像添付や `ACTION_SEND_MULTIPLE` は未対応。
- 共有シート上部に候補を出すDirectShare（Sharing Shortcuts）は未実装。
- 文字数上限はMisskeyの標準実装で3000文字（Unicodeコードポイント）に固定されています。超過した場合はAPIのエラーをToastで表示します。
- 拡張例: 複数アカウント切替、CW（注釈）入力、投稿履歴、投稿に失敗した下書きのキュー保存、DataStoreへの移行。

## トラブルシューティング

- **ブラウザから戻ってこない**: インストールされていない古いブラウザや、リダイレクトをブロックする環境が原因のことがあります。設定画面の「認証状況を確認」で手動チェックできます。
- **「まだ認証が完了していません」**: ブラウザ側で「許可する」を押す前に確認ボタンを押した場合に表示されます。
- **投稿エラー（HTTP 400等）**: 文字数上限や空文字投稿の可能性があります。確認画面の文字数表示をチェックしてください。
- **トークンを失効させたい**: Misskeyの設定 → アプリ連携 から削除できます。アプリ側は「連携を解除」でクリアされます。
