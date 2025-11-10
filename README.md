# チャットアプリケーション

Spring Bootを使用したリアルタイムチャットアプリケーションです。

## 機能

- ✅ ユーザー登録・ログイン
- ✅ リアルタイムチャット（WebSocket）
- ✅ マルチルームチャット（ルーム切り替え機能）
- ✅ セキュリティ（Spring Security）
- ✅ データベース連携（SQLite）
- ✅ PWA対応（Service Worker）
- ✅ レスポンシブデザイン

## 技術スタック

- **バックエンド**: Spring Boot 3.5.4
- **認証**: Spring Security
- **WebSocket**: Spring WebSocket + STOMP
- **テンプレートエンジン**: Thymeleaf
- **データベース**: SQLite Database
- **ビルドツール**: Maven
- **Java**: 24

## セットアップ

### 前提条件
- Java 24
- Maven

### 実行方法

```bash
# プロジェクトをクローン
git clone <repository-url>
cd chatapp

# アプリケーションを起動
./mvnw spring-boot:run
```

### アクセス
- アプリケーション: http://localhost:9090
- H2コンソール: http://localhost:9090/h2-console

## 重要な修正履歴

### 2025年11月7日 - ルーム切り替え機能の修正
- **問題**: ルーム切り替え時にメッセージ履歴が表示されない問題を解決
- **原因**: ブラウザ/Service Workerキャッシュによる古いAPIレスポンスの返却
- **解決策**: 
  - 動的キャッシュクリア機能の実装
  - ルーム切り替え時の自動キャッシュ無効化
  - Service Workerキャッシュの適切な管理
  - APIリクエスト時のキャッシュバスター追加

### デバッグ機能
- 包括的なログ出力システム
- フロントエンドとバックエンドの詳細なデバッグログ
- リアルタイムでの問題追跡機能

## プロジェクト構成

```
src/
├── main/
│   ├── java/com/example/chatapp/
│   │   ├── config/          # 設定クラス
│   │   ├── controller/      # コントローラー
│   │   ├── entity/          # エンティティ
│   │   ├── repository/      # リポジトリ
│   │   ├── service/         # サービス
│   │   └── dto/             # DTO
│   └── resources/
│       ├── templates/       # Thymeleafテンプレート
│       └── application.properties
└── test/                    # テストコード
```

## 設計書

詳細な設計については `設計書.md` を参照してください。

## 開発状況

- [x] 基本的なプロジェクト構造
- [x] ユーザー認証機能
- [x] WebSocket設定
- [x] 基本的なチャット機能
- [x] マルチルームチャット機能
- [x] ルーム切り替え機能
- [x] キャッシュ問題の解決
- [x] PWA対応
- [x] デバッグシステム
- [ ] フレンド機能
- [ ] グループチャット
- [ ] ランダムマッチング

## トラブルシューティング

### キャッシュ問題
ルーム切り替え時にメッセージが表示されない場合：
1. ブラウザのハードリフレッシュ（Ctrl+Shift+R）
2. シークレットモードでの確認
3. Service Workerの更新確認

### 開発時のキャッシュ無効化
- デベロッパーツールの Network タブで「Disable cache」を有効化
- アプリケーションが自動的にキャッシュクリア機能を実行

## 作成日

2025年8月5日
