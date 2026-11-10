# AGENTS.md

本リポジトリでコミットを行う際は、以下のルールに従ってください。

## コミットメッセージ

- Conventional Commits に準拠した形式を使用する。
  - 形式: `<type>(<scope>): <subject>`（scope は任意）
  - 例: `feat(post): ポストの共有機能を追加` / `fix(network): タイムアウト処理を修正`
- type は Conventional Commits の標準タイプを使用する:
  - `feat`: 新機能
  - `fix`: バグ修正
  - `refactor`: リファクタリング
  - `perf`: パフォーマンス改善
  - `docs`: ドキュメント変更
  - `test`: テスト追加・修正
  - `chore`: ビルド・設定などその他
- BREAKING CHANGE を含める場合は、footer に `BREAKING CHANGE: ...` を記載する。

## コミット単位

- 1コミット = 1論理変更にする。
- 無関係な変更（例: バグ修正とリファクタリングの混在）を同一コミットにまとめない。
- 論理が複数ある場合は、コミットを分割して個別に記録する。
