# 🚀 チャットアプリ セットアップガイド

このドキュメントでは、別のPCでこのプロジェクトをクローンして開発環境をセットアップする手順を説明します。

## 📋 必要な環境

### 1. Java Development Kit (JDK)
- **バージョン**: Java 24以降
- **ダウンロード**: 
  - [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
  - [OpenJDK](https://openjdk.org/)
  - [Amazon Corretto](https://aws.amazon.com/jp/corretto/)

**インストール確認**:
```bash
java -version
```

### 2. Git
- **ダウンロード**: [Git公式サイト](https://git-scm.com/downloads)

**インストール確認**:
```bash
git --version
```

### 3. Visual Studio Code
- **ダウンロード**: [VS Code公式サイト](https://code.visualstudio.com/)

### 4. Maven（オプション）
- このプロジェクトには **Maven Wrapper** (`mvnw`, `mvnw.cmd`) が含まれているため、Mavenを個別にインストールする必要はありません
- システムにMavenをインストールしたい場合: [Maven公式サイト](https://maven.apache.org/download.cgi)

## 🔧 VS Code 拡張機能

以下の拡張機能をインストールすることを推奨します：

1. **Extension Pack for Java**
   - 拡張機能ID: `vscjava.vscode-java-pack`
   - Java開発に必要な拡張機能のパック
   - [Marketplace](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

2. **Spring Boot Extension Pack**
   - 拡張機能ID: `vmware.vscode-boot-dev-pack`
   - Spring Boot開発支援
   - [Marketplace](https://marketplace.visualstudio.com/items?itemName=vmware.vscode-boot-dev-pack)

3. **Thymeleaf**
   - 拡張機能ID: `junstyle.vscode-thymeleaf`
   - Thymeleafテンプレートのシンタックスハイライト
   - [Marketplace](https://marketplace.visualstudio.com/items?itemName=junstyle.vscode-thymeleaf)

4. **REST Client**（オプション）
   - 拡張機能ID: `humao.rest-client`
   - API テスト用
   - [Marketplace](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)

**VS Codeでの拡張機能インストール方法**:
1. VS Codeを開く
2. サイドバーの拡張機能アイコン（またはCtrl+Shift+X）をクリック
3. 検索ボックスに拡張機能IDまたは名前を入力
4. 「インストール」ボタンをクリック

## 📥 プロジェクトのクローンとセットアップ

### ステップ1: リポジトリをクローン

```bash
git clone https://github.com/SuYu0513/newchatapp.git
cd newchatapp
```

### ステップ2: VS Codeでプロジェクトを開く

```bash
code .
```

または、VS Codeから「ファイル > フォルダーを開く」でプロジェクトフォルダーを選択します。

### ステップ3: 依存関係のインストールとビルド

プロジェクトを初めて開くと、VS CodeのJava拡張機能が自動的に依存関係の解決を開始します。

手動でビルドする場合:

**Windows (PowerShell/CMD)**:
```bash
.\mvnw.cmd clean install
```

**Linux/Mac**:
```bash
./mvnw clean install
```

### ステップ4: データベースの初期化

このプロジェクトはSQLiteを使用しており、初回起動時に自動的に `chatapp.db` ファイルが作成されます。
必要に応じて `src/main/resources/data.sql` が実行され、初期データが投入されます。

## ▶️ アプリケーションの起動方法

### 方法1: VS Codeのタスクを使用

1. VS Codeで `Ctrl+Shift+P` を押してコマンドパレットを開く
2. 「Tasks: Run Task」を選択
3. 「Spring Boot Run」を選択

### 方法2: コマンドラインから起動

**Windows**:
```bash
.\mvnw.cmd spring-boot:run
```

**Linux/Mac**:
```bash
./mvnw spring-boot:run
```

### 方法3: VS CodeのSpring Boot Dashboard

Spring Boot拡張機能をインストールしている場合：
1. サイドバーの「Spring Boot Dashboard」を開く
2. アプリケーションを右クリック
3. 「Start」を選択

## 🌐 アプリケーションへのアクセス

アプリケーションが起動したら、ブラウザで以下のURLにアクセスします：

```
http://localhost:9090
```

## 📁 プロジェクト構造

```
chatapp/
├── src/
│   ├── main/
│   │   ├── java/com/example/chatapp/
│   │   │   ├── config/           # 設定クラス（Security, WebSocket等）
│   │   │   ├── controller/       # コントローラー
│   │   │   ├── entity/           # エンティティ（データモデル）
│   │   │   ├── repository/       # データアクセス層
│   │   │   └── service/          # ビジネスロジック
│   │   └── resources/
│   │       ├── static/           # 静的リソース（CSS, JS, 画像）
│   │       ├── templates/        # Thymeleafテンプレート
│   │       ├── application.properties  # アプリケーション設定
│   │       └── data.sql          # 初期データ
│   └── test/                     # テストコード
├── pom.xml                       # Maven設定
├── mvnw, mvnw.cmd               # Maven Wrapper
└── chatapp.db                    # SQLiteデータベース（自動生成）
```

## 🛠️ トラブルシューティング

### Java バージョンエラー

エラー: `Unsupported class file major version`

**解決方法**: Java 24以降がインストールされ、VS Codeで正しく設定されているか確認してください。

1. VS Codeで `Ctrl+,` を押して設定を開く
2. 「java.configuration.runtimes」で検索
3. settings.jsonで以下のように設定:
```json
"java.configuration.runtimes": [
  {
    "name": "JavaSE-24",
    "path": "C:\\Program Files\\Java\\jdk-24",
    "default": true
  }
]
```

### ポート競合エラー

エラー: `Port 9090 was already in use`

**解決方法**: 
1. 既存のアプリケーションを停止する
2. または `application.properties` でポート番号を変更:
```properties
server.port=8080
```

### Maven Wrapper の実行権限エラー（Linux/Mac）

エラー: `Permission denied`

**解決方法**:
```bash
chmod +x mvnw
```

### データベースロックエラー

エラー: `database is locked`

**解決方法**: 
1. 他のプロセスで `chatapp.db` を開いているか確認
2. アプリケーションを完全に停止してから再起動

## 🔐 認証情報

初期ユーザーやテストアカウントが `data.sql` に設定されている場合があります。
詳細は [data.sql](src/main/resources/data.sql) を確認してください。

## 📦 使用技術スタック

- **バックエンド**: Spring Boot 3.5.4
- **言語**: Java 24
- **テンプレートエンジン**: Thymeleaf
- **セキュリティ**: Spring Security
- **WebSocket**: Spring WebSocket + STOMP
- **データベース**: SQLite
- **ORM**: Spring Data JPA + Hibernate
- **ビルドツール**: Maven
- **フロントエンド**: HTML, CSS, JavaScript
- **PWA**: Service Worker, Manifest

## 📝 開発時の注意事項

1. **ホットリロード**: 
   - Java コードの変更時は、Spring Boot DevToolsがインストールされていれば自動リロードされます
   - テンプレートやCSSの変更はブラウザをリフレッシュしてください

2. **データベース**: 
   - SQLiteデータベースファイル (`chatapp.db`) は `.gitignore` で除外されています
   - 各開発者は独自のローカルデータベースを持ちます

3. **ログファイル**: 
   - `logs/` ディレクトリにログが出力されます
   - エラー発生時は `logs/error.log` を確認してください

## 🤝 貢献

バグ報告や機能提案は、GitHubのIssuesで受け付けています。

## 📞 サポート

問題が発生した場合は、以下を確認してください：
1. このSETUP.mdドキュメント
2. プロジェクトのREADME.md
3. GitHubのIssues
4. ログファイル（`logs/error.log`）

---

**Happy Coding! 🎉**
