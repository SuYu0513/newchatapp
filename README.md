# チャットアプリケーション

Spring Bootを使用したリアルタイムチャットアプリケーションです。

## 機能

- ✅ ユーザー登録・ログイン
- ✅ リアルタイムチャット（WebSocket）
- ✅ セキュリティ（Spring Security）
- ✅ データベース連携（H2）

## 技術スタック

- **バックエンド**: Spring Boot 3.5.4
- **認証**: Spring Security
- **WebSocket**: Spring WebSocket + STOMP
- **テンプレートエンジン**: Thymeleaf
- **データベース**: H2 Database
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
- アプリケーション: http://localhost:8080
- H2コンソール: http://localhost:8080/h2-console

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
- [ ] フレンド機能
- [ ] グループチャット
- [ ] ランダムマッチング

## 作成日

2025年8月5日
